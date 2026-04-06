package ostashko.diploma.zerotrust.demo.policy;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoOpaController {

    @PostMapping("/opa/authorize")
    public Map<String, Object> authorize(@RequestBody Map<String, Object> request) {
        Object rawInput = request.get("input");
        Map<?, ?> input = rawInput instanceof Map<?, ?> map ? map : Map.of();
        Object rawPath = input.get("path");
        String path = rawPath == null ? "" : String.valueOf(rawPath);
        Object rawAuthorities = input.get("authorities");
        List<?> authorities = rawAuthorities instanceof List<?> list ? list : List.of();
        Object rawClaims = input.get("claims");
        Map<?, ?> claims = rawClaims instanceof Map<?, ?> map ? map : Map.of();

        if (path.startsWith("/api/policy/finance-report")) {
            boolean financeUser = "finance".equals(String.valueOf(claims.get("department")));
            boolean admin = authorities.stream().map(String::valueOf).anyMatch("ROLE_ADMIN"::equals);
            if (financeUser || admin) {
                return Map.of("allow", true, "reason", "finance policy passed");
            }
            return Map.of("allow", false, "reason", "finance report requires department=finance or ROLE_ADMIN");
        }
        return Map.of("allow", true, "reason", "default allow");
    }
}
