package com.example.banking.models.insights;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "insight_confidence_metadata")
public class InsightConfidenceMetadata {
    @Id
    @Column(name = "confidence_id", nullable = false, length = 36)
    private String confidenceId;

    @Column(name = "insight_id", nullable = false, length = 36)
    private String insightId;

    @Column(name = "coverage_ratio", nullable = false)
    private BigDecimal coverageRatio;

    @Column(name = "confidence_level", nullable = false, length = 16)
    private String confidenceLevel;

    @Column(name = "missing_category_count", nullable = false)
    private int missingCategoryCount;

    @Column(name = "minimum_threshold_satisfied", nullable = false)
    private boolean minimumThresholdSatisfied;

    @Column(name = "notes", length = 512)
    private String notes;

    @PrePersist
    void onCreate() {
        if (confidenceId == null) {
            confidenceId = UUID.randomUUID().toString();
        }
    }

    public String getConfidenceId() {
        return confidenceId;
    }

    public void setConfidenceId(String confidenceId) {
        this.confidenceId = confidenceId;
    }

    public String getInsightId() {
        return insightId;
    }

    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }

    public BigDecimal getCoverageRatio() {
        return coverageRatio;
    }

    public void setCoverageRatio(BigDecimal coverageRatio) {
        this.coverageRatio = coverageRatio;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public int getMissingCategoryCount() {
        return missingCategoryCount;
    }

    public void setMissingCategoryCount(int missingCategoryCount) {
        this.missingCategoryCount = missingCategoryCount;
    }

    public boolean isMinimumThresholdSatisfied() {
        return minimumThresholdSatisfied;
    }

    public void setMinimumThresholdSatisfied(boolean minimumThresholdSatisfied) {
        this.minimumThresholdSatisfied = minimumThresholdSatisfied;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
