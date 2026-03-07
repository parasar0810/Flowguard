package com.flowguard.controller;

import com.flowguard.dto.response.ApiResponse;
import com.flowguard.model.RequestLog;
import com.flowguard.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final RequestLogRepository requestLogRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        long total = requestLogRepository.count();
        long blocked = requestLogRepository.countByBlocked(true);
        long allowed = total - blocked;
        double blockRate = total == 0 ? 0 : (double) blocked / total * 100;
        Double avgMs = requestLogRepository.findAvgProcessingTime(LocalDateTime.now().minusHours(1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalRequests", total);
        data.put("allowedRequests", allowed);
        data.put("blockedRequests", blocked);
        data.put("blockRatePct", Math.round(blockRate * 10.0) / 10.0);
        data.put("avgProcessingMs", avgMs != null ? Math.round(avgMs * 10.0) / 10.0 : 0);
        data.put("status", "HEALTHY");
        data.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success("Summary", data));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTimeline(
            @RequestParam(defaultValue = "10") int minutes) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = minutes - 1; i >= 0; i--) {
            LocalDateTime from = now.minusMinutes(i + 1);
            LocalDateTime to = now.minusMinutes(i);
            List<RequestLog> logs = requestLogRepository.findByTimestampBetween(from, to);

            long total = logs.size();
            long bl = logs.stream().filter(RequestLog::isBlocked).count();
            double avgMs = logs.stream().mapToLong(RequestLog::getProcessingTimeMs).average().orElse(0);

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label", String.format("%02d:%02d", to.getHour(), to.getMinute()));
            pt.put("total", total);
            pt.put("blocked", bl);
            pt.put("allowed", total - bl);
            pt.put("avgMs", Math.round(avgMs));
            result.add(pt);
        }
        return ResponseEntity.ok(ApiResponse.success("Timeline", result));
    }

    @GetMapping("/top-blocked-ips")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopBlockedIps() {
        List<Object[]> raw = requestLogRepository.findTopBlockedIpAddresses();
        List<Map<String, Object>> result = raw.stream().limit(10).map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Top blocked IPs", result));
    }

    @GetMapping("/top-endpoints")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopEndpoints() {
        List<Object[]> raw = requestLogRepository.findTopEndpoints();
        List<Map<String, Object>> result = raw.stream().limit(10).map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("endpoint", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Top endpoints", result));
    }

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getMethods() {
        List<Object[]> raw = requestLogRepository.findRequestsByMethod();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] r : raw)
            result.put((String) r[0], (Long) r[1]);
        return ResponseEntity.ok(ApiResponse.success("Methods", result));
    }

    @GetMapping("/block-reasons")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBlockReasons() {
        List<Object[]> raw = requestLogRepository.findBlockReasonBreakdown();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] r : raw)
            result.put(r[0] != null ? (String) r[0] : "UNKNOWN", (Long) r[1]);
        return ResponseEntity.ok(ApiResponse.success("Block reasons", result));
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentLogs() {
        List<RequestLog> logs = requestLogRepository.findTop50ByOrderByTimestampDesc();
        List<Map<String, Object>> result = logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("requestId", log.getRequestId());
            m.put("ip", log.getIpAddress());
            m.put("method", log.getMethod());
            m.put("uri", log.getRequestUri());
            m.put("status", log.getStatusCode());
            m.put("blocked", log.isBlocked());
            m.put("blockReason", log.getBlockReason());
            m.put("processingMs", log.getProcessingTimeMs());
            m.put("timestamp", log.getTimestamp() != null ? log.getTimestamp().toString() : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Recent logs", result));
    }
}
