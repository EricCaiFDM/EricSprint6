# Phase 0 Research: Standing Orders

## Decision 1: Lifecycle State Model
- Decision: Model standing order lifecycle states as `ACTIVE`, `PAUSED`, `CANCELLED`, and `COMPLETED` with explicit transition rules.
- Rationale: Satisfies FR-001 and BR-001 by ensuring only active orders are executable.
- Alternatives considered:
  - Boolean active flag only: rejected due to insufficient lifecycle traceability.
  - Hard-delete on cancel: rejected because auditability requires preserved records.

## Decision 2: Schedule and Cadence Validation
- Decision: Validate cadence, effective dates, and next-run calculations at create/update time using UTC canonical schedule rules.
- Rationale: Satisfies FR-002 and BR-003, preventing invalid recurring configuration from entering the system.
- Alternatives considered:
  - Validate only at execution time: rejected due to repeated runtime failures.

## Decision 3: Execution Window Processing
- Decision: Use a scheduler worker that scans due active standing orders and dispatches execution attempts within configured windows.
- Rationale: Meets FR-003 and SC-002 by decoupling scheduled execution from request/response APIs.
- Alternatives considered:
  - Execute from API process loop only: rejected due to poor operational isolation.

## Decision 4: Execution-Time Funds and Eligibility Checks
- Decision: Re-evaluate source/destination eligibility and funds availability for every due execution attempt.
- Rationale: Required by FR-004 and BR-002 because account conditions can change after setup.
- Alternatives considered:
  - Assume setup-time validation remains valid indefinitely: rejected as unsafe.

## Decision 5: Retry and Outage Handling
- Decision: Apply predefined retry policy with bounded attempts and terminal failure reason codes when dependency outages persist.
- Rationale: Aligns with C-002 and E-004 while maintaining deterministic outcome tracking.
- Alternatives considered:
  - Infinite retries: rejected due to runaway processing risk.

## Decision 6: Audit and Outcome Recording
- Decision: Persist immutable lifecycle events and execution outcome events with references to triggered transfer attempts.
- Rationale: Satisfies FR-005 and SC-003 auditability requirements.
- Alternatives considered:
  - Log-only execution history: rejected because durable queryable audit records are required.

## Decision 7: Authorization Scope Rules
- Decision: Enforce hybrid RBAC where customers manage only owned-account standing orders and admins can manage cross-scope.
- Rationale: Consistent with assumptions and C-001 delegated cross-scope exclusion.
- Alternatives considered:
  - Customer delegated setup for third-party scopes: rejected as out of scope.

## Resolved Clarifications
- Time handling: UTC is canonical for schedule evaluation and processing.
- Execution policy: only active orders execute; paused/cancelled orders do not execute.
- Retry policy: bounded retries and reason-coded terminal outcomes.
