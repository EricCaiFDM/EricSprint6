package com.example.banking.lib;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.AccountEntity;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccountIdAndDeletedAtIsNull(String accountId);

    List<AccountEntity> findByCustomerIdAndDeletedAtIsNull(String customerId);

    @Query("SELECT COUNT(a) > 0 FROM AccountEntity a WHERE a.customerId = :customerId")
    boolean existsByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT COUNT(a) > 0 FROM AccountEntity a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    boolean existsByCustomerIdAndDeletedAtIsNull(@Param("customerId") String customerId);

    boolean existsByAccountNumberIgnoreCase(String accountNumber);
}
