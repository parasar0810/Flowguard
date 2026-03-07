package com.flowguard.service;

import com.flowguard.dto.RateLimitInfo;

public interface RateLimitingService {
    boolean isRequestAllowed(String key, int limit, int windowSeconds);

    RateLimitInfo getRateLimitInfo(String key, int limit, int windowSeconds);
}
