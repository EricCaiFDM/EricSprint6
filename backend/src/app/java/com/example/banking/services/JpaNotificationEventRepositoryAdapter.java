package com.example.banking.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
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

    @Override
    public List<NotificationEventEntity> listRecent(int size) {
        int normalizedSize = Math.max(1, Math.min(size, 100));
        return notificationEventJpaRepository
                .findAllByOrderByTriggeredAtUtcDesc(PageRequest.of(0, normalizedSize))
                .getContent();
    }
}
