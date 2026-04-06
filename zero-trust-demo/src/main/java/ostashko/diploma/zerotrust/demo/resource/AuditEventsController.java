package ostashko.diploma.zerotrust.demo.resource;

import java.util.List;
import java.util.Map;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import ostashko.diploma.zerotrust.observability.store.InMemorySecurityEventStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditEventsController {

    private final ObjectProvider<InMemorySecurityEventStore> eventStoreProvider;

    public AuditEventsController(ObjectProvider<InMemorySecurityEventStore> eventStoreProvider) {
        this.eventStoreProvider = eventStoreProvider;
    }

    @GetMapping("/admin/audit/events")
    public Map<String, Object> auditEvents() {
        InMemorySecurityEventStore eventStore = eventStoreProvider.getIfAvailable();
        List<ZeroTrustSecurityEvent> events = eventStore == null ? List.of() : eventStore.snapshot();
        return Map.of(
                "count", events.size(),
                "events", events
        );
    }
}
