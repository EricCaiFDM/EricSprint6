# Quickstart: Spending Insights Feature Validation

## Prerequisites
- Java 17+
- Maven or Gradle
- React 18 toolchain (Vite or CRA) with TypeScript support
- PostgreSQL 15+
- Postman and Prism mock server available for API validation and mocking
- Environment configured for Spring Boot backend, React frontend, RBAC fixtures, and taxonomy seed data

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed posted transaction history, taxonomy mappings, and RBAC fixtures.
4. Start backend API service and frontend application.

## Validation Scenarios

### Scenario 1: View Insights for Valid Data
1. Request insights for a valid scope and period with sufficient posted transactions.
2. Verify category summaries are returned.
3. Verify trend indicators are populated.

Expected outcome:
- Insight response includes categorized summary and trend metadata.
- Confidence metadata indicates acceptable coverage.

### Scenario 2: Sparse Data Confidence Output
1. Request insights for a sparse-data scope/period.
2. Verify limited-coverage output.
3. Verify confidence metadata fields.

Expected outcome:
- Response is returned with partial/limited insight status.
- Confidence level and coverage ratio are included.

### Scenario 3: Unauthorized Access Denial
1. Request insights for out-of-scope data as a non-authorized user.

Expected outcome:
- Request is denied with permission error.
- Retrieval audit event records denied outcome.

### Scenario 4: Hidden Record Protection
1. Request insights with category filters where some underlying records are hidden by policy.

Expected outcome:
- Response includes only permitted aggregated outputs.
- Hidden underlying records are not exposed in response fields.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and relationships are documented in [data-model.md](data-model.md).
