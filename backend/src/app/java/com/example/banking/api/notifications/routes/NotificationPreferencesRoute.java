package com.example.banking.api.notifications.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.notifications.schemas.NotificationPreferencesRequestSchema;
import com.example.banking.api.notifications.schemas.NotificationPreferencesResponseSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.NotificationPreferencesService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/notifications/preferences")
@Validated
public class NotificationPreferencesRoute {
    private final NotificationPreferencesService notificationPreferencesService;
    private final CustomerPrincipalResolver principalResolver;

    public NotificationPreferencesRoute(
            NotificationPreferencesService notificationPreferencesService,
            CustomerPrincipalResolver principalResolver) {
        this.notificationPreferencesService = notificationPreferencesService;
        this.principalResolver = principalResolver;
    }

    @Operation(
            summary = "Get notification preferences",
            description = "Returns current notification topic preferences for the authenticated user.")
        @ApiResponse(
            responseCode = "200",
            description = "Current notification preferences",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationPreferencesResponseSchema.class),
                examples = @ExampleObject(value = "{\"depositAlertsEnabled\":true,\"withdrawalAlertsEnabled\":true,\"transferAlertsEnabled\":true,\"statementAlertsEnabled\":true,\"offersEnabled\":false}")))
    @GetMapping
    public ResponseEntity<NotificationPreferencesResponseSchema> getPreferences(Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        NotificationPreferencesResponseSchema response = notificationPreferencesService.getPreferences(
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }

        @Operation(
            summary = "Update notification preferences",
            description = "Updates notification topic preferences for the authenticated user.")
        @ApiResponse(
            responseCode = "200",
            description = "Updated notification preferences",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationPreferencesResponseSchema.class),
                examples = @ExampleObject(value = "{\"depositAlertsEnabled\":true,\"withdrawalAlertsEnabled\":false,\"transferAlertsEnabled\":true,\"statementAlertsEnabled\":true,\"offersEnabled\":true}")))
    @PatchMapping
    public ResponseEntity<NotificationPreferencesResponseSchema> updatePreferences(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = NotificationPreferencesRequestSchema.class),
                    examples = @ExampleObject(value = "{\"depositAlertsEnabled\":true,\"withdrawalAlertsEnabled\":false,\"transferAlertsEnabled\":true,\"statementAlertsEnabled\":true,\"offersEnabled\":true}")))
            @Valid @RequestBody NotificationPreferencesRequestSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        NotificationPreferencesResponseSchema response = notificationPreferencesService.updatePreferences(
                principal.userId(),
                principal.role(),
                request);
        return ResponseEntity.ok(response);
    }
}
