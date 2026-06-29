package com.example.banking.lib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "statement")
public class StatementModuleConfig {
    private int maxPageSize = 100;
    private boolean schedulerEnabled = true;
    private String schedulerCron = "0 5 0 1 * *";
    private int generationBatchSize = 250;
    private String artifactBaseUri = "/statements";

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public String getSchedulerCron() {
        return schedulerCron;
    }

    public void setSchedulerCron(String schedulerCron) {
        if (schedulerCron == null || schedulerCron.isBlank()) {
            this.schedulerCron = "0 5 0 1 * *";
            return;
        }
        this.schedulerCron = schedulerCron.trim();
    }

    public int getGenerationBatchSize() {
        return generationBatchSize;
    }

    public void setGenerationBatchSize(int generationBatchSize) {
        this.generationBatchSize = generationBatchSize;
    }

    public String getArtifactBaseUri() {
        return artifactBaseUri;
    }

    public void setArtifactBaseUri(String artifactBaseUri) {
        if (artifactBaseUri == null || artifactBaseUri.isBlank()) {
            this.artifactBaseUri = "/statements";
            return;
        }
        this.artifactBaseUri = artifactBaseUri.trim();
    }
}
