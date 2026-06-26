# Quickstart: Standing Orders Feature Validation

## Prerequisites
- Java 21
- Maven (pom.xml)
- React 18 + React Query v5 + Axios + Vite (JavaScript ES2022 frontend)
- MySQL (runtime) and H2 (local development/test execution)
- Postman and Prism mock server available for API validation and mocking
- Environment configured for Spring Boot backend, React frontend, DB connection, RBAC seed data, and scheduler worker execution

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed role/scope fixtures and eligible account fixtures.
4. Start backend API service and standing-order scheduler worker.

## Validation Scenarios

### Scenario 1: Configure Standing Order
1. Create a standing order with valid source/destination, amount, cadence, and dates.
2. Pause the standing order.
3. Resume the standing order.
4. Cancel the standing order.

Expected outcome:
- Create succeeds and returns standing-order identifier and schedule metadata.
- Pause/resume/cancel transitions are persisted and auditable.

### Scenario 2: Reject Invalid Configuration
1. Submit invalid cadence/date combinations.
2. Submit invalid amount and ineligible account relationships.

Expected outcome:
- Invalid setup/update requests are rejected with validation reason codes.
- No invalid standing order is persisted.

### Scenario 3: Scheduled Execution Success Path
1. Create active standing order that is due within current schedule window.
2. Trigger scheduler run.
3. Inspect execution event and linked transfer reference.

Expected outcome:
- Due order is executed once per schedule window according to policy.
- Execution outcome is recorded as success with transfer reference.

### Scenario 4: Scheduled Execution Failure and Retry
1. Force insufficient-funds condition for due order.
2. Trigger scheduler run and observe failure outcome.
3. Simulate dependency outage and validate retry behavior.

Expected outcome:
- Insufficient funds and outage outcomes are reason-coded and auditable.
- Retry scheduling follows predefined retry policy bounds.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and relationships are documented in [data-model.md](data-model.md).
