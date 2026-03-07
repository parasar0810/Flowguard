package com.flowguard.service;

import com.flowguard.event.RequestEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Production EventPublisher: sends events to Kafka topic.
 * Kafka consumer (RequestEventConsumer) then writes to DB and broadcasts SSE.
 * Protected by Resilience4j circuit breaker to handle Kafka outages gracefully.
 */
@Service
@Profile("!dev")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private static final String TOPIC_REQUESTS = "flowguard.requests";
    private static final String TOPIC_BLOCKED = "flowguard.requests.blocked";

    private final KafkaTemplate<String, RequestEvent> kafkaTemplate;

    @Override
    @CircuitBreaker(name = "kafka-cb", fallbackMethod = "publishFallback")
    public void publishRequestEvent(RequestEvent event) {
        kafkaTemplate.send(TOPIC_REQUESTS, event.getIpAddress(), event);
        if (event.isBlocked()) {
            kafkaTemplate.send(TOPIC_BLOCKED, event.getIpAddress(), event);
            log.warn("[KAFKA] Blocked event published for IP: {}", event.getIpAddress());
        }
    }

    @SuppressWarnings("unused")
    private void publishFallback(RequestEvent event, Throwable t) {
        log.error(
                "[CIRCUIT-BREAKER] Kafka unavailable, logging event to file. requestId={}, uri={}, blocked={}, error={}",
                event.getRequestId(), event.getRequestUri(), event.isBlocked(), t.getMessage());
    }
}
