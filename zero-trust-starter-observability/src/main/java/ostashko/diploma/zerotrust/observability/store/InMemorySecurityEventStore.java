package ostashko.diploma.zerotrust.observability.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.observability.config.ZeroTrustAuditProperties;
import org.springframework.context.event.EventListener;

public class InMemorySecurityEventStore {

    private final ZeroTrustAuditProperties properties;
    private final CopyOnWriteArrayList<ZeroTrustSecurityEvent> events = new CopyOnWriteArrayList<>();

    public InMemorySecurityEventStore(ZeroTrustAuditProperties properties) {
        this.properties = properties;
    }

    @EventListener
    public void onEvent(ZeroTrustSecurityEvent event) {
        if (shouldStore(event)) {
            events.add(event);
        }
    }

    public List<ZeroTrustSecurityEvent> snapshot() {
        return List.copyOf(events);
    }

    public long countByType(ZeroTrustSecurityEventType type) {
        return events.stream().filter(event -> event.type() == type).count();
    }

    private boolean shouldStore(ZeroTrustSecurityEvent event) {
        return switch (event.type()) {
            case AUTHENTICATION_SUCCESS -> properties.isLogSuccess();
            case AUTHENTICATION_FAILURE, AUTHORIZATION_DENIED, GUARDRAIL_BLOCKED -> properties.isLogFailures();
            default -> true;
        };
    }
}
