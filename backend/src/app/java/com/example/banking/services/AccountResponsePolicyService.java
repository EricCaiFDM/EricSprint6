package com.example.banking.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.banking.api.account.dto.AccountResponse;
import com.example.banking.models.AccountEntity;

@Service
public class AccountResponsePolicyService {
    private final AccountFieldMaskingService maskingService;

    public AccountResponsePolicyService(AccountFieldMaskingService maskingService) {
        this.maskingService = maskingService;
    }

    public AccountResponse toResponse(AccountEntity entity, boolean masked) {
        String accountNumber = masked ? maskingService.maskAccountNumber(entity.getAccountNumber()) : entity.getAccountNumber();
        BigDecimal normalizedBalance = entity.getBalance() == null ? BigDecimal.ZERO.setScale(2) : entity.getBalance();
        BigDecimal normalizedInterestRate = entity.getInterestRate() == null ? BigDecimal.ZERO.setScale(4) : entity.getInterestRate();
        String balance = normalizedBalance.toPlainString();
        return new AccountResponse(
                entity.getAccountId(),
                accountNumber,
            entity.getCheckingNumber(),
                entity.getCustomerId(),
                entity.getAccountType(),
            normalizedInterestRate.toPlainString(),
                entity.getStatus(),
                entity.getCurrencyCode(),
                entity.getNickname(),
                balance,
                balance,
                balance,
                entity.getOpenedAtUtc().toString(),
                entity.getClosedAtUtc() == null ? null : entity.getClosedAtUtc().toString());
    }
}
