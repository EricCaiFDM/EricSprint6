package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.IdempotencyRecordEntity;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, String> {
    Optional<IdempotencyRecordEntity> findByIdempotencyKeyAndOperationType(
            String idempotencyKey,
            IdempotencyOperationType operationType);
}
