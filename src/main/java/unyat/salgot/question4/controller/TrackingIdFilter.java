package unyat.salgot.question4.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class TrackingIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TrackingIdFilter.class);

    public static final String TRACKING_ID_HEADER_NAME = "X-Tracking-Id";

    public static final String MDC_TRACKING_ID_NAME = "x.tracking.id";

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        var trackingId = req.getHeader(TRACKING_ID_HEADER_NAME);
        if (trackingId != null) {
            MDC.put(MDC_TRACKING_ID_NAME, trackingId);
            logger.info("Setting tracking id");
            var resp = (HttpServletResponse) response;
            resp.setHeader(TRACKING_ID_HEADER_NAME, trackingId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (trackingId != null) {
                logger.info("Removing tracking id");
                MDC.remove(MDC_TRACKING_ID_NAME);
            }
        }
    }
}
