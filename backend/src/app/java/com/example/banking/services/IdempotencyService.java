package com.example.banking.services;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.lib.IdempotencyRecordJpaRepository;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.IdempotencyRecordEntity;
import com.example.banking.models.IdempotencyStatus;

@Service
public class IdempotencyService {
    private final IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;
    private final TransactionModuleConfig transactionModuleConfig;

    public IdempotencyService(
            IdempotencyRecordJpaRepository idempotencyRecordJpaRepository,
            TransactionModuleConfig transactionModuleConfig) {
        this.idempotencyRecordJpaRepository = idempotencyRecordJpaRepository;
        this.transactionModuleConfig = transactionModuleConfig;
    }

    @Transactional
    public AcquireResult acquire(IdempotencyOperationType operationType, String idempotencyKey, String requestHash) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String normalizedHash = normalizeRequestHash(requestHash);

        return idempotencyRecordJpaRepository.findByIdempotencyKeyAndOperationType(normalizedKey, operationType)
                .map(record -> evaluateExisting(record, normalizedHash))
                .orElseGet(() -> createOrResolveRace(operationType, normalizedKey, normalizedHash));
    }

    @Transactional
    public IdempotencyRecordEntity markSucceeded(
            IdempotencyRecordEntity record,
            String responseTransactionId,
            String responsePayload) {
        record.setStatus(IdempotencyStatus.SUCCEEDED);
        record.setResponseTransactionId(responseTransactionId);
        record.setResponsePayload(responsePayload);
        record.setFailureReason(null);
        return idempotencyRecordJpaRepository.save(record);
    }

    @Transactional
    public IdempotencyRecordEntity markFailed(IdempotencyRecordEntity record, String failureReason) {
        record.setStatus(IdempotencyStatus.FAILED);
        record.setFailureReason(failureReason == null ? "UNKNOWN" : failureReason);
        return idempotencyRecordJpaRepository.save(record);
    }

    private AcquireResult createOrResolveRace(
            IdempotencyOperationType operationType,
            String idempotencyKey,
            String requestHash) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        Instant now = Instant.now();
        record.setIdempotencyKey(idempotencyKey);
        record.setOperationType(operationType);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.IN_PROGRESS);
        record.setCreatedAtUtc(now);
        record.setExpiresAtUtc(now.plusSeconds(Math.max(1, transactionModuleConfig.getIdempotencyTtlHours()) * 3600L));

        try {
            return new AcquireResult(idempotencyRecordJpaRepository.save(record), false);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecordEntity existing = idempotencyRecordJpaRepository
                    .findByIdempotencyKeyAndOperationType(idempotencyKey, operationType)
                    .orElseThrow(() -> TransactionErrors.idempotencyConflict("Unable to acquire idempotency key"));
            return evaluateExisting(existing, requestHash);
        }
    }

    private AcquireResult evaluateExisting(IdempotencyRecordEntity existing, String requestHash) {
        Instant now = Instant.now();

        if (existing.getExpiresAtUtc() != null
                && existing.getExpiresAtUtc().isBefore(now)
                && existing.getStatus() != IdempotencyStatus.IN_PROGRESS) {
            existing.setRequestHash(requestHash);
            existing.setStatus(IdempotencyStatus.IN_PROGRESS);
            existing.setResponseTransactionId(null);
            existing.setResponsePayload(null);
            existing.setFailureReason(null);
            existing.setCreatedAtUtc(now);
            existing.setExpiresAtUtc(now.plusSeconds(Math.max(1, transactionModuleConfig.getIdempotencyTtlHours()) * 3600L));
            return new AcquireResult(idempotencyRecordJpaRepository.save(existing), false);
        }

        if (!requestHash.equals(existing.getRequestHash())) {
            throw TransactionErrors.idempotencyConflict("Idempotency-Key has already been used with a different payload");
        }

        if (existing.getStatus() == IdempotencyStatus.SUCCEEDED) {
            return new AcquireResult(existing, true);
        }

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw TransactionErrors.idempotencyConflict("An operation with this Idempotency-Key is already in progress");
        }

        throw TransactionErrors.idempotencyConflict("The previous request with this Idempotency-Key failed");
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw TransactionErrors.validation("Idempotency-Key header is required", "Idempotency-Key");
        }

        String normalized = idempotencyKey.trim();
        if (normalized.length() < 8 || normalized.length() > 128) {
            throw TransactionErrors.validation(
                    "Idempotency-Key must be between 8 and 128 characters",
                    "Idempotency-Key");
        }
        return normalized;
    }

    private String normalizeRequestHash(String requestHash) {
        if (requestHash == null || requestHash.isBlank()) {
            throw TransactionErrors.idempotencyConflict("Request hash could not be generated for idempotency validation");
        }
        return requestHash.trim();
    }

    public record AcquireResult(IdempotencyRecordEntity record, boolean replay) {
    }
}
