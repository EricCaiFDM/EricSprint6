# Quickstart: Account Management Feature Validation

## Prerequisites
- Node.js 22+
- PostgreSQL 15+
- Environment configured for API, DB connection, and RBAC seed data

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed role/scope test fixtures (Customer and Admin).
4. Start the backend API service.

## Validation Scenarios

### Scenario 1: Create and Retrieve Account
1. Submit valid create request for checking account.
2. Submit valid create request for savings account.
3. Retrieve both accounts as authorized requester.

Expected outcome:
- Both create requests succeed and return account identifiers.
- Retrieve returns authorized account profile with masked fields per policy.

### Scenario 2: List and Update Accounts
1. List accounts for customer with pagination (`page=1&pageSize=20`).
2. Apply account-type/status filter.
3. Update editable attributes (for example nickname and status transition where allowed).

Expected outcome:
- List returns paginated collection and metadata.
- Filtered results are scoped and accurate.
- Update succeeds for valid editable fields and valid state transition.

### Scenario 3: Unauthorized Scope Access
1. Attempt retrieve/update/delete from customer role outside ownership scope.

Expected outcome:
- Operation is denied with permission error.
- Restricted fields are not disclosed.

### Scenario 4: Delete with Policy Checks
1. Attempt delete/closure for account with no blockers.
2. Attempt delete/closure for account with dependency/retention blocker.

Expected outcome:
- Eligible delete/closure succeeds.
- Blocked delete/closure returns policy-compliant error reason.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and state transitions are documented in [data-model.md](data-model.md).
