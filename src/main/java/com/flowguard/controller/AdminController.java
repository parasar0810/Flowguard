package com.flowguard.controller;

import com.flowguard.dto.response.ApiResponse;
import com.flowguard.dto.response.UserResponse;
import com.flowguard.model.ApiKey;
import com.flowguard.model.Plan;
import com.flowguard.repository.ApiKeyRepository;
import com.flowguard.repository.PlanRepository;
import com.flowguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PlanRepository planRepository;

    // ── Users ────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Users", users));
    }

    // ── API Keys ─────────────────────────────────────────────────────────

    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKey>>> getAllApiKeys() {
        return ResponseEntity.ok(ApiResponse.success("API keys", apiKeyRepository.findAll()));
    }

    @PostMapping("/api-keys/{id}/revoke")
    public ResponseEntity<ApiResponse<String>> revokeKey(@PathVariable String id) {
        return apiKeyRepository.findById(java.util.UUID.fromString(id)).map(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
            return ResponseEntity.ok(ApiResponse.success("API key revoked", key.getPrefix() + "..."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api-keys/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateKey(@PathVariable String id) {
        return apiKeyRepository.findById(java.util.UUID.fromString(id)).map(key -> {
            key.setActive(true);
            apiKeyRepository.save(key);
            return ResponseEntity.ok(ApiResponse.success("API key activated", key.getPrefix() + "..."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Plans ─────────────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success("Plans", planRepository.findAll()));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<Plan>> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(ApiResponse.success("Plan created", planRepository.save(plan)));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Plan>> updatePlan(@PathVariable Integer id, @RequestBody Plan updated) {
        return planRepository.findById(id).map(plan -> {
            plan.setName(updated.getName());
            plan.setRequests(updated.getRequests());
            plan.setTimeWindowSeconds(updated.getTimeWindowSeconds());
            return ResponseEntity.ok(ApiResponse.success("Plan updated", planRepository.save(plan)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.success("Admin stats", Map.of(
                "totalUsers", userRepository.count(),
                "totalApiKeys", apiKeyRepository.count(),
                "totalPlans", planRepository.count())));
    }
}
