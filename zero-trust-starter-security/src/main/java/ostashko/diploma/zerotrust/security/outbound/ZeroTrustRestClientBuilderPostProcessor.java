package ostashko.diploma.zerotrust.security.outbound;

import java.time.Instant;
import java.util.List;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.security.web.CorrelationContextHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.client.RestClient;

public class ZeroTrustRestClientBuilderPostProcessor implements BeanPostProcessor {

    private final ZeroTrustRestClientBuilderConfigurer configurer;

    public ZeroTrustRestClientBuilderPostProcessor(ZeroTrustRestClientBuilderConfigurer configurer) {
        this.configurer = configurer;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RestClient.Builder builder) {
            configurer.configure(builder);
        }
        return bean;
    }
}
