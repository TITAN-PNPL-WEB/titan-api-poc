package poc.titan.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poc.titan.api.analysis.PluginScanner;
import poc.titan.api.dto.AnalysesResponse;
import poc.titan.api.dto.AnalysisRequest;
import poc.titan.api.dto.AnalysisResponse;
import poc.titan.api.service.AnalysisService;

/**
 * REST endpoints for running PNPL analyses and checking which ones are available.
 */
@RestController
@RequestMapping("/pnpl")
public class AnalysisController {

    private final AnalysisService service;
    private final PluginScanner pluginScanner;

    public AnalysisController(AnalysisService service, PluginScanner pluginScanner) {
        this.service = service;
        this.pluginScanner = pluginScanner;
    }

    /**
     * Runs the requested analysis on a model and returns the result.
     */
    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody AnalysisRequest req) throws Exception {
        if (req == null || req.vrbPath == null || req.vrbPath.isBlank()) {
            throw new IllegalArgumentException("vrbPath is required");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (req.getType() == null || req.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        return service.run(req);
    }

    /**
     * Lists the analyses currently available, discovered from the plugins folder.
     */
    @GetMapping("/analyses")
    public ResponseEntity<AnalysesResponse> getAnalyses() {
        AnalysesResponse response = new AnalysesResponse();
        response.analyses = pluginScanner.getAnalyses()
                .stream()
                .map(d -> new AnalysesResponse.AnalysisInfo(d.name, d.type))
                .toList();
        return ResponseEntity.ok(response);
    }
}