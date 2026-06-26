package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.DeletionPolicyCheckEntity;

public interface DeletionPolicyCheckJpaRepository extends JpaRepository<DeletionPolicyCheckEntity, String> {
}
