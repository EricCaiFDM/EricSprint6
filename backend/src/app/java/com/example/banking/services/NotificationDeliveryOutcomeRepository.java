package com.example.banking.services;

import java.util.Optional;

import com.example.banking.models.NotificationDeliveryOutcomeEntity;

public interface NotificationDeliveryOutcomeRepository {
    NotificationDeliveryOutcomeEntity save(NotificationDeliveryOutcomeEntity entity);

    Optional<NotificationDeliveryOutcomeEntity> findByEventId(String notificationEventId);
}
