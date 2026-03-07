package com.flowguard.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;

@Component("tokenBucket")
@RequiredArgsConstructor
@Profile("!dev")
public class TokenBucketStrategy implements RateLimitingStrategy {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    @Override
    public boolean isAllowed(String key, int limit, int timeWindowSeconds) {
        long now = Instant.now().getEpochSecond();
        double refillRate = (double) limit / timeWindowSeconds; // Simplified refill rate

        // ARGV[1]: capacity (limit)
        // ARGV[2]: refill rate
        // ARGV[3]: time window
        // ARGV[4]: current timestamp

        // Note: My Lua script implementation expects specific args.
        // Refill logic in Lua: tokens += (now - last_refill) * rate

        Long result = redisTemplate.execute(tokenBucketScript,
                Collections.singletonList("rate_limit:tb:" + key),
                String.valueOf(limit),
                String.valueOf(refillRate),
                String.valueOf(timeWindowSeconds),
                String.valueOf(now));

        return result != null && result == 1L;
    }

    @Override
    public long getRemainingTokens(String key, int limit, int timeWindowSeconds) {
        // Implementation for visibility (omitted for brevity in MVP, requiring another
        // Lua script or modifying existing)
        return -1;
    }
}
