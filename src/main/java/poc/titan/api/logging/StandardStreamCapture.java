package poc.titan.api.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Turns the output of the TITAN core (System.out / System.err) into log events of the "titan.core"
 * logger, and adds the appender that sends the events of each log session to LogStreamService.
 * System.out lines are logged as INFO, System.err lines as WARN.
 */
@Component
public class StandardStreamCapture {

    public static final String CORE_LOGGER = "titan.core";

    private static final Logger log = LoggerFactory.getLogger(StandardStreamCapture.class);

    private final LogStreamService logStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private SessionLogAppender appender;

    public StandardStreamCapture(LogStreamService logStream) {
        this.logStream = logStream;
    }

    @PostConstruct
    public void install() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        originalOut = System.out;
        originalErr = System.err;

        // Logback console appenders look up System.out on every write: point them to the real streams
        // first, or each log line would come back through the capture below
        for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext(); ) {
            if (it.next() instanceof ConsoleAppender<ILoggingEvent> console) {
                boolean err = "System.err".equals(console.getTarget());
                console.setOutputStream(new NonClosingOutputStream(err ? originalErr : originalOut));
            }
        }

        appender = new SessionLogAppender(logStream);
        appender.setContext(context);
        appender.setName("log-session-stream");
        appender.start();
        root.addAppender(appender);

        Logger core = LoggerFactory.getLogger(CORE_LOGGER);
        System.setOut(new PrintStream(new LineLoggingOutputStream(originalOut, core, Level.INFO), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LineLoggingOutputStream(originalErr, core, Level.WARN), true, StandardCharsets.UTF_8));

        log.info("Core output captured by the {} logger", CORE_LOGGER);
    }

    @PreDestroy
    public void uninstall() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
        appender.stop();
    }

    // Stopping an appender closes its stream: keep the process stdout and stderr open
    private static final class NonClosingOutputStream extends FilterOutputStream {
        NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
