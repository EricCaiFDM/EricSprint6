package com.example.banking.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.example.banking.lib.NotificationDispatchAttemptJpaRepository;
import com.example.banking.models.NotificationDispatchAttemptEntity;

@Repository
public class JpaNotificationDispatchAttemptRepositoryAdapter implements NotificationDispatchAttemptRepository {
    private final NotificationDispatchAttemptJpaRepository attemptJpaRepository;

    public JpaNotificationDispatchAttemptRepositoryAdapter(NotificationDispatchAttemptJpaRepository attemptJpaRepository) {
        this.attemptJpaRepository = attemptJpaRepository;
    }

    @Override
    public NotificationDispatchAttemptEntity save(NotificationDispatchAttemptEntity entity) {
        return attemptJpaRepository.save(entity);
    }

    @Override
    public Page<NotificationDispatchAttemptEntity> listByEventId(String notificationEventId, int page, int pageSize) {
        int normalizedPage = Math.max(0, page - 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        return attemptJpaRepository.findByNotificationEventIdOrderByAttemptNumberAsc(
                notificationEventId,
                PageRequest.of(normalizedPage, normalizedPageSize));
    }

    @Override
    public long countByEventId(String notificationEventId) {
        return attemptJpaRepository.countByNotificationEventId(notificationEventId);
    }
}
