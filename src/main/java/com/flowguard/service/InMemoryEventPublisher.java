package com.flowguard.service;

import com.flowguard.event.RequestEvent;
import com.flowguard.model.RequestLog;
import com.flowguard.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Dev-profile EventPublisher: bypasses Kafka entirely.
 * Events are processed in-memory and written directly to H2.
 */
@Service
@Profile("dev")
@Primary
@RequiredArgsConstructor
@Slf4j
public class InMemoryEventPublisher implements EventPublisher {

    private final RequestLogRepository requestLogRepository;
    private final SseEmitterService sseEmitterService;

    @Override
    public void publishRequestEvent(RequestEvent event) {
        try {
            RequestLog log = RequestLog.builder()
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
            requestLogRepository.save(log);

            // Push to SSE clients in real time
            sseEmitterService.broadcast(event);
        } catch (Exception e) {
            log.error("Failed to persist request event", e);
        }
    }
}
