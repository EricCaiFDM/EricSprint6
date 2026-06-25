# Quickstart: Customer Management Feature Validation

## Prerequisites
- Java 21
- Maven (pom.xml)
- React 18 + React Query v5 + Axios + Vite (JavaScript ES2022 frontend)
- MySQL (runtime) and H2 (local development/test execution)
- Postman and Prism mock server available for API validation and mocking
- Environment configured for Spring Boot backend, React frontend, DB connection, and RBAC seed data

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed role/scope test fixtures (Customer and Admin).
4. Start the backend API service.

## Validation Scenarios

### Scenario 1: Create Customer
1. Submit valid create request as authorized caller.
2. Repeat with duplicate business identifier.

Expected outcome:
- First request succeeds and returns customer identifier.
- Duplicate request fails with conflict response.

### Scenario 2: Update + Get Customer Details
1. Update editable profile fields for an existing customer.
2. Retrieve customer details as owner customer role.
3. Retrieve same customer details as admin role.

Expected outcome:
- Update succeeds with validated fields.
- Customer role sees owned scoped profile (with policy masking).
- Admin role can retrieve across scopes per policy.

### Scenario 3: Unauthorized Scope Access
1. Attempt get/update/delete as a customer role outside ownership scope.

Expected outcome:
- Operation is denied with permission error.
- No restricted profile fields are disclosed.

### Scenario 4: Delete with Policy Checks
1. Attempt delete for customer with no blockers.
2. Attempt delete for customer with dependency/retention blocker.

Expected outcome:
- Eligible delete succeeds.
- Blocked delete fails with policy-compliant reason.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and transitions are in [data-model.md](data-model.md).
