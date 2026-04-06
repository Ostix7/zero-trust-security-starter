package ostashko.diploma.zerotrust.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.core.web.SecurityErrorResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ZeroTrustAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;

    public ZeroTrustAuthenticationEntryPoint(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, String serviceName) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        SecurityEventSupport.publish(
                eventPublisher,
                ZeroTrustSecurityEventType.AUTHENTICATION_FAILURE,
                serviceName,
                "anonymous",
                request.getMethod(),
                request.getRequestURI(),
                "DENY",
                authException.getMessage(),
                List.of()
        );
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new SecurityErrorResponse(
                Instant.now().toString(),
                401,
                "UNAUTHORIZED",
                "Authentication is required to access this resource",
                request.getRequestURI(),
                CorrelationContextHolder.getCorrelationId()
        ));
    }
}
