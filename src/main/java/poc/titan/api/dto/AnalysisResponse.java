package poc.titan.api.dto;

/**
 * Response body for POST /pnpl/analyze: the outcome of running an analysis.
 */
public class AnalysisResponse {
    public String analysis;
    public boolean allProducts;
    public long timeMs;
    public String message;
}