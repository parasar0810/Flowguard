package com.flowguard.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Runs the production Lua scripts against a real Redis (set REDIS_HOST, e.g. a local
 * redis:7 container or the CI service). Skipped when REDIS_HOST is not set.
 */
class RedisLuaScriptsIntegrationTest {

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;

    @BeforeAll
    static void connect() {
        String host = System.getenv("REDIS_HOST");
        Assumptions.assumeTrue(host != null && !host.isBlank(), "REDIS_HOST not set");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void close() {
        if (factory != null) factory.destroy();
    }

    private static DefaultRedisScript<Long> script(String name) {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setLocation(new ClassPathResource("scripts/" + name));
        s.setResultType(Long.class);
        return s;
    }

    private static String key() {
        return "it-" + UUID.randomUUID();
    }

    @Test
    void slidingWindowAllowsExactlyTheLimit() {
        SlidingWindowStrategy sw = new SlidingWindowStrategy(redis, script("sliding_window.lua"));
        String k = key();
        for (int i = 0; i < 5; i++) assertTrue(sw.isAllowed(k, 5, 60), "request " + (i + 1));
        assertFalse(sw.isAllowed(k, 5, 60), "6th request must be rejected");
    }

    @Test
    void slidingWindowIsAtomicUnderConcurrency() throws Exception {
        SlidingWindowStrategy sw = new SlidingWindowStrategy(redis, script("sliding_window.lua"));
        assertEquals(10, concurrentAllowed(() -> sw, key(), 200, 10));
    }

    @Test
    void tokenBucketAllowsCapacityThenRejects() {
        TokenBucketStrategy tb = new TokenBucketStrategy(redis, script("token_bucket.lua"));
        String k = key();
        for (int i = 0; i < 3; i++) assertTrue(tb.isAllowed(k, 3, 3600), "request " + (i + 1));
        assertFalse(tb.isAllowed(k, 3, 3600), "bucket must be empty");
    }

    @Test
    void tokenBucketIsAtomicUnderConcurrency() throws Exception {
        TokenBucketStrategy tb = new TokenBucketStrategy(redis, script("token_bucket.lua"));
        assertEquals(10, concurrentAllowed(() -> tb, key(), 200, 10));
    }

    private static int concurrentAllowed(java.util.function.Supplier<RateLimitingStrategy> s, String k, int requests, int limit)
            throws InterruptedException {
        RateLimitingStrategy strategy = s.get();
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        for (int i = 0; i < requests; i++) {
            pool.submit(() -> {
                start.await();
                if (strategy.isAllowed(k, limit, 3600)) allowed.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        return allowed.get();
    }
}
