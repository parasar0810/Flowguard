package com.flowguard.controller;

import com.flowguard.dto.response.ApiResponse;
import com.flowguard.model.NetworkRule;
import com.flowguard.service.NetworkRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/admin/network-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NetworkRuleController {

    private final NetworkRuleService networkRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NetworkRule>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success("Network rules", networkRuleService.getAllRules()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NetworkRule>> createRule(@RequestBody NetworkRule rule) {
        NetworkRule created = networkRuleService.createRule(rule);
        return ResponseEntity.ok(ApiResponse.success("Rule created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NetworkRule>> updateRule(@PathVariable Long id, @RequestBody NetworkRule rule) {
        return networkRuleService.updateRule(id, rule)
                .map(updated -> ResponseEntity.ok(ApiResponse.success("Rule updated", updated)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        networkRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Boolean>> testIp(@RequestParam String ip) {
        boolean allowed = networkRuleService.isAllowed(ip);
        String message = allowed ? "IP " + ip + " would be ALLOWED" : "IP " + ip + " would be DENIED";
        return ResponseEntity.ok(ApiResponse.success(message, allowed));
    }
}
