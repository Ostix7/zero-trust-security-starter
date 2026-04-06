package ostashko.diploma.zerotrust.core.validation;

import java.util.Objects;
import ostashko.diploma.zerotrust.core.config.OutboundTokenMode;
import ostashko.diploma.zerotrust.core.config.ZeroTrustMode;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class ZeroTrustStartupValidator implements SmartInitializingSingleton {

    private final ZeroTrustProperties properties;
    private final Environment environment;

    public ZeroTrustStartupValidator(ZeroTrustProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validateInboundJwt();
        validatePublicPaths();
        validateCors();
        validateOutboundMode();
        validatePolicy();
        validateRateLimit();
    }

    private void validateInboundJwt() {
        if (!properties.getInbound().isEnabled() || !properties.getGuardrails().isFailOnMissingJwtConfig()) {
            return;
        }
        ZeroTrustProperties.Jwt jwt = properties.getInbound().getJwt();
        boolean hasSharedSecret = StringUtils.hasText(jwt.getSharedSecret());
        boolean hasIssuerUri = StringUtils.hasText(jwt.getIssuerUri());
        if (!hasSharedSecret && !hasIssuerUri) {
            throw new IllegalStateException("Zero Trust requires either zero-trust.inbound.jwt.shared-secret or zero-trust.inbound.jwt.issuer-uri.");
        }
        if (!StringUtils.hasText(jwt.getIssuer())) {
            throw new IllegalStateException("Zero Trust requires zero-trust.inbound.jwt.issuer for issuer validation.");
        }
    }

    private void validatePublicPaths() {
        if (!properties.getGuardrails().isFailOnWildcardPublicPaths()) {
            return;
        }
        boolean wildcardPublic = properties.getInbound().getPublicPaths().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch("/**"::equals);
        if (wildcardPublic) {
            throw new IllegalStateException("Zero Trust guardrail rejected '/**' as a public path.");
        }
    }

    private void validateCors() {
        if (!properties.getGuardrails().isFailOnPermissiveCors()) {
            return;
        }
        boolean permissive = properties.getInbound().getCors().getAllowedOrigins().stream()
                .anyMatch("*"::equals);
        if (permissive && properties.getMode() == ZeroTrustMode.STRICT) {
            throw new IllegalStateException("Zero Trust guardrail rejected '*' in CORS allowed origins for STRICT mode.");
        }
    }

    private void validateOutboundMode() {
        if (!properties.getOutbound().isEnabled()) {
            return;
        }
        if (properties.getOutbound().getTokenMode() == OutboundTokenMode.SERVICE_ONLY
                && !StringUtils.hasText(properties.getOutbound().getServiceToken())
                && !StringUtils.hasText(properties.getOutbound().getServiceTokenEnvironmentVariable())
                && !StringUtils.hasText(properties.getOutbound().getServiceTokenSecretPath())) {
            throw new IllegalStateException("SERVICE_ONLY outbound mode requires zero-trust.outbound.service-token, zero-trust.outbound.service-token-environment-variable, or zero-trust.outbound.service-token-secret-path.");
        }
    }

    private void validateRateLimit() {
        ZeroTrustProperties.RateLimit rateLimit = properties.getRateLimit();
        if (!rateLimit.isEnabled()) {
            return;
        }
        if (rateLimit.getRequestsPerSecond() <= 0) {
            throw new IllegalStateException("Zero Trust rate-limit requires a positive zero-trust.rate-limit.requests-per-second value.");
        }
        if (rateLimit.getBurstCapacity() < rateLimit.getRequestsPerSecond()) {
            throw new IllegalStateException("Zero Trust rate-limit burst-capacity must be >= requests-per-second.");
        }
    }

    private void validatePolicy() {
        ZeroTrustProperties.Policy policy = properties.getInbound().getPolicy();
        if (!policy.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(policy.getEndpoint())) {
            throw new IllegalStateException("Zero Trust policy integration requires zero-trust.inbound.policy.endpoint.");
        }
        if (policy.getProtectedPaths().isEmpty()) {
            throw new IllegalStateException("Zero Trust policy integration requires at least one zero-trust.inbound.policy.protected-paths value.");
        }
    }
}
