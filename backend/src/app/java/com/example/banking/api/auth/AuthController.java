package com.example.banking.api.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.auth.dto.ErrorResponse;
import com.example.banking.api.auth.dto.GenericAcknowledgeResponse;
import com.example.banking.api.auth.dto.LoginRequest;
import com.example.banking.api.auth.dto.LoginResponse;
import com.example.banking.api.auth.dto.PasswordResetRequest;
import com.example.banking.api.auth.dto.RefreshRequest;
import com.example.banking.api.auth.dto.RefreshResponse;
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
            UUID userId = authService.register(
                    request.email(),
                    request.password(),
                    request.passwordConfirmation(),
                    request.role());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse("CREATED", userId.toString()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("DUPLICATE_IDENTITY", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginTokens tokens = authService.login(request.identity(), request.password());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTHENTICATION_FAILED", ex.getMessage()));
        }
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<GenericAcknowledgeResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.identity());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenericAcknowledgeResponse(
                "ACCEPTED",
                "If the account exists, reset instructions will be sent."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshRequest request) {
        try {
            AuthService.LoginTokens tokens = authService.refreshAccessToken(request.refreshToken());
            return ResponseEntity.ok(new RefreshResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH_TOKEN", ex.getMessage()));
        }
    }
}
