package com.boschtech.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {

    private static final String VALID_KEY = "test-key-123";

    private ApiKeyAuthFilter filter;
    private FilterChain chain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter(VALID_KEY);
        chain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassThroughGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassThroughOptionsRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/orders");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassThroughHeadRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/api/orders");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-API-Key", VALID_KEY);
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectMissingApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        filter.doFilterInternal(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectInvalidApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-API-Key", "wrong-key");
        filter.doFilterInternal(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing or invalid API key"));
    }
}
