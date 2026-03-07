package com.flowguard.service;

import com.flowguard.model.RequestLog;
import com.flowguard.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncLogger {

    private final RequestLogRepository requestLogRepository;

    @Async
    public void logRequest(String apiKeyHash, String ip, String uri, String method, int status, boolean blocked) {
        try {
            RequestLog logEntry = RequestLog.builder()
                    .apiKeyHash(apiKeyHash)
                    .ipAddress(ip)
                    .requestUri(uri)
                    .method(method)
                    .statusCode(status)
                    .blocked(blocked)
                    .timestamp(LocalDateTime.now())
                    .build();

            requestLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to async log request: {}", e.getMessage());
        }
    }
}
