package ostashko.diploma.zerotrust.security.outbound;

import java.time.Instant;
import java.util.List;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.security.web.CorrelationContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

public class ZeroTrustRestClientBuilderConfigurer {

    private final ZeroTrustTokenResolver tokenResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;

    public ZeroTrustRestClientBuilderConfigurer(
            ZeroTrustTokenResolver tokenResolver,
            ApplicationEventPublisher eventPublisher,
            String serviceName
    ) {
        this.tokenResolver = tokenResolver;
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    public RestClient.Builder configure(RestClient.Builder builder) {
        builder.requestInterceptor((request, body, execution) -> {
            tokenResolver.resolve().ifPresent(resolvedToken -> {
                request.getHeaders().setBearerAuth(resolvedToken.tokenValue());
                publishEvent(
                        resolvedToken.serviceToken()
                                ? ZeroTrustSecurityEventType.SERVICE_TOKEN_USED
                                : ZeroTrustSecurityEventType.OUTBOUND_TOKEN_PROPAGATED,
                        request.getMethod().name(),
                        request.getURI().getPath()
                );
            });
            String correlationId = CorrelationContextHolder.getCorrelationId();
            if (correlationId != null && !correlationId.isBlank()) {
                request.getHeaders().set("X-Correlation-Id", correlationId);
            }
            return execution.execute(request, body);
        });
        return builder;
    }

    private void publishEvent(ZeroTrustSecurityEventType type, String method, String path) {
        eventPublisher.publishEvent(new ZeroTrustSecurityEvent(
                type,
                Instant.now(),
                serviceName,
                serviceName,
                path,
                method,
                "ALLOW",
                "outbound request",
                CorrelationContextHolder.getCorrelationId(),
                List.of()
        ));
    }
}
