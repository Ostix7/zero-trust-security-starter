package ostashko.diploma.zerotrust.security.outbound;

import java.util.Optional;
import ostashko.diploma.zerotrust.core.config.OutboundTokenMode;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.secrets.resolver.VaultStyleSecretResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

public class ZeroTrustTokenResolver {

    private final ZeroTrustProperties properties;
    private final VaultStyleSecretResolver secretResolver;

    public ZeroTrustTokenResolver(ZeroTrustProperties properties, VaultStyleSecretResolver secretResolver) {
        this.properties = properties;
        this.secretResolver = secretResolver;
    }

    public Optional<ResolvedToken> resolve() {
        Optional<String> userToken = currentUserToken();
        String serviceToken = resolveServiceToken();
        OutboundTokenMode mode = properties.getOutbound().getTokenMode();
        return switch (mode) {
            case USER_ONLY -> userToken.map(token -> new ResolvedToken(token, false));
            case USER_OR_SERVICE -> userToken
                    .map(token -> new ResolvedToken(token, false))
                    .or(() -> StringUtils.hasText(serviceToken)
                            ? Optional.of(new ResolvedToken(serviceToken, true))
                            : Optional.empty());
            case SERVICE_ONLY -> StringUtils.hasText(serviceToken)
                    ? Optional.of(new ResolvedToken(serviceToken, true))
                    : Optional.empty();
        };
    }

    private Optional<String> currentUserToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return Optional.of(jwtAuthenticationToken.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    private String resolveServiceToken() {
        if (StringUtils.hasText(properties.getOutbound().getServiceToken())) {
            return properties.getOutbound().getServiceToken();
        }
        String environmentVariable = properties.getOutbound().getServiceTokenEnvironmentVariable();
        if (StringUtils.hasText(environmentVariable)) {
            String fromEnvironment = System.getenv(environmentVariable);
            if (StringUtils.hasText(fromEnvironment)) {
                return fromEnvironment;
            }
            String fromSystemProperty = System.getProperty(environmentVariable);
            if (StringUtils.hasText(fromSystemProperty)) {
                return fromSystemProperty;
            }
        }
        return resolveFromSecretBackend();
    }

    private String resolveFromSecretBackend() {
        String path = properties.getOutbound().getServiceTokenSecretPath();
        String key = properties.getOutbound().getServiceTokenSecretKey();
        return secretResolver.resolve(path, key).orElse(null);
    }

    public record ResolvedToken(String tokenValue, boolean serviceToken) {
    }
}
