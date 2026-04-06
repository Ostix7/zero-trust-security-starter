package ostashko.diploma.zerotrust.security.auth;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ZeroTrustAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR = new OAuth2Error("invalid_token", "Required audience is missing.", null);

    private final List<String> requiredAudiences;

    public ZeroTrustAudienceValidator(List<String> requiredAudiences) {
        this.requiredAudiences = List.copyOf(requiredAudiences);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        boolean valid = token.getAudience().stream().anyMatch(requiredAudiences::contains);
        return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(ERROR);
    }
}
