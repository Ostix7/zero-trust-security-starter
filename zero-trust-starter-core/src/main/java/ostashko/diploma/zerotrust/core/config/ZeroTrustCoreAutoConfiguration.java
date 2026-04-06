package ostashko.diploma.zerotrust.core.config;

import ostashko.diploma.zerotrust.core.validation.ZeroTrustStartupValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zero-trust", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZeroTrustProperties.class)
public class ZeroTrustCoreAutoConfiguration {

    @Bean
    ZeroTrustStartupValidator zeroTrustStartupValidator(ZeroTrustProperties properties, Environment environment) {
        return new ZeroTrustStartupValidator(properties, environment);
    }
}
