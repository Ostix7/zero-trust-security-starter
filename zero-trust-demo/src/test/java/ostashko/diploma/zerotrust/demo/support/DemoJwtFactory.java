package ostashko.diploma.zerotrust.demo.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class DemoJwtFactory {

    private DemoJwtFactory() {
    }

    public static String token(String issuer, String audience, String secret, String subject, List<String> roles) {
        return token(issuer, audience, secret, subject, roles, Map.of());
    }

    public static String token(
            String issuer,
            String audience,
            String secret,
            String subject,
            List<String> roles,
            Map<String, Object> extraClaims
    ) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now.minusSeconds(5)))
                    .expirationTime(Date.from(now.plusSeconds(600)))
                    .claim("roles", roles);
            extraClaims.forEach(claimsBuilder::claim);
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            signedJwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to generate demo JWT", exception);
        }
    }
}
