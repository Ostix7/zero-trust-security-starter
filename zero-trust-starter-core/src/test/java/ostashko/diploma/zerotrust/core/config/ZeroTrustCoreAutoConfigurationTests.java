package ostashko.diploma.zerotrust.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import ostashko.diploma.zerotrust.core.validation.ZeroTrustStartupValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZeroTrustCoreAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeroTrustCoreAutoConfiguration.class))
            .withPropertyValues(
                    "zero-trust.inbound.jwt.issuer=https://test",
                    "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901"
            );

    @Test
    void registersValidatorByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ZeroTrustStartupValidator.class);
            assertThat(context).hasSingleBean(ZeroTrustProperties.class);
        });
    }

    @Test
    void disabledWhenPropertyIsFalse() {
        runner.withPropertyValues("zero-trust.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ZeroTrustStartupValidator.class);
                });
    }

    @Test
    void propertiesBindCorrectly() {
        runner.withPropertyValues(
                        "zero-trust.mode=BALANCED",
                        "zero-trust.inbound.jwt.issuer=https://test",
                        "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901",
                        "zero-trust.rate-limit.enabled=true",
                        "zero-trust.rate-limit.requests-per-second=50",
                        "zero-trust.rate-limit.burst-capacity=100"
                )
                .run(context -> {
                    ZeroTrustProperties properties = context.getBean(ZeroTrustProperties.class);
                    assertThat(properties.getMode()).isEqualTo(ZeroTrustMode.BALANCED);
                    assertThat(properties.getRateLimit().isEnabled()).isTrue();
                    assertThat(properties.getRateLimit().getRequestsPerSecond()).isEqualTo(50);
                    assertThat(properties.getRateLimit().getBurstCapacity()).isEqualTo(100);
                });
    }
}
