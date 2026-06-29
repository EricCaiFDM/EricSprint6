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
