package ostashko.diploma.zerotrust.security.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ZeroTrustJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ZeroTrustProperties properties;

    public ZeroTrustJwtAuthenticationConverter(ZeroTrustProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(readAuthorities(jwt, properties.getInbound().getJwt().getRolesClaim(), "ROLE_"));
        authorities.addAll(readAuthorities(jwt, "scope", "SCOPE_"));
        authorities.addAll(readAuthorities(jwt, "scp", "SCOPE_"));
        String principalClaim = properties.getInbound().getJwt().getPrincipalClaim();
        String principal = jwt.getClaimAsString(principalClaim);
        if (principal == null || principal.isBlank()) {
            principal = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> readAuthorities(Jwt jwt, String claimName, String prefix) {
        Object raw = jwt.getClaims().get(claimName);
        if (raw == null) {
            return List.of();
        }
        Collection<String> values;
        if (raw instanceof String stringValue) {
            values = List.of(stringValue.split("[,\\s]+"));
        } else if (raw instanceof Collection<?> collection) {
            values = collection.stream().filter(Objects::nonNull).map(Object::toString).toList();
        } else {
            values = List.of(raw.toString());
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.startsWith(prefix) ? value : prefix + value)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
