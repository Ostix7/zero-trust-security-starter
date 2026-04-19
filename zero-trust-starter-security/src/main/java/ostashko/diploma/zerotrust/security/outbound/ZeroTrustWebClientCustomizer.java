package ostashko.diploma.zerotrust.security.outbound;

import java.time.Instant;
import java.util.List;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.security.web.CorrelationContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

public class ZeroTrustWebClientCustomizer {

    private final ZeroTrustTokenResolver tokenResolver;
    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;

    public ZeroTrustWebClientCustomizer(
            ZeroTrustTokenResolver tokenResolver,
            ApplicationEventPublisher eventPublisher,
            String serviceName
    ) {
        this.tokenResolver = tokenResolver;
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    public void customize(WebClient.Builder builder) {
        builder.filter(zeroTrustFilter());
    }

    ExchangeFilterFunction zeroTrustFilter() {
        return (request, next) -> {
            ClientRequest decorated = decorate(request);
            return next.exchange(decorated);
        };
    }

    private ClientRequest decorate(ClientRequest request) {
        return ClientRequest.from(request)
                .headers(headers -> {
                    tokenResolver.resolve().ifPresent(resolvedToken -> {
                        headers.setBearerAuth(resolvedToken.tokenValue());
                        publishEvent(
                                resolvedToken.serviceToken()
                                        ? ZeroTrustSecurityEventType.SERVICE_TOKEN_USED
                                        : ZeroTrustSecurityEventType.OUTBOUND_TOKEN_PROPAGATED,
                                request.method().name(),
                                request.url().getPath()
                        );
                    });
                    String correlationId = CorrelationContextHolder.getCorrelationId();
                    if (correlationId != null && !correlationId.isBlank()) {
                        headers.set("X-Correlation-Id", correlationId);
                    }
                })
                .build();
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
