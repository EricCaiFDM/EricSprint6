package com.example.banking.services;

import java.util.Optional;

import com.example.banking.models.CustomerEntity;

public interface CustomerRepository {
    CustomerEntity save(CustomerEntity customer);

    Optional<CustomerEntity> findActiveById(String customerId);

    boolean existsByExternalCustomerKey(String externalCustomerKey);

    boolean existsByPrimaryEmail(String primaryEmail);
}
