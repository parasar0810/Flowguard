package com.flowguard.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestEvent {
    private String requestId;
    private String apiKeyHash;
    private String ipAddress;
    private String requestUri;
    private String method;
    private Integer statusCode;
    private boolean blocked;
    private String blockReason;
    private long processingTimeMs;
    private LocalDateTime timestamp;
}
