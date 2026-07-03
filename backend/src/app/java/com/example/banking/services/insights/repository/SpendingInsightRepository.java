package com.example.banking.services.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.insights.SpendingInsight;

public interface SpendingInsightRepository extends JpaRepository<SpendingInsight, String> {
}
