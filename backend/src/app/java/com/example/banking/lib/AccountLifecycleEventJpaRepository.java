package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.AccountLifecycleEventEntity;

public interface AccountLifecycleEventJpaRepository extends JpaRepository<AccountLifecycleEventEntity, String> {
}
