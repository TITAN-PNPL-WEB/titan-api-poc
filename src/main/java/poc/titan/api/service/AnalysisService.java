package poc.titan.api.service;

import jakarta.annotation.Nonnull;
import org.pnpl.analysis.analyzer.AbstractAnalysis;
import org.springframework.stereotype.Service;
import poc.titan.api.analysis.AnalysisDescriptor;
import poc.titan.api.analysis.PluginScanner;
import poc.titan.api.dto.AnalysisRequest;
import poc.titan.api.dto.AnalysisResponse;

import java.nio.file.Path;
import java.util.Optional;

@Service
public class AnalysisService {

    private final PluginScanner pluginScanner;

    public AnalysisService(PluginScanner pluginScanner) {
        this.pluginScanner = pluginScanner;
    }

    public AnalysisResponse run(@Nonnull AnalysisRequest req) throws Exception {
        Optional<AnalysisDescriptor> descriptor = pluginScanner.find(req.getName(), req.getType());

        if (descriptor.isEmpty()) {
            throw new IllegalArgumentException(
                    "Analysis not found: name='" + req.getName() + "' type='" + req.getType() + "'"
            );
        }

        Class<?> clazz = pluginScanner.getPluginClassLoader().loadClass(descriptor.get().className);
        AbstractAnalysis analysis = (AbstractAnalysis) clazz.getDeclaredConstructor().newInstance();

        Path vrbPath = Path.of(req.vrbPath).toAbsolutePath().normalize();
        analysis.loadVariabilityFile(vrbPath);

        long start = System.currentTimeMillis();
        boolean result = analysis.run();
        long end = System.currentTimeMillis();

        AnalysisResponse res = new AnalysisResponse();
        res.analysis = req.getName();
        res.allProducts = result;
        res.timeMs = end - start;
        res.message = result
                ? "All products satisfy " + req.getName()
                : "Some products do not satisfy " + req.getName();
        return res;
    }
}