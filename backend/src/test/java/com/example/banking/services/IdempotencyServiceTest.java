package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.IdempotencyRecordJpaRepository;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.IdempotencyRecordEntity;
import com.example.banking.models.IdempotencyStatus;

class IdempotencyServiceTest {

    private RepositoryDouble repositoryDouble;
    private TransactionModuleConfig transactionModuleConfig;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        repositoryDouble = new RepositoryDouble();
        transactionModuleConfig = new TransactionModuleConfig();
        transactionModuleConfig.setIdempotencyTtlHours(24);
        service = new IdempotencyService(repositoryDouble.proxy(), transactionModuleConfig);
    }

    @Test
    void acquireRejectsMissingOrBlankIdempotencyKey() {
        ApiErrorException missing = captureAcquireError(IdempotencyOperationType.DEPOSIT, null, "hash-1");
        assertNotNull(missing);
        assertEquals("TRANSACTION_VALIDATION_ERROR", missing.getCode());
        assertEquals("Idempotency-Key", missing.getField());

        ApiErrorException blank = captureAcquireError(IdempotencyOperationType.DEPOSIT, "   ", "hash-1");
        assertNotNull(blank);
        assertEquals("TRANSACTION_VALIDATION_ERROR", blank.getCode());
        assertEquals("Idempotency-Key", blank.getField());
    }

    @Test
    void acquireRejectsIdempotencyKeyOutsideLengthBounds() {
        ApiErrorException shortKey = captureAcquireError(IdempotencyOperationType.DEPOSIT, "short7", "hash-1");
        assertNotNull(shortKey);
        assertEquals("TRANSACTION_VALIDATION_ERROR", shortKey.getCode());
        assertEquals("Idempotency-Key", shortKey.getField());

        ApiErrorException longKey = captureAcquireError(
                IdempotencyOperationType.DEPOSIT,
                "x".repeat(129),
                "hash-1");
        assertNotNull(longKey);
        assertEquals("TRANSACTION_VALIDATION_ERROR", longKey.getCode());
        assertEquals("Idempotency-Key", longKey.getField());
    }

    @Test
    void acquireRejectsMissingOrBlankRequestHash() {
        ApiErrorException missing = captureAcquireError(IdempotencyOperationType.TRANSFER, "idem-key-1", null);
        assertNotNull(missing);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", missing.getCode());
        assertEquals("Idempotency-Key", missing.getField());

        ApiErrorException blank = captureAcquireError(IdempotencyOperationType.TRANSFER, "idem-key-1", " ");
        assertNotNull(blank);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", blank.getCode());
        assertEquals("Idempotency-Key", blank.getField());
    }

    @Test
    void acquireCreatesNewRecordUsingNormalizedValuesAndMinimumTtl() {
        transactionModuleConfig.setIdempotencyTtlHours(0);

        IdempotencyService.AcquireResult result = service.acquire(
                IdempotencyOperationType.WITHDRAWAL,
                "  idem-key-2  ",
                "  request-hash-2  ");

        assertFalse(result.replay());
        assertNotNull(result.record());
        assertEquals("idem-key-2", result.record().getIdempotencyKey());
        assertEquals("request-hash-2", result.record().getRequestHash());
        assertEquals(IdempotencyOperationType.WITHDRAWAL, result.record().getOperationType());
        assertEquals(IdempotencyStatus.IN_PROGRESS, result.record().getStatus());
        assertNotNull(result.record().getCreatedAtUtc());
        assertNotNull(result.record().getExpiresAtUtc());
        assertEquals(3600L, Duration.between(result.record().getCreatedAtUtc(), result.record().getExpiresAtUtc()).getSeconds());
        assertEquals(1, repositoryDouble.saveCalls());
    }

    @Test
    void acquireReturnsReplayForSucceededRecordWithMatchingHash() {
        IdempotencyRecordEntity existing = record(
                "idem-key-3",
                IdempotencyOperationType.TRANSFER,
                "hash-3",
                IdempotencyStatus.SUCCEEDED,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600));
        repositoryDouble.put(existing);

        IdempotencyService.AcquireResult result = service.acquire(
                IdempotencyOperationType.TRANSFER,
                "  idem-key-3 ",
                " hash-3 ");

        assertTrue(result.replay());
        assertSame(existing, result.record());
        assertEquals(0, repositoryDouble.saveCalls());
    }

    @Test
    void acquireTreatsNullExpiryAsNonExpiredRecord() {
        IdempotencyRecordEntity existing = record(
                "idem-key-3b",
                IdempotencyOperationType.TRANSFER,
                "hash-3b",
                IdempotencyStatus.SUCCEEDED,
                Instant.now().minusSeconds(60),
                null);
        repositoryDouble.put(existing);

        IdempotencyService.AcquireResult result = service.acquire(
                IdempotencyOperationType.TRANSFER,
                "idem-key-3b",
                "hash-3b");

        assertTrue(result.replay());
        assertSame(existing, result.record());
        assertEquals(0, repositoryDouble.saveCalls());
    }

    @Test
    void acquireRejectsDifferentPayloadForExistingKey() {
        IdempotencyRecordEntity existing = record(
                "idem-key-4",
                IdempotencyOperationType.TRANSFER,
                "hash-a",
                IdempotencyStatus.SUCCEEDED,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600));
        repositoryDouble.put(existing);

        ApiErrorException conflict = captureAcquireError(IdempotencyOperationType.TRANSFER, "idem-key-4", "hash-b");
        assertNotNull(conflict);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", conflict.getCode());
        assertTrue(conflict.getMessage().contains("different payload"));
    }

    @Test
    void acquireRejectsInProgressRecordEvenIfExpired() {
        IdempotencyRecordEntity existing = record(
                "idem-key-5",
                IdempotencyOperationType.DEPOSIT,
                "hash-5",
                IdempotencyStatus.IN_PROGRESS,
                Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(1));
        repositoryDouble.put(existing);

        ApiErrorException conflict = captureAcquireError(IdempotencyOperationType.DEPOSIT, "idem-key-5", "hash-5");
        assertNotNull(conflict);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", conflict.getCode());
        assertTrue(conflict.getMessage().contains("already in progress"));
    }

    @Test
    void acquireRejectsFailedRecordWhenNotExpired() {
        IdempotencyRecordEntity existing = record(
                "idem-key-6",
                IdempotencyOperationType.WITHDRAWAL,
                "hash-6",
                IdempotencyStatus.FAILED,
                Instant.now().minusSeconds(600),
                Instant.now().plusSeconds(1200));
        repositoryDouble.put(existing);

        ApiErrorException conflict = captureAcquireError(IdempotencyOperationType.WITHDRAWAL, "idem-key-6", "hash-6");
        assertNotNull(conflict);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", conflict.getCode());
        assertTrue(conflict.getMessage().contains("previous request"));
    }

    @Test
    void acquireRefreshesExpiredNonInProgressRecord() {
        transactionModuleConfig.setIdempotencyTtlHours(2);

        Instant oldCreatedAt = Instant.now().minusSeconds(10_000);
        IdempotencyRecordEntity existing = record(
                "idem-key-7",
                IdempotencyOperationType.TRANSFER,
                "old-hash",
                IdempotencyStatus.FAILED,
                oldCreatedAt,
                Instant.now().minusSeconds(10));
        existing.setResponseTransactionId("txn-old");
        existing.setResponsePayload("payload-old");
        existing.setFailureReason("declined");
        repositoryDouble.put(existing);

        IdempotencyService.AcquireResult result = service.acquire(
                IdempotencyOperationType.TRANSFER,
                "idem-key-7",
                "new-hash");

        assertFalse(result.replay());
        assertSame(existing, result.record());
        assertEquals(IdempotencyStatus.IN_PROGRESS, existing.getStatus());
        assertEquals("new-hash", existing.getRequestHash());
        assertNull(existing.getResponseTransactionId());
        assertNull(existing.getResponsePayload());
        assertNull(existing.getFailureReason());
        assertTrue(existing.getCreatedAtUtc().isAfter(oldCreatedAt));
        assertEquals(7200L, Duration.between(existing.getCreatedAtUtc(), existing.getExpiresAtUtc()).getSeconds());
        assertEquals(1, repositoryDouble.saveCalls());
    }

    @Test
    void acquireResolvesRaceByReadingExistingRecordAfterInsertConflict() {
        repositoryDouble.failNextSaveWithIntegrityViolation();

        IdempotencyRecordEntity existing = record(
                "idem-key-8",
                IdempotencyOperationType.DEPOSIT,
                "hash-8",
                IdempotencyStatus.SUCCEEDED,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600));
            repositoryDouble.setRaceRecordAfterIntegrityViolation(existing);

        IdempotencyService.AcquireResult result = service.acquire(
                IdempotencyOperationType.DEPOSIT,
                "idem-key-8",
                "hash-8");

        assertTrue(result.replay());
        assertSame(existing, result.record());
        assertEquals(1, repositoryDouble.saveCalls());
    }

    @Test
    void acquireThrowsConflictWhenRaceLookupCannotFindRecord() {
        repositoryDouble.failNextSaveWithIntegrityViolation();

        ApiErrorException conflict = captureAcquireError(
                IdempotencyOperationType.DEPOSIT,
                "idem-key-9",
                "hash-9");

        assertNotNull(conflict);
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", conflict.getCode());
        assertTrue(conflict.getMessage().contains("Unable to acquire idempotency key"));
        assertEquals(1, repositoryDouble.saveCalls());
    }

    @Test
    void markSucceededUpdatesResponseAndClearsFailureReason() {
        IdempotencyRecordEntity record = record(
                "idem-key-10",
                IdempotencyOperationType.TRANSFER,
                "hash-10",
                IdempotencyStatus.IN_PROGRESS,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(1800));
        record.setFailureReason("old-failure");

        IdempotencyRecordEntity saved = service.markSucceeded(record, "txn-10", "payload-10");

        assertSame(record, saved);
        assertEquals(IdempotencyStatus.SUCCEEDED, record.getStatus());
        assertEquals("txn-10", record.getResponseTransactionId());
        assertEquals("payload-10", record.getResponsePayload());
        assertNull(record.getFailureReason());
        assertEquals(1, repositoryDouble.saveCalls());
    }

    @Test
    void markFailedUsesProvidedReasonOrUnknownFallback() {
        IdempotencyRecordEntity explicit = record(
                "idem-key-11",
                IdempotencyOperationType.WITHDRAWAL,
                "hash-11",
                IdempotencyStatus.IN_PROGRESS,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(1800));
        IdempotencyRecordEntity unknown = record(
                "idem-key-12",
                IdempotencyOperationType.WITHDRAWAL,
                "hash-12",
                IdempotencyStatus.IN_PROGRESS,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(1800));

        service.markFailed(explicit, "DECLINED");
        service.markFailed(unknown, null);

        assertEquals(IdempotencyStatus.FAILED, explicit.getStatus());
        assertEquals("DECLINED", explicit.getFailureReason());
        assertEquals(IdempotencyStatus.FAILED, unknown.getStatus());
        assertEquals("UNKNOWN", unknown.getFailureReason());
        assertEquals(2, repositoryDouble.saveCalls());
    }

    private ApiErrorException captureAcquireError(
            IdempotencyOperationType operationType,
            String idempotencyKey,
            String requestHash) {
        ApiErrorException exception = null;
        try {
            service.acquire(operationType, idempotencyKey, requestHash);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private IdempotencyRecordEntity record(
            String idempotencyKey,
            IdempotencyOperationType operationType,
            String requestHash,
            IdempotencyStatus status,
            Instant createdAtUtc,
            Instant expiresAtUtc) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setIdempotencyKey(idempotencyKey);
        record.setOperationType(operationType);
        record.setRequestHash(requestHash);
        record.setStatus(status);
        record.setCreatedAtUtc(createdAtUtc);
        record.setExpiresAtUtc(expiresAtUtc);
        return record;
    }

    private static final class RepositoryDouble implements InvocationHandler {
        private final Map<String, IdempotencyRecordEntity> storage = new HashMap<>();
        private int saveCalls;
        private boolean failNextSaveWithIntegrityViolation;
        private IdempotencyRecordEntity raceRecordAfterIntegrityViolation;

        private IdempotencyRecordJpaRepository proxy() {
            return (IdempotencyRecordJpaRepository) Proxy.newProxyInstance(
                    IdempotencyRecordJpaRepository.class.getClassLoader(),
                    new Class<?>[] { IdempotencyRecordJpaRepository.class },
                    this);
        }

        private void put(IdempotencyRecordEntity record) {
            storage.put(storageKey(record.getIdempotencyKey(), record.getOperationType()), record);
        }

        private void failNextSaveWithIntegrityViolation() {
            this.failNextSaveWithIntegrityViolation = true;
        }

        private void setRaceRecordAfterIntegrityViolation(IdempotencyRecordEntity record) {
            this.raceRecordAfterIntegrityViolation = record;
        }

        private int saveCalls() {
            return saveCalls;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            if ("findByIdempotencyKeyAndOperationType".equals(methodName)) {
                String key = (String) args[0];
                IdempotencyOperationType operationType = (IdempotencyOperationType) args[1];
                return Optional.ofNullable(storage.get(storageKey(key, operationType)));
            }

            if ("save".equals(methodName)) {
                saveCalls += 1;
                if (failNextSaveWithIntegrityViolation) {
                    failNextSaveWithIntegrityViolation = false;
                    if (raceRecordAfterIntegrityViolation != null) {
                        storage.put(
                                storageKey(
                                        raceRecordAfterIntegrityViolation.getIdempotencyKey(),
                                        raceRecordAfterIntegrityViolation.getOperationType()),
                                raceRecordAfterIntegrityViolation);
                    }
                    throw new DataIntegrityViolationException("duplicate key");
                }

                IdempotencyRecordEntity record = (IdempotencyRecordEntity) args[0];
                if (record.getId() == null) {
                    record.setId(UUID.randomUUID().toString());
                }
                storage.put(storageKey(record.getIdempotencyKey(), record.getOperationType()), record);
                return record;
            }

            if (method.getReturnType().equals(boolean.class)) {
                return false;
            }
            if (method.getReturnType().equals(long.class)) {
                return 0L;
            }
            if (method.getReturnType().equals(int.class)) {
                return 0;
            }
            if (Optional.class.equals(method.getReturnType())) {
                return Optional.empty();
            }
            if (List.class.isAssignableFrom(method.getReturnType())) {
                return List.of();
            }
            return null;
        }

        private String storageKey(String idempotencyKey, IdempotencyOperationType operationType) {
            return idempotencyKey + "|" + operationType.name();
        }
    }
}