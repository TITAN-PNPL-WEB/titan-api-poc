package poc.titan.api.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import poc.titan.api.logging.LogStreamService;

/**
 * Live log stream of a front-end session (Server-Sent Events).
 */
@RestController
@RequestMapping("/pnpl")
public class LogController {

    private final LogStreamService logStream;

    public LogController(LogStreamService logStream) {
        this.logStream = logStream;
    }

    /**
     * Streams the logs of the requests sent with the X-Log-Session header set to sessionId:
     * first the recent ones, then the new ones as they happen. Each SSE event is named "log",
     * has the sequence number as id and a LogEvent as JSON data. A reconnecting EventSource
     * sends Last-Event-ID and only gets the events it missed.
     */
    @GetMapping(path = "/logs/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String sessionId,
                             @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
                             HttpServletResponse response) {
        if (!LogStreamService.isValidSessionId(sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must be a UUID");
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // reverse proxies must not buffer the stream
        return logStream.subscribe(sessionId, parseEventId(lastEventId));
    }

    private static long parseEventId(String id) {
        try {
            return id == null ? 0 : Math.max(0, Long.parseLong(id.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
