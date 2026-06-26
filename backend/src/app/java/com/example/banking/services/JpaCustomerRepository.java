package com.example.banking.services;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.CustomerEntity;

@Repository
public class JpaCustomerRepository implements CustomerRepository {
    private final CustomerJpaRepository customerJpaRepository;

    public JpaCustomerRepository(CustomerJpaRepository customerJpaRepository) {
        this.customerJpaRepository = customerJpaRepository;
    }

    @Override
    public CustomerEntity save(CustomerEntity customer) {
        return customerJpaRepository.save(customer);
    }

    @Override
    public Optional<CustomerEntity> findActiveById(String customerId) {
        return customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    @Override
    public Optional<CustomerEntity> findLatestActiveByOwnerUserId(String ownerUserId) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
            return Optional.empty();
        }

        return customerJpaRepository.findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(ownerUserId);
    }

    @Override
    public Optional<CustomerEntity> findLatestActiveByCreatorUserId(String creatorUserId) {
        if (creatorUserId == null || creatorUserId.isBlank()) {
            return Optional.empty();
        }

        return customerJpaRepository.findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(creatorUserId);
    }

    @Override
    public boolean existsByExternalCustomerKey(String externalCustomerKey) {
        if (externalCustomerKey == null) {
            return false;
        }
        return customerJpaRepository.existsByExternalCustomerKeyIgnoreCaseAndDeletedAtIsNull(
                externalCustomerKey.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean existsByPrimaryEmail(String primaryEmail) {
        if (primaryEmail == null) {
            return false;
        }
        return customerJpaRepository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull(
                primaryEmail.toLowerCase(Locale.ROOT));
    }
}
