package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.CustomerLifecycleEventEntity;

public interface CustomerLifecycleEventJpaRepository extends JpaRepository<CustomerLifecycleEventEntity, String> {
}
