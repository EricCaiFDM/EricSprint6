# Guardrails Reference

This document consolidates project guardrail rules and links each rule to where it is enforced and where evidence is tracked.

## Canonical Sources

- Definition of Done: [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md)
- Feature planning checklist template: [.specify/templates/plan-template.md](.specify/templates/plan-template.md)
- Feature task completion checklists: [specs](specs)

## Guardrail Rules

### 1) Prompt/workflow references the feature spec

- Enforced by:
  - [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md) (Section 4: Guardrails Enforced)
  - [.specify/templates/plan-template.md](.specify/templates/plan-template.md) (Guardrails Enforced checklist)
- Evidence tracked in:
  - Feature `tasks.md` files under each feature in [specs](specs), item: "Verify guardrails (spec reference, secure dependencies, static analysis, secret scan, dependency audit)"

### 2) No insecure AI-suggested dependencies

- Enforced by:
  - [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md) (Section 4)
  - [.specify/templates/plan-template.md](.specify/templates/plan-template.md) (Guardrails Enforced checklist)
- Evidence tracked in:
  - Feature `tasks.md` DoD checklist item "Verify guardrails"
  - Feature quality checklists where present (example: [specs/008-monthly-statements/checklists/quality.md](specs/008-monthly-statements/checklists/quality.md))

### 3) Static analysis passes

- Enforced by:
  - [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md) (Section 4)
  - [.specify/templates/plan-template.md](.specify/templates/plan-template.md) (Guardrails Enforced checklist)
- Evidence tracked in:
  - Feature `tasks.md` DoD checklist item "Verify guardrails"
  - Example explicit evidence: [specs/008-monthly-statements/checklists/quality.md](specs/008-monthly-statements/checklists/quality.md)

### 4) Secret scanning passes

- Enforced by:
  - [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md) (Section 4)
  - [.specify/templates/plan-template.md](.specify/templates/plan-template.md) (Guardrails Enforced checklist)
- Evidence tracked in:
  - Feature `tasks.md` DoD checklist item "Verify guardrails"
  - Example explicit evidence: [specs/008-monthly-statements/checklists/quality.md](specs/008-monthly-statements/checklists/quality.md)

### 5) Dependency audit passes

- Enforced by:
  - [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md) (Section 4)
  - [.specify/templates/plan-template.md](.specify/templates/plan-template.md) (Guardrails Enforced checklist)
- Evidence tracked in:
  - Feature `tasks.md` DoD checklist item "Verify guardrails"
  - Example explicit evidence (`npm audit --omit=dev --json`): [specs/008-monthly-statements/checklists/quality.md](specs/008-monthly-statements/checklists/quality.md)

## Feature Guardrail Status (from tasks checklists)

Status here reflects only whether each feature task file marks the "Verify guardrails" checklist item as done.

| Feature | Guardrail Verification Item |
|---|---|
| [002-authentication](specs/002-authentication/tasks.md) | Done |
| [003-customer-management](specs/003-customer-management/tasks.md) | Pending |
| [004-account-management](specs/004-account-management/tasks.md) | Pending |
| [005-transaction-operations](specs/005-transaction-operations/tasks.md) | Pending |
| [006-standing-orders](specs/006-standing-orders/tasks.md) | Pending |
| [007-notifications](specs/007-notifications/tasks.md) | Pending |
| [008-monthly-statements](specs/008-monthly-statements/tasks.md) | Done |
| [009-spending-insights](specs/009-spending-insights/tasks.md) | Done |

## Where Guardrails Are Explicitly Explained

- Global expectations are defined in [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md).
- Planning-time guardrail checklist is defined in [.specify/templates/plan-template.md](.specify/templates/plan-template.md).
- Concrete local guardrail evidence is explicitly described in [specs/008-monthly-statements/checklists/quality.md](specs/008-monthly-statements/checklists/quality.md).

## Suggested Ongoing Practice

For each feature, keep both artifacts updated:

- Mark the "Verify guardrails" item in the feature `tasks.md` once complete.
- Add explicit command/output evidence to a quality checklist (for example static analysis, dependency audit, and secret scan outcomes).
