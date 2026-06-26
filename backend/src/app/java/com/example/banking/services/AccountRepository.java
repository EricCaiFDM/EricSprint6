package com.example.banking.services;

import java.util.List;
import java.util.Optional;

import com.example.banking.models.AccountEntity;

public interface AccountRepository {
    AccountEntity save(AccountEntity account);

    Optional<AccountEntity> findActiveById(String accountId);

    List<AccountEntity> findActiveByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    boolean existsByAccountNumber(String accountNumber);
}
