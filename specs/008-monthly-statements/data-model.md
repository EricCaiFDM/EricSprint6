# Data Model: Monthly Statements

## Entity: MonthlyStatement
- Description: Versioned monthly statement artifact metadata for an account and period.
- Fields:
  - id (UUID, immutable)
  - accountId (UUID)
  - periodYearMonth (string, format: YYYY-MM)
  - periodStartUtc (timestamp)
  - periodEndUtc (timestamp)
  - openingBalance (decimal(18,2))
  - closingBalance (decimal(18,2))
  - currencyCode (string, ISO-4217)
  - artifactVersion (integer, >= 1)
  - artifactUri (string)
  - generationMode (enum: STANDARD, CORRECTION)
  - generatedAtUtc (timestamp)
  - status (enum: GENERATED, CORRECTED, FAILED)
- Validation Rules:
  - periodStartUtc < periodEndUtc.
  - artifactVersion must be unique per accountId + periodYearMonth.
  - CORRECTION mode requires artifactVersion > 1.

## Entity: StatementActivitySummary
- Description: Transaction activity aggregation included in statement output.
- Fields:
  - id (UUID)
  - statementId (UUID)
  - debitTotal (decimal(18,2))
  - creditTotal (decimal(18,2))
  - transactionCount (integer)
  - includedEventStartUtc (timestamp)
  - includedEventEndUtc (timestamp)
- Validation Rules:
  - included event range must align with statement period boundaries.
  - totals must be non-negative.

## Entity: StatementGenerationEvent
- Description: Auditable generation attempt and completion record.
- Fields:
  - id (UUID)
  - statementId (UUID, nullable for pre-artifact failures)
  - accountId (UUID)
  - periodYearMonth (string)
  - eventType (enum: GENERATION_STARTED, GENERATION_SUCCEEDED, GENERATION_FAILED, CORRECTION_GENERATED)
  - occurredAtUtc (timestamp)
  - status (enum: SUCCESS, FAILURE)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - reasonCode required when status is FAILURE.

## Entity: StatementRetrievalEvent
- Description: Auditable retrieval request and authorization outcome.
- Fields:
  - id (UUID)
  - statementId (UUID)
  - requesterUserId (UUID)
  - requesterRole (enum: CUSTOMER, ADMIN)
  - occurredAtUtc (timestamp)
  - outcome (enum: ALLOWED, DENIED_NOT_FOUND, DENIED_PERMISSION)
  - reasonCode (string, nullable)
- Validation Rules:
  - requesterRole CUSTOMER requires ownership scope check.
  - reasonCode required for denied outcomes.

## Entity: StatementAccessPolicy
- Description: RBAC scope context used during statement retrieval.
- Fields:
  - id (UUID)
  - userId (UUID)
  - role (enum: CUSTOMER, ADMIN)
  - customerScopeId (UUID, nullable)
  - effectiveFromUtc (timestamp)
  - effectiveToUtc (timestamp, nullable)
- Validation Rules:
  - CUSTOMER role requires customerScopeId.

## Relationships
- MonthlyStatement 1..1 StatementActivitySummary
- MonthlyStatement 1..* StatementGenerationEvent
- MonthlyStatement 1..* StatementRetrievalEvent
- StatementAccessPolicy governs StatementRetrievalEvent authorization outcomes
