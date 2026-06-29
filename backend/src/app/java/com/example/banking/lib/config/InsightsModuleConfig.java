package com.example.banking.lib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "insights")
public class InsightsModuleConfig {
    private int minimumTransactionCount = 3;
    private int mediumConfidenceTransactionCount = 6;
    private int maxCategories = 8;
    private String taxonomyVersion = "v1";

    public int getMinimumTransactionCount() {
        return minimumTransactionCount;
    }

    public void setMinimumTransactionCount(int minimumTransactionCount) {
        this.minimumTransactionCount = Math.max(1, minimumTransactionCount);
    }

    public int getMediumConfidenceTransactionCount() {
        return mediumConfidenceTransactionCount;
    }

    public void setMediumConfidenceTransactionCount(int mediumConfidenceTransactionCount) {
        this.mediumConfidenceTransactionCount = Math.max(1, mediumConfidenceTransactionCount);
    }

    public int getMaxCategories() {
        return maxCategories;
    }

    public void setMaxCategories(int maxCategories) {
        this.maxCategories = Math.max(1, maxCategories);
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public void setTaxonomyVersion(String taxonomyVersion) {
        if (taxonomyVersion == null || taxonomyVersion.isBlank()) {
            this.taxonomyVersion = "v1";
            return;
        }
        this.taxonomyVersion = taxonomyVersion.trim();
    }
}
