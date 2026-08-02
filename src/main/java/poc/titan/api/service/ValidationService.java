package poc.titan.api.service;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.springframework.stereotype.Service;
import poc.titan.api.analysis.XtextInitializer;
import poc.titan.api.dto.ValidationResponse;
import poc.titan.api.dto.ValidationResponse.ValidationIssue;
import PetriNets.PetriNetsPackage;
import variability.VariabilityPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ValidationService {

    private final XtextInitializer xtextInitializer;

    public ValidationService(XtextInitializer xtextInitializer) {
        this.xtextInitializer = xtextInitializer;
    }

    public ValidationResponse validate(String vrbPathStr) {
        Path vrbPath = Path.of(vrbPathStr).toAbsolutePath().normalize();

        if (!Files.exists(vrbPath)) {
            return errorResponse("File not found: " + vrbPath);
        }

        try {
            // 1. Create a fresh ResourceSet for this request (not thread-safe, so one per request)
            XtextResourceSet rs = xtextInitializer.getInjector().getInstance(XtextResourceSet.class);
            rs.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);

            // Register packages and factories in this ResourceSet
            rs.getPackageRegistry().put(PetriNetsPackage.eNS_URI, PetriNetsPackage.eINSTANCE);
            rs.getPackageRegistry().put(VariabilityPackage.eNS_URI, VariabilityPackage.eINSTANCE);
            rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("petrinets", new XMIResourceFactoryImpl());

            // 2. Pre-load the .petrinets file so cross-references resolve
            Path baseDir = vrbPath.getParent();
            String pnFileName = parsePetriNetFileName(vrbPath);

            if (pnFileName != null) {
                Path pnPath = baseDir.resolve(pnFileName).normalize();
                if (Files.exists(pnPath)) {
                    URI pnUri = URI.createFileURI(pnPath.toString());
                    rs.getResource(pnUri, true);
                }
            }

            // 3. Load the .vrb resource (Xtext parses it using the grammar)
            URI vrbUri = URI.createFileURI(vrbPath.toString());
            Resource resource = rs.getResource(vrbUri, true);

            // 4. Run all validations: syntax + cross-references + @Check methods
            IResourceValidator validator = xtextInitializer.getInjector()
                    .getInstance(IResourceValidator.class);
            List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);

            // 5. Convert to response
            List<ValidationIssue> issueList = issues.stream()
                    .map(issue -> new ValidationIssue(
                            issue.getSeverity().name(),
                            issue.getMessage(),
                            issue.getLineNumber() != null ? issue.getLineNumber() : 0,
                            issue.getColumn() != null ? issue.getColumn() : 0
                    ))
                    .toList();

            ValidationResponse response = new ValidationResponse();
            response.setValid(issueList.stream().noneMatch(i -> "ERROR".equals(i.getSeverity())));
            response.setIssues(issueList);
            return response;

        } catch (Exception e) {
            return errorResponse("Validation failed: " + e.getMessage());
        }
    }

    /**
     * Reads the .vrb file as text to extract the .petrinets filename.
     * Looks for the line: pn "filename.petrinets"
     */
    private String parsePetriNetFileName(Path vrbPath) {
        try (BufferedReader reader = Files.newBufferedReader(vrbPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("pn")) {
                    int first = line.indexOf("\"");
                    int last = line.lastIndexOf("\"");
                    if (first >= 0 && last > first) {
                        return line.substring(first + 1, last);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore - validation will catch the missing file
        }
        return null;
    }

    private ValidationResponse errorResponse(String message) {
        ValidationResponse response = new ValidationResponse();
        response.setValid(false);
        response.setIssues(List.of(new ValidationIssue("ERROR", message, 0, 0)));
        return response;
    }
}