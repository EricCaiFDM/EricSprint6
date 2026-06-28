package com.example.banking.services;

import com.example.banking.models.NotificationChannel;

public interface ChannelDispatchAdapter {
    ChannelDispatchResult dispatch(
            NotificationChannel channel,
            String templateCode,
            String sanitizedTemplateContext,
            String notificationEventId,
            int attemptNumber);
}
