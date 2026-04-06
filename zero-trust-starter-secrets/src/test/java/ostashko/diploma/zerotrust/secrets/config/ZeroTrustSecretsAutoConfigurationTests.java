package ostashko.diploma.zerotrust.secrets.config;

import static org.assertj.core.api.Assertions.assertThat;

import ostashko.diploma.zerotrust.secrets.backend.InlineVaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.backend.VaultStyleSecretBackend;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZeroTrustSecretsAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeroTrustSecretsAutoConfiguration.class));

    @Test
    void registersResolverByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(VaultStyleSecretResolver.class);
            assertThat(context).doesNotHaveBean(VaultStyleSecretBackend.class);
        });
    }

    @Test
    void registersInlineBackendWhenVaultEnabled() {
        runner.withPropertyValues(
                        "zero-trust.secrets.vault.enabled=true",
                        "zero-trust.secrets.vault.entries[0].path=secret/test",
                        "zero-trust.secrets.vault.entries[0].key=token",
                        "zero-trust.secrets.vault.entries[0].value=my-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(VaultStyleSecretBackend.class);
                    assertThat(context).hasSingleBean(VaultStyleSecretResolver.class);
                    VaultStyleSecretBackend backend = context.getBean(VaultStyleSecretBackend.class);
                    assertThat(backend.read("secret/test")).isPresent()
                            .hasValueSatisfying(map -> assertThat(map).containsEntry("token", "my-secret"));
                });
    }

    @Test
    void disabledWhenPropertyIsFalse() {
        runner.withPropertyValues("zero-trust.secrets.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(VaultStyleSecretResolver.class);
                    assertThat(context).doesNotHaveBean(VaultStyleSecretBackend.class);
                });
    }

    @Test
    void customBackendTakesPrecedence() {
        VaultStyleSecretBackend custom = path -> Optional.of(Map.of("custom", "value"));
        runner.withPropertyValues("zero-trust.secrets.vault.enabled=true")
                .withBean(VaultStyleSecretBackend.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(VaultStyleSecretBackend.class);
                    assertThat(context.getBean(VaultStyleSecretBackend.class)).isSameAs(custom);
                });
    }
}
