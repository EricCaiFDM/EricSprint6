package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.AccountDeletionPolicyCheckEntity;

public interface AccountDeletionPolicyCheckJpaRepository extends JpaRepository<AccountDeletionPolicyCheckEntity, String> {
}
