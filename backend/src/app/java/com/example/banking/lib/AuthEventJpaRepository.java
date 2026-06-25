package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.banking.models.AuthEventEntity;

@Repository
public interface AuthEventJpaRepository extends JpaRepository<AuthEventEntity, String> {
}
