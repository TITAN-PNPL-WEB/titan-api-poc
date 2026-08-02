package poc.titan.api.dto;

import java.util.List;

public class ValidationResponse {
    private boolean valid;
    private List<ValidationIssue> issues;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues;
    }

    public static class ValidationIssue {
        private String severity;  // "ERROR", "WARNING", "INFO"
        private String message;
        private int line;
        private int column;

        public ValidationIssue(String severity, String message, int line, int column) {
            this.severity = severity;
            this.message = message;
            this.line = line;
            this.column = column;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }
    }
}