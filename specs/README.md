# Spec Organization

This workspace now contains a consolidated master spec plus feature-specific split specs.

Canonical tool stack reference:
- [Project Tool Stack](../TOOL-STACK.md)
- [Definition of Done](../DEFINITION-OF-DONE.md)

## Feature Creation Policy
- Every new feature MUST include test artifacts and test tasks from the start.
- At minimum, include negative-path tests plus integration/contract tests where endpoints or external contracts exist.
- A feature is not considered complete until tests are implemented and passing according to [Definition of Done](../DEFINITION-OF-DONE.md).

## Master Spec
- [001 User Auth Flows](001-user-auth-flows/spec.md)

## Split Feature Specs
- [002 Authentication](002-authentication/spec.md)
- [003 Customer Management](003-customer-management/spec.md)
- [004 Account Management](004-account-management/spec.md)
- [005 Transaction Operations](005-transaction-operations/spec.md)
- [006 Standing Orders](006-standing-orders/spec.md)
- [007 Notifications](007-notifications/spec.md)
- [008 Monthly Statements](008-monthly-statements/spec.md)
- [009 Spending Insights](009-spending-insights/spec.md)

Each split feature directory includes:
- `spec.md`
- `checklists/requirements.md`
