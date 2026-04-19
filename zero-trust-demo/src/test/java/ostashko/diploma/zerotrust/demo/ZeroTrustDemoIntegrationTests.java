package ostashko.diploma.zerotrust.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.demo.gateway.GatewayServiceApplication;
import ostashko.diploma.zerotrust.demo.policy.PolicyServiceApplication;
import ostashko.diploma.zerotrust.demo.resource.ResourceServiceApplication;
import ostashko.diploma.zerotrust.demo.support.DemoJwtFactory;
import ostashko.diploma.zerotrust.demo.support.HttpTestClient;
import ostashko.diploma.zerotrust.observability.store.InMemorySecurityEventStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

class ZeroTrustDemoIntegrationTests {

    private static final String ISSUER = "https://issuer.diploma.local";
    private static final String AUDIENCE = "diploma-services";
    private static final String SECRET = "01234567890123456789012345678901";

    private static ConfigurableApplicationContext policyContext;
    private static ConfigurableApplicationContext resourceContext;
    private static ConfigurableApplicationContext gatewayContext;
    private static HttpTestClient policyClient;
    private static HttpTestClient resourceClient;
    private static HttpTestClient gatewayClient;

    @BeforeAll
    static void startApplications() {
        policyContext = new SpringApplicationBuilder(PolicyServiceApplication.class)
                .properties(
                        "server.port=0",
                        "spring.application.name=policy-service",
                        "zero-trust.enabled=false",
                        "zero-trust.inbound.enabled=false"
                )
                .run();
        int policyPort = ((ServletWebServerApplicationContext) policyContext).getWebServer().getPort();
        policyClient = new HttpTestClient("http://localhost:" + policyPort);

        String serviceToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "gateway-service", List.of("SERVICE"));
        resourceContext = new SpringApplicationBuilder(ResourceServiceApplication.class)
                .properties(commonProperties("resource-service", policyClient.baseUrl()))
                .properties(
                        "server.port=0",
                        "zero-trust.audit.in-memory-store=true"
                )
                .run();
        int resourcePort = ((ServletWebServerApplicationContext) resourceContext).getWebServer().getPort();
        resourceClient = new HttpTestClient("http://localhost:" + resourcePort);

