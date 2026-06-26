package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.StandingOrderLifecycleEventEntity;

public interface StandingOrderLifecycleEventJpaRepository extends JpaRepository<StandingOrderLifecycleEventEntity, String> {
}
