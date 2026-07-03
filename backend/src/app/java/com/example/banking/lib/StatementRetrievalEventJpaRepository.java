package com.example.banking.lib;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.statement.StatementRetrievalEvent;

public interface StatementRetrievalEventJpaRepository extends JpaRepository<StatementRetrievalEvent, String> {
}
