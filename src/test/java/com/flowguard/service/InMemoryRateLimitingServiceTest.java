package com.flowguard.service;

import com.flowguard.dto.RateLimitInfo;
import com.flowguard.service.impl.InMemoryRateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRateLimitingServiceTest {

    private InMemoryRateLimitingService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryRateLimitingService();
    }

    @Test
    @DisplayName("Should allow requests within the limit")
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 50; i++) {
            assertTrue(service.isRequestAllowed("test-key", 50, 60),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block requests exceeding the limit")
    void blocksExceedingRequests() {
        for (int i = 0; i < 10; i++) {
            service.isRequestAllowed("test-key", 10, 60);
        }
        assertFalse(service.isRequestAllowed("test-key", 10, 60),
                "Request 11 should be blocked");
    }

    @Test
    @DisplayName("Should track separate keys independently")
    void separateKeysAreIndependent() {
        for (int i = 0; i < 5; i++) {
            service.isRequestAllowed("key-a", 5, 60);
        }
        // key-a is exhausted
        assertFalse(service.isRequestAllowed("key-a", 5, 60));
        // key-b should still have capacity
        assertTrue(service.isRequestAllowed("key-b", 5, 60));
    }

    @Test
    @DisplayName("Should return correct rate limit info")
    void rateLimitInfoIsAccurate() {
        for (int i = 0; i < 3; i++) {
            service.isRequestAllowed("info-key", 10, 60);
        }
        RateLimitInfo info = service.getRateLimitInfo("info-key", 10, 60);
        assertEquals(10, info.limit());
        assertEquals(7, info.remaining());
        assertTrue(info.resetSeconds() > 0 && info.resetSeconds() <= 60);
    }

    @Test
    @DisplayName("Should handle concurrent requests atomically")
    void concurrentRequestsAreAtomic() throws InterruptedException, ExecutionException {
        int limit = 100;
        int totalRequests = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < totalRequests; i++) {
            futures.add(executor.submit(() -> service.isRequestAllowed("concurrent-key", limit, 60)));
        }

        long allowed = 0;
        long blocked = 0;
        for (Future<Boolean> f : futures) {
            if (f.get())
                allowed++;
            else
                blocked++;
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(limit, allowed, "Exactly " + limit + " should be allowed");
        assertEquals(totalRequests - limit, blocked, "Remaining should be blocked");
    }
}
