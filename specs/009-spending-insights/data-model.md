# Data Model: Spending Insights

## Entity: SpendingInsightRequest
- Description: Authorized request context for generating spending insights.
- Fields:
  - id (UUID)
  - scopeType (enum: CUSTOMER, ACCOUNT, ADMIN)
  - scopeId (UUID)
  - periodStartUtc (timestamp)
  - periodEndUtc (timestamp)
  - categoryFilters (array<string>, nullable)
  - requestedByUserId (UUID)
  - requestedAtUtc (timestamp)
- Validation Rules:
  - periodStartUtc < periodEndUtc.
  - scope and requester RBAC relationship must be valid.

## Entity: SpendingInsight
- Description: Derived analytics artifact for a request scope and period.
- Fields:
  - id (UUID, immutable)
  - requestId (UUID)
  - taxonomyVersion (string)
  - generatedAtUtc (timestamp)
  - status (enum: GENERATED, PARTIAL, FAILED)
  - totalSpendAmount (decimal(18,2))
  - currencyCode (string, ISO-4217)
  - trendDirection (enum: UP, DOWN, FLAT, INSUFFICIENT_DATA)
  - trendDeltaPercent (decimal(8,3), nullable)
- Validation Rules:
  - FAILED status must not include category aggregates.
  - INSUFFICIENT_DATA trendDirection requires sparse confidence metadata.

## Entity: InsightCategorySummary
- Description: Category-level aggregated spending output.
- Fields:
  - id (UUID)
  - insightId (UUID)
  - categoryCode (string)
  - categoryLabel (string)
  - amount (decimal(18,2))
  - transactionCount (integer)
  - periodSharePercent (decimal(5,2))
- Validation Rules:
  - categoryCode must exist in approved taxonomy.
  - amount and transactionCount must be non-negative.

## Entity: InsightConfidenceMetadata
- Description: Reliability indicators for sparse or partial datasets.
- Fields:
  - id (UUID)
  - insightId (UUID)
  - coverageRatio (decimal(5,2))
  - confidenceLevel (enum: HIGH, MEDIUM, LOW)
  - missingCategoryCount (integer)
  - minimumThresholdSatisfied (boolean)
  - notes (string, nullable)
- Validation Rules:
  - coverageRatio must be between 0 and 100.
  - LOW confidence required when minimumThresholdSatisfied is false.

## Entity: InsightRetrievalEvent
- Description: Immutable audit event for insight retrieval attempts.
- Fields:
  - id (UUID)
  - requestId (UUID)
  - insightId (UUID, nullable)
  - requesterUserId (UUID)
  - requesterRole (enum: CUSTOMER, ADMIN)
  - scopeType (enum: CUSTOMER, ACCOUNT, ADMIN)
  - scopeId (UUID)
  - occurredAtUtc (timestamp)
  - outcome (enum: ALLOWED, DENIED_PERMISSION, FAILED_DEPENDENCY, INVALID_FILTER)
  - reasonCode (string, nullable)
- Validation Rules:
  - reasonCode required for non-ALLOWED outcomes.

## Relationships
- SpendingInsightRequest 1..1 SpendingInsight
- SpendingInsight 1..* InsightCategorySummary
- SpendingInsight 1..1 InsightConfidenceMetadata
- SpendingInsightRequest 1..* InsightRetrievalEvent
