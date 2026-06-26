package com.example.banking.lib;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.CustomerEntity;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, String> {
    Optional<CustomerEntity> findByCustomerIdAndDeletedAtIsNull(String customerId);

    Optional<CustomerEntity> findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(String ownerUserId);

    Optional<CustomerEntity> findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(String createdByUserId);

    List<CustomerEntity> findByDeletedAtIsNullOrderByCreatedAtUtcDesc();

    boolean existsByExternalCustomerKeyIgnoreCaseAndDeletedAtIsNull(String externalCustomerKey);

    boolean existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull(String primaryEmail);
}
