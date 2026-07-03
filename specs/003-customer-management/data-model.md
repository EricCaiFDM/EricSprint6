# Data Model: Customer Management

## Entity: Customer
- Description: Canonical customer profile record.
- Fields:
  - id (UUID, immutable)
  - externalCustomerKey (string, unique)
  - legalName (string)
  - primaryEmail (string, normalized)
  - phoneNumber (string, nullable)
  - status (enum: ACTIVE, SUSPENDED, CLOSED)
  - createdAtUtc (timestamp)
  - updatedAtUtc (timestamp)
  - createdByUserId (UUID)
  - ownerUserId (UUID)
- Validation Rules:
  - externalCustomerKey must be unique.
  - primaryEmail must be valid and normalized.
  - Required profile fields must be present at create time.
- State Transitions:
  - ACTIVE -> SUSPENDED
  - SUSPENDED -> ACTIVE
  - ACTIVE|SUSPENDED -> CLOSED (subject to deletion policy)

## Entity: CustomerAccessPolicy
- Description: Role and scope metadata used for authorization and field visibility.
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

## Entity: CustomerLifecycleEvent
- Description: Immutable audit event for customer lifecycle actions.
- Fields:
  - id (UUID)
  - customerId (UUID, nullable for failed create attempts)
  - eventType (enum: CREATE, UPDATE, GET, DELETE_ATTEMPT, DELETE_BLOCKED, DELETE_SUCCESS)
  - actorUserId (UUID)
  - actorRole (enum: CUSTOMER, ADMIN)
  - occurredAtUtc (timestamp)
  - outcome (enum: SUCCESS, FAILURE)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - Sensitive values must be redacted from metadata.
  - eventType and outcome must be consistent.

## Entity: DeletionPolicyCheck
- Description: Evaluation artifact for retention/dependency blockers before deletion.
- Fields:
  - id (UUID)
  - customerId (UUID)
  - evaluatedAtUtc (timestamp)
  - hasDependencyBlocker (boolean)
  - hasRetentionBlocker (boolean)
  - blockerReasons (JSON array)
  - decision (enum: ALLOW_DELETE, BLOCK_DELETE)

## Relationships
- Customer 1..* CustomerLifecycleEvent
- Customer 1..* DeletionPolicyCheck
- CustomerAccessPolicy references requester scope and role used in customer operations
