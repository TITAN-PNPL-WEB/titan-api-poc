package poc.titan.api.dto;

/**
 * Response body for POST /pnpl/upload.
 */
public class UploadResponse {
    private String vrbPath;
    private String message;

    public UploadResponse(String vrbPath, String message) {
        this.vrbPath = vrbPath;
        this.message = message;
    }

    public String getVrbPath() {
        return vrbPath;
    }

    public String getMessage() {
        return message;
    }
}