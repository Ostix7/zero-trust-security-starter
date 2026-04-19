package ostashko.diploma.zerotrust.security.outbound;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public class ZeroTrustRestTemplateBeanPostProcessor implements BeanPostProcessor {

    private final ZeroTrustRestTemplateInterceptor interceptor;

    public ZeroTrustRestTemplateBeanPostProcessor(ZeroTrustRestTemplateInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RestTemplate restTemplate) {
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            if (!interceptors.contains(interceptor)) {
                interceptors.add(interceptor);
                restTemplate.setInterceptors(interceptors);
            }
        }
        return bean;
    }
}
