package com.boschtech.orderservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        // Exhaust all 100 tokens from the same IP
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }

        // 101st request should be rejected
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    void shouldUseXForwardedForWhenPresent() throws Exception {
        // Exhaust tokens for the forwarded IP
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
            req.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }

        // 101st from same forwarded IP should be rejected
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        assertEquals(429, response.getStatus());

        // Different IP should still be allowed
        MockHttpServletRequest otherIp = new MockHttpServletRequest("GET", "/api/orders");
        otherIp.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse otherResp = new MockHttpServletResponse();
        filter.doFilterInternal(otherIp, otherResp, chain);
        assertNotEquals(429, otherResp.getStatus());
    }

    @Test
    void shouldFallBackToRemoteAddrWhenXForwardedForIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("X-Forwarded-For", "  ");
        request.setRemoteAddr("**********");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldDefaultToOneHundredRequestsPerMinute() {
        assertEquals(100, new RateLimitFilter().getRequestsPerMinute());
    }

    @Test
    void shouldEnforceCustomConfiguredLimit() throws Exception {
        RateLimitFilter limitedFilter = new RateLimitFilter(3);
        String clientIp = "***********";

        assertEquals(3, limitedFilter.getRequestsPerMinute());

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            limitedFilter.doFilterInternal(req, res, chain);
            assertEquals(200, res.getStatus(), "request " + (i + 1) + " should be allowed");
        }

        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/orders");
        blocked.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(blocked, blockedRes, chain);

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    void shouldNotRateLimitActuatorEndpoints() throws Exception {
        RateLimitFilter limitedFilter = new RateLimitFilter(1);
        String clientIp = "***********";

        // Consume the single available token with an API request.
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/orders");
        apiRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse apiResponse = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(apiRequest, apiResponse, chain);
        assertEquals(200, apiResponse.getStatus());

        // Health polling must never be throttled, even from an exhausted client IP.
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/actuator/health");
            healthRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse healthResponse = new MockHttpServletResponse();
            limitedFilter.doFilterInternal(healthRequest, healthResponse, chain);

            verify(chain).doFilter(healthRequest, healthResponse);
            assertEquals(200, healthResponse.getStatus());
        }

        // API traffic from the same IP is still limited.
        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/orders");
        blocked.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(blocked, blockedRes, chain);
        assertEquals(429, blockedRes.getStatus());
    }

    @Test
    void shouldRefillTokensAfterInterval() throws Exception {
        String clientIp = "*********";

        // Exhaust all tokens
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
            req.setRemoteAddr(clientIp);
            filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }

        // Verify exhausted
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/api/orders");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilterInternal(blockedReq, blockedRes, chain);
        assertEquals(429, blockedRes.getStatus());

        // Simulate time passing via reflection
        Field bucketsField = RateLimitFilter.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var buckets = (java.util.concurrent.ConcurrentHashMap<String, ?>) bucketsField.get(filter);
        Object bucket = buckets.get(clientIp);

        Field lastRefillField = bucket.getClass().getDeclaredField("lastRefillTime");
        lastRefillField.setAccessible(true);
        lastRefillField.set(bucket, System.currentTimeMillis() - 61_000);

        // Should succeed after refill
        MockHttpServletRequest refreshedReq = new MockHttpServletRequest("GET", "/api/orders");
        refreshedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse refreshedRes = new MockHttpServletResponse();
        filter.doFilterInternal(refreshedReq, refreshedRes, chain);
        assertEquals(200, refreshedRes.getStatus());
    }
}
