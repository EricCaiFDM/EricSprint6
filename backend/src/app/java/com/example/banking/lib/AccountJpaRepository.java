package com.example.banking.lib;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.AccountEntity;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccountIdAndDeletedAtIsNull(String accountId);

    List<AccountEntity> findByDeletedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.accountId = :accountId AND a.deletedAt IS NULL")
    Optional<AccountEntity> findByAccountIdAndDeletedAtIsNullForUpdate(@Param("accountId") String accountId);

    List<AccountEntity> findByCustomerIdAndDeletedAtIsNull(String customerId);

    @Query("SELECT COUNT(a) > 0 FROM AccountEntity a WHERE a.customerId = :customerId")
    boolean existsByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT COUNT(a) > 0 FROM AccountEntity a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    boolean existsByCustomerIdAndDeletedAtIsNull(@Param("customerId") String customerId);

    boolean existsByAccountNumberIgnoreCase(String accountNumber);
}
