package pocfreechoice.service;

import pocfreechoice.dto.AnalyzeResponse;
import org.pnpl.analysis.freechoice.analysis.AnalysisPNPL;

import java.nio.file.Path;

public class FreeChoiceService {
    public AnalyzeResponse run(Path vrbPath) throws Exception {
        long start = System.currentTimeMillis();

        AnalysisPNPL analysis = new AnalysisPNPL();
        analysis.loadVariabilityFile(vrbPath);   // <- overload nuevo (Path)
        boolean all = analysis.run();

        long end = System.currentTimeMillis();

        AnalyzeResponse res = new AnalyzeResponse();
        res.allProducts = all;
        res.timeMs = (end - start);
        res.message = (all ? "All products are Free Choice" : "Some products are not Free Choice");
        return res;
    }
}
