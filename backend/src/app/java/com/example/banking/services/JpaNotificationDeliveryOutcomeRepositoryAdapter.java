package com.example.banking.services;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.NotificationDeliveryOutcomeJpaRepository;
import com.example.banking.models.NotificationDeliveryOutcomeEntity;

@Repository
public class JpaNotificationDeliveryOutcomeRepositoryAdapter implements NotificationDeliveryOutcomeRepository {
    private final NotificationDeliveryOutcomeJpaRepository outcomeJpaRepository;

    public JpaNotificationDeliveryOutcomeRepositoryAdapter(NotificationDeliveryOutcomeJpaRepository outcomeJpaRepository) {
        this.outcomeJpaRepository = outcomeJpaRepository;
    }

    @Override
    public NotificationDeliveryOutcomeEntity save(NotificationDeliveryOutcomeEntity entity) {
        return outcomeJpaRepository.save(entity);
    }

    @Override
    public Optional<NotificationDeliveryOutcomeEntity> findByEventId(String notificationEventId) {
        return outcomeJpaRepository.findByNotificationEventId(notificationEventId);
    }
}
