# Data Model: Authentication

## Entity: UserAccount
- Description: Registered identity principal used for authentication.
- Fields:
  - id (UUID, immutable)
  - email (string, unique, normalized)
  - passwordHash (string, secret)
  - status (enum: ACTIVE, LOCKED, DISABLED, PENDING_VERIFICATION)
  - createdAtUtc (timestamp)
  - updatedAtUtc (timestamp)
  - lastLoginAtUtc (timestamp, nullable)
- Validation Rules:
  - Email must be syntactically valid and normalized.
  - Email must be unique.
  - passwordHash must be generated using approved hashing policy.
- State Transitions:
  - PENDING_VERIFICATION -> ACTIVE
  - ACTIVE -> LOCKED or DISABLED
  - LOCKED -> ACTIVE (administrative unlock)

## Entity: RefreshSession
- Description: Stateful refresh token session record for rotation/revocation.
- Fields:
  - id (UUID)
  - userId (UUID, FK -> UserAccount.id)
  - tokenId (string, unique)
  - tokenHash (string)
  - issuedAtUtc (timestamp)
  - expiresAtUtc (timestamp)
  - revokedAtUtc (timestamp, nullable)
  - replacedByTokenId (string, nullable)
  - ipAddress (string, nullable)
  - userAgent (string, nullable)
- Validation Rules:
  - expiresAtUtc must be later than issuedAtUtc.
  - tokenId must be unique.
- State Transitions:
  - ACTIVE -> ROTATED
  - ACTIVE -> REVOKED
  - ACTIVE -> EXPIRED

## Entity: PasswordResetRequest
- Description: Time-bound reset request artifact for account recovery initiation.
- Fields:
  - id (UUID)
  - userId (UUID, nullable for non-existing identity requests to support generic response pattern)
  - requestIdentity (string, normalized)
  - requestTokenHash (string, nullable)
  - requestedAtUtc (timestamp)
  - expiresAtUtc (timestamp, nullable)
  - status (enum: REQUESTED, EXPIRED, CONSUMED, CANCELED)
- Validation Rules:
  - requestIdentity must be normalized before persistence.
  - Expiry must follow configured TTL.

## Entity: AuthEvent
- Description: Auditable security-relevant event trail.
- Fields:
  - id (UUID)
  - eventType (enum: REGISTER_SUCCESS, REGISTER_FAIL, LOGIN_SUCCESS, LOGIN_FAIL, RESET_REQUEST, REFRESH_SUCCESS, REFRESH_FAIL)
  - userId (UUID, nullable)
  - occurredAtUtc (timestamp)
  - correlationId (string)
  - outcome (enum: SUCCESS, FAILURE)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - Sensitive payload data must be redacted.
  - eventType/outcome must be consistent.

## Relationships
- UserAccount 1..* RefreshSession
- UserAccount 1..* PasswordResetRequest
- UserAccount 1..* AuthEvent
