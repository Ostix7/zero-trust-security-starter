package ostashko.diploma.zerotrust.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;

public class ZeroTrustRequestAuditFilter extends OncePerRequestFilter {

    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;

    public ZeroTrustRequestAuditFilter(ApplicationEventPublisher eventPublisher, String serviceName) {
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return;
        }
        if (response.getStatus() >= 400) {
            return;
        }
        SecurityEventSupport.publish(
                eventPublisher,
                ZeroTrustSecurityEventType.AUTHENTICATION_SUCCESS,
                serviceName,
                authentication.getName(),
                request.getMethod(),
                request.getRequestURI(),
                "ALLOW",
                "request completed",
                SecurityEventSupport.authorities(authentication)
        );
    }
}
