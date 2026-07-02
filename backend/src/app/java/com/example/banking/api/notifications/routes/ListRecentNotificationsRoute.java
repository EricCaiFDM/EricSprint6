package com.example.banking.api.notifications.routes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.notifications.schemas.NotificationFeedItemSchema;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.ListRecentNotificationsService;

import io.swagger.v3.oas.annotations.media.ArraySchema;
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
public class ListRecentNotificationsRoute {
    private final ListRecentNotificationsService listRecentNotificationsService;
    private final CustomerPrincipalResolver principalResolver;

    public ListRecentNotificationsRoute(
            ListRecentNotificationsService listRecentNotificationsService,
            CustomerPrincipalResolver principalResolver) {
        this.listRecentNotificationsService = listRecentNotificationsService;
        this.principalResolver = principalResolver;
    }

        @Operation(
            summary = "List recent notifications",
            description = "Returns a recent notifications feed for the authenticated scope with optional size control.")
        @ApiResponse(
            responseCode = "200",
            description = "Recent notifications",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = NotificationFeedItemSchema.class)),
                examples = @ExampleObject(value = "[{\"notificationEventId\":\"31f5fd3d-40cb-4f9f-bec7-d3eec559c5b8\",\"eventType\":\"LOW_BALANCE\",\"title\":\"Low balance alert\",\"message\":\"Your balance is below AUD 100.00\",\"channel\":\"EMAIL\",\"createdAtUtc\":\"2026-07-02T05:00:00Z\",\"status\":\"DELIVERED\"}]")))
    @GetMapping
    public ResponseEntity<List<NotificationFeedItemSchema>> listRecent(
            @RequestParam(defaultValue = "6") int size,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        List<NotificationFeedItemSchema> response = listRecentNotificationsService.listRecent(
                size,
                principal.userId(),
                principal.role());
        return ResponseEntity.ok(response);
    }
}
