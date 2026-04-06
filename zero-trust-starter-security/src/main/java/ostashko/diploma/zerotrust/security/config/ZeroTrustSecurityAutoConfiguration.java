package ostashko.diploma.zerotrust.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.policy.engine.ZeroTrustPolicyEvaluator;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import ostashko.diploma.zerotrust.security.auth.ZeroTrustAudienceValidator;
import ostashko.diploma.zerotrust.security.auth.ZeroTrustJwtAuthenticationConverter;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustRestClientBuilderConfigurer;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustRestClientBuilderPostProcessor;
import ostashko.diploma.zerotrust.security.outbound.ZeroTrustTokenResolver;
import ostashko.diploma.zerotrust.security.web.CorrelationIdFilter;
import ostashko.diploma.zerotrust.security.web.TenantPolicyEnforcementFilter;
import ostashko.diploma.zerotrust.security.web.ZeroTrustAccessDeniedHandler;
import ostashko.diploma.zerotrust.security.web.RateLimitFilter;
import ostashko.diploma.zerotrust.security.web.ZeroTrustAuthenticationEntryPoint;
import ostashko.diploma.zerotrust.security.web.ZeroTrustRequestAuditFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@AutoConfiguration
@AutoConfigureBefore(name = {
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration"
})
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "zero-trust", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "zero-trust.inbound", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ZeroTrustSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustJwtAuthenticationConverter zeroTrustJwtAuthenticationConverter(ZeroTrustProperties properties) {
        return new ZeroTrustJwtAuthenticationConverter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder zeroTrustJwtDecoder(ZeroTrustProperties properties) {
        ZeroTrustProperties.Jwt jwt = properties.getInbound().getJwt();
        if (!StringUtils.hasText(jwt.getSharedSecret()) && !StringUtils.hasText(jwt.getIssuerUri())) {
            throw new IllegalStateException("Zero Trust requires either zero-trust.inbound.jwt.shared-secret or zero-trust.inbound.jwt.issuer-uri.");
        }
        JwtDecoder decoder = StringUtils.hasText(jwt.getSharedSecret())
                ? sharedSecretDecoder(jwt.getSharedSecret())
                : JwtDecoders.fromIssuerLocation(jwt.getIssuerUri());
        if (decoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(jwt.getIssuer());
            if (!jwt.getAudiences().isEmpty()) {
                validator = new DelegatingOAuth2TokenValidator<>(validator, new ZeroTrustAudienceValidator(jwt.getAudiences()));
            }
            nimbusJwtDecoder.setJwtValidator(validator);
        }
        return decoder;
    }

    private JwtDecoder sharedSecretDecoder(String secret) {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    @ConditionalOnMissingBean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper zeroTrustObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustAuthenticationEntryPoint zeroTrustAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new ZeroTrustAuthenticationEntryPoint(objectMapper, eventPublisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    AccessDeniedHandler zeroTrustAccessDeniedHandler(
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new ZeroTrustAccessDeniedHandler(objectMapper, eventPublisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustRequestAuditFilter zeroTrustRequestAuditFilter(
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new ZeroTrustRequestAuditFilter(eventPublisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    TenantPolicyEnforcementFilter tenantPolicyEnforcementFilter(
            ZeroTrustProperties properties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<ZeroTrustPolicyEvaluator> policyEvaluatorProvider,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new TenantPolicyEnforcementFilter(
                properties,
                objectMapper,
                eventPublisher,
                policyEvaluatorProvider.getIfAvailable(),
                serviceName
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zero-trust.rate-limit", name = "enabled", havingValue = "true")
    RateLimitFilter rateLimitFilter(
            ZeroTrustProperties properties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new RateLimitFilter(properties, objectMapper, eventPublisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustTokenResolver zeroTrustTokenResolver(
            ZeroTrustProperties properties,
            ObjectProvider<VaultStyleSecretResolver> secretResolverProvider
    ) {
        return new ZeroTrustTokenResolver(properties, secretResolverProvider.getIfAvailable(() -> new VaultStyleSecretResolver(() -> null)));
    }

    @Bean
    @ConditionalOnMissingBean
    ZeroTrustRestClientBuilderConfigurer zeroTrustRestClientBuilderConfigurer(
            ZeroTrustTokenResolver tokenResolver,
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        return new ZeroTrustRestClientBuilderConfigurer(tokenResolver, eventPublisher, serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    BeanPostProcessor zeroTrustRestClientBuilderPostProcessor(ZeroTrustRestClientBuilderConfigurer configurer) {
        return new ZeroTrustRestClientBuilderPostProcessor(configurer);
    }

    @Bean
    SecurityFilterChain zeroTrustSecurityFilterChain(
            HttpSecurity http,
            ZeroTrustProperties properties,
            JwtDecoder jwtDecoder,
            ZeroTrustJwtAuthenticationConverter jwtAuthenticationConverter,
            CorrelationIdFilter correlationIdFilter,
            ZeroTrustRequestAuditFilter zeroTrustRequestAuditFilter,
            TenantPolicyEnforcementFilter tenantPolicyEnforcementFilter,
            ZeroTrustAuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            ObjectProvider<CorsConfigurationSource> corsConfigurationSourceProvider,
            ObjectProvider<RateLimitFilter> rateLimitFilterProvider
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.headers(headers -> headers
                .httpStrictTransportSecurity(Customizer.withDefaults())
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
        );
        CorsConfigurationSource corsConfigurationSource = corsConfigurationSourceProvider.getIfAvailable();
        if (corsConfigurationSource != null) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        }
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );
        http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(jwtAuthenticationConverter)
        ));
        http.authorizeHttpRequests(authz -> {
            properties.getInbound().getPublicPaths().forEach(path -> authz.requestMatchers(path).permitAll());
            properties.getInbound().getAdminPaths().forEach(path -> authz.requestMatchers(path).hasRole("ADMIN"));
            properties.getInbound().getServicePaths().forEach(path -> authz.requestMatchers(path).hasRole("SERVICE"));
            authz.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            authz.anyRequest().authenticated();
        });
        http.addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(zeroTrustRequestAuditFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantPolicyEnforcementFilter, BearerTokenAuthenticationFilter.class);
        RateLimitFilter rateLimitFilter = rateLimitFilterProvider.getIfAvailable();
        if (rateLimitFilter != null) {
            http.addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "zero-trust.inbound.cors", name = "allowed-origins")
    CorsConfigurationSource zeroTrustCorsConfigurationSource(ZeroTrustProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(false);
        configuration.setAllowedOrigins(properties.getInbound().getCors().getAllowedOrigins());
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
