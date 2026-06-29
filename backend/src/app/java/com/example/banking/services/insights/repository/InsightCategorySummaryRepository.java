package com.example.banking.services.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.insights.InsightCategorySummary;

public interface InsightCategorySummaryRepository extends JpaRepository<InsightCategorySummary, String> {
}
