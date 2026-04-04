package poc.titan.api.dto;

public class AnalysisRequest {
    public String vrbPath;
    private String name; //"Free Choice", "Marked Graph"
    private String type; //"PNPL", "Products"

    public String getVrbPath() {
        return vrbPath;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}
