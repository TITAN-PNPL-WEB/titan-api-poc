package poc.titan.api.service;

import jakarta.annotation.Nonnull;
import org.pnpl.analysis.analyzer.AbstractAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import poc.titan.api.analysis.AnalysisDescriptor;
import poc.titan.api.analysis.PluginScanner;
import poc.titan.api.dto.AnalysisRequest;
import poc.titan.api.dto.AnalysisResponse;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads the requested analysis plugin and runs it against a model.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final PluginScanner pluginScanner;

    public AnalysisService(PluginScanner pluginScanner) {
        this.pluginScanner = pluginScanner;
    }

    /**
     * Finds the analysis by name and type, loads its class, runs it on the
     * given model, and times how long it took.
     */
    public AnalysisResponse run(@Nonnull AnalysisRequest req) throws Exception {
        Optional<AnalysisDescriptor> descriptor = pluginScanner.find(req.getName(), req.getType());

        if (descriptor.isEmpty()) {
            log.warn("Analysis not found: name='{}' type='{}'", req.getName(), req.getType());
            throw new IllegalArgumentException(
                    "Analysis not found: name='" + req.getName() + "' type='" + req.getType() + "'"
            );
        }

        Path vrbPath = Path.of(req.vrbPath).toAbsolutePath().normalize();
        log.info("Running {} on {}", descriptor.get(), vrbPath.getFileName());

        boolean result;
        long start, end;
        try {
            Class<?> clazz = pluginScanner.getPluginClassLoader().loadClass(descriptor.get().className);
            AbstractAnalysis analysis = (AbstractAnalysis) clazz.getDeclaredConstructor().newInstance();
            analysis.loadVariabilityFile(vrbPath);

            start = System.currentTimeMillis();
            result = analysis.run();
            end = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("{} failed: {}", descriptor.get(), e.toString());
            throw e;
        }

        AnalysisResponse res = new AnalysisResponse();
        res.analysis = req.getName();
        res.allProducts = result;
        res.timeMs = end - start;
        res.message = result
                ? "All products satisfy " + req.getName()
                : "Some products do not satisfy " + req.getName();
        log.info("{}: {} ({} ms)", descriptor.get(), res.message, res.timeMs);
        return res;
    }
}