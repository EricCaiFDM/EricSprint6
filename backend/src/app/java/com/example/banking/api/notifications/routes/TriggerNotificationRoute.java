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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Notifications")
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

    @Operation(
            summary = "Trigger notification event",
            description = "Creates a notification event for dispatch processing and returns the accepted event reference.")
        @ApiResponse(
            responseCode = "202",
            description = "Notification accepted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationEventAcceptedResponseSchema.class),
                examples = @ExampleObject(value = "{\"notificationEventId\":\"31f5fd3d-40cb-4f9f-bec7-d3eec559c5b8\",\"status\":\"ACCEPTED\",\"createdAtUtc\":\"2026-07-02T05:00:00Z\"}")))
    @PostMapping
    public ResponseEntity<NotificationEventAcceptedResponseSchema> trigger(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TriggerNotificationSchema.class),
                    examples = @ExampleObject(value = "{\"eventType\":\"LOW_BALANCE\",\"channel\":\"EMAIL\",\"recipient\":\"customer@example.com\",\"subject\":\"Low balance alert\",\"message\":\"Your account balance is below AUD 100.00\"}")))
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
