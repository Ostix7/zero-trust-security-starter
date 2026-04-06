package ostashko.diploma.zerotrust.observability.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import org.springframework.context.event.EventListener;

public class ZeroTrustMetricsListener {

    private final MeterRegistry meterRegistry;

    public ZeroTrustMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void onEvent(ZeroTrustSecurityEvent event) {
        Counter.builder("zero.trust.events")
                .description("Count of Zero Trust security events")
                .tag("type", event.type().name())
                .tag("service", event.serviceName() == null ? "unknown" : event.serviceName())
                .register(meterRegistry)
                .increment();
    }
}
