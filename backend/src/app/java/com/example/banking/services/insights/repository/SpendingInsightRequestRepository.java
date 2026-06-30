package com.example.banking.services.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.insights.SpendingInsightRequest;

public interface SpendingInsightRequestRepository extends JpaRepository<SpendingInsightRequest, String> {
}
