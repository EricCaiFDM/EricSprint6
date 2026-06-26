package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.AccountEntity;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    @Query("SELECT COUNT(a) > 0 FROM AccountEntity a WHERE a.customerId = :customerId")
    boolean existsByCustomerId(@Param("customerId") String customerId);
}
