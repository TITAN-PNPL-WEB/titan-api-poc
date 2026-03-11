package pocfreechoice.controller;

import pocfreechoice.dto.AnalyzeRequest;
import pocfreechoice.dto.AnalyzeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import pocfreechoice.service.FreeChoiceService;
import org.springframework.web.bind.annotation.RestController;


import java.nio.file.*;

@RestController
@RequestMapping("/pnpl")
public class PnplController {

    private final FreeChoiceService service = new FreeChoiceService();

    @PostMapping("/freechoice")
    public AnalyzeResponse analyzeFreeChoice(@RequestBody AnalyzeRequest req) throws Exception {
        if (req == null || req.vrbPath == null || req.vrbPath.isBlank()) {
            throw new IllegalArgumentException("vrbPath is required");
        }
        Path vrb = Paths.get(req.vrbPath).toAbsolutePath().normalize();
        return service.run(vrb);
    }
}
