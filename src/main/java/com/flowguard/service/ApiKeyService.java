package com.flowguard.service;

import com.flowguard.model.ApiKey;
import com.flowguard.model.Plan;
import com.flowguard.model.User;
import com.flowguard.repository.ApiKeyRepository;
import com.flowguard.repository.PlanRepository;
import com.flowguard.util.KeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PlanRepository planRepository;
    private final KeyGenerator keyGenerator;

    @Transactional
    public String createApiKey(User user, String planName) {
        Plan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        String rawKey = keyGenerator.generateKey();
        String keyHash = hashKey(rawKey);
        String prefix = rawKey.substring(0, 8);

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .plan(plan)
                .keyHash(keyHash)
                .prefix(prefix)
                .isActive(true)
                .build();

        apiKeyRepository.save(apiKey);

        // Return raw key to user - this is the only time they see it
        return rawKey;
    }

    @Transactional(readOnly = true)
    public ApiKey validateApiKey(String rawKey) {
        String keyHash = hashKey(rawKey);
        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(ApiKey::isActive)
                .map(key -> {
                    // Force initialization of User to prevent LazyInitializationException in filter
                    key.getUser().getEmail();
                    return key;
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive API key"));
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
