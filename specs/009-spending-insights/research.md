# Phase 0 Research: Spending Insights

## Decision 1: Source Data Eligibility
- Decision: Build insights exclusively from posted transaction history within the requested policy-defined period.
- Rationale: Satisfies FR-001 and prevents unstable analytics based on pending events.
- Alternatives considered:
  - Including pending transactions: rejected due to volatility and reduced trust.

## Decision 2: Categorization Strategy
- Decision: Apply business-approved taxonomy mapping rules before aggregation and enforce taxonomy version tagging in responses.
- Rationale: Satisfies FR-002 and BR-002 with reproducible category outputs.
- Alternatives considered:
  - Ad-hoc model-generated categories: rejected due to governance and explainability risk.

## Decision 3: Trend Indicator Computation
- Decision: Compute trend indicators by comparing current period category totals against prior equivalent periods with normalized baselines.
- Rationale: Provides meaningful directional insight while maintaining deterministic calculations.
- Alternatives considered:
  - Single-period absolute totals only: rejected because trend indicators are required.

## Decision 4: Sparse Data Confidence Model
- Decision: Return coverage ratio and confidence level (HIGH, MEDIUM, LOW) based on minimum transaction count and category distribution thresholds.
- Rationale: Satisfies FR-003 and User Story 2 by exposing reliability for limited datasets.
- Alternatives considered:
  - Binary sufficient/insufficient flag only: rejected due to weak explanatory power.

## Decision 5: RBAC and Data Leakage Controls
- Decision: Enforce hybrid RBAC scope checks per request and redact/aggregate results to avoid exposing hidden underlying records.
- Rationale: Satisfies FR-004 and C-002 with strict access and privacy boundaries.
- Alternatives considered:
  - Returning raw transaction excerpts for transparency: rejected due to hidden-record exposure risk.

## Decision 6: Audit Retrieval Events
- Decision: Persist immutable insight retrieval audit events with requester context, scope, period, and outcome.
- Rationale: Satisfies FR-005 and supports operational compliance diagnostics.
- Alternatives considered:
  - Debug logs only: rejected due to weak traceability and retention guarantees.

## Resolved Clarifications
- Confidence metadata: includes coverage ratio and confidence level.
- Trend policy: compares against prior equivalent period windows.
- Hidden records: never directly exposed in insight output payloads.
