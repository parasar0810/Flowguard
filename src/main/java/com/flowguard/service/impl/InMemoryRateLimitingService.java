package com.flowguard.service.impl;

import com.flowguard.dto.RateLimitInfo;
import com.flowguard.service.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("dev")
@Slf4j
@Primary
public class InMemoryRateLimitingService implements RateLimitingService {

    private final Map<String, Long> counters = new ConcurrentHashMap<>();
    // Track the window ID for each base key so we know which entries are stale
    private final Map<String, Long> lastWindowIds = new ConcurrentHashMap<>();

    @Override
    public boolean isRequestAllowed(String key, int limit, int windowSeconds) {
        long currentWindow = System.currentTimeMillis() / (windowSeconds * 1000L);
        String counterKey = key + ":" + currentWindow;

        // Track the latest window for this key
        lastWindowIds.put(key, currentWindow);

        long count = counters.merge(counterKey, 1L, Long::sum);

        if (count > limit) {
            log.warn("Rate limit exceeded for key: {} (Limit: {})", key, limit);
            return false;
        }
        return true;
    }

    @Override
    public RateLimitInfo getRateLimitInfo(String key, int limit, int windowSeconds) {
        long currentWindow = System.currentTimeMillis() / (windowSeconds * 1000L);
        String counterKey = key + ":" + currentWindow;
        long count = counters.getOrDefault(counterKey, 0L);
        long remaining = Math.max(0, limit - count);

        // Calculate seconds until current window resets
        long windowStartMs = currentWindow * windowSeconds * 1000L;
        long windowEndMs = windowStartMs + (windowSeconds * 1000L);
        long resetSeconds = Math.max(1, (windowEndMs - System.currentTimeMillis()) / 1000);

        return new RateLimitInfo(limit, remaining, resetSeconds);
    }

    /**
     * Cleanup stale counter entries every 60 seconds to prevent memory leak.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis() / 1000L; // current time in seconds
        int removed = 0;
        for (String counterKey : counters.keySet()) {
            int lastColon = counterKey.lastIndexOf(':');
            if (lastColon > 0) {
                try {
                    long windowId = Long.parseLong(counterKey.substring(lastColon + 1));
                    // Remove entries older than 2 minutes (generous buffer)
                    // windowId is millis / (windowSeconds * 1000), so we compare roughly
                    // A simpler heuristic: if the entry is more than 120s old in wall clock
                    // we can remove it
                    long entryAgeEstimate = (now / 60) - windowId; // very rough estimate
                    if (entryAgeEstimate > 2) {
                        counters.remove(counterKey);
                        removed++;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} stale rate limit entries", removed);
        }
    }
}
