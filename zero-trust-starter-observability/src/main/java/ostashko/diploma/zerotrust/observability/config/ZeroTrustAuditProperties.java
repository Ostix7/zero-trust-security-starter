package ostashko.diploma.zerotrust.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zero-trust.audit")
public class ZeroTrustAuditProperties {

    private boolean enabled = true;

    private boolean logSuccess = true;

    private boolean logFailures = true;

    private boolean inMemoryStore = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogSuccess() {
        return logSuccess;
    }

    public void setLogSuccess(boolean logSuccess) {
        this.logSuccess = logSuccess;
    }

    public boolean isLogFailures() {
        return logFailures;
    }

    public void setLogFailures(boolean logFailures) {
        this.logFailures = logFailures;
    }

    public boolean isInMemoryStore() {
        return inMemoryStore;
    }

    public void setInMemoryStore(boolean inMemoryStore) {
        this.inMemoryStore = inMemoryStore;
    }
}
