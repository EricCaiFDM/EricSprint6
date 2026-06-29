package com.example.banking.api.notifications;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.banking.api.notifications.routes.GetNotificationEventRoute;
import com.example.banking.api.notifications.routes.ListNotificationAttemptsRoute;
import com.example.banking.api.notifications.routes.ListRecentNotificationsRoute;
import com.example.banking.api.notifications.routes.NotificationPreferencesRoute;
import com.example.banking.api.notifications.routes.TriggerNotificationRoute;

@Configuration
@Import({
        TriggerNotificationRoute.class,
        ListRecentNotificationsRoute.class,
        GetNotificationEventRoute.class,
        ListNotificationAttemptsRoute.class,
        NotificationPreferencesRoute.class
})
public class NotificationApiModule {
}
