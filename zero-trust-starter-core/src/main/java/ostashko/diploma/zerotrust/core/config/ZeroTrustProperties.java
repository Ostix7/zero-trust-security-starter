package ostashko.diploma.zerotrust.core.config;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zero-trust")
public class ZeroTrustProperties {

    private boolean enabled = true;

    private ZeroTrustMode mode = ZeroTrustMode.STRICT;

    @Valid
    private final Inbound inbound = new Inbound();

    @Valid
    private final Outbound outbound = new Outbound();

    @Valid
    private final RateLimit rateLimit = new RateLimit();

    @Valid
    private final Guardrails guardrails = new Guardrails();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ZeroTrustMode getMode() {
        return mode;
    }

    public void setMode(ZeroTrustMode mode) {
        this.mode = mode;
    }

    public Inbound getInbound() {
        return inbound;
    }

    public Outbound getOutbound() {
        return outbound;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Guardrails getGuardrails() {
        return guardrails;
    }

    public static class Inbound {

        private boolean enabled = true;

        private final Jwt jwt = new Jwt();

        private final Cors cors = new Cors();

        private final Tenant tenant = new Tenant();

        private final Policy policy = new Policy();

        private List<String> publicPaths = new ArrayList<>(List.of("/public/**", "/actuator/health"));

        private List<String> adminPaths = new ArrayList<>(List.of("/admin/**"));

        private List<String> servicePaths = new ArrayList<>(List.of("/internal/**"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public Cors getCors() {
            return cors;
        }

        public Tenant getTenant() {
            return tenant;
        }

        public Policy getPolicy() {
            return policy;
        }

        public List<String> getPublicPaths() {
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }

        public List<String> getAdminPaths() {
            return adminPaths;
        }

        public void setAdminPaths(List<String> adminPaths) {
            this.adminPaths = adminPaths;
        }

        public List<String> getServicePaths() {
            return servicePaths;
        }

        public void setServicePaths(List<String> servicePaths) {
            this.servicePaths = servicePaths;
        }
    }

    public static class Jwt {

        private String issuer;

        private String issuerUri;

        private String sharedSecret;

        private List<String> audiences = new ArrayList<>();

        private String rolesClaim = "roles";

        private String principalClaim = "sub";

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getSharedSecret() {
            return sharedSecret;
        }

        public void setSharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
        }

        public List<String> getAudiences() {
            return audiences;
        }

        public void setAudiences(List<String> audiences) {
            this.audiences = audiences;
        }

        public String getRolesClaim() {
            return rolesClaim;
        }

        public void setRolesClaim(String rolesClaim) {
            this.rolesClaim = rolesClaim;
        }

        public String getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = principalClaim;
        }
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Outbound {

        private boolean enabled = true;

        private OutboundTokenMode tokenMode = OutboundTokenMode.USER_OR_SERVICE;

        private String serviceToken;

        private String serviceTokenEnvironmentVariable;

        private String serviceTokenSecretPath;

        private String serviceTokenSecretKey = "token";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public OutboundTokenMode getTokenMode() {
            return tokenMode;
        }

        public void setTokenMode(OutboundTokenMode tokenMode) {
            this.tokenMode = tokenMode;
        }

        public String getServiceToken() {
            return serviceToken;
        }

        public void setServiceToken(String serviceToken) {
            this.serviceToken = serviceToken;
        }

        public String getServiceTokenEnvironmentVariable() {
            return serviceTokenEnvironmentVariable;
        }

        public void setServiceTokenEnvironmentVariable(String serviceTokenEnvironmentVariable) {
            this.serviceTokenEnvironmentVariable = serviceTokenEnvironmentVariable;
        }

        public String getServiceTokenSecretPath() {
            return serviceTokenSecretPath;
        }

        public void setServiceTokenSecretPath(String serviceTokenSecretPath) {
            this.serviceTokenSecretPath = serviceTokenSecretPath;
        }

        public String getServiceTokenSecretKey() {
            return serviceTokenSecretKey;
        }

        public void setServiceTokenSecretKey(String serviceTokenSecretKey) {
            this.serviceTokenSecretKey = serviceTokenSecretKey;
        }
    }

    public static class Tenant {

        private boolean enabled = false;

        private String headerName = "X-Tenant-Id";

        private String claimName = "tenant_id";

        private List<String> protectedPaths = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

        public List<String> getProtectedPaths() {
            return protectedPaths;
        }

        public void setProtectedPaths(List<String> protectedPaths) {
            this.protectedPaths = protectedPaths;
        }
    }

    public static class Policy {

        private boolean enabled = false;

        private String engine = "OPA";

        private String endpoint;

        private boolean failClosed = true;

        private List<String> protectedPaths = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public boolean isFailClosed() {
            return failClosed;
        }

        public void setFailClosed(boolean failClosed) {
            this.failClosed = failClosed;
        }

        public List<String> getProtectedPaths() {
            return protectedPaths;
        }

        public void setProtectedPaths(List<String> protectedPaths) {
            this.protectedPaths = protectedPaths;
        }
    }

    public static class RateLimit {

        private boolean enabled = false;

        private int requestsPerSecond = 10;

        private int burstCapacity = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    public static class Guardrails {

        private boolean failOnMissingJwtConfig = true;

        private boolean failOnWildcardPublicPaths = true;

        private boolean failOnPermissiveCors = true;

        public boolean isFailOnMissingJwtConfig() {
            return failOnMissingJwtConfig;
        }

        public void setFailOnMissingJwtConfig(boolean failOnMissingJwtConfig) {
            this.failOnMissingJwtConfig = failOnMissingJwtConfig;
        }

        public boolean isFailOnWildcardPublicPaths() {
            return failOnWildcardPublicPaths;
        }

        public void setFailOnWildcardPublicPaths(boolean failOnWildcardPublicPaths) {
            this.failOnWildcardPublicPaths = failOnWildcardPublicPaths;
        }

        public boolean isFailOnPermissiveCors() {
            return failOnPermissiveCors;
        }

        public void setFailOnPermissiveCors(boolean failOnPermissiveCors) {
            this.failOnPermissiveCors = failOnPermissiveCors;
        }
    }
}
