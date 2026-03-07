package com.flowguard.service;

import com.flowguard.dto.RateLimitInfo;
import com.flowguard.service.strategy.RateLimitingStrategy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev")
@Slf4j
public class RedisRateLimitingService implements RateLimitingService {

    private final RateLimitingStrategy rateLimitingStrategy;

    public RedisRateLimitingService(@Qualifier("slidingWindow") RateLimitingStrategy rateLimitingStrategy) {
        this.rateLimitingStrategy = rateLimitingStrategy;
    }

    @Override
    @CircuitBreaker(name = "redis-cb", fallbackMethod = "isRequestAllowedFallback")
    public boolean isRequestAllowed(String key, int limit, int timeWindowSeconds) {
        return rateLimitingStrategy.isAllowed(key, limit, timeWindowSeconds);
    }

    @Override
    @CircuitBreaker(name = "redis-cb", fallbackMethod = "getRateLimitInfoFallback")
    public RateLimitInfo getRateLimitInfo(String key, int limit, int timeWindowSeconds) {
        long remaining = rateLimitingStrategy.getRemainingTokens(key, limit, timeWindowSeconds);
        if (remaining < 0)
            remaining = limit; // fallback if not implemented
        return new RateLimitInfo(limit, remaining, timeWindowSeconds);
    }

    // Fail-open: allow requests when Redis is down
    @SuppressWarnings("unused")
    private boolean isRequestAllowedFallback(String key, int limit, int timeWindowSeconds, Throwable t) {
        log.error("[CIRCUIT-BREAKER] Redis unavailable, allowing request (fail-open): {}", t.getMessage());
        return true;
    }

    @SuppressWarnings("unused")
    private RateLimitInfo getRateLimitInfoFallback(String key, int limit, int timeWindowSeconds, Throwable t) {
        log.error("[CIRCUIT-BREAKER] Redis unavailable for rate limit info: {}", t.getMessage());
        return new RateLimitInfo(limit, limit, timeWindowSeconds);
    }
}
