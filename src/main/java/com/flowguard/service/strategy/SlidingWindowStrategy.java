package com.flowguard.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Component("slidingWindow")
@RequiredArgsConstructor
@Profile("!dev")
public class SlidingWindowStrategy implements RateLimitingStrategy {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> slidingWindowScript;

    @Override
    public boolean isAllowed(String key, int limit, int timeWindowSeconds) {
        long nowSec = Instant.now().getEpochSecond();
        String uniqueId = UUID.randomUUID().toString(); // Collision-proof member ID

        // KEYS[1]: limit key
        // ARGV[1]: window size
        // ARGV[2]: limit
        // ARGV[3]: unique member ID (UUID — zero collisions)
        // ARGV[4]: current timestamp seconds

        Long result = redisTemplate.execute(slidingWindowScript,
                Collections.singletonList("rate_limit:sw:" + key),
                String.valueOf(timeWindowSeconds),
                String.valueOf(limit),
                uniqueId,
                String.valueOf(nowSec));

        return result != null && result == 1L;
    }

    @Override
    public long getRemainingTokens(String key, int limit, int timeWindowSeconds) {
        return -1;
    }
}
