package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.NotificationPreferenceSnapshotEntity;

public interface NotificationPreferenceSnapshotJpaRepository extends JpaRepository<NotificationPreferenceSnapshotEntity, String> {
    Optional<NotificationPreferenceSnapshotEntity> findByNotificationEventId(String notificationEventId);
}
