package ostashko.diploma.zerotrust.policy.engine;

public record PolicyDecision(boolean allowed, boolean error, String reason) {

    public static PolicyDecision allow(String reason) {
        return new PolicyDecision(true, false, reason);
    }

    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(false, false, reason);
    }

    public static PolicyDecision error(String reason) {
        return new PolicyDecision(false, true, reason);
    }
}
