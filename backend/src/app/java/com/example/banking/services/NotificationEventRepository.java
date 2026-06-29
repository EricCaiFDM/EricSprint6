package com.example.banking.services;

import java.util.List;
import java.util.Optional;

import com.example.banking.models.NotificationEventEntity;

public interface NotificationEventRepository {
    NotificationEventEntity save(NotificationEventEntity entity);

    Optional<NotificationEventEntity> findById(String notificationEventId);

    List<NotificationEventEntity> listRecent(int size);
}
