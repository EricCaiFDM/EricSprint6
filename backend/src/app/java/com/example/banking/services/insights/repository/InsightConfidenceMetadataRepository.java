package com.example.banking.services.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.insights.InsightConfidenceMetadata;

public interface InsightConfidenceMetadataRepository extends JpaRepository<InsightConfidenceMetadata, String> {
}
