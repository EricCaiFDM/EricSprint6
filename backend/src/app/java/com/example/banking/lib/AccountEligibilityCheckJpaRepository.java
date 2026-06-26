package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.AccountEligibilityCheckEntity;

public interface AccountEligibilityCheckJpaRepository extends JpaRepository<AccountEligibilityCheckEntity, String> {
}
