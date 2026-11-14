package poc.titan.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Binds the log session sent by the front end (X-Log-Session header) to the request thread,
 * so every log of the request, including the TITAN core output, reaches that session's stream.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogSessionFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Log-Session";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String session = request.getHeader(HEADER);
        if (!LogStreamService.isValidSessionId(session)) {
            chain.doFilter(request, response);
            return;
        }

        MDC.put(LogStreamService.MDC_KEY, session);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(LogStreamService.MDC_KEY);
        }
    }
}
