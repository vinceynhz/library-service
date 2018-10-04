package app.tandv.services.filter;

import app.tandv.services.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * To handle logging on request reception and before sending the final response
 *
 * @author Vic on 9/5/2018
 **/
@Component
@RequestScope
public class RequestLogging implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogging.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURI().contains("isRunning")) {
            return true;
        }
        StringBuilder str = new StringBuilder(request.getMethod()).append(" ").append(request.getRequestURI());
        if (StringUtils.validString(request.getQueryString())) {
            str.append("?").append(request.getQueryString());
        }
        LOGGER.info("Received: " + str.toString());
        request.setAttribute("ST", String.valueOf(System.currentTimeMillis()));
        request.setAttribute("RQ", str.toString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getRequestURI().contains("isRunning")) {
            return;
        }

        Object st = request.getAttribute("ST");
        long tt = -1L;
        if (st != null) {
            try {
                tt = System.currentTimeMillis() - Long.valueOf(st.toString());
            } catch (NumberFormatException exception) {
                LOGGER.error("Error parsing starting time", exception);
            }
        }
        LOGGER.info("Total time: " + request.getAttribute("RQ") + " " + tt + "ms " + response.getStatus());
    }
}
