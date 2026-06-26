package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.models.IdempotencyOperationType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MonetaryIdempotencyOrchestrator {
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public MonetaryIdempotencyOrchestrator(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(
            IdempotencyOperationType operationType,
            String idempotencyKey,
            String requestHash,
            Supplier<T> operation,
            Class<T> responseType,
            Function<T, String> responseTransactionIdExtractor) {
        IdempotencyService.AcquireResult acquireResult = idempotencyService.acquire(operationType, idempotencyKey, requestHash);

        if (acquireResult.replay()) {
            String payload = acquireResult.record().getResponsePayload();
            if (payload == null || payload.isBlank()) {
                throw TransactionErrors.idempotencyConflict("Stored idempotent response payload is missing");
            }
            return deserialize(payload, responseType);
        }

        try {
            T response = operation.get();
            String payload = serialize(response);
            String responseTransactionId = responseTransactionIdExtractor.apply(response);
            idempotencyService.markSucceeded(acquireResult.record(), responseTransactionId, payload);
            return response;
        } catch (ApiErrorException exception) {
            idempotencyService.markFailed(acquireResult.record(), exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.markFailed(acquireResult.record(), "TRANSACTION_INTERNAL_ERROR");
            throw exception;
        }
    }

    public String hashRequest(String canonicalPayload) {
        if (canonicalPayload == null || canonicalPayload.isBlank()) {
            throw TransactionErrors.idempotencyConflict("Request payload cannot be hashed for idempotency");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw TransactionErrors.idempotencyConflict("SHA-256 is unavailable for idempotency hashing");
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw TransactionErrors.idempotencyConflict("Failed to serialize idempotent response");
        }
    }

    private <T> T deserialize(String payload, Class<T> responseType) {
        try {
            return objectMapper.readValue(payload, responseType);
        } catch (JsonProcessingException exception) {
            throw TransactionErrors.idempotencyConflict("Failed to deserialize idempotent response");
        }
    }
}
