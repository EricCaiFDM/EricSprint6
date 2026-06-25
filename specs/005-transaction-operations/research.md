# Phase 0 Research: Transaction Operations

## Decision 1: Monetary Precision and Rounding
- Decision: Represent monetary values as fixed-precision decimal values and apply a centralized rounding policy at posting boundaries.
- Rationale: Satisfies BR-001 and avoids floating-point drift in balance and transaction computations.
- Alternatives considered:
  - Floating-point arithmetic: rejected due to non-deterministic precision behavior.
  - Per-endpoint rounding rules: rejected due to inconsistency risk.

## Decision 2: No-Overdraft Enforcement
- Decision: Enforce available-balance checks before withdrawal and transfer debit posting; reject when debit amount exceeds available funds.
- Rationale: Required by FR-002, FR-003, BR-002, and SC-004.
- Alternatives considered:
  - Allow configurable overdraft per account: rejected because overdraft is explicitly out of scope.
  - Post-facto reversal on insufficient funds: rejected due to consistency and user-experience risks.

## Decision 3: Atomic Transfer Processing
- Decision: Process transfer debit and credit legs in one database transaction with rollback on any failure.
- Rationale: Meets FR-004 and C-002 strong consistency requirements.
- Alternatives considered:
  - Saga/eventual consistency transfer: rejected due to temporary imbalance risk.
  - Two separate API calls for debit/credit: rejected due to non-atomic behavior.

## Decision 4: Idempotency for Monetary Requests
- Decision: Require and persist idempotency keys for deposit, withdrawal, and transfer operations with deterministic replay responses.
- Rationale: Satisfies FR-006 and mitigates retry-induced duplicate postings.
- Alternatives considered:
  - Best-effort dedupe by payload hash only: rejected due to collision/ambiguity risk.
  - No idempotency support: rejected due to duplicate posting risk on client/network retries.

## Decision 5: Immutable Transaction Ledger Model
- Decision: Persist append-only immutable transaction records with linked references for transfer legs.
- Rationale: Satisfies FR-005 and BR-003 while preserving auditable financial history.
- Alternatives considered:
  - Mutable transaction rows: rejected due to auditability and traceability gaps.

## Decision 6: Transaction History Query Strategy
- Decision: Provide history retrieval with explicit filters (date range, type), pagination, and deterministic ordering by posting timestamp and transaction identifier.
- Rationale: Satisfies FR-007 and ensures stable paging semantics.
- Alternatives considered:
  - Unbounded history fetch: rejected due to performance and payload size risks.
  - Non-deterministic ordering by insertion order only: rejected due to unstable pagination.

## Decision 7: Authorization Scope Enforcement
- Decision: Enforce hybrid RBAC on all transaction operations with customer-owned scope and admin global scope.
- Rationale: Required by FR-008 and aligns with cross-feature access policies.
- Alternatives considered:
  - Admin-only transactional operations: rejected due to customer self-service requirements.

## Resolved Clarifications
- Overdraft: Disallowed for this release.
- Consistency: Strong consistency required for balance updates and transfer posting.
- Idempotency: Mandatory for retried monetary operations.
- Time handling: UTC is canonical processing time.
