# Data Model: Account Management

## Entity: Account
- Description: Customer-owned financial account record.
- Fields:
  - id (UUID, immutable)
  - accountNumber (string, unique, system-generated)
  - customerId (UUID)
  - accountType (enum: CHECKING, SAVINGS)
  - status (enum: ACTIVE, SUSPENDED, CLOSED)
  - currencyCode (string, ISO-4217)
  - openedAtUtc (timestamp)
  - closedAtUtc (timestamp, nullable)
  - nickname (string, nullable)
  - createdByUserId (UUID)
  - ownerUserId (UUID auth user id that owns account scope)
  - updatedAtUtc (timestamp)
- Validation Rules:
  - accountType must be CHECKING or SAVINGS.
  - customerId must reference an existing eligible customer.
  - ownerUserId must resolve from the linked customer's ownerUserId at create time.
  - accountNumber must be unique and immutable after creation.
- State Transitions:
  - ACTIVE -> SUSPENDED
  - SUSPENDED -> ACTIVE
  - ACTIVE|SUSPENDED -> CLOSED (subject to dependency/retention policy)

## Entity: AccountAccessPolicy
- Description: Role and scope metadata for authorization and field-level visibility.
- Fields:
  - id (UUID)
  - userId (UUID)
  - role (enum: CUSTOMER, ADMIN)
  - customerScopeId (UUID, nullable for ADMIN)
  - effectiveFromUtc (timestamp)
  - effectiveToUtc (timestamp, nullable)
- Validation Rules:
  - CUSTOMER role requires customerScopeId.
  - ADMIN role may omit customerScopeId.

## Entity: AccountEligibilityCheck
- Description: Eligibility evaluation artifact used during account creation.
- Fields:
  - id (UUID)
  - customerId (UUID)
  - accountType (enum: CHECKING, SAVINGS)
  - evaluatedAtUtc (timestamp)
  - isEligible (boolean)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - isEligible must be true before account creation can proceed.
  - reasonCode is required when isEligible is false.

## Entity: AccountDeletionPolicyCheck
- Description: Pre-delete policy evaluation for dependency and retention blockers.
- Fields:
  - id (UUID)
  - accountId (UUID)
  - evaluatedAtUtc (timestamp)
  - hasDependencyBlocker (boolean)
  - hasRetentionBlocker (boolean)
  - blockerReasons (JSON array)
  - decision (enum: ALLOW_DELETE, BLOCK_DELETE)
- Validation Rules:
  - decision must be BLOCK_DELETE when any blocker flag is true.

## Entity: AccountLifecycleEvent
- Description: Immutable audit event for lifecycle operations.
- Fields:
  - id (UUID)
  - accountId (UUID, nullable for failed create attempts)
  - eventType (enum: CREATE, GET, LIST, UPDATE, DELETE_ATTEMPT, DELETE_BLOCKED, DELETE_SUCCESS)
  - actorUserId (UUID)
  - actorRole (enum: CUSTOMER, ADMIN)
  - occurredAtUtc (timestamp)
  - outcome (enum: SUCCESS, FAILURE)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - metadata must not include restricted field values.
  - eventType/outcome combinations must be semantically valid.

## Relationships
- Customer 1..* Account
- Customer.ownerUserId 1..* Account.ownerUserId
- Account 1..* AccountLifecycleEvent
- Account 1..* AccountDeletionPolicyCheck
- AccountEligibilityCheck references customer and requested account type for create-time policy decisions
