package com.flowguard.filter;

import com.flowguard.dto.RateLimitInfo;
import com.flowguard.event.RequestEvent;
import com.flowguard.model.ApiKey;
import com.flowguard.service.AbuseDetectionService;
import com.flowguard.service.ApiKeyService;
import com.flowguard.service.EventPublisher;
import com.flowguard.service.RateLimitingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final RateLimitingService rateLimitingService;
    private final EventPublisher eventPublisher;
    private final AbuseDetectionService abuseDetectionService;
    private final MeterRegistry meterRegistry;

    private static final String API_KEY_HEADER = "X-API-KEY";

    private Counter requestsAllowedCounter;
    private Counter requestsBlockedCounter;
    private Counter requestsTotalCounter;

    @PostConstruct
    void initMetrics() {
        requestsTotalCounter = Counter.builder("flowguard.requests.total")
                .description("Total API requests processed").register(meterRegistry);
        requestsAllowedCounter = Counter.builder("flowguard.requests.allowed")
                .description("Requests that passed rate limiting").register(meterRegistry);
        requestsBlockedCounter = Counter.builder("flowguard.requests.blocked")
                .description("Requests blocked by rate limiting").register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String requestUri = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();

        // Attach request ID to response for tracing
        response.setHeader("X-Request-ID", requestId);

        // Skip rate limiting for public endpoints and static resources
        if (requestUri.startsWith("/v1/api/auth") || requestUri.startsWith("/h2-console")
                || requestUri.startsWith("/v1/api/stream") || requestUri.startsWith("/v1/api/analytics")
                || requestUri.startsWith("/v1/api/admin") || requestUri.startsWith("/v1/api/dashboard")
                || requestUri.startsWith("/actuator")
                || requestUri.startsWith("/swagger-ui") || requestUri.startsWith("/v3/api-docs")
                || requestUri.endsWith(".html") || requestUri.endsWith(".css")
                || requestUri.endsWith(".js") || requestUri.endsWith(".ico")
                || requestUri.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Abuse check
        if (abuseDetectionService.isIpBlocked(ipAddress)) {
            long ms = System.currentTimeMillis() - startTime;
            requestsTotalCounter.increment();
            requestsBlockedCounter.increment();
            publishEvent(null, ipAddress, requestUri, request.getMethod(),
                    HttpStatus.FORBIDDEN.value(), true, "IP_BLOCKED", ms, requestId);
            sendError(response, HttpStatus.FORBIDDEN, "IP is temporarily blocked due to suspicious activity");
            return;
        }

        String apiKeyRaw = request.getHeader(API_KEY_HEADER);

        if (apiKeyRaw == null || apiKeyRaw.isBlank()) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Missing API Key");
            return;
        }

        try {
            ApiKey apiKey = apiKeyService.validateApiKey(apiKeyRaw);
            String limitKey = apiKey.getKeyHash();
            int limit = apiKey.getPlan().getRequests();
            int window = apiKey.getPlan().getTimeWindowSeconds();

            requestsTotalCounter.increment();

            if (rateLimitingService.isRequestAllowed(limitKey, limit, window)) {
                requestsAllowedCounter.increment();

                // Set rate limit headers
                RateLimitInfo info = rateLimitingService.getRateLimitInfo(limitKey, limit, window);
                response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining()));
                response.setHeader("X-RateLimit-Reset", String.valueOf(info.resetSeconds()));

                // Authenticated
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        apiKey.getUser().getEmail(), null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + apiKey.getUser().getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                filterChain.doFilter(request, response);

                long ms = System.currentTimeMillis() - startTime;
                publishEvent(apiKey.getKeyHash(), ipAddress, requestUri, request.getMethod(),
                        response.getStatus(), false, null, ms, requestId);

            } else {
                requestsBlockedCounter.increment();

                // Set rate limit headers even on 429
                RateLimitInfo info = rateLimitingService.getRateLimitInfo(limitKey, limit, window);
                response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(info.resetSeconds()));

                long ms = System.currentTimeMillis() - startTime;
                publishEvent(apiKey.getKeyHash(), ipAddress, requestUri, request.getMethod(),
                        HttpStatus.TOO_MANY_REQUESTS.value(), true, "RATE_LIMIT_EXCEEDED", ms, requestId);
                sendError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }

        } catch (IllegalArgumentException e) {
            sendError(response, HttpStatus.FORBIDDEN, "Invalid API Key");
        } catch (Exception e) {
            log.error("Filter error", e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private void publishEvent(String keyHash, String ip, String uri, String method,
            int status, boolean blocked, String blockReason, long ms, String requestId) {
        try {
            eventPublisher.publishRequestEvent(RequestEvent.builder()
                    .requestId(requestId)
                    .apiKeyHash(keyHash)
                    .ipAddress(ip)
                    .requestUri(uri)
                    .method(method)
                    .statusCode(status)
                    .blocked(blocked)
                    .blockReason(blockReason)
                    .processingTimeMs(ms)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish request event", e);
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
