package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.NotificationEventEntity;

public interface NotificationEventJpaRepository extends JpaRepository<NotificationEventEntity, String> {
    Optional<NotificationEventEntity> findByNotificationEventId(String notificationEventId);
}
