package ostashko.diploma.zerotrust.demo.gateway;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class GatewayProxyController {

    private final RestClient resourceRestClient;

    public GatewayProxyController(RestClient resourceRestClient) {
        this.resourceRestClient = resourceRestClient;
    }

    @GetMapping("/api/proxy/me")
    public ResponseEntity<String> proxyMe() {
        return proxy("/api/me");
    }

    @GetMapping("/api/proxy/admin-report")
    public ResponseEntity<String> proxyAdminReport() {
        return proxy("/admin/report");
    }

    @GetMapping("/api/proxy/audit-events")
    public ResponseEntity<String> proxyAuditEvents() {
        return proxy("/admin/audit/events");
    }

    @GetMapping("/api/proxy/finance-report")
    public ResponseEntity<String> proxyFinanceReport() {
        return proxy("/api/policy/finance-report");
    }

    @GetMapping("/api/proxy/tenant-orders")
    public ResponseEntity<String> proxyTenantOrders(HttpServletRequest request) {
        return proxy("/tenant/orders", tenantHeaders(request));
    }

    @GetMapping("/public/service-ping")
    public ResponseEntity<String> proxyServicePing() {
        return proxy("/internal/service-ping");
    }

    private ResponseEntity<String> proxy(String uri) {
        return proxy(uri, Map.of());
    }

    private ResponseEntity<String> proxy(String uri, Map<String, String> headers) {
        return resourceRestClient.get()
                .uri(uri)
                .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                })
                .toEntity(String.class);
    }

    private Map<String, String> tenantHeaders(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            return Map.of();
        }
        return Map.of("X-Tenant-Id", tenantId);
    }
}
