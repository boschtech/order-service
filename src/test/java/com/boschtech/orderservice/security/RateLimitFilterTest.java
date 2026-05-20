package com.boschtech.orderservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
        request.setRemoteAddr("172.16.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
