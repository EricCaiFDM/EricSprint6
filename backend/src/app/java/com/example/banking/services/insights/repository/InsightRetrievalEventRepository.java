package com.example.banking.services.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.insights.InsightRetrievalEvent;

public interface InsightRetrievalEventRepository extends JpaRepository<InsightRetrievalEvent, String> {
}
