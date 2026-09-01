package poc.titan.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import poc.titan.api.dto.UploadResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Endpoint for uploading the three model files needed before validating or analyzing.
 */
@RestController
@RequestMapping("/pnpl")
public class FileUploadController {

    /**
     * Saves the uploaded .vrb, .petrinets and feature model files into a temp
     * folder together, so the .vrb can resolve the other two by relative path.
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("vrb") MultipartFile vrb,
            @RequestParam("petrinets") MultipartFile petrinets,
            @RequestParam("featureModel") MultipartFile featureModel
    ) throws IOException {

        // Create a temp directory for this request
        Path tempDir = Files.createTempDirectory("titan-");

        // Save all 3 files in the same directory (vrb references the others by relative path)
        Path vrbPath = tempDir.resolve(Objects.requireNonNull(vrb.getOriginalFilename()));
        Path pnPath = tempDir.resolve(Objects.requireNonNull(petrinets.getOriginalFilename()));
        Path fmPath = tempDir.resolve(Objects.requireNonNull(featureModel.getOriginalFilename()));

        vrb.transferTo(vrbPath);
        petrinets.transferTo(pnPath);
        featureModel.transferTo(fmPath);

        System.out.println("[upload] Files saved to: " + tempDir);

        return ResponseEntity.ok(
                new UploadResponse(vrbPath.toString(), "Files uploaded successfully")
        );
    }
}