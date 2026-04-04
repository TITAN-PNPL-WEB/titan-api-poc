package pocfreechoice.dto;

import java.util.List;

public class AnalysesResponse {
    public List<AnalysisInfo> analyses;

    public static class AnalysisInfo {
        public String name;
        public String type;

        public AnalysisInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}