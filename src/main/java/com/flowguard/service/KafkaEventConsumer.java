package com.flowguard.service;

import com.flowguard.event.RequestEvent;
import com.flowguard.model.RequestLog;
import com.flowguard.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes RequestEvents from Kafka and persists them to the database.
 * Also broadcasts each event via SSE for real-time dashboard updates.
 * Only active in production (non-dev) profile.
 */
@Service
@Profile("!dev")
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final RequestLogRepository requestLogRepository;
    private final SseEmitterService sseEmitterService;

    @KafkaListener(topics = "flowguard.requests", groupId = "flowguard-consumer")
    public void consumeRequestEvent(RequestEvent event) {
        try {
            RequestLog logEntry = RequestLog.builder()
                    .apiKeyHash(event.getApiKeyHash())
                    .ipAddress(event.getIpAddress())
                    .requestUri(event.getRequestUri())
                    .method(event.getMethod())
                    .statusCode(event.getStatusCode())
                    .blocked(event.isBlocked())
                    .blockReason(event.getBlockReason())
                    .processingTimeMs(event.getProcessingTimeMs())
                    .requestId(event.getRequestId())
                    .build();

            requestLogRepository.save(logEntry);
            sseEmitterService.broadcast(event);

            if (event.isBlocked()) {
                log.warn("[KAFKA-CONSUMER] Blocked request persisted: IP={} URI={} reason={}",
                        event.getIpAddress(), event.getRequestUri(), event.getBlockReason());
            }
        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER] Failed to process event: {}", e.getMessage(), e);
        }
    }
}
