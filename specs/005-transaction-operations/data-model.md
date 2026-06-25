# Data Model: Transaction Operations

## Entity: AccountBalance
- Description: Current available and ledger balance state for an account.
- Fields:
  - accountId (UUID, primary key)
  - currencyCode (string, ISO-4217)
  - availableAmount (decimal(18,2))
  - ledgerAmount (decimal(18,2))
  - version (integer, optimistic concurrency)
  - updatedAtUtc (timestamp)
- Validation Rules:
  - availableAmount and ledgerAmount must be non-negative.
  - account currency must match transaction currency.

## Entity: Transaction
- Description: Immutable monetary operation record.
- Fields:
  - id (UUID, immutable)
  - accountId (UUID)
  - transactionType (enum: DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT)
  - amount (decimal(18,2), positive)
  - currencyCode (string, ISO-4217)
  - postedAtUtc (timestamp)
  - idempotencyKey (string)
  - correlationId (UUID, nullable, used for transfer pairing)
  - actorUserId (UUID)
  - actorRole (enum: CUSTOMER, ADMIN)
  - balanceBefore (decimal(18,2))
  - balanceAfter (decimal(18,2))
  - metadata (JSON, redacted)
- Validation Rules:
  - amount must be greater than zero.
  - transactionType determines required account role in correlation.
  - idempotencyKey must be unique within operation scope.

## Entity: TransferLink
- Description: Correlation between transfer debit and credit legs.
- Fields:
  - id (UUID)
  - debitTransactionId (UUID)
  - creditTransactionId (UUID)
  - sourceAccountId (UUID)
  - destinationAccountId (UUID)
  - amount (decimal(18,2))
  - currencyCode (string, ISO-4217)
  - createdAtUtc (timestamp)
- Validation Rules:
  - sourceAccountId must differ from destinationAccountId.
  - debit and credit leg amounts must match.

## Entity: IdempotencyRecord
- Description: Request replay protection and deterministic response tracking.
- Fields:
  - id (UUID)
  - idempotencyKey (string)
  - operationType (enum: DEPOSIT, WITHDRAWAL, TRANSFER)
  - requestHash (string)
  - responseTransactionId (UUID, nullable)
  - responsePayload (JSON)
  - status (enum: IN_PROGRESS, SUCCEEDED, FAILED)
  - createdAtUtc (timestamp)
  - expiresAtUtc (timestamp)
- Validation Rules:
  - idempotencyKey + operationType uniqueness must be enforced.
  - requestHash must match on retries for a key.

## Entity: TransactionHistoryQuery
- Description: Query envelope for deterministic history retrieval.
- Fields:
  - scopeType (enum: ACCOUNT, CUSTOMER)
  - scopeId (UUID)
  - startDateUtc (timestamp, nullable)
  - endDateUtc (timestamp, nullable)
  - transactionTypes (array<enum>, nullable)
  - page (integer)
  - pageSize (integer)
  - sortOrder (enum: POSTED_AT_DESC_ID_DESC, POSTED_AT_ASC_ID_ASC)
- Validation Rules:
  - page >= 1 and 1 <= pageSize <= 100.
  - startDateUtc <= endDateUtc when both provided.

## Relationships
- AccountBalance 1..* Transaction
- TransferLink 1..1 Transaction (debit leg) and 1..1 Transaction (credit leg)
- IdempotencyRecord 1..0..1 Transaction (operation outcome)
- TransactionHistoryQuery addresses Transaction records through account or customer scope filters
