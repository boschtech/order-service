package com.boschtech.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token-bucket rate limiter, applied per client IP.
 *
 * <p>The limit defaults to 100 requests per minute and can be overridden with the
 * {@code app.security.rate-limit.requests-per-minute} property (env var {@code RATE_LIMIT_RPM}),
 * which CI raises because every Playwright worker shares a single client IP.
 *
 * <p>Actuator endpoints are never rate limited so that health polling does not consume
 * the API budget.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int DEFAULT_MAX_TOKENS = 100;
    private static final long REFILL_INTERVAL_MS = 60_000;
    private static final String ACTUATOR_PATH_PREFIX = "/actuator";

    private final int maxTokens;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Creates a filter using the default limit of 100 requests per minute per client IP.
     */
    public RateLimitFilter() {
        this(DEFAULT_MAX_TOKENS);
    }

    @Autowired
    public RateLimitFilter(
            @Value("${app.security.rate-limit.requests-per-minute:100}") int requestsPerMinute) {
        this.maxTokens = requestsPerMinute > 0 ? requestsPerMinute : DEFAULT_MAX_TOKENS;
    }

    /**
     * @return the configured number of requests allowed per minute per client IP
     */
    public int getRequestsPerMinute() {
        return maxTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isActuatorRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(maxTokens));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again later.\"}");
        }
    }

    private boolean isActuatorRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(ACTUATOR_PATH_PREFIX);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final AtomicLong tokens;
        private volatile long lastRefillTime = System.currentTimeMillis();

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicLong(maxTokens);
        }

        boolean tryConsume() {
            refillIfNeeded();
            return tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) > 0;
        }

        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTime >= REFILL_INTERVAL_MS) {
                tokens.set(maxTokens);
                lastRefillTime = now;
            }
        }
    }
}
