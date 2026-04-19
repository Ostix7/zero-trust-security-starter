package ostashko.diploma.zerotrust.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import ostashko.diploma.zerotrust.core.config.ZeroTrustCoreAutoConfiguration;
import ostashko.diploma.zerotrust.policy.config.ZeroTrustPolicyAutoConfiguration;
import ostashko.diploma.zerotrust.secrets.config.ZeroTrustSecretsAutoConfiguration;
import ostashko.diploma.zerotrust.security.auth.ZeroTrustJwtAuthenticationConverter;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustRestClientBuilderConfigurer;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustRestTemplateInterceptor;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustTokenResolver;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustWebClientCustomizer;
import ostashko.diploma.zerotrust.security.web.CorrelationIdFilter;
import ostashko.diploma.zerotrust.security.web.RateLimitFilter;
import ostashko.diploma.zerotrust.security.web.TenantPolicyEnforcementFilter;
import ostashko.diploma.zerotrust.security.web.ZeroTrustAuthenticationEntryPoint;
import ostashko.diploma.zerotrust.security.web.ZeroTrustRequestAuditFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

class ZeroTrustSecurityAutoConfigurationTests {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    ZeroTrustCoreAutoConfiguration.class,
                    ZeroTrustSecretsAutoConfiguration.class,
                    ZeroTrustPolicyAutoConfiguration.class,
                    ZeroTrustSecurityAutoConfiguration.class
            ))
            .withPropertyValues(
                    "zero-trust.inbound.jwt.issuer=https://issuer.test",
                    "zero-trust.inbound.jwt.shared-secret=01234567890123456789012345678901"
            );

    @Test
    void registersAllSecurityBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).hasSingleBean(ZeroTrustJwtAuthenticationConverter.class);
            assertThat(context).hasSingleBean(CorrelationIdFilter.class);
            assertThat(context).hasSingleBean(ZeroTrustAuthenticationEntryPoint.class);
            assertThat(context).hasSingleBean(ZeroTrustRequestAuditFilter.class);
            assertThat(context).hasSingleBean(TenantPolicyEnforcementFilter.class);
            assertThat(context).hasSingleBean(ZeroTrustTokenResolver.class);
            assertThat(context).hasSingleBean(ZeroTrustRestClientBuilderConfigurer.class);
        });
    }

    @Test
    void disabledWhenZeroTrustDisabled() {
        runner.withPropertyValues("zero-trust.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ZeroTrustJwtAuthenticationConverter.class);
                    assertThat(context).doesNotHaveBean(CorrelationIdFilter.class);
                });
    }

    @Test
    void disabledWhenInboundDisabled() {
        runner.withPropertyValues("zero-trust.inbound.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ZeroTrustJwtAuthenticationConverter.class);
                    assertThat(context).doesNotHaveBean(CorrelationIdFilter.class);
                });
    }

    @Test
    void rateLimitFilterNotCreatedByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(RateLimitFilter.class);
        });
    }

    @Test
    void rateLimitFilterCreatedWhenEnabled() {
        runner.withPropertyValues("zero-trust.rate-limit.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitFilter.class);
                });
    }

    @Test
    void registersWebClientCustomizerByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ZeroTrustWebClientCustomizer.class);
        });
    }

    @Test
    void registersRestTemplateInterceptorByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ZeroTrustRestTemplateInterceptor.class);
        });
    }

    @Test
    void customJwtConverterTakesPrecedence() {
        runner.withBean(ZeroTrustJwtAuthenticationConverter.class, () -> {
                    var props = new ostashko.diploma.zerotrust.core.config.ZeroTrustProperties();
                    return new ZeroTrustJwtAuthenticationConverter(props);
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(ZeroTrustJwtAuthenticationConverter.class);
                });
    }
}
