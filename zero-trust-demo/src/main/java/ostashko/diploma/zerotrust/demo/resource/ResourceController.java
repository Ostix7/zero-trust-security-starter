package ostashko.diploma.zerotrust.demo.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import ostashko.diploma.zerotrust.security.web.CorrelationContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourceController {

    @GetMapping("/public/hello")
    public Map<String, Object> publicHello() {
        return Map.of("message", "public resource");
    }

    @GetMapping("/api/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        return payload("user endpoint", authentication);
    }

    @GetMapping("/admin/report")
    public Map<String, Object> adminReport(Authentication authentication) {
        return payload("admin report", authentication);
    }

    @GetMapping("/internal/service-ping")
    public Map<String, Object> servicePing(Authentication authentication) {
        return payload("service ping", authentication);
    }

    @GetMapping("/tenant/orders")
    public Map<String, Object> tenantOrders(Authentication authentication) {
        return payload("tenant orders", authentication);
    }

    @GetMapping("/api/policy/finance-report")
    public Map<String, Object> financeReport(Authentication authentication) {
        return payload("finance report", authentication);
    }

    private Map<String, Object> payload(String label, Authentication authentication) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", label);
        payload.put("principal", authentication.getName());
        payload.put("authorities", authentication.getAuthorities().stream().map(Object::toString).toList());
        payload.put("correlationId", CorrelationContextHolder.getCorrelationId());
        payload.put("tenantId", claim(authentication, "tenant_id"));
        payload.put("department", claim(authentication, "department"));
        return payload;
    }

    private Object claim(Authentication authentication, String claimName) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getClaims().get(claimName);
        }
        return null;
    }
}
