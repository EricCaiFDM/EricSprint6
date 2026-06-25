# Phase 0 Research: Notifications

## Decision 1: Event-to-Dispatch Processing Model
- Decision: Use asynchronous event-driven processing where triggers enqueue dispatch work and worker processes resolve channel delivery attempts.
- Rationale: Supports SC-001 latency target while isolating dispatch reliability from synchronous API paths.
- Alternatives considered:
  - Fully synchronous dispatch in request flow: rejected due to latency and dependency-coupling risk.

## Decision 2: Consent and Preference Evaluation
- Decision: Evaluate consent and channel preferences before each dispatch attempt and block restricted deliveries with reason-coded outcomes.
- Rationale: Satisfies FR-002, BR-001, BR-002, and SC-002 compliance requirements.
- Alternatives considered:
  - Preference check only at trigger time: rejected because preferences can change before dispatch.

## Decision 3: Retry and Fallback Policy
- Decision: Apply bounded retry behavior with configured fallback channel ordering where policy allows.
- Rationale: Satisfies FR-003 while preventing indefinite retry loops.
- Alternatives considered:
  - Infinite retries: rejected due to operational risk.
  - No fallback support: rejected where policy requires alternate channel behavior.

## Decision 4: Delivery Outcome Ledger
- Decision: Persist immutable notification dispatch attempts and final outcomes including failure reason codes.
- Rationale: Satisfies FR-004 and provides auditable operations for compliance and troubleshooting.
- Alternatives considered:
  - Log-only outcome tracking: rejected due to weak queryability and audit guarantees.

## Decision 5: Template Rendering and Sensitive Data Controls
- Decision: Render templates with scoped context and enforce redaction/allowlist rules before dispatch payload creation.
- Rationale: Addresses C-002 and prevents sensitive data leakage through notifications.
- Alternatives considered:
  - Free-form template context passthrough: rejected due to data exposure risk.

## Decision 6: Channel Availability Handling
- Decision: Classify channel outages as retryable failures with policy-bound retries and final failure reason when exhausted.
- Rationale: Aligns with E-001 and E-004 style resilience requirements.
- Alternatives considered:
  - Immediate terminal failure on first outage: rejected where retry policy exists.

## Resolved Clarifications
- Restricted deliveries: must be blocked and logged, never sent.
- Preferences: applied for every dispatch attempt.
- Outcome recording: mandatory for both success and failure attempts.
