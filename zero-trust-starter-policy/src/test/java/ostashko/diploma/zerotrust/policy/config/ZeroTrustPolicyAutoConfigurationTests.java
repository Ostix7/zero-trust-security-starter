package ostashko.diploma.zerotrust.policy.config;

import static org.assertj.core.api.Assertions.assertThat;

import ostashko.diploma.zerotrust.core.config.ZeroTrustCoreAutoConfiguration;
import ostashko.diploma.zerotrust.policy.engine.PolicyDecision;
import ostashko.diploma.zerotrust.policy.engine.PolicyEvaluationContext;
import ostashko.diploma.zerotrust.policy.engine.ZeroTrustPolicyEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZeroTrustPolicyAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ZeroTrustCoreAutoConfiguration.class,
                    ZeroTrustPolicyAutoConfiguration.class
            ))
            .withPropertyValues(
                    "zero-trust.inbound.jwt.issuer=https://test",
                    "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901"
            );

    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(ZeroTrustPolicyEvaluator.class);
        });
    }

    @Test
    void createsEvaluatorWhenEnabled() {
        runner.withPropertyValues(
                        "zero-trust.inbound.jwt.issuer=https://test",
                        "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901",
                        "zero-trust.inbound.policy.enabled=true",
                        "zero-trust.inbound.policy.endpoint=http://localhost:8082/opa/authorize",
                        "zero-trust.inbound.policy.protected-paths=/api/policy/**"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ZeroTrustPolicyEvaluator.class);
                });
    }

    @Test
    void customEvaluatorTakesPrecedence() {
        runner.withPropertyValues(
                        "zero-trust.inbound.jwt.issuer=https://test",
                        "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901",
                        "zero-trust.inbound.policy.enabled=true",
                        "zero-trust.inbound.policy.endpoint=http://localhost:8082/opa/authorize",
                        "zero-trust.inbound.policy.protected-paths=/api/policy/**"
                )
                .withBean(ZeroTrustPolicyEvaluator.class, () -> ctx -> PolicyDecision.deny("custom"))
                .run(context -> {
                    assertThat(context).hasSingleBean(ZeroTrustPolicyEvaluator.class);
                    ZeroTrustPolicyEvaluator evaluator = context.getBean(ZeroTrustPolicyEvaluator.class);
                    assertThat(evaluator.evaluate(null).reason()).isEqualTo("custom");
                });
    }
}
