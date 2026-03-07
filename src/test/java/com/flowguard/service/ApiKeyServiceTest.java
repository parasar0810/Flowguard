package com.flowguard.service;

import com.flowguard.model.ApiKey;
import com.flowguard.model.Plan;
import com.flowguard.model.User;
import com.flowguard.repository.ApiKeyRepository;
import com.flowguard.repository.PlanRepository;
import com.flowguard.util.KeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private KeyGenerator keyGenerator;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    @DisplayName("Should create API key with correct plan")
    void createsApiKeyWithPlan() {
        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setEmail("test@example.com");

        Plan plan = new Plan();
        plan.setId(1);
        plan.setName("FREE");
        plan.setRequests(50);
        plan.setTimeWindowSeconds(60);

        when(planRepository.findByName("FREE")).thenReturn(Optional.of(plan));
        when(keyGenerator.generateKey()).thenReturn("test-raw-api-key-123");
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawKey = apiKeyService.createApiKey(user, "FREE");

        assertNotNull(rawKey);
        assertFalse(rawKey.isBlank());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("Should throw exception for unknown plan")
    void throwsForUnknownPlan() {
        User user = new User();
        user.setEmail("test@example.com");

        when(planRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> apiKeyService.createApiKey(user, "UNKNOWN"));
    }

    @Test
    @DisplayName("Should validate existing API key")
    void validatesExistingApiKey() {
        Plan plan = new Plan();
        plan.setName("PRO");
        plan.setRequests(500);
        plan.setTimeWindowSeconds(60);

        User user = new User();
        user.setEmail("pro@example.com");

        ApiKey existingKey = new ApiKey();
        existingKey.setKeyHash("hashed-key-value");
        existingKey.setPlan(plan);
        existingKey.setUser(user);
        existingKey.setActive(true);

        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(existingKey));

        // Use a known hash - the service uses SHA-256
        // We mock the repo to return our ApiKey for any hash
        ApiKey result = apiKeyService.validateApiKey("some-raw-key");

        assertNotNull(result);
        assertEquals("PRO", result.getPlan().getName());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("Should throw exception for invalid API key")
    void throwsForInvalidApiKey() {
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> apiKeyService.validateApiKey("invalid-key"));
    }
}
