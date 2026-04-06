package ostashko.diploma.zerotrust.observability.support;

import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

public class ZeroTrustAuditLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(ZeroTrustAuditLoggingListener.class);

    @EventListener
    public void onEvent(ZeroTrustSecurityEvent event) {
        log.info(
                "zero-trust-event type={} service={} principal={} method={} path={} outcome={} reason={} correlationId={} authorities={}",
                event.type(),
                event.serviceName(),
                event.principal(),
                event.method(),
                event.path(),
                event.outcome(),
                event.reason(),
                event.correlationId(),
                event.authorities()
        );
    }
}
