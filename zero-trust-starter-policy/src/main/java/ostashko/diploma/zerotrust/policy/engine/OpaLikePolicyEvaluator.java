package ostashko.diploma.zerotrust.policy.engine;

import java.util.Map;
import org.springframework.web.client.RestClient;

public class OpaLikePolicyEvaluator implements ZeroTrustPolicyEvaluator {

    private final RestClient restClient;
    private final String endpoint;

    public OpaLikePolicyEvaluator(RestClient restClient, String endpoint) {
        this.restClient = restClient;
        this.endpoint = endpoint;
    }

    @Override
    public PolicyDecision evaluate(PolicyEvaluationContext context) {
        try {
            OpaLikeResponse response = restClient.post()
                    .uri(endpoint)
                    .body(Map.of("input", context))
                    .retrieve()
                    .body(OpaLikeResponse.class);
            if (response == null) {
                return PolicyDecision.error("policy engine returned no response");
            }
            return response.allow()
                    ? PolicyDecision.allow(response.reason())
                    : PolicyDecision.deny(response.reason() == null ? "external policy denied the request" : response.reason());
        } catch (RuntimeException exception) {
            return PolicyDecision.error("policy engine error: " + exception.getMessage());
        }
    }

    public record OpaLikeResponse(boolean allow, String reason) {
    }
}
