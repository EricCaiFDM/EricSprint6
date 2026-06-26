package com.example.banking.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.account.dto.AccountListResponse;
import com.example.banking.api.account.dto.AccountResponse;
import com.example.banking.api.account.dto.CreateAccountRequest;
import com.example.banking.api.account.dto.UpdateAccountRequest;
import com.example.banking.api.common.ApiErrorException;
import com.example.banking.models.AccountEntity;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountResponsePolicyService responsePolicyService;
    private final AccountAccessPolicyService accessPolicyService;
    private final AccountLifecycleAuditService lifecycleAuditService;
    private final AccountEligibilityService eligibilityService;
    private final AccountDeletionPolicyService deletionPolicyService;

    public AccountService(
            AccountRepository accountRepository,
            AccountResponsePolicyService responsePolicyService,
            AccountAccessPolicyService accessPolicyService,
            AccountLifecycleAuditService lifecycleAuditService,
            AccountEligibilityService eligibilityService,
            AccountDeletionPolicyService deletionPolicyService) {
        this.accountRepository = accountRepository;
        this.responsePolicyService = responsePolicyService;
        this.accessPolicyService = accessPolicyService;
        this.lifecycleAuditService = lifecycleAuditService;
        this.eligibilityService = eligibilityService;
        this.deletionPolicyService = deletionPolicyService;
    }

    public AccountResponse createAccount(CreateAccountRequest request, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            accessPolicyService.enforceCreateAccess(role);
            String customerId = normalizeUuid(request.customerId(), "customerId");
            String accountType = normalizeAccountType(request.accountType());
            eligibilityService.enforceEligibility(customerId, accountType);

            AccountEntity entity = new AccountEntity();
            entity.setAccountId(UUID.randomUUID().toString());
            entity.setCustomerId(customerId);
            entity.setAccountNumber(generateUniqueAccountNumber());
            entity.setAccountType(accountType);
            entity.setStatus("ACTIVE");
            entity.setNickname(normalizeOptional(request.nickname()));
            entity.setBalance(BigDecimal.ZERO.setScale(2));
            entity.setCurrencyCode(request.currencyCode().trim().toUpperCase(Locale.ROOT));
            entity.setOpenedAtUtc(Instant.now());
            entity.setClosedAtUtc(null);
            entity.setCreatedByUserId(actorId);
            entity.setOwnerUserId(customerId);
            entity.setUpdatedAtUtc(Instant.now());
            entity.setDeletedAt(null);

            AccountEntity saved = accountRepository.save(entity);
            lifecycleAuditService.recordSuccess(saved.getAccountId(), "CREATE", actorId, role);
            return responsePolicyService.toResponse(saved, false);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(null, "CREATE", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public AccountResponse getAccountById(String accountId, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            String normalizedAccountId = normalizeUuid(accountId, "accountId");
            accessPolicyService.enforceReadAccess(role);
            AccountEntity account = activeAccountOrThrow(normalizedAccountId);
            accessPolicyService.enforceOwnershipIfRequired(role, actorId, account.getOwnerUserId(), "read");

            boolean masked = "CUSTOMER".equalsIgnoreCase(role);
            lifecycleAuditService.recordSuccess(account.getAccountId(), "GET", actorId, role);
            return responsePolicyService.toResponse(account, masked);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(accountId, "GET", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public AccountListResponse listAccounts(
            String customerId,
            int page,
            int pageSize,
            String accountType,
            String status,
            String actorUserId,
            String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            accessPolicyService.enforceListAccess(role);
            String normalizedCustomerId = normalizeUuid(customerId, "customerId");
            accessPolicyService.enforceListScope(role, actorId, normalizedCustomerId);

            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));

            List<AccountEntity> scoped = accountRepository.findActiveByCustomerId(normalizedCustomerId).stream()
                    .filter(account -> matchesType(account, accountType))
                    .filter(account -> matchesStatus(account, status))
                    .sorted(Comparator.comparing(AccountEntity::getOpenedAtUtc).reversed())
                    .collect(Collectors.toList());

            int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, scoped.size());
            int toIndex = Math.min(fromIndex + normalizedPageSize, scoped.size());
            List<AccountResponse> items = scoped.subList(fromIndex, toIndex).stream()
                    .map(account -> responsePolicyService.toResponse(account, "CUSTOMER".equalsIgnoreCase(role)))
                    .toList();

            long totalItems = scoped.size();
            int totalPages = (int) Math.ceil(totalItems / (double) normalizedPageSize);
            lifecycleAuditService.recordSuccess(null, "LIST", actorId, role);
            return new AccountListResponse(items, normalizedPage, normalizedPageSize, totalItems, Math.max(totalPages, 1));
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(null, "LIST", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public AccountResponse updateAccount(String accountId, UpdateAccountRequest request, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            String normalizedAccountId = normalizeUuid(accountId, "accountId");
            accessPolicyService.enforceUpdateAccess(role);
            AccountEntity account = activeAccountOrThrow(normalizedAccountId);
            accessPolicyService.enforceOwnershipIfRequired(role, actorId, account.getOwnerUserId(), "update");

            if (request.nickname() == null && request.status() == null) {
                throw new ApiErrorException(
                        HttpStatus.BAD_REQUEST,
                        "ACCOUNT_VALIDATION_ERROR",
                        "At least one mutable field is required",
                        null);
            }

            if (request.nickname() != null) {
                account.setNickname(normalizeOptional(request.nickname()));
            }

            if (request.status() != null) {
                String nextStatus = request.status().trim().toUpperCase(Locale.ROOT);
                if (!isAllowedTransition(account.getStatus(), nextStatus)) {
                    throw new ApiErrorException(
                            HttpStatus.BAD_REQUEST,
                            "ACCOUNT_VALIDATION_ERROR",
                            "Invalid status transition",
                            "status");
                }
                account.setStatus(nextStatus);
                if ("CLOSED".equals(nextStatus)) {
                    account.setClosedAtUtc(Instant.now());
                }
            }

            account.setUpdatedAtUtc(Instant.now());
            AccountEntity saved = accountRepository.save(account);
            lifecycleAuditService.recordSuccess(saved.getAccountId(), "UPDATE", actorId, role);
            return responsePolicyService.toResponse(saved, false);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(accountId, "UPDATE", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    public String deleteAccount(String accountId, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);
        try {
            String normalizedAccountId = normalizeUuid(accountId, "accountId");
            accessPolicyService.enforceDeleteAccess(role);
            AccountEntity account = activeAccountOrThrow(normalizedAccountId);
            accessPolicyService.enforceOwnershipIfRequired(role, actorId, account.getOwnerUserId(), "delete");

            lifecycleAuditService.recordSuccess(account.getAccountId(), "DELETE_ATTEMPT", actorId, role);

            AccountDeletionPolicyService.DeletionDecision decision = deletionPolicyService.evaluateDeletion(account);
            if ("BLOCK_DELETE".equals(decision.decision())) {
                lifecycleAuditService.recordFailure(
                        account.getAccountId(),
                        "DELETE_BLOCKED",
                        actorId,
                        role,
                        "ACCOUNT_DELETE_BLOCKED",
                        "{\"reasons\":\"" + String.join(",", decision.blockerReasons()) + "\"}");
            }
            deletionPolicyService.enforceDeletionAllowed(decision);

            account.setDeletedAt(Instant.now());
            account.setUpdatedAtUtc(Instant.now());
            account.setStatus("CLOSED");
            if (account.getClosedAtUtc() == null) {
                account.setClosedAtUtc(Instant.now());
            }
            accountRepository.save(account);

            lifecycleAuditService.recordSuccess(account.getAccountId(), "DELETE_SUCCESS", actorId, role);
            return "CLOSED";
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(accountId, "DELETE_ATTEMPT", actorId, role, exception.getCode(), "{}");
            throw exception;
        }
    }

    private AccountEntity activeAccountOrThrow(String accountId) {
        return accountRepository.findActiveById(accountId)
                .orElseThrow(() -> new ApiErrorException(
                        HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND",
                        "No account found with the provided accountId",
                        "accountId"));
    }

    private String normalizeUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiErrorException(HttpStatus.BAD_REQUEST, "ACCOUNT_VALIDATION_ERROR", field + " is required", field);
        }
        try {
            UUID uuid = UUID.fromString(value.trim());
            return uuid.toString();
        } catch (IllegalArgumentException exception) {
            throw new ApiErrorException(HttpStatus.BAD_REQUEST, "ACCOUNT_VALIDATION_ERROR", field + " must be a UUID", field);
        }
    }

    private String normalizeAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            throw new ApiErrorException(
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_VALIDATION_ERROR",
                    "accountType is required",
                    "accountType");
        }
        return accountType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = "NB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    private boolean matchesType(AccountEntity account, String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return true;
        }
        return account.getAccountType().equalsIgnoreCase(accountType.trim());
    }

    private boolean matchesStatus(AccountEntity account, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return account.getStatus().equalsIgnoreCase(status.trim());
    }

    private boolean isAllowedTransition(String currentStatus, String nextStatus) {
        String current = currentStatus == null ? "ACTIVE" : currentStatus.toUpperCase(Locale.ROOT);
        if (current.equals(nextStatus)) {
            return true;
        }
        return ("ACTIVE".equals(current) && "SUSPENDED".equals(nextStatus))
                || ("SUSPENDED".equals(current) && "ACTIVE".equals(nextStatus))
                || (("ACTIVE".equals(current) || "SUSPENDED".equals(current)) && "CLOSED".equals(nextStatus));
    }

}
