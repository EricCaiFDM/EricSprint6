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

import jakarta.validation.Valid;

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

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponseSchema> getPreferences(Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        NotificationPreferencesResponseSchema response = notificationPreferencesService.getPreferences(
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<NotificationPreferencesResponseSchema> updatePreferences(
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
