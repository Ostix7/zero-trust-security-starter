package ostashko.diploma.zerotrust.secrets.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import org.junit.jupiter.api.Test;

class VaultStyleSecretResolverTests {

    @Test
    void resolvesSecretByPathAndKey() {
        VaultStyleSecretBackend backend = path -> "secret/services/gateway".equals(path)
                ? Optional.of(Map.of("token", "service-jwt"))
                : Optional.empty();

        VaultStyleSecretResolver resolver = new VaultStyleSecretResolver(backend);

        assertThat(resolver.resolve("secret/services/gateway", "token")).hasValue("service-jwt");
    }
}
