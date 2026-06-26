package com.example.banking.services;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.models.AccountEntity;

@Service
public class BalanceConsistencyService {
    private final AccountJpaRepository accountJpaRepository;
    private final MoneyPolicy moneyPolicy;

    public BalanceConsistencyService(AccountJpaRepository accountJpaRepository, MoneyPolicy moneyPolicy) {
        this.accountJpaRepository = accountJpaRepository;
        this.moneyPolicy = moneyPolicy;
    }

    @Transactional
    public AccountEntity lockActiveAccount(String accountId) {
        AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNullForUpdate(accountId)
                .orElseThrow(() -> TransactionErrors.accountNotFound("accountId"));
        ensureActiveStatus(account);
        return account;
    }

    @Transactional
    public LockedAccountPair lockAccountPair(String sourceAccountId, String destinationAccountId) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw TransactionErrors.conflict("sourceAccountId and destinationAccountId must be different", "destinationAccountId");
        }

        String first = sourceAccountId.compareTo(destinationAccountId) <= 0 ? sourceAccountId : destinationAccountId;
        String second = sourceAccountId.compareTo(destinationAccountId) <= 0 ? destinationAccountId : sourceAccountId;

        AccountEntity firstAccount = lockActiveAccount(first);
        AccountEntity secondAccount = lockActiveAccount(second);

        if (sourceAccountId.equals(first)) {
            return new LockedAccountPair(firstAccount, secondAccount);
        }
        return new LockedAccountPair(secondAccount, firstAccount);
    }

    @Transactional
    public BalanceMutation applyCredit(AccountEntity account, BigDecimal amount, String currencyCode) {
        ensureActiveStatus(account);
        moneyPolicy.ensureSameCurrency(account.getCurrencyCode(), currencyCode, "currencyCode");

        BigDecimal balanceBefore = moneyPolicy.normalizeBalance(account.getBalance());
        BigDecimal balanceAfter = moneyPolicy.credit(balanceBefore, amount);

        account.setBalance(balanceAfter);
        account.setUpdatedAtUtc(Instant.now());
        AccountEntity saved = accountJpaRepository.save(account);

        return new BalanceMutation(saved, balanceBefore, balanceAfter);
    }

    @Transactional
    public BalanceMutation applyDebit(AccountEntity account, BigDecimal amount, String currencyCode) {
        ensureActiveStatus(account);
        moneyPolicy.ensureSameCurrency(account.getCurrencyCode(), currencyCode, "currencyCode");

        BigDecimal balanceBefore = moneyPolicy.normalizeBalance(account.getBalance());
        BigDecimal balanceAfter = moneyPolicy.debit(balanceBefore, amount);

        account.setBalance(balanceAfter);
        account.setUpdatedAtUtc(Instant.now());
        AccountEntity saved = accountJpaRepository.save(account);

        return new BalanceMutation(saved, balanceBefore, balanceAfter);
    }

    private void ensureActiveStatus(AccountEntity account) {
        if (account == null || account.getStatus() == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw TransactionErrors.conflict("Transactions can only be posted to ACTIVE accounts", "accountId");
        }
    }

    public record BalanceMutation(AccountEntity account, BigDecimal balanceBefore, BigDecimal balanceAfter) {
    }

    public record LockedAccountPair(AccountEntity sourceAccount, AccountEntity destinationAccount) {
    }
}
