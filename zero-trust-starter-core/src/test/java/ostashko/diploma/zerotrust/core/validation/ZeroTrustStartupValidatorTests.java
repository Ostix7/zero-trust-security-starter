package ostashko.diploma.zerotrust.core.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ZeroTrustStartupValidatorTests {

    @Test
    void rejectsMissingJwtConfiguration() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .hasMessageContaining("shared-secret or zero-trust.inbound.jwt.issuer-uri");
    }

    @Test
    void rejectsWildcardPublicPath() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");
        properties.getInbound().getJwt().setSharedSecret("01234567890123456789012345678901");
        properties.getInbound().setPublicPaths(List.of("/**"));

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .hasMessageContaining("rejected '/**' as a public path");
    }

    @Test
    void rejectsPermissiveCorsInStrictMode() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");
        properties.getInbound().getJwt().setSharedSecret("01234567890123456789012345678901");
        properties.getInbound().getCors().setAllowedOrigins(List.of("*"));

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .hasMessageContaining("rejected '*' in CORS allowed origins");
    }

    @Test
    void rejectsServiceOnlyModeWithoutServiceTokenSource() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");
        properties.getInbound().getJwt().setSharedSecret("01234567890123456789012345678901");
        properties.getOutbound().setTokenMode(ostashko.diploma.zerotrust.core.config.OutboundTokenMode.SERVICE_ONLY);

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .hasMessageContaining("SERVICE_ONLY outbound mode requires");
    }

    @Test
    void acceptsServiceOnlyModeWithVaultStyleSecretPath() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");
        properties.getInbound().getJwt().setSharedSecret("01234567890123456789012345678901");
        properties.getOutbound().setTokenMode(ostashko.diploma.zerotrust.core.config.OutboundTokenMode.SERVICE_ONLY);
        properties.getOutbound().setServiceTokenSecretPath("secret/services/gateway");

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatCode(validator::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void rejectsEnabledPolicyWithoutEndpoint() {
        ZeroTrustProperties properties = new ZeroTrustProperties();
        properties.getInbound().getJwt().setIssuer("https://issuer");
        properties.getInbound().getJwt().setSharedSecret("01234567890123456789012345678901");
        properties.getInbound().getPolicy().setEnabled(true);
        properties.getInbound().getPolicy().setProtectedPaths(List.of("/api/policy/**"));

        ZeroTrustStartupValidator validator = new ZeroTrustStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .hasMessageContaining("zero-trust.inbound.policy.endpoint");
    }
}
