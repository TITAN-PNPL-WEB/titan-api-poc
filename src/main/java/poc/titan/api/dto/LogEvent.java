package poc.titan.api.dto;

import java.time.Instant;

/**
 * One log line sent through GET /pnpl/logs/{sessionId}.
 *
 * @param seq       position in the session, starting at 1 (also the SSE event id, used to resume with Last-Event-ID)
 * @param timestamp when the line was logged
 * @param level     "INFO", "WARN" or "ERROR"
 * @param source    "core" (output of the TITAN core), "api" (this API) or "server" (Spring, Tomcat, libraries)
 * @param message   the log text, with the stack trace appended if there is one
 */
public record LogEvent(long seq, Instant timestamp, String level, String source, String message) {
}
