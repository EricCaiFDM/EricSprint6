# Phase 0 Research: Customer Management

## Decision 1: RBAC Enforcement Model
- Decision: Enforce hybrid RBAC with two roles: `Customer` (owned scope only) and `Admin` (cross-customer operational scope).
- Rationale: Matches feature specification while preserving least-privilege for customer self-service and required administrative access.
- Alternatives considered:
  - Customer-only scope: rejected because admin cross-scope operations are required.
  - Full enterprise RBAC matrix: rejected as out of scope for this release.

## Decision 2: Customer Uniqueness and Identity Keys
- Decision: Enforce uniqueness on business identifiers (for example normalized primary email plus business-specific unique key).
- Rationale: Prevents duplicate profile creation and supports deterministic retrieval/update semantics.
- Alternatives considered:
  - Soft deduplication only: rejected due to ambiguity and data-quality risk.

## Decision 3: Deletion Policy Strategy
- Decision: Apply pre-delete policy checks for dependency and retention/legal hold blockers; return policy-compliant blocked response when constraints fail.
- Rationale: Aligns with spec constraints and avoids destructive policy violations.
- Alternatives considered:
  - Always hard-delete: rejected due to compliance and dependency risk.
  - Immediate soft-delete without checks: rejected because blockers must be enforced before lifecycle change.

## Decision 4: Sensitive Field Exposure Control
- Decision: Apply field-level response masking based on requester role/scope policy.
- Rationale: Satisfies requirement for sensitive data protection while preserving operational utility.
- Alternatives considered:
  - Return full profile to all authorized callers: rejected as overexposure risk.

## Decision 5: Auditable Lifecycle Eventing
- Decision: Record immutable lifecycle events for create/update/get/delete actions with outcome and reason metadata.
- Rationale: Supports traceability, compliance evidence, and incident diagnostics.
- Alternatives considered:
  - Log-only without structured event records: rejected due to weaker audit reliability.

## Resolved Clarifications
- Authorization: Hybrid RBAC (Customer owned scope + Admin global scope).
- Deletion behavior: Must be blocked when dependency/retention policy constraints apply.
- Sensitive data: Must be masked according to access policy.
