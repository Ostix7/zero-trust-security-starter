package ostashko.diploma.zerotrust.security.web;

import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;

final class SecurityEventSupport {

    private SecurityEventSupport() {
    }

    static void publish(
            ApplicationEventPublisher eventPublisher,
            ZeroTrustSecurityEventType type,
            String serviceName,
            String principal,
            String method,
            String path,
            String outcome,
            String reason,
            List<String> authorities
    ) {
        eventPublisher.publishEvent(new ZeroTrustSecurityEvent(
                type,
                Instant.now(),
                serviceName,
                principal,
                path,
                method,
                outcome,
                reason,
                CorrelationContextHolder.getCorrelationId(),
                authorities
        ));
    }

    static List<String> authorities(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
