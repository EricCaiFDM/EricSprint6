package com.example.banking.models;

public enum StandingOrderExecutionStatus {
    SUCCEEDED,
    FAILED_INSUFFICIENT_FUNDS,
    FAILED_INELIGIBLE_ACCOUNT,
    FAILED_DEPENDENCY_OUTAGE,
    RETRY_SCHEDULED
}
