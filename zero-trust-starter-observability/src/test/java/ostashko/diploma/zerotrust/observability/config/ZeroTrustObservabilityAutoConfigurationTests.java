package ostashko.diploma.zerotrust.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ostashko.diploma.zerotrust.observability.store.InMemorySecurityEventStore;
import ostashko.diploma.zerotrust.observability.support.ZeroTrustAuditLoggingListener;
import ostashko.diploma.zerotrust.observability.support.ZeroTrustMetricsListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZeroTrustObservabilityAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZeroTrustObservabilityAutoConfiguration.class));

    @Test
    void registersAuditListenerByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ZeroTrustAuditLoggingListener.class);
            assertThat(context).doesNotHaveBean(ZeroTrustMetricsListener.class);
            assertThat(context).doesNotHaveBean(InMemorySecurityEventStore.class);
        });
    }

    @Test
    void registersMetricsListenerWhenMeterRegistryPresent() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ZeroTrustAuditLoggingListener.class);
                    assertThat(context).hasSingleBean(ZeroTrustMetricsListener.class);
                });
    }

    @Test
    void registersInMemoryStoreWhenEnabled() {
        runner.withPropertyValues("zero-trust.audit.in-memory-store=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(InMemorySecurityEventStore.class);
                });
    }

    @Test
    void disabledWhenPropertyIsFalse() {
        runner.withPropertyValues("zero-trust.audit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ZeroTrustAuditLoggingListener.class);
                    assertThat(context).doesNotHaveBean(ZeroTrustMetricsListener.class);
                    assertThat(context).doesNotHaveBean(InMemorySecurityEventStore.class);
                });
    }

    @Test
    void propertiesBindCorrectly() {
        runner.withPropertyValues(
                        "zero-trust.audit.log-success=false",
                        "zero-trust.audit.log-failures=true",
                        "zero-trust.audit.in-memory-store=true"
                )
                .run(context -> {
                    ZeroTrustAuditProperties properties = context.getBean(ZeroTrustAuditProperties.class);
                    assertThat(properties.isLogSuccess()).isFalse();
                    assertThat(properties.isLogFailures()).isTrue();
                    assertThat(properties.isInMemoryStore()).isTrue();
                });
    }
}
