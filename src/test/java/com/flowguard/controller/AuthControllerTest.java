package com.flowguard.controller;

import com.flowguard.dto.request.RegisterRequest;
import com.flowguard.model.Role;
import com.flowguard.model.User;
import com.flowguard.repository.UserRepository;
import com.flowguard.service.AbuseDetectionService;
import com.flowguard.service.ApiKeyService;
import com.flowguard.service.UserService;
import com.flowguard.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AbuseDetectionService abuseDetectionService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest makeRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    @DisplayName("Register should return 200 with API key")
    void registerReturns200WithApiKey() {
        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setEmail("new@example.com");
        user.setRole(Role.USER);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(user);
        when(apiKeyService.createApiKey(user, "FREE")).thenReturn("test-api-key-123");

        ResponseEntity<?> response = authController.register(makeRequest("new@example.com", "Pass123!"), "FREE");

        assertEquals(200, response.getStatusCode().value());
        verify(apiKeyService).createApiKey(user, "FREE");
    }

    @Test
    @DisplayName("Register with DEMO plan should use DEMO plan")
    void registerWithDemoPlan() {
        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setEmail("pro@example.com");
        user.setRole(Role.USER);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(user);
        when(apiKeyService.createApiKey(user, "DEMO")).thenReturn("demo-key-456");

        ResponseEntity<?> response = authController.register(makeRequest("pro@example.com", "Pass123!"), "DEMO");

        assertEquals(200, response.getStatusCode().value());
        verify(apiKeyService).createApiKey(user, "DEMO");
    }
}
