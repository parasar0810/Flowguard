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
@RequestMapping("/v1/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final RequestLogRepository requestLogRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long totalRequests = requestLogRepository.count();
        long blockedRequests = requestLogRepository.countByBlocked(true);
        long allowedRequests = totalRequests - blockedRequests;
        double blockRate = totalRequests == 0 ? 0 : (double) blockedRequests / totalRequests * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests);
        stats.put("blockedRequests", blockedRequests);
        stats.put("allowedRequests", allowedRequests);
        stats.put("blockRate", Math.round(blockRate * 10.0) / 10.0);
        stats.put("activeNodes", 1);
        stats.put("status", "HEALTHY");
        stats.put("uptime", "Running");
        stats.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentLogs() {
        List<RequestLog> logs = requestLogRepository.findTop50ByOrderByTimestampDesc();
        List<Map<String, Object>> result = logs.stream().map(log -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", log.getId());
            entry.put("ipAddress", log.getIpAddress());
            entry.put("requestUri", log.getRequestUri());
            entry.put("method", log.getMethod());
            entry.put("statusCode", log.getStatusCode());
            entry.put("blocked", log.isBlocked());
            entry.put("timestamp", log.getTimestamp() != null ? log.getTimestamp().toString() : null);
            return entry;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Recent logs retrieved", result));
    }

    @GetMapping("/top-ips")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopIps() {
        List<Object[]> raw = requestLogRepository.findTopIpAddresses();
        List<Map<String, Object>> result = raw.stream().limit(10).map(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ipAddress", row[0]);
            entry.put("count", row[1]);
            return entry;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Top IPs retrieved", result));
    }

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getMethodBreakdown() {
        List<Object[]> raw = requestLogRepository.findRequestsByMethod();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            result.put((String) row[0], (Long) row[1]);
        }
        return ResponseEntity.ok(ApiResponse.success("Method breakdown retrieved", result));
    }

    @GetMapping("/traffic")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrafficOverTime() {
        // Return last 10 minutes of traffic, per-minute
        List<Map<String, Object>> timeline = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 9; i >= 0; i--) {
            LocalDateTime from = now.minusMinutes(i + 1);
            LocalDateTime to = now.minusMinutes(i);
            List<RequestLog> logs = requestLogRepository.findByTimestampBetween(from, to);

            long total = logs.size();
            long blocked = logs.stream().filter(RequestLog::isBlocked).count();

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("minute", to.getHour() + ":" + String.format("%02d", to.getMinute()));
            point.put("total", total);
            point.put("blocked", blocked);
            point.put("allowed", total - blocked);
            timeline.add(point);
        }

        return ResponseEntity.ok(ApiResponse.success("Traffic over time retrieved", timeline));
    }
}
