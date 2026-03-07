package com.flowguard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * A simple test endpoint that goes through the full FlowGuard pipeline:
 * NetworkRuleFilter → RateLimitFilter (API key + rate limit + Kafka publish) →
 * Response
 *
 * Use this to test burst traffic and see Kafka in action.
 */
@RestController
@RequestMapping("/v1/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "pong",
                "timestamp", Instant.now().toString()));
    }

    @GetMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestParam(defaultValue = "hello") String msg) {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "echo", msg,
                "timestamp", Instant.now().toString()));
    }
}
