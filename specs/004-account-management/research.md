# Phase 0 Research: Account Management

## Decision 1: Authorization Enforcement Model
- Decision: Enforce hybrid RBAC with `Customer` limited to owned-account scope and `Admin` with cross-customer operational scope.
- Rationale: Directly satisfies FR-007 and BR-003 while maintaining least privilege.
- Alternatives considered:
  - Customer-only model: rejected because admin global operations are required.
  - Expanded role matrix: rejected as unnecessary complexity for this feature scope.

## Decision 2: Account Type and Eligibility Validation
- Decision: Restrict account creation to `CHECKING` and `SAVINGS` with pre-create eligibility evaluation per customer policy.
- Rationale: Aligns with FR-001, FR-002, and BR-002, and prevents unsupported product provisioning.
- Alternatives considered:
  - Open-ended account type enum: rejected due to unsupported types and policy ambiguity.
  - Post-create eligibility reconciliation: rejected because invalid accounts should be prevented, not corrected later.

## Decision 3: Retrieval and Field-Level Visibility
- Decision: Apply field-level response masking/filtering based on requester role and scope policy.
- Rationale: Satisfies FR-003 and C-003 while allowing authorized operational access.
- Alternatives considered:
  - Return full account profile to all authorized callers: rejected as data overexposure risk.

## Decision 4: Listing Pattern
- Decision: Implement customer-scoped account listing with pagination (`page`, `pageSize`) and account-type/status filters.
- Rationale: Satisfies FR-004 and supports stable client navigation with predictable API performance.
- Alternatives considered:
  - Unpaginated list responses: rejected due to latency and payload growth risk.

## Decision 5: Delete Policy Strategy
- Decision: Execute dependency and retention policy checks before delete/closure and return blocked reason when constraints fail.
- Rationale: Meets FR-006 and E-005; protects compliance and referential integrity.
- Alternatives considered:
  - Always hard-delete: rejected due to blocker policy violations.
  - Deferred delete queue without pre-checks: rejected because immediate policy enforcement is required.

## Decision 6: Auditable Lifecycle Events
- Decision: Persist immutable account lifecycle events for create/retrieve/list/update/delete attempts and outcomes.
- Rationale: Required by FR-008 and supports traceability and operational diagnostics.
- Alternatives considered:
  - Application logs only: rejected because logs alone are weaker for queryable compliance evidence.

## Resolved Clarifications
- RBAC: Hybrid RBAC (Customer owned scope + Admin global scope).
- Account type scope: Checking and savings only.
- Deletion behavior: Block when dependency/retention policy constraints exist.
- Listing behavior: Paginated and filterable list responses.
