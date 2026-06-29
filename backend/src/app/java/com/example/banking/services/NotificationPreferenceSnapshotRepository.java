package com.example.banking.services;

import java.util.Optional;

import com.example.banking.models.NotificationPreferenceSnapshotEntity;

public interface NotificationPreferenceSnapshotRepository {
    NotificationPreferenceSnapshotEntity save(NotificationPreferenceSnapshotEntity entity);

    Optional<NotificationPreferenceSnapshotEntity> findByEventId(String notificationEventId);
}
