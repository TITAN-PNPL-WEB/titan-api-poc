package poc.titan.api.logging;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Replacement for System.out / System.err: logs every complete line written to it.
 * Each thread has its own line buffer, so concurrent requests do not mix their lines.
 * Anything written while that logging is in progress (an appender that still writes to
 * System.out) goes straight to the original stream, to avoid an endless loop.
 */
final class LineLoggingOutputStream extends OutputStream {

    private static final ThreadLocal<Boolean> LOGGING = ThreadLocal.withInitial(() -> false);
    private static final int MAX_LINE = 64 * 1024;

    private final PrintStream original;
    private final Logger logger;
    private final Level level;
    private final ThreadLocal<ByteArrayOutputStream> line = ThreadLocal.withInitial(ByteArrayOutputStream::new);

    LineLoggingOutputStream(PrintStream original, Logger logger, Level level) {
        this.original = original;
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void write(int b) {
        if (LOGGING.get()) {
            original.write(b);
            return;
        }
        if (b == '\n') {
            emit();
            return;
        }
        ByteArrayOutputStream buffer = line.get();
        buffer.write(b);
        if (buffer.size() >= MAX_LINE) emit();
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (LOGGING.get()) {
            original.write(b, off, len);
            return;
        }
        for (int i = off; i < off + len; i++) write(b[i]);
    }

    @Override
    public void flush() {
        // Lines are logged when complete: PrintStream flushes after every write, also in the middle of a line
        if (LOGGING.get()) original.flush();
    }

    private void emit() {
        ByteArrayOutputStream buffer = line.get();
        String text = buffer.toString(StandardCharsets.UTF_8).stripTrailing();
        buffer.reset();
        if (text.isEmpty()) return;

        LOGGING.set(true);
        try {
            logger.atLevel(level).log(text);
        } finally {
            LOGGING.set(false);
        }
    }
}
