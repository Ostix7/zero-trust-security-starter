package ostashko.diploma.zerotrust.secrets.config;

import ostashko.diploma.zerotrust.secrets.backend.InlineVaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zero-trust.secrets", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZeroTrustSecretsProperties.class)
public class ZeroTrustSecretsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zero-trust.secrets.vault", name = "enabled", havingValue = "true")
    VaultStyleSecretBackend inlineVaultStyleSecretBackend(ZeroTrustSecretsProperties properties) {
        return new InlineVaultStyleSecretBackend(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    VaultStyleSecretResolver vaultStyleSecretResolver(ObjectProvider<VaultStyleSecretBackend> backendProvider) {
        return new VaultStyleSecretResolver(() -> backendProvider.getIfAvailable());
    }
}
