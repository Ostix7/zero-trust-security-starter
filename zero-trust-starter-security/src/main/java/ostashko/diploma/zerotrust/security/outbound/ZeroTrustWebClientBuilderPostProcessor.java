package ostashko.diploma.zerotrust.security.outbound;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.reactive.function.client.WebClient;

public class ZeroTrustWebClientBuilderPostProcessor implements BeanPostProcessor {

    private final ZeroTrustWebClientCustomizer customizer;

    public ZeroTrustWebClientBuilderPostProcessor(ZeroTrustWebClientCustomizer customizer) {
        this.customizer = customizer;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebClient.Builder builder) {
            customizer.customize(builder);
        }
        return bean;
    }
}
