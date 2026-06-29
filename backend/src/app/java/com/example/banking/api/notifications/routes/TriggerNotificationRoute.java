package com.example.banking.api.notifications.routes;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.notifications.NotificationResponseMapper;
import com.example.banking.api.notifications.schemas.NotificationEventAcceptedResponseSchema;
import com.example.banking.api.notifications.schemas.TriggerNotificationSchema;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.TriggerNotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications/events")
@Validated
public class TriggerNotificationRoute {
    private final TriggerNotificationService triggerNotificationService;
    private final CustomerPrincipalResolver principalResolver;
    private final NotificationResponseMapper responseMapper;

    public TriggerNotificationRoute(
            TriggerNotificationService triggerNotificationService,
            CustomerPrincipalResolver principalResolver,
            NotificationResponseMapper responseMapper) {
        this.triggerNotificationService = triggerNotificationService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<NotificationEventAcceptedResponseSchema> trigger(
            @Valid @RequestBody TriggerNotificationSchema request,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        NotificationEventEntity event = triggerNotificationService.trigger(request, principal.userId(), principal.role());
        NotificationEventAcceptedResponseSchema response = responseMapper.toAcceptedResponse(event);

        return ResponseEntity.accepted()
                .location(URI.create("/notifications/events/" + response.notificationEventId()))
                .body(response);
    }
}
