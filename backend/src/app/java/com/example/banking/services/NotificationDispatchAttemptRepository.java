package com.example.banking.services;

import org.springframework.data.domain.Page;

import com.example.banking.models.NotificationDispatchAttemptEntity;

public interface NotificationDispatchAttemptRepository {
    NotificationDispatchAttemptEntity save(NotificationDispatchAttemptEntity entity);

    Page<NotificationDispatchAttemptEntity> listByEventId(String notificationEventId, int page, int pageSize);

    long countByEventId(String notificationEventId);
}
