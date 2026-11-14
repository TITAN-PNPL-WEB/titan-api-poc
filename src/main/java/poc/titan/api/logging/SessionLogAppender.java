package poc.titan.api.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.time.Instant;

/**
 * Logback appender that sends the INFO and higher events logged inside a log session
 * (MDC key set by LogSessionFilter) to LogStreamService. Other events are ignored.
 */
class SessionLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final LogStreamService logStream;

    SessionLogAppender(LogStreamService logStream) {
        this.logStream = logStream;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String session = event.getMDCPropertyMap().get(LogStreamService.MDC_KEY);
        if (session == null || !event.getLevel().isGreaterOrEqual(Level.INFO)) return;

        String message = event.getFormattedMessage();
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) message += System.lineSeparator() + ThrowableProxyUtil.asString(throwable);

        logStream.publish(session, Instant.ofEpochMilli(event.getTimeStamp()), event.getLevel().toString(),
                sourceOf(event.getLoggerName()), message);
    }

    private static String sourceOf(String loggerName) {
        if (StandardStreamCapture.CORE_LOGGER.equals(loggerName)) return "core";
        if (loggerName.startsWith("poc.titan.api")) return "api";
        return "server";
    }
}
