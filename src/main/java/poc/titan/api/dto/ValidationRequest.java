package poc.titan.api.dto;

/**
 * Request body for POST /pnpl/validate.
 */
public class ValidationRequest {
    private String vrbPath;

    public String getVrbPath() {
        return vrbPath;
    }

    public void setVrbPath(String vrbPath) {
        this.vrbPath = vrbPath;
    }
}