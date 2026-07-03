package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.TransactionLifecycleEventEntity;

public interface TransactionLifecycleEventJpaRepository extends JpaRepository<TransactionLifecycleEventEntity, String> {
}
