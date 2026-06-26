# Quickstart: Monthly Statements Feature Validation

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
3. Seed eligible account, transaction, and RBAC fixtures.
4. Start backend API service and statement-generation scheduler job.

## Validation Scenarios

### Scenario 1: Generate Standard Monthly Statement
1. Trigger generation for an eligible account and valid month period.
2. Verify opening and closing balances are computed.
3. Verify activity summary aligns to period boundaries.

Expected outcome:
- Statement artifact is created with version 1.
- Generation event records success with timestamps.

### Scenario 2: Late-Posted Event Correction
1. Generate and close a statement period.
2. Insert a valid late-posted event tied to the closed period.
3. Trigger correction generation mode.

Expected outcome:
- Original artifact remains immutable.
- Corrected artifact is created as a new version with audit trail.

### Scenario 3: Authorized Retrieval
1. Retrieve statement as owning customer.
2. Retrieve same statement as admin.

Expected outcome:
- Both authorized requests succeed with statement content.
- Retrieval events are auditable.

### Scenario 4: Unauthorized Retrieval
1. Attempt retrieval as non-owner customer out of scope.

Expected outcome:
- Retrieval is denied with permission error.
- Denied retrieval event is recorded.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and relationships are documented in [data-model.md](data-model.md).
