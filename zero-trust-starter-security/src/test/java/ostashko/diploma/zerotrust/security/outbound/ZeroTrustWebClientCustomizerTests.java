package ostashko.diploma.zerotrust.security.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import ostashko.diploma.zerotrust.core.config.OutboundTokenMode;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import ostashko.diploma.zerotrust.security.web.CorrelationContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ZeroTrustWebClientCustomizerTests {

    @AfterEach
    void clearCorrelation() {
        CorrelationContextHolder.clear();
    }

    @Test
    void filterAttachesBearerTokenAndCorrelationId() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getOutbound().setTokenMode(OutboundTokenMode.SERVICE_ONLY);
        properties.getOutbound().setServiceTokenSecretPath("secret/services/gateway");
        properties.getOutbound().setServiceTokenSecretKey("token");
        VaultStyleSecretBackend backend = path -> "secret/services/gateway".equals(path)
                ? Optional.of(Map.of("token", "service-jwt"))
                : Optional.empty();
        ZeroTrustTokenResolver tokenResolver = new ZeroTrustTokenResolver(
                properties,
                new VaultStyleSecretResolver(backend)
        );
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CorrelationContextHolder.setCorrelationId("corr-42");

        ZeroTrustWebClientCustomizer customizer = new ZeroTrustWebClientCustomizer(
                tokenResolver,
                eventPublisher,
                "test-service"
        );

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction stubExchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };
        WebClient.Builder builder = WebClient.builder().exchangeFunction(stubExchange);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        webClient.get().uri("http://downstream/api/resource").retrieve().toBodilessEntity().block();

        ClientRequest outgoing = captured.get();
        assertThat(outgoing).isNotNull();
        HttpHeaders headers = outgoing.headers();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-jwt");
        assertThat(headers.getFirst("X-Correlation-Id")).isEqualTo("corr-42");
        assertThat(eventPublisher.lastEvent).isNotNull();
        assertThat(eventPublisher.lastEvent.type()).isEqualTo(ZeroTrustSecurityEventType.SERVICE_TOKEN_USED);
    }

    @Test
    void filterSkipsHeadersWhenNoTokenResolved() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getOutbound().setTokenMode(OutboundTokenMode.USER_ONLY);
        ZeroTrustTokenResolver tokenResolver = new ZeroTrustTokenResolver(
                properties,
                new VaultStyleSecretResolver((VaultStyleSecretBackend) path -> Optional.empty())
        );
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();

        ZeroTrustWebClientCustomizer customizer = new ZeroTrustWebClientCustomizer(
                tokenResolver,
                eventPublisher,
                "test-service"
        );

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        });
        customizer.customize(builder);
        WebClient webClient = builder.build();

        webClient.get().uri("http://downstream/api/resource").retrieve().toBodilessEntity().block();

        assertThat(captured.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(eventPublisher.lastEvent).isNull();
    }

    private static final class CapturingEventPublisher implements ApplicationEventPublisher {
        ZeroTrustSecurityEvent lastEvent;

        @Override
        public void publishEvent(Object event) {
            if (event instanceof ZeroTrustSecurityEvent securityEvent) {
                this.lastEvent = securityEvent;
            }
        }
    }
}
