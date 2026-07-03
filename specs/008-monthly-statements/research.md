# Phase 0 Research: Monthly Statements

## Decision 1: Statement Period Boundary Strategy
- Decision: Use UTC discrete monthly intervals (`[periodStartUtc, periodEndUtc)`) for all generation boundary calculations.
- Rationale: Satisfies BR-001 and BR-002 and avoids timezone ambiguity at month transitions.
- Alternatives considered:
  - Local-time tenant boundaries: rejected due to inconsistent cross-region behavior.

## Decision 2: Opening/Closing Balance Computation
- Decision: Compute opening balance from the ledger state at `periodStartUtc` and closing balance at `periodEndUtc` using transaction event-time ordering.
- Rationale: Satisfies FR-001 and FR-002 with deterministic reproducibility.
- Alternatives considered:
  - Processing-time inclusion only: rejected because event-time inclusion is required.

## Decision 3: Late-Posted Event Correction Handling
- Decision: Preserve immutable original statement artifacts and issue corrected artifact versions for closed periods when eligible late-posted events are detected.
- Rationale: Satisfies FR-003 and BR-003 by balancing auditability and corrected output requirements.
- Alternatives considered:
  - Mutating original statement artifact: rejected due to immutability/audit constraints.
  - Ignoring late events for closed periods: rejected due to correction requirement.

## Decision 4: Generation Orchestration Model
- Decision: Run monthly generation as scheduled batch jobs with account-level partitioning and generation event recording.
- Rationale: Meets SC-001 performance objective and operational reliability for volume scenarios.
- Alternatives considered:
  - Synchronous per-request generation only: rejected due to high latency and throughput risk.

## Decision 5: Authorized Retrieval Enforcement
- Decision: Enforce hybrid RBAC (customer owned scope, admin global scope) on every retrieval request before artifact access.
- Rationale: Satisfies FR-004 and C-002 with strict access controls.
- Alternatives considered:
  - Artifact-level pre-shared access links only: rejected due to insufficient RBAC guarantees.

## Decision 6: Audit Trail Model
- Decision: Persist immutable generation and retrieval audit events including requester role, statement version, and outcome reason codes.
- Rationale: Satisfies FR-005 and supports compliance diagnostics.
- Alternatives considered:
  - Log-only audit trail: rejected due to weak queryability and retention guarantees.

## Resolved Clarifications
- Period boundaries: UTC monthly intervals.
- Corrected output: modeled as new immutable artifact versions.
- Retrieval authorization: enforced for every request under hybrid RBAC.
