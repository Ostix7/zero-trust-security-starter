package ostashko.diploma.zerotrust.policy.engine;

public interface ZeroTrustPolicyEvaluator {

    PolicyDecision evaluate(PolicyEvaluationContext context);
}
