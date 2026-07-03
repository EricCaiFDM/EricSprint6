# Data Model: Notifications

## Entity: NotificationEvent
- Description: Trigger event representing a candidate notification dispatch.
- Fields:
  - id (UUID, immutable)
  - eventType (string)
  - recipientScopeType (enum: CUSTOMER, ACCOUNT, ADMIN)
  - recipientScopeId (UUID)
  - templateCode (string)
  - templateContext (JSON, sanitized)
  - triggeredAtUtc (timestamp)
  - status (enum: PENDING, PROCESSING, COMPLETED, BLOCKED, FAILED)
  - correlationId (UUID, nullable)
- Validation Rules:
  - templateContext must pass sensitive-field allowlist checks.
  - eventType must map to configured notification policy.

## Entity: NotificationPreferenceSnapshot
- Description: Consent and channel preference state used at dispatch time.
- Fields:
  - id (UUID)
  - notificationEventId (UUID)
  - recipientId (UUID)
  - consentStatus (enum: CONSENTED, RESTRICTED)
  - allowedChannels (array<enum: EMAIL, SMS, PUSH, IN_APP>)
  - restrictedChannels (array<enum>)
  - capturedAtUtc (timestamp)
- Validation Rules:
  - RESTRICTED consent blocks all channels for restricted categories.
  - allowedChannels and restrictedChannels must not conflict.

## Entity: NotificationDispatchAttempt
- Description: Attempt record for a single channel dispatch operation.
- Fields:
  - id (UUID)
  - notificationEventId (UUID)
  - channel (enum: EMAIL, SMS, PUSH, IN_APP)
  - attemptNumber (integer)
  - queuedAtUtc (timestamp)
  - startedAtUtc (timestamp, nullable)
  - completedAtUtc (timestamp, nullable)
  - status (enum: SUCCEEDED, FAILED_CHANNEL_UNAVAILABLE, FAILED_TEMPLATE_RESOLUTION, FAILED_RESTRICTED, RETRY_SCHEDULED)
  - providerReferenceId (string, nullable)
  - reasonCode (string, nullable)
- Validation Rules:
  - providerReferenceId required when status is SUCCEEDED.
  - reasonCode required for all non-success statuses.

## Entity: NotificationDeliveryOutcome
- Description: Final delivery result for a notification event.
- Fields:
  - id (UUID)
  - notificationEventId (UUID)
  - finalStatus (enum: DELIVERED, BLOCKED_RESTRICTED, FAILED)
  - deliveredChannel (enum: EMAIL, SMS, PUSH, IN_APP, nullable)
  - completedAtUtc (timestamp)
  - reasonCode (string, nullable)
  - metadata (JSON, redacted)
- Validation Rules:
  - deliveredChannel required when finalStatus is DELIVERED.
  - finalStatus BLOCKED_RESTRICTED must not include provider references.

## Entity: NotificationChannelPolicy
- Description: Effective policy definition for supported channels and fallback order.
- Fields:
  - id (UUID)
  - policyCode (string)
  - supportedChannels (array<enum>)
  - fallbackOrder (array<enum>)
  - retryMaxAttempts (integer)
  - retryBackoffPolicy (string)
  - effectiveFromUtc (timestamp)
  - effectiveToUtc (timestamp, nullable)
- Validation Rules:
  - fallbackOrder must be subset of supportedChannels.
  - retryMaxAttempts must be >= 0.

## Relationships
- NotificationEvent 1..* NotificationDispatchAttempt
- NotificationEvent 1..1 NotificationDeliveryOutcome
- NotificationEvent 1..1 NotificationPreferenceSnapshot
- NotificationChannelPolicy governs channel selection and retry/fallback behavior during NotificationDispatchAttempt processing
