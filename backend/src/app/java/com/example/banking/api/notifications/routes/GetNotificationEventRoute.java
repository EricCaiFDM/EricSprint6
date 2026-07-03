package com.example.banking.api.notifications.routes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.notifications.NotificationResponseMapper;
import com.example.banking.api.notifications.schemas.GetNotificationEventSchema;
import com.example.banking.api.notifications.schemas.NotificationEventResponseSchema;
import com.example.banking.models.NotificationEvent;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.GetNotificationEventService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/notifications/events")
@Validated
public class GetNotificationEventRoute {
    private final GetNotificationEventService getNotificationEventService;
    private final CustomerPrincipalResolver principalResolver;
    private final NotificationResponseMapper responseMapper;

    public GetNotificationEventRoute(
            GetNotificationEventService getNotificationEventService,
            CustomerPrincipalResolver principalResolver,
            NotificationResponseMapper responseMapper) {
        this.getNotificationEventService = getNotificationEventService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Get notification event",
            description = "Returns current status and delivery outcome details for a specific notification event.")
        @ApiResponse(
            responseCode = "200",
            description = "Notification event details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationEventResponseSchema.class),
                examples = @ExampleObject(value = "{\"notificationEventId\":\"31f5fd3d-40cb-4f9f-bec7-d3eec559c5b8\",\"eventType\":\"LOW_BALANCE\",\"status\":\"DELIVERED\",\"channel\":\"EMAIL\",\"recipient\":\"customer@example.com\",\"createdAtUtc\":\"2026-07-02T05:00:00Z\",\"updatedAtUtc\":\"2026-07-02T05:00:03Z\"}")))
    @GetMapping("/{notificationEventId}")
    public ResponseEntity<NotificationEventResponseSchema> getById(
            @PathVariable String notificationEventId,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        GetNotificationEventSchema query = new GetNotificationEventSchema(notificationEventId);
        NotificationEvent event = getNotificationEventService.getById(
                query.notificationEventId(),
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(responseMapper.toEventResponse(event));
    }
}
