package ostashko.diploma.zerotrust.core.event;

import java.time.Instant;
import java.util.List;

public record ZeroTrustSecurityEvent(
        ZeroTrustSecurityEventType type,
        Instant timestamp,
        String serviceName,
        String principal,
        String path,
        String method,
        String outcome,
        String reason,
        String correlationId,
        List<String> authorities
) {

    public ZeroTrustSecurityEvent {
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
    }
}
