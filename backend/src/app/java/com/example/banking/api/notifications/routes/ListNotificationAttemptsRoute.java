package com.example.banking.api.notifications.routes;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.api.notifications.NotificationResponseMapper;
import com.example.banking.api.notifications.schemas.ListNotificationAttemptsSchema;
import com.example.banking.api.notifications.schemas.NotificationAttemptListResponseSchema;
import com.example.banking.models.NotificationDispatchAttemptEntity;
import com.example.banking.services.CustomerPrincipal;
import com.example.banking.services.CustomerPrincipalResolver;
import com.example.banking.services.ListNotificationAttemptsService;

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
public class ListNotificationAttemptsRoute {
    private final ListNotificationAttemptsService listNotificationAttemptsService;
    private final CustomerPrincipalResolver principalResolver;
    private final NotificationResponseMapper responseMapper;

    public ListNotificationAttemptsRoute(
            ListNotificationAttemptsService listNotificationAttemptsService,
            CustomerPrincipalResolver principalResolver,
            NotificationResponseMapper responseMapper) {
        this.listNotificationAttemptsService = listNotificationAttemptsService;
        this.principalResolver = principalResolver;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "List notification attempts",
            description = "Returns paginated dispatch attempts and channel outcomes for a notification event.")
    @ApiResponse(
            responseCode = "200",
            description = "Notification attempt page",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = NotificationAttemptListResponseSchema.class),
                    examples = @ExampleObject(value = "{\"items\":[{\"attemptId\":\"fef39f26-2f7c-4d74-b0c4-5a8f2065fdbb\",\"notificationEventId\":\"31f5fd3d-40cb-4f9f-bec7-d3eec559c5b8\",\"channel\":\"EMAIL\",\"status\":\"DELIVERED\",\"attemptedAtUtc\":\"2026-07-02T05:00:02Z\",\"providerReference\":\"smtp-20260702050002\"}],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1}")))
    @GetMapping("/{notificationEventId}/attempts")
    public ResponseEntity<NotificationAttemptListResponseSchema> listAttempts(
            @PathVariable String notificationEventId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CustomerPrincipal principal = principalResolver.resolve(authentication);
        ListNotificationAttemptsSchema query = new ListNotificationAttemptsSchema(notificationEventId, page, pageSize);

        Page<NotificationDispatchAttemptEntity> resultPage = listNotificationAttemptsService.list(
                query.notificationEventId(),
                query.page(),
                query.pageSize(),
                principal.userId(),
                principal.role());

        NotificationAttemptListResponseSchema response = new NotificationAttemptListResponseSchema(
                responseMapper.toAttemptItems(resultPage.getContent()),
                Math.max(1, query.page()),
                Math.max(1, query.pageSize()),
                resultPage.getTotalElements(),
                Math.max(1, resultPage.getTotalPages()));

        return ResponseEntity.ok(response);
    }
}
