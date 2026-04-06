package ostashko.diploma.zerotrust.observability.config;

import io.micrometer.core.instrument.MeterRegistry;
import ostashko.diploma.zerotrust.observability.store.InMemorySecurityEventStore;
import ostashko.diploma.zerotrust.observability.support.ZeroTrustAuditLoggingListener;
import ostashko.diploma.zerotrust.observability.support.ZeroTrustMetricsListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zero-trust.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZeroTrustAuditProperties.class)
public class ZeroTrustObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustAuditLoggingListener zeroTrustAuditLoggingListener() {
        return new ZeroTrustAuditLoggingListener();
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    ZeroTrustMetricsListener zeroTrustMetricsListener(MeterRegistry meterRegistry) {
        return new ZeroTrustMetricsListener(meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "zero-trust.audit", name = "in-memory-store", havingValue = "true")
    InMemorySecurityEventStore inMemorySecurityEventStore(ZeroTrustAuditProperties properties) {
        return new InMemorySecurityEventStore(properties);
    }
}
