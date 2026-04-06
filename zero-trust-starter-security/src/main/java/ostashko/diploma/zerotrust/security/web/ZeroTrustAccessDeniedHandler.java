package ostashko.diploma.zerotrust.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.core.web.SecurityErrorResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

public class ZeroTrustAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;

    public ZeroTrustAccessDeniedHandler(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, String serviceName) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityEventSupport.publish(
                eventPublisher,
                ZeroTrustSecurityEventType.AUTHORIZATION_DENIED,
                serviceName,
                authentication == null ? "anonymous" : authentication.getName(),
                request.getMethod(),
                request.getRequestURI(),
                "DENY",
                accessDeniedException.getMessage(),
                SecurityEventSupport.authorities(authentication)
        );
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new SecurityErrorResponse(
                Instant.now().toString(),
                403,
                "ACCESS_DENIED",
                "Access is denied for this resource",
                request.getRequestURI(),
                CorrelationContextHolder.getCorrelationId()
        ));
    }
}
