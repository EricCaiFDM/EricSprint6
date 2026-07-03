package com.example.banking.services;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.NotificationPreferenceSnapshotJpaRepository;
import com.example.banking.models.NotificationPreferenceSnapshotEntity;

@Repository
public class JpaNotificationPreferenceSnapshotRepositoryAdapter implements NotificationPreferenceSnapshotRepository {
    private final NotificationPreferenceSnapshotJpaRepository snapshotJpaRepository;

    public JpaNotificationPreferenceSnapshotRepositoryAdapter(NotificationPreferenceSnapshotJpaRepository snapshotJpaRepository) {
        this.snapshotJpaRepository = snapshotJpaRepository;
    }

    @Override
    public NotificationPreferenceSnapshotEntity save(NotificationPreferenceSnapshotEntity entity) {
        return snapshotJpaRepository.save(entity);
    }

    @Override
    public Optional<NotificationPreferenceSnapshotEntity> findByEventId(String notificationEventId) {
        return snapshotJpaRepository.findByNotificationEventId(notificationEventId);
    }
}
