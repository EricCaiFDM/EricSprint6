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
