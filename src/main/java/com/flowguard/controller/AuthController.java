package com.flowguard.controller;

import com.flowguard.dto.request.LoginRequest;
import com.flowguard.dto.request.RegisterRequest;
import com.flowguard.dto.response.ApiResponse;
import com.flowguard.dto.response.AuthResponse;
import com.flowguard.model.User;
import com.flowguard.repository.UserRepository;
import com.flowguard.service.AbuseDetectionService;
import com.flowguard.service.ApiKeyService;
import com.flowguard.service.UserService;
import com.flowguard.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AbuseDetectionService abuseDetectionService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestParam(value = "plan", defaultValue = "FREE") String plan) {
        User user = userService.registerUser(request);

        // Generate API key with specified plan (defaults to FREE)
        String rawKey = apiKeyService.createApiKey(user, plan.toUpperCase());

        AuthResponse response = AuthResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .apiKey(rawKey)
                .message("User registered successfully. Securely save your API Key!")
                .build();

        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        // Simple IP extraction (behind proxy needs X-Forwarded-For)
        String clientIp = httpRequest.getRemoteAddr();

        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Fetch User details for the response
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String token = jwtUtil.generateToken(user.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .message("Token: " + token)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (Exception e) {
            abuseDetectionService.reportFailedLogin(clientIp);
            throw e;
        }
    }
}
