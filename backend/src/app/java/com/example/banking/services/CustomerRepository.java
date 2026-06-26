package com.example.banking.services;

import java.util.List;
import java.util.Optional;

import com.example.banking.models.CustomerEntity;

public interface CustomerRepository {
    CustomerEntity save(CustomerEntity customer);

    Optional<CustomerEntity> findActiveById(String customerId);

    Optional<CustomerEntity> findLatestActiveByOwnerUserId(String ownerUserId);

    Optional<CustomerEntity> findLatestActiveByCreatorUserId(String creatorUserId);

    List<CustomerEntity> findActiveCustomers();

    boolean existsByExternalCustomerKey(String externalCustomerKey);

    boolean existsByPrimaryEmail(String primaryEmail);
}
