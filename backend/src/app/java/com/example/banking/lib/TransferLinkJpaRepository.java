package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.TransferLinkEntity;

public interface TransferLinkJpaRepository extends JpaRepository<TransferLinkEntity, String> {
    Optional<TransferLinkEntity> findByTransferId(String transferId);
}
