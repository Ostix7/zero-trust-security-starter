package ostashko.diploma.zerotrust.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.core.web.SecurityErrorResponse;
import ostashko.diploma.zerotrust.policy.engine.PolicyDecision;
import ostashko.diploma.zerotrust.policy.engine.PolicyEvaluationContext;
import ostashko.diploma.zerotrust.policy.engine.ZeroTrustPolicyEvaluator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantPolicyEnforcementFilter extends OncePerRequestFilter {

    private final ZeroTrustProperties properties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ZeroTrustPolicyEvaluator policyEvaluator;
    private final String serviceName;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantPolicyEnforcementFilter(
            ZeroTrustProperties properties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            ZeroTrustPolicyEvaluator policyEvaluator,
            String serviceName
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.policyEvaluator = policyEvaluator;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldCheckPolicies(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (shouldCheckTenant(request.getRequestURI())
                && tenantPolicyDenied(request, response, authentication, jwtAuthenticationToken)) {
            return;
        }
        if (shouldCheckExternalPolicy(request.getRequestURI())
                && externalPolicyDenied(request, response, authentication, jwtAuthenticationToken)) {
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean tenantPolicyDenied(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            JwtAuthenticationToken jwtAuthenticationToken
    ) throws IOException {
        String tenantHeader = request.getHeader(properties.getInbound().getTenant().getHeaderName());
        String tenantClaim = jwtAuthenticationToken.getToken().getClaimAsString(properties.getInbound().getTenant().getClaimName());
        if (StringUtils.hasText(tenantHeader) && StringUtils.hasText(tenantClaim) && tenantClaim.equals(tenantHeader)) {
            return false;
        }
        deny(
                response,
                authentication,
                request,
                403,
                "TENANT_POLICY_DENIED",
                "Tenant context does not match the authenticated principal",
                "tenant policy mismatch"
        );
        return true;
    }

    private boolean externalPolicyDenied(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            JwtAuthenticationToken jwtAuthenticationToken
    ) throws IOException {
        if (policyEvaluator == null) {
            return false;
        }
        PolicyDecision decision = policyEvaluator.evaluate(new PolicyEvaluationContext(
                serviceName,
                authentication.getName(),
                request.getRequestURI(),
                request.getMethod(),
                SecurityEventSupport.authorities(authentication),
                new LinkedHashMap<>(jwtAuthenticationToken.getToken().getClaims()),
                extractHeaders(request)
        ));
        if (decision.allowed()) {
            return false;
        }
        if (decision.error() && !properties.getInbound().getPolicy().isFailClosed()) {
            return false;
        }
        int status = decision.error() ? 503 : 403;
        String error = decision.error() ? "POLICY_EVALUATION_FAILED" : "POLICY_DENIED";
        String message = decision.error()
                ? "External policy engine is unavailable"
                : "External policy denied the request";
        deny(response, authentication, request, status, error, message, decision.reason());
        return true;
    }

    private void deny(
            HttpServletResponse response,
            Authentication authentication,
            HttpServletRequest request,
            int status,
            String error,
            String message,
            String reason
    ) throws IOException {
        SecurityEventSupport.publish(
                eventPublisher,
                ZeroTrustSecurityEventType.AUTHORIZATION_DENIED,
                serviceName,
                authentication.getName(),
                request.getMethod(),
                request.getRequestURI(),
                "DENY",
                reason,
                SecurityEventSupport.authorities(authentication)
        );
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new SecurityErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                request.getRequestURI(),
                CorrelationContextHolder.getCorrelationId()
        ));
    }

    private boolean shouldCheckPolicies(String path) {
        return shouldCheckTenant(path) || shouldCheckExternalPolicy(path);
    }

    private boolean shouldCheckTenant(String path) {
        ZeroTrustProperties.Tenant tenant = properties.getInbound().getTenant();
        return tenant.isEnabled() && tenant.getProtectedPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean shouldCheckExternalPolicy(String path) {
        ZeroTrustProperties.Policy policy = properties.getInbound().getPolicy();
        return policy.isEnabled() && policy.getProtectedPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        java.util.Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }
}
