package com.example.banking.lib;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.NotificationDispatchAttemptEntity;

public interface NotificationDispatchAttemptJpaRepository extends JpaRepository<NotificationDispatchAttemptEntity, String> {
    Page<NotificationDispatchAttemptEntity> findByNotificationEventIdOrderByAttemptNumberAsc(
            String notificationEventId,
            Pageable pageable);

    long countByNotificationEventId(String notificationEventId);
}
