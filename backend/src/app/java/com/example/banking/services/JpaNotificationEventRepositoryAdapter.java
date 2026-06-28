package com.example.banking.services;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.NotificationEventJpaRepository;
import com.example.banking.models.NotificationEventEntity;

@Repository
public class JpaNotificationEventRepositoryAdapter implements NotificationEventRepository {
    private final NotificationEventJpaRepository notificationEventJpaRepository;

    public JpaNotificationEventRepositoryAdapter(NotificationEventJpaRepository notificationEventJpaRepository) {
        this.notificationEventJpaRepository = notificationEventJpaRepository;
    }

    @Override
    public NotificationEventEntity save(NotificationEventEntity entity) {
        return notificationEventJpaRepository.save(entity);
    }

    @Override
    public Optional<NotificationEventEntity> findById(String notificationEventId) {
        return notificationEventJpaRepository.findByNotificationEventId(notificationEventId);
    }
}
