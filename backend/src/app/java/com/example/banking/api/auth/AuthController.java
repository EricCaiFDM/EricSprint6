package com.example.banking.api.auth;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.auth.dto.LoginRequest;
import com.example.banking.api.auth.dto.LoginResponse;
import com.example.banking.api.auth.dto.RegisterRequest;
import com.example.banking.api.auth.dto.RegisterResponse;
import com.example.banking.services.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UUID userId = authService.register(request.email(), request.password(), request.passwordConfirmation());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse("CREATED", userId.toString()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginTokens tokens = authService.login(request.identity(), request.password());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
        }
    }
}
