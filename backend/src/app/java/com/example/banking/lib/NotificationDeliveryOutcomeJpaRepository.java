package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.NotificationDeliveryOutcomeEntity;

public interface NotificationDeliveryOutcomeJpaRepository extends JpaRepository<NotificationDeliveryOutcomeEntity, String> {
    Optional<NotificationDeliveryOutcomeEntity> findByNotificationEventId(String notificationEventId);
}
