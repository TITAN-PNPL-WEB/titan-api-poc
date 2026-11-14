package poc.titan.api.logging;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import poc.titan.api.dto.LogEvent;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The recent log events of one front-end session and the streams currently reading them.
 */
final class LogSession {

    /**
     * An open stream. lastSent is only touched by the dispatcher thread of LogStreamService.
     */
    static final class Subscriber {
        final SseEmitter emitter;
        long lastSent;

        Subscriber(SseEmitter emitter, long lastSent) {
            this.emitter = emitter;
            this.lastSent = lastSent;
        }
    }

    final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    final AtomicBoolean flushScheduled = new AtomicBoolean();

    private final int capacity;
    private final Deque<LogEvent> buffer = new ArrayDeque<>();
    private long nextSeq = 1;
    private volatile Instant lastActivity = Instant.now();

    LogSession(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Adds an event, dropping the oldest one when the buffer is full.
     */
    synchronized void append(Instant timestamp, String level, String source, String message) {
        buffer.addLast(new LogEvent(nextSeq++, timestamp, level, source, message));
        if (buffer.size() > capacity) buffer.removeFirst();
        touch();
    }

    /**
     * Returns the buffered events with a sequence number greater than seq, oldest first.
     */
    synchronized List<LogEvent> eventsAfter(long seq) {
        return buffer.stream().filter(e -> e.seq() > seq).toList();
    }

    /**
     * Sequence number of the last event added, 0 if none.
     */
    synchronized long lastSeq() {
        return nextSeq - 1;
    }

    void touch() {
        lastActivity = Instant.now();
    }

    Instant lastActivity() {
        return lastActivity;
    }
}
