package com.flowguard.service.strategy;

public interface RateLimitingStrategy {
    boolean isAllowed(String key, int limit, int timeWindowSeconds);

    long getRemainingTokens(String key, int limit, int timeWindowSeconds);
}
