package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.NotificationEventEntity;

public interface NotificationEventJpaRepository extends JpaRepository<NotificationEventEntity, String> {
    Optional<NotificationEventEntity> findByNotificationEventId(String notificationEventId);

    Page<NotificationEventEntity> findAllByOrderByTriggeredAtUtcDesc(Pageable pageable);
}
