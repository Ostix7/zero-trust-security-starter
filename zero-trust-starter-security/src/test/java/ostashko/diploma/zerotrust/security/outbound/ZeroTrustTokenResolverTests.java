package ostashko.diploma.zerotrust.security.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import ostashko.diploma.zerotrust.core.config.OutboundTokenMode;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import org.junit.jupiter.api.Test;

class ZeroTrustTokenResolverTests {

    @Test
    void resolvesServiceTokenFromVaultStyleSecretResolver() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getOutbound().setTokenMode(OutboundTokenMode.SERVICE_ONLY);
        properties.getOutbound().setServiceTokenSecretPath("secret/services/gateway");
        properties.getOutbound().setServiceTokenSecretKey("token");

        VaultStyleSecretBackend backend = path -> "secret/services/gateway".equals(path)
                ? Optional.of(Map.of("token", "service-jwt"))
                : Optional.empty();

        ZeroTrustTokenResolver resolver = new ZeroTrustTokenResolver(properties, new VaultStyleSecretResolver(backend));

        assertThat(resolver.resolve())
                .hasValueSatisfying(token -> {
                    assertThat(token.tokenValue()).isEqualTo("service-jwt");
                    assertThat(token.serviceToken()).isTrue();
                });
    }
}
