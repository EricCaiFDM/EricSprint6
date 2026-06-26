package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountEligibilityCheckJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.AccountEligibilityCheckEntity;

@Service
public class AccountEligibilityService {
    private static final Set<String> SUPPORTED_TYPES = Set.of("CHECKING", "SAVINGS");

    private final CustomerJpaRepository customerJpaRepository;
    private final AccountEligibilityCheckJpaRepository eligibilityRepository;

    public AccountEligibilityService(
            CustomerJpaRepository customerJpaRepository,
            AccountEligibilityCheckJpaRepository eligibilityRepository) {
        this.customerJpaRepository = customerJpaRepository;
        this.eligibilityRepository = eligibilityRepository;
    }

    public void enforceEligibility(String customerId, String accountType) {
        String normalizedType = normalizeType(accountType);

        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            recordCheck(customerId, normalizedType, false, "UNSUPPORTED_ACCOUNT_TYPE");
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_CONFLICT",
                    "Unsupported accountType",
                    "accountType");
        }

        boolean exists = customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId).isPresent();
        if (!exists) {
            recordCheck(customerId, normalizedType, false, "CUSTOMER_NOT_FOUND");
            throw new ApiErrorException(
                    HttpStatus.NOT_FOUND,
                    "CUSTOMER_NOT_FOUND",
                    "No customer found with the provided customerId",
                    "customerId");
        }

        recordCheck(customerId, normalizedType, true, null);
    }

    private String normalizeType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return "";
        }
        return accountType.trim().toUpperCase(Locale.ROOT);
    }

    private void recordCheck(String customerId, String accountType, boolean isEligible, String reasonCode) {
        AccountEligibilityCheckEntity check = new AccountEligibilityCheckEntity();
        check.setCustomerId(customerId);
        check.setAccountType(accountType);
        check.setEvaluatedAtUtc(Instant.now());
        check.setEligible(isEligible);
        check.setReasonCode(reasonCode);
        check.setMetadata("{}");
        eligibilityRepository.save(check);
    }
}
