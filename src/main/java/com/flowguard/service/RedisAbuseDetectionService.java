package com.flowguard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!dev")
public class RedisAbuseDetectionService implements AbuseDetectionService {

    private final StringRedisTemplate redisTemplate;

    private static final int FAILED_LOGIN_THRESHOLD = 5;
    private static final int BLOCK_DURATION_SECONDS = 300; // 5 minutes

    // Scheduled check every minute
    @Scheduled(fixedRate = 60000)
    public void detectAbuse() {
        log.info("Running abuse detection scanning...");
        // Logic reserved for future advanced analysis
    }

    // Triggered method (better for real-time)
    public void reportFailedLogin(String ip) {
        String key = "abuse:failed_login:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 5, TimeUnit.MINUTES);
        }

        if (count != null && count >= FAILED_LOGIN_THRESHOLD) {
            blockIp(ip);
            redisTemplate.delete(key); // Reset counter after block
        }
    }

    public void blockIp(String ip) {
        log.warn("Blocking IP due to abuse detection: {}", ip);
        redisTemplate.opsForValue().set("block:ip:" + ip, "true", BLOCK_DURATION_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isIpBlocked(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("block:ip:" + ip));
    }
}
