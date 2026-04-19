package ostashko.diploma.zerotrust.security.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class ZeroTrustRestTemplateInterceptorTests {

    @AfterEach
    void clearCorrelation() {
        CorrelationContextHolder.clear();
    }

    @Test
    void addsBearerTokenAndCorrelationHeader() throws IOException {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getOutbound().setTokenMode(OutboundTokenMode.SERVICE_ONLY);
        properties.getOutbound().setServiceToken("service-jwt");
        ZeroTrustTokenResolver tokenResolver = new ZeroTrustTokenResolver(
                properties,
                new VaultStyleSecretResolver((VaultStyleSecretBackend) path -> Optional.empty())
        );
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CorrelationContextHolder.setCorrelationId("corr-123");

        ZeroTrustRestTemplateInterceptor interceptor = new ZeroTrustRestTemplateInterceptor(
                tokenResolver,
                eventPublisher,
                "test-service"
        );

        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://downstream/api/resource"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertThat(result).isSameAs(response);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-jwt");
        assertThat(headers.getFirst("X-Correlation-Id")).isEqualTo("corr-123");
        assertThat(eventPublisher.lastEvent).isNotNull();
        assertThat(eventPublisher.lastEvent.type()).isEqualTo(ZeroTrustSecurityEventType.SERVICE_TOKEN_USED);
        assertThat(eventPublisher.lastEvent.path()).isEqualTo("/api/resource");
    }

    @Test
    void doesNothingWhenTokenCannotBeResolved() throws IOException {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getOutbound().setTokenMode(OutboundTokenMode.USER_ONLY);
        ZeroTrustTokenResolver tokenResolver = new ZeroTrustTokenResolver(
                properties,
                new VaultStyleSecretResolver((VaultStyleSecretBackend) path -> Optional.empty())
        );
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();

        ZeroTrustRestTemplateInterceptor interceptor = new ZeroTrustRestTemplateInterceptor(
                tokenResolver,
                eventPublisher,
                "test-service"
        );

        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://downstream/api/resource"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
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

    private static Map.Entry<String, String> entry(String k, String v) {
        return Map.entry(k, v);
    }
}
