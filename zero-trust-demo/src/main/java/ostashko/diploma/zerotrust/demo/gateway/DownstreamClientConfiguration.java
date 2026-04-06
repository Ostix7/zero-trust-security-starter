package ostashko.diploma.zerotrust.demo.gateway;

import ostashko.diploma.zerotrust.security.outbound.ZeroTrustRestClientBuilderConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfiguration {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient resourceRestClient(
            RestClient.Builder builder,
            ZeroTrustRestClientBuilderConfigurer configurer,
            @Value("${demo.resource.base-url}") String baseUrl
    ) {
        return configurer.configure(builder).baseUrl(baseUrl).build();
    }
}