        gatewayContext = new SpringApplicationBuilder(GatewayServiceApplication.class)
                .properties(commonProperties("gateway-service", policyClient.baseUrl()))
                .properties(
                        "server.port=0",
                        "demo.resource.base-url=http://localhost:" + resourcePort,
                        "zero-trust.outbound.service-token=" + serviceToken,
                        "zero-trust.audit.in-memory-store=true"
                )
                .run();
        int gatewayPort = ((ServletWebServerApplicationContext) gatewayContext).getWebServer().getPort();
        gatewayClient = new HttpTestClient("http://localhost:" + gatewayPort);
    }

    @AfterAll
    static void stopApplications() {
        if (gatewayContext != null) {
            gatewayContext.close();
        }
        if (resourceContext != null) {
            resourceContext.close();
        }
        if (policyContext != null) {
            policyContext.close();
        }
    }

    @Test
    void publicEndpointIsAccessibleWithoutToken() {
        HttpTestClient.HttpResult response = resourceClient.get("/public/hello", null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(resourceClient.json(response)).containsEntry("message", "public resource");
        assertThat(response.header("x-correlation-id")).isNotBlank();
    }

    @Test
    void protectedEndpointRequiresAuthentication() {
        HttpTestClient.HttpResult response = resourceClient.get("/api/me", null);

        assertThat(response.status()).isEqualTo(401);
        assertThat(resourceClient.json(response)).containsEntry("error", "UNAUTHORIZED");
    }

    @Test
    void adminEndpointRejectsUserWithoutAdminRole() {
        String userToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "alice", List.of("USER"));

        HttpTestClient.HttpResult response = resourceClient.get("/admin/report", userToken);

        assertThat(response.status()).isEqualTo(403);
        assertThat(resourceClient.json(response)).containsEntry("error", "ACCESS_DENIED");
    }

    @Test
    void adminEndpointAcceptsAdminRole() {
        String adminToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "admin", List.of("ADMIN"));

        HttpTestClient.HttpResult response = resourceClient.get("/admin/report", adminToken);

        assertThat(response.status()).isEqualTo(200);
        assertThat(resourceClient.json(response)).containsEntry("principal", "admin");
    }

    @Test
    void gatewayPropagatesUserTokenToResourceService() {
        String userToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "bob", List.of("USER"));

        HttpTestClient.HttpResult response = gatewayClient.get("/api/proxy/me", userToken);

        assertThat(response.status()).isEqualTo(200);
        assertThat(gatewayClient.json(response)).containsEntry("principal", "bob");
        assertThat(gatewayClient.json(response)).containsEntry("correlationId", response.header("x-correlation-id"));

        InMemorySecurityEventStore gatewayEvents = gatewayContext.getBean(InMemorySecurityEventStore.class);
        assertThat(gatewayEvents.countByType(ZeroTrustSecurityEventType.OUTBOUND_TOKEN_PROPAGATED)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void gatewayPropagatesUserTokenViaWebClient() {
        String userToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "bob-webclient", List.of("USER"));

        HttpTestClient.HttpResult response = gatewayClient.get("/api/proxy-webclient/me", userToken);

        assertThat(response.status()).isEqualTo(200);
        assertThat(gatewayClient.json(response)).containsEntry("principal", "bob-webclient");

        InMemorySecurityEventStore gatewayEvents = gatewayContext.getBean(InMemorySecurityEventStore.class);
        assertThat(gatewayEvents.countByType(ZeroTrustSecurityEventType.OUTBOUND_TOKEN_PROPAGATED)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void gatewayUsesServiceTokenForPublicServiceCall() {
        HttpTestClient.HttpResult response = gatewayClient.get("/public/service-ping", null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(gatewayClient.json(response)).containsEntry("principal", "gateway-service");

        InMemorySecurityEventStore gatewayEvents = gatewayContext.getBean(InMemorySecurityEventStore.class);
        assertThat(gatewayEvents.countByType(ZeroTrustSecurityEventType.SERVICE_TOKEN_USED)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void tenantPolicyAllowsMatchingTenantHeader() {
        String tenantToken = DemoJwtFactory.token(
                ISSUER,
                AUDIENCE,
                SECRET,
                "tenant-user",
                List.of("USER"),
                Map.of("tenant_id", "acme")
        );

        HttpTestClient.HttpResult response = resourceClient.get(
                "/tenant/orders",
                tenantToken,
                Map.of("X-Tenant-Id", "acme")
        );

        assertThat(response.status()).isEqualTo(200);
        assertThat(resourceClient.json(response)).containsEntry("principal", "tenant-user");
    }

    @Test
    void tenantPolicyRejectsMismatchedTenantHeader() {
        String tenantToken = DemoJwtFactory.token(
                ISSUER,
                AUDIENCE,
                SECRET,
                "tenant-user",
                List.of("USER"),
                Map.of("tenant_id", "acme")
        );

        HttpTestClient.HttpResult response = resourceClient.get(
                "/tenant/orders",
                tenantToken,
                Map.of("X-Tenant-Id", "other")
        );

        assertThat(response.status()).isEqualTo(403);
        assertThat(resourceClient.json(response)).containsEntry("error", "TENANT_POLICY_DENIED");
        InMemorySecurityEventStore resourceEvents = resourceContext.getBean(InMemorySecurityEventStore.class);
        assertThat(resourceEvents.countByType(ZeroTrustSecurityEventType.AUTHORIZATION_DENIED)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void gatewayPropagatesTenantContextToResourceService() {
        String tenantToken = DemoJwtFactory.token(
                ISSUER,
                AUDIENCE,
                SECRET,
                "tenant-proxy-user",
                List.of("USER"),
                Map.of("tenant_id", "acme")
        );

        HttpTestClient.HttpResult response = gatewayClient.get(
                "/api/proxy/tenant-orders",
                tenantToken,
                Map.of("X-Tenant-Id", "acme")
        );

        assertThat(response.status()).isEqualTo(200);
        assertThat(gatewayClient.json(response)).containsEntry("tenantId", "acme");
    }

    @Test
    void externalPolicyAllowsFinanceDepartment() {
        String financeToken = DemoJwtFactory.token(
                ISSUER,
                AUDIENCE,
                SECRET,
                "fin-user",
                List.of("USER"),
                Map.of("department", "finance")
        );

        HttpTestClient.HttpResult response = gatewayClient.get("/api/proxy/finance-report", financeToken);

        assertThat(response.status()).isEqualTo(200);
        assertThat(gatewayClient.json(response)).containsEntry("department", "finance");
    }

    @Test
    void externalPolicyRejectsUnauthorizedDepartment() {
        String salesToken = DemoJwtFactory.token(
                ISSUER,
                AUDIENCE,
                SECRET,
                "sales-user",
                List.of("USER"),
                Map.of("department", "sales")
        );

        HttpTestClient.HttpResult response = gatewayClient.get("/api/proxy/finance-report", salesToken);

        assertThat(response.status()).isEqualTo(403);
        assertThat(gatewayClient.json(response)).containsEntry("error", "POLICY_DENIED");
    }

    @Test
    void policyEngineFailureFailsClosedForProtectedEndpoint() {
        try (ConfigurableApplicationContext brokenPolicyResourceContext = new SpringApplicationBuilder(ResourceServiceApplication.class)
                .properties(commonProperties("resource-fail-closed", "http://localhost:65530"))
                .properties(
                        "server.port=0",
                        "zero-trust.inbound.policy.fail-closed=true"
                )
                .run()) {
            int port = ((ServletWebServerApplicationContext) brokenPolicyResourceContext).getWebServer().getPort();
            HttpTestClient client = new HttpTestClient("http://localhost:" + port);
            String financeToken = DemoJwtFactory.token(
                    ISSUER,
                    AUDIENCE,
                    SECRET,
                    "fin-user",
                    List.of("USER"),
                    Map.of("department", "finance")
            );

            HttpTestClient.HttpResult response = client.get("/api/policy/finance-report", financeToken);

            assertThat(response.status()).isEqualTo(503);
            assertThat(client.json(response)).containsEntry("error", "POLICY_EVALUATION_FAILED");
        }
    }

    @Test
    void policyEngineFailureCanFailOpenWhenConfigured() {
        try (ConfigurableApplicationContext failOpenResourceContext = new SpringApplicationBuilder(ResourceServiceApplication.class)
                .properties(commonProperties("resource-fail-open", "http://localhost:65530"))
                .properties(
                        "server.port=0",
                        "zero-trust.inbound.policy.fail-closed=false"
                )
                .run()) {
            int port = ((ServletWebServerApplicationContext) failOpenResourceContext).getWebServer().getPort();
            HttpTestClient client = new HttpTestClient("http://localhost:" + port);
            String financeToken = DemoJwtFactory.token(
                    ISSUER,
                    AUDIENCE,
                    SECRET,
                    "fin-user",
                    List.of("USER"),
                    Map.of("department", "finance")
            );

            HttpTestClient.HttpResult response = client.get("/api/policy/finance-report", financeToken);

            assertThat(response.status()).isEqualTo(200);
            assertThat(client.json(response)).containsEntry("department", "finance");
        }
    }

    @Test
    void adminCanReadSecurityAuditEventsThroughGateway() {
        String adminToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "security-admin", List.of("ADMIN"));
        gatewayClient.get("/api/proxy/me", adminToken);

        HttpTestClient.HttpResult response = gatewayClient.get("/api/proxy/audit-events", adminToken);

        assertThat(response.status()).isEqualTo(200);
        Map<String, Object> payload = gatewayClient.json(response);
        assertThat(payload.get("count")).isInstanceOf(Number.class);
        assertThat(((Number) payload.get("count")).intValue()).isGreaterThan(0);
        assertThat(payload.get("events")).isInstanceOf(List.class);
    }

    @Test
    void gatewayCanUseServiceTokenFromSystemPropertyFallback() {
        String serviceToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "gateway-system-property", List.of("SERVICE"));
        String propertyName = "ZT_DEMO_SERVICE_TOKEN";
        System.setProperty(propertyName, serviceToken);

        try (ConfigurableApplicationContext propertyGatewayContext = new SpringApplicationBuilder(GatewayServiceApplication.class)
                .properties(commonProperties("gateway-service-property", policyClient.baseUrl()))
                .properties(
                        "server.port=0",
                        "demo.resource.base-url=" + resourceClient.baseUrl(),
                        "zero-trust.outbound.service-token-environment-variable=" + propertyName,
                        "zero-trust.audit.in-memory-store=true"
                )
                .run()) {
            int gatewayPort = ((ServletWebServerApplicationContext) propertyGatewayContext).getWebServer().getPort();
            HttpTestClient propertyGatewayClient = new HttpTestClient("http://localhost:" + gatewayPort);

            HttpTestClient.HttpResult response = propertyGatewayClient.get("/public/service-ping", null);

            assertThat(response.status()).isEqualTo(200);
            assertThat(propertyGatewayClient.json(response)).containsEntry("principal", "gateway-system-property");
        } finally {
            System.clearProperty(propertyName);
        }
    }

    @Test
    void metricsAreRecordedForSecurityEvents() {
        String userToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "metrics-user", List.of("USER"));
        gatewayClient.get("/api/proxy/me", userToken);

        MeterRegistry meterRegistry = gatewayContext.getBean(MeterRegistry.class);
        double count = meterRegistry.get("zero.trust.events")
                .tag("type", ZeroTrustSecurityEventType.OUTBOUND_TOKEN_PROPAGATED.name())
                .counter()
                .count();

        assertThat(count).isGreaterThan(0.0d);
    }

    @Test
    void rateLimitRejectsExcessiveRequests() {
        try (ConfigurableApplicationContext rateLimitContext = new SpringApplicationBuilder(ResourceServiceApplication.class)
                .properties(commonProperties("resource-ratelimit", policyClient.baseUrl()))
                .properties(
                        "server.port=0",
                        "zero-trust.rate-limit.enabled=true",
                        "zero-trust.rate-limit.requests-per-second=2",
                        "zero-trust.rate-limit.burst-capacity=3"
                )
                .run()) {
            int port = ((ServletWebServerApplicationContext) rateLimitContext).getWebServer().getPort();
            HttpTestClient client = new HttpTestClient("http://localhost:" + port);
            String userToken = DemoJwtFactory.token(ISSUER, AUDIENCE, SECRET, "rate-limit-user", List.of("USER"));

            int rejected = 0;
            for (int i = 0; i < 10; i++) {
                HttpTestClient.HttpResult response = client.get("/api/me", userToken);
                if (response.status() == 429) {
                    rejected++;
                }
            }

            assertThat(rejected).isGreaterThan(0);
        }
    }

    @Test
    void startupFailsWithoutJwtConfiguration() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(ResourceServiceApplication.class)
                .properties(
                        "server.port=0",
                        "spring.application.name=broken-resource",
                        "zero-trust.enabled=true",
                        "zero-trust.inbound.enabled=true",
                        "zero-trust.inbound.jwt.issuer=" + ISSUER
                )
                .run()
        ).hasMessageContaining("shared-secret or zero-trust.inbound.jwt.issuer-uri");
    }

    private static String[] commonProperties(String applicationName, String policyBaseUrl) {
        return new String[]{
                "spring.application.name=" + applicationName,
                "management.endpoints.web.exposure.include=health,metrics",
                "zero-trust.enabled=true",
                "zero-trust.mode=STRICT",
                "zero-trust.inbound.enabled=true",
                "zero-trust.inbound.jwt.issuer=" + ISSUER,
                "zero-trust.inbound.jwt.shared-secret=" + SECRET,
                "zero-trust.inbound.jwt.audiences[0]=" + AUDIENCE,
                "zero-trust.inbound.public-paths[0]=/public/**",
                "zero-trust.inbound.public-paths[1]=/actuator/health",
                "zero-trust.inbound.admin-paths[0]=/admin/**",
                "zero-trust.inbound.service-paths[0]=/internal/**",
                "zero-trust.inbound.tenant.enabled=true",
                "zero-trust.inbound.tenant.header-name=X-Tenant-Id",
                "zero-trust.inbound.tenant.claim-name=tenant_id",
                "zero-trust.inbound.tenant.protected-paths[0]=/tenant/**",
                "zero-trust.inbound.policy.enabled=true",
                "zero-trust.inbound.policy.endpoint=" + policyBaseUrl + "/opa/authorize",
                "zero-trust.inbound.policy.protected-paths[0]=/api/policy/**",
                "zero-trust.audit.enabled=true",
                "zero-trust.audit.log-success=true",
                "zero-trust.audit.log-failures=true",
                "zero-trust.outbound.enabled=true",
                "zero-trust.outbound.token-mode=USER_OR_SERVICE"
        };
    }
}
