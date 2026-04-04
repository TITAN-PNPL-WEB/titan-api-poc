package pocfreechoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pocfreechoice.analysis.PluginScanner;
import pocfreechoice.dto.AnalysesResponse;
import pocfreechoice.dto.AnalyzeRequest;
import pocfreechoice.dto.AnalyzeResponse;
import pocfreechoice.service.FreeChoiceService;


import java.nio.file.*;

@RestController
@RequestMapping("/pnpl")
public class PnplController {

    private final FreeChoiceService service;
    private final PluginScanner pluginScanner;

    public PnplController(FreeChoiceService service, PluginScanner pluginScanner) {
        this.service = service;
        this.pluginScanner = pluginScanner;
    }

    @PostMapping("/freechoice")
    public AnalyzeResponse analyzeFreeChoice(@RequestBody AnalyzeRequest req) throws Exception {
        if (req == null || req.vrbPath == null || req.vrbPath.isBlank()) {
            throw new IllegalArgumentException("vrbPath is required");
        }
        Path vrb = Paths.get(req.vrbPath).toAbsolutePath().normalize();
        return service.run(vrb);
    }

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
