package com.flowguard.filter;

import com.flowguard.service.NetworkRuleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs BEFORE RateLimitFilter. Checks the client IP against configured CIDR
 * allow/deny rules.
 *
 * Army / Enterprise use case:
 * - Add ALLOW rule: cidr="10.0.0.0/8", priority=100 → only internal network
 * passes
 * - Add DENY rule: cidr="0.0.0.0/0", priority=1 → deny everything else
 * Result: Only 10.x.x.x IPs can reach your API.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class NetworkRuleFilter extends OncePerRequestFilter {

    private final NetworkRuleService networkRuleService;

    // Public endpoints always bypass network rules
    private static final String[] PUBLIC_PATHS = { "/v1/api/auth", "/h2-console", "/actuator", "/v1/api/stream",
            "/v1/api/dashboard", "/v1/api/analytics", "/v1/api/admin", "/v1/api/test",
            "/swagger-ui", "/v3/api-docs" };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Skip network rules for static resources
        if (requestUri.endsWith(".html") || requestUri.endsWith(".css")
                || requestUri.endsWith(".js") || requestUri.endsWith(".ico")
                || requestUri.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        for (String pub : PUBLIC_PATHS) {
            if (requestUri.startsWith(pub)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String ipAddress = request.getRemoteAddr();
        if (!networkRuleService.isAllowed(ipAddress)) {
            log.warn("[NETWORK RULE] Denied access from IP: {}", ipAddress);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Access denied: your network is not authorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
