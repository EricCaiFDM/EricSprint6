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
import com.example.banking.api.auth.dto.PasswordResetConfirmRequest;
import com.example.banking.api.auth.dto.PasswordResetRequest;
import com.example.banking.api.auth.dto.RefreshRequest;
import com.example.banking.api.auth.dto.RefreshResponse;
import com.example.banking.api.auth.dto.RegisterRequest;
import com.example.banking.api.auth.dto.RegisterResponse;
import com.example.banking.services.AuthService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a user",
            description = "Creates a new user account with role-aware validation and returns the created user identifier.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "201",
                description = "User registered",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterResponse.class),
                    examples = @ExampleObject(value = "{\"status\":\"CREATED\",\"userId\":\"f1d82f83-c1f0-4988-9e8f-2adf2bd8b6b1\"}"))),
            @ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"password and passwordConfirmation must match\",\"field\":\"passwordConfirmation\"}"))),
            @ApiResponse(
                responseCode = "409",
                description = "Duplicate identity",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"DUPLICATE_IDENTITY\",\"message\":\"identity already exists\"}")))
        })
    @PostMapping("/register")
        public ResponseEntity<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(value = "{\"email\":\"customer@example.com\",\"password\":\"StrongPass123!\",\"passwordConfirmation\":\"StrongPass123!\",\"role\":\"CUSTOMER\"}")))
            @Valid @RequestBody RegisterRequest request) {
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

        @Operation(
            summary = "Authenticate user",
            description = "Validates credentials and issues an access token and refresh token for authenticated API calls.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Authenticated",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class),
                    examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"d2f4c1ce-b6d6-4d6e-b45f-f8b17f13a6d2\",\"expiresIn\":600}"))),
            @ApiResponse(
                responseCode = "401",
                description = "Authentication failed",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"AUTHENTICATION_FAILED\",\"message\":\"Invalid credentials\"}")))
        })
    @PostMapping("/login")
        public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(value = "{\"identity\":\"customer@example.com\",\"password\":\"StrongPass123!\"}")))
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginTokens tokens = authService.login(request.identity(), request.password());
            return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTHENTICATION_FAILED", ex.getMessage()));
        }
    }

        @Operation(
            summary = "Request password reset",
            description = "Accepts a password reset request and returns a generic acknowledgment to avoid identity disclosure.")
        @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GenericAcknowledgeResponse.class),
                examples = @ExampleObject(value = "{\"status\":\"ACCEPTED\",\"message\":\"If the account exists, reset instructions will be sent.\"}")))
    @PostMapping("/password-reset/request")
    public ResponseEntity<GenericAcknowledgeResponse> requestPasswordReset(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordResetRequest.class),
                    examples = @ExampleObject(value = "{\"identity\":\"customer@example.com\"}")))
            @Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.identity());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenericAcknowledgeResponse(
                "ACCEPTED",
                "If the account exists, reset instructions will be sent."));
    }

        @Operation(
            summary = "Confirm password reset",
            description = "Applies a new password for the requested identity after confirmation and validation checks.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "202",
                description = "Accepted",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GenericAcknowledgeResponse.class),
                    examples = @ExampleObject(value = "{\"status\":\"ACCEPTED\",\"message\":\"If the account exists, account access has been reset.\"}"))),
            @ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"password and passwordConfirmation must match\",\"field\":\"passwordConfirmation\"}")))
        })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordResetConfirmRequest.class),
                    examples = @ExampleObject(value = "{\"identity\":\"customer@example.com\",\"password\":\"NewStrongPass123!\",\"passwordConfirmation\":\"NewStrongPass123!\"}")))
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            authService.confirmPasswordReset(request.identity(), request.password(), request.passwordConfirmation());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenericAcknowledgeResponse(
                    "ACCEPTED",
                    "If the account exists, account access has been reset."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
        }
    }

        @Operation(
            summary = "Refresh access token",
            description = "Rotates refresh credentials and returns a fresh access token for continuing authenticated sessions.")
        @ApiResponses({
            @ApiResponse(
                responseCode = "200",
                description = "Refreshed",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshResponse.class),
                    examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"4186a82f-fdf0-4f3a-bb4f-df15b79c64f4\",\"expiresIn\":600}"))),
            @ApiResponse(
                responseCode = "401",
                description = "Invalid refresh token",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_REFRESH_TOKEN\",\"message\":\"Refresh token is invalid\"}")))
        })
    @PostMapping("/token/refresh")
        public ResponseEntity<?> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshRequest.class),
                    examples = @ExampleObject(value = "{\"refreshToken\":\"d2f4c1ce-b6d6-4d6e-b45f-f8b17f13a6d2\"}")))
            @Valid @RequestBody RefreshRequest request) {
        try {
            AuthService.LoginTokens tokens = authService.refreshAccessToken(request.refreshToken());
            return ResponseEntity.ok(new RefreshResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH_TOKEN", ex.getMessage()));
        }
    }
}
