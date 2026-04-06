package ostashko.diploma.zerotrust.policy.config;

import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.policy.engine.OpaLikePolicyEvaluator;
import ostashko.diploma.zerotrust.policy.engine.ZeroTrustPolicyEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnProperty(prefix = "zero-trust.inbound.policy", name = "enabled", havingValue = "true")
public class ZeroTrustPolicyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustPolicyEvaluator zeroTrustPolicyEvaluator(ZeroTrustProperties properties) {
        return new OpaLikePolicyEvaluator(
                RestClient.builder().build(),
                properties.getInbound().getPolicy().getEndpoint()
        );
    }
}
