package ostashko.diploma.zerotrust.demo.gateway;

import java.util.Map;
import java.util.Optional;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DemoVaultBackendConfiguration {

    @Bean
    VaultStyleSecretBackend demoVaultStyleSecretBackend(
            @Value("${demo.vault.secret-path:${DEMO_VAULT_SECRET_PATH:secret/services/gateway}}") String path,
            @Value("${demo.vault.secret-key:${DEMO_VAULT_SECRET_KEY:token}}") String key,
            @Value("${demo.vault.service-token:${DEMO_VAULT_SERVICE_TOKEN:}}") String token
    ) {
        return requestedPath -> path.equals(requestedPath) && StringUtils.hasText(token)
                ? Optional.of(Map.of(key, token))
                : Optional.empty();
    }
}
