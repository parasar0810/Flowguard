package com.flowguard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs", indexes = {
        @Index(name = "idx_request_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_request_logs_ip", columnList = "ip_address"),
        @Index(name = "idx_request_logs_api_key", columnList = "api_key_hash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_hash")
    private String apiKeyHash;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "request_uri", nullable = false)
    private String requestUri;

    @Column(nullable = false)
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    private boolean blocked;

    @Column(name = "processing_time_ms")
    private long processingTimeMs;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "block_reason")
    private String blockReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
