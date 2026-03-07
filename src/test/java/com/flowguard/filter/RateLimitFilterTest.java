package com.flowguard.filter;

import com.flowguard.dto.RateLimitInfo;
import com.flowguard.model.ApiKey;
import com.flowguard.model.Plan;
import com.flowguard.model.Role;
import com.flowguard.model.User;
import com.flowguard.service.AbuseDetectionService;
import com.flowguard.service.ApiKeyService;
import com.flowguard.service.EventPublisher;
import com.flowguard.service.RateLimitingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private RateLimitingService rateLimitingService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AbuseDetectionService abuseDetectionService;
    @Mock
    private FilterChain filterChain;

    private MeterRegistry meterRegistry;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
        filter = new RateLimitFilter(apiKeyService, rateLimitingService, eventPublisher,
                abuseDetectionService, meterRegistry);
        // Trigger @PostConstruct
        filter.getClass().getDeclaredMethod("initMetrics").invoke(filter);
    }

    @Test
    @DisplayName("Should skip rate limiting for public paths")
    void skipsPublicPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(apiKeyService, never()).validateApiKey(anyString());
    }

    @Test
    @DisplayName("Should return 401 when API key is missing")
    void returns401WhenApiKeyMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/test/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should set rate limit headers on allowed requests")
    void setsRateLimitHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/test/ping");
        request.addHeader("X-API-KEY", "valid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Plan plan = new Plan();
        plan.setRequests(100);
        plan.setTimeWindowSeconds(60);

        User user = new User();
        user.setEmail("test@test.com");
        user.setRole(Role.USER);

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash("hashed");
        apiKey.setPlan(plan);
        apiKey.setUser(user);

        when(abuseDetectionService.isIpBlocked(anyString())).thenReturn(false);
        when(apiKeyService.validateApiKey("valid-key")).thenReturn(apiKey);
        when(rateLimitingService.isRequestAllowed(anyString(), eq(100), eq(60))).thenReturn(true);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(100), eq(60)))
                .thenReturn(new RateLimitInfo(100, 99, 55));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("100", response.getHeader("X-RateLimit-Limit"));
        assertEquals("99", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("55", response.getHeader("X-RateLimit-Reset"));
        assertNotNull(response.getHeader("X-Request-ID"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    void returns429WhenRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/test/ping");
        request.addHeader("X-API-KEY", "valid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Plan plan = new Plan();
        plan.setRequests(10);
        plan.setTimeWindowSeconds(60);

        User user = new User();
        user.setEmail("test@test.com");
        user.setRole(Role.USER);

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash("hashed");
        apiKey.setPlan(plan);
        apiKey.setUser(user);

        when(abuseDetectionService.isIpBlocked(anyString())).thenReturn(false);
        when(apiKeyService.validateApiKey("valid-key")).thenReturn(apiKey);
        when(rateLimitingService.isRequestAllowed(anyString(), eq(10), eq(60))).thenReturn(false);
        when(rateLimitingService.getRateLimitInfo(anyString(), eq(10), eq(60)))
                .thenReturn(new RateLimitInfo(10, 0, 45));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should block requests from blocked IPs")
    void blocksBlockedIps() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/test/ping");
        request.addHeader("X-API-KEY", "valid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(abuseDetectionService.isIpBlocked(anyString())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }
}
