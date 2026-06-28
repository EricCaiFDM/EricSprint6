package com.example.banking.api.notifications;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.banking.api.notifications.routes.GetNotificationEventRoute;
import com.example.banking.api.notifications.routes.ListNotificationAttemptsRoute;
import com.example.banking.api.notifications.routes.TriggerNotificationRoute;

@Configuration
@Import({
        TriggerNotificationRoute.class,
        GetNotificationEventRoute.class,
        ListNotificationAttemptsRoute.class
})
public class NotificationApiModule {
}
