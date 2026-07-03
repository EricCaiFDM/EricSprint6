package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.statement.StatementGenerationEvent;

public interface StatementGenerationEventJpaRepository extends JpaRepository<StatementGenerationEvent, String> {
}
