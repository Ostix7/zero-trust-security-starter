package ostashko.diploma.zerotrust.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import ostashko.diploma.zerotrust.core.config.ZeroTrustProperties;
import ostashko.diploma.zerotrust.core.event.ZeroTrustSecurityEventType;
import ostashko.diploma.zerotrust.core.web.SecurityErrorResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private final int requestsPerSecond;
    private final int burstCapacity;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final String serviceName;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            ZeroTrustProperties properties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            String serviceName
    ) {
        this.requestsPerSecond = properties.getRateLimit().getRequestsPerSecond();
        this.burstCapacity = properties.getRateLimit().getBurstCapacity();
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = resolveKey(request);
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(burstCapacity, requestsPerSecond));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SecurityEventSupport.publish(
                    eventPublisher,
                    ZeroTrustSecurityEventType.RATE_LIMIT_EXCEEDED,
                    serviceName,
                    key,
                    request.getMethod(),
                    request.getRequestURI(),
                    "DENY",
                    "Rate limit exceeded",
                    SecurityEventSupport.authorities(authentication)
            );
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "1");
            objectMapper.writeValue(response.getWriter(), new SecurityErrorResponse(
                    Instant.now().toString(),
                    429,
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests",
                    request.getRequestURI(),
                    CorrelationContextHolder.getCorrelationId()
            ));
        }
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static final class TokenBucket {

        private final int capacity;
        private final int refillRate;
        private final AtomicLong tokens;
        private volatile long lastRefillNanos;

        TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillNanos = System.nanoTime();
        }

        boolean tryConsume() {
            refill();
            long current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            long tokensToAdd = (elapsed * refillRate) / 1_000_000_000L;
            if (tokensToAdd > 0) {
                lastRefillNanos = now;
                tokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
            }
        }
    }
}
