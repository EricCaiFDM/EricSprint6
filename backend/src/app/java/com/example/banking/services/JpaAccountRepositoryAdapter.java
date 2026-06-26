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
}
