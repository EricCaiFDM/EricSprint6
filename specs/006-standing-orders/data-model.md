# Data Model: Standing Orders

## Entity: StandingOrder
- Description: Recurring transfer instruction between eligible accounts.
- Fields:
  - id (UUID, immutable)
  - sourceAccountId (UUID)
  - destinationAccountId (UUID)
  - amount (decimal(18,2), positive)
  - currencyCode (string, ISO-4217)
  - cadence (enum: DAILY, WEEKLY, MONTHLY)
  - scheduleConfig (JSON)
  - effectiveFromUtc (timestamp)
  - effectiveToUtc (timestamp, nullable)
  - nextExecutionAtUtc (timestamp)
  - lifecycleState (enum: ACTIVE, PAUSED, CANCELLED, COMPLETED)
  - retryPolicyCode (string)
  - createdByUserId (UUID)
  - updatedAtUtc (timestamp)
- Validation Rules:
  - sourceAccountId must differ from destinationAccountId.
  - amount must be greater than zero.
  - effectiveFromUtc <= effectiveToUtc when end date exists.
  - lifecycleState `ACTIVE` requires non-null nextExecutionAtUtc.

## Entity: StandingOrderLifecycleEvent
- Description: Immutable audit record for setup and lifecycle transitions.
- Fields:
  - id (UUID)
  - standingOrderId (UUID)
  - eventType (enum: CREATED, UPDATED, PAUSED, RESUMED, CANCELLED, COMPLETED)
  - actorUserId (UUID)
  - actorRole (enum: CUSTOMER, ADMIN)
  - occurredAtUtc (timestamp)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - eventType must align with valid lifecycle transitions.
  - metadata must exclude sensitive values.

## Entity: StandingOrderExecutionEvent
- Description: Immutable record of each scheduled execution attempt and outcome.
- Fields:
  - id (UUID)
  - standingOrderId (UUID)
  - dueAtUtc (timestamp)
  - startedAtUtc (timestamp)
  - completedAtUtc (timestamp, nullable)
  - status (enum: SUCCEEDED, FAILED_INSUFFICIENT_FUNDS, FAILED_INELIGIBLE_ACCOUNT, FAILED_DEPENDENCY_OUTAGE, RETRY_SCHEDULED)
  - transferReferenceId (UUID, nullable)
  - attemptNumber (integer)
  - nextRetryAtUtc (timestamp, nullable)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - transferReferenceId required when status is SUCCEEDED.
  - nextRetryAtUtc required when status is RETRY_SCHEDULED.

## Entity: StandingOrderScheduleCursor
- Description: Scheduler control cursor for due-window scanning and concurrency safety.
- Fields:
  - id (UUID)
  - workerId (string)
  - windowStartUtc (timestamp)
  - windowEndUtc (timestamp)
  - claimedAtUtc (timestamp)
  - completedAtUtc (timestamp, nullable)
  - status (enum: CLAIMED, COMPLETED, ABANDONED)
- Validation Rules:
  - claimed windows must not overlap for active workers.

## Entity: StandingOrderAccessPolicy
- Description: RBAC scope metadata for standing-order operations.
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
- StandingOrder 1..* StandingOrderLifecycleEvent
- StandingOrder 1..* StandingOrderExecutionEvent
- StandingOrderExecutionEvent optionally references transfer transaction artifacts
- StandingOrderScheduleCursor tracks scheduler worker claims over due windows
