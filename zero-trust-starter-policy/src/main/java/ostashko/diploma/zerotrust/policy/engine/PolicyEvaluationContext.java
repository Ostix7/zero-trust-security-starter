package ostashko.diploma.zerotrust.policy.engine;

import java.util.List;
import java.util.Map;

public record PolicyEvaluationContext(
        String serviceName,
        String principal,
        String path,
        String method,
        List<String> authorities,
        Map<String, Object> claims,
        Map<String, String> headers
) {
}
