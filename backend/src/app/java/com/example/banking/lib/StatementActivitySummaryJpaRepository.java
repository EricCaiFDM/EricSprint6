package com.example.banking.lib;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.statement.StatementActivitySummary;

public interface StatementActivitySummaryJpaRepository extends JpaRepository<StatementActivitySummary, String> {
    Optional<StatementActivitySummary> findByStatementId(String statementId);
}
