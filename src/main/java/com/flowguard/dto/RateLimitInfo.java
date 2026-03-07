package com.flowguard.dto;

/**
 * Holds rate limit metadata returned alongside API responses.
 */
public record RateLimitInfo(int limit, long remaining, long resetSeconds) {
}
