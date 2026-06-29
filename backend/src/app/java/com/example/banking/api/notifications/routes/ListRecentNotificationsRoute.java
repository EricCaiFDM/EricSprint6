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
