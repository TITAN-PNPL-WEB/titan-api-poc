package poc.titan.api.dto;

import java.util.List;

/**
 * Response body for GET /pnpl/analyses: the list of analyses currently available.
 */
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