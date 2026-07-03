package com.example.banking.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.models.AccountEntity;

@Repository
public class JpaAccountRepositoryAdapter implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;

    public JpaAccountRepositoryAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public AccountEntity save(AccountEntity account) {
        return accountJpaRepository.save(account);
    }

    @Override
    public Optional<AccountEntity> findActiveById(String accountId) {
        return accountJpaRepository.findByAccountIdAndDeletedAtIsNull(accountId);
    }

    @Override
    public List<AccountEntity> findActiveByCustomerId(String customerId) {
        return accountJpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    @Override
    public boolean existsByCustomerId(String customerId) {
        return accountJpaRepository.existsByCustomerIdAndDeletedAtIsNull(customerId);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountJpaRepository.existsByAccountNumberIgnoreCase(accountNumber);
    }

    @Override
    public boolean existsByCustomerIdAndCheckingNumber(String customerId, int checkingNumber) {
        return accountJpaRepository.existsByCustomerIdAndCheckingNumber(customerId, checkingNumber);
    }

    @Override
    public int nextCheckingNumber(String customerId) {
        Integer maxCheckingNumber = accountJpaRepository.findMaxCheckingNumberByCustomerId(customerId);
        int base = maxCheckingNumber == null ? 0 : Math.max(0, maxCheckingNumber);
        return base + 1;
    }
}
