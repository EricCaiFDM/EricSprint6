# Quickstart: Notifications Feature Validation

## Prerequisites
- Java 21
- Maven (pom.xml)
- React 18 + React Query v5 + Axios + Vite (JavaScript ES2022 frontend)
- MySQL (runtime) and H2 (local development/test execution)
- Postman and Prism mock server available for API validation and mocking
- Environment configured for Spring Boot backend, React frontend, DB connection, notification policy fixtures, and dispatch worker process

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed notification templates, channel policy, and consent/preference fixtures.
4. Start backend API service and notification worker.

## Validation Scenarios

### Scenario 1: Event Notification Trigger and Dispatch
1. Trigger a configured account or transaction notification event.
2. Verify dispatch attempt is enqueued and processed.
3. Verify delivery outcome is recorded.

Expected outcome:
- Dispatch attempt is made for allowed channel(s).
- Outcome is recorded with success/failure details.

### Scenario 2: Consent and Preference Restrictions
1. Configure recipient with restricted consent/channel preferences.
2. Trigger event for restricted notification category.

Expected outcome:
- Restricted delivery is blocked and logged.
- Final outcome records blocked status with reason code.

### Scenario 3: Retry and Fallback Behavior
1. Simulate primary channel unavailability.
2. Trigger event and observe retry/fallback sequence.
3. Exhaust retry policy to force terminal failure if needed.

Expected outcome:
- Retry/fallback follows configured policy.
- Final outcome and all attempts are recorded with reason codes.

### Scenario 4: Template Resolution Failure Path
1. Trigger event with intentionally invalid template context.
2. Verify template resolution failure handling.

Expected outcome:
- Dispatch is not sent with malformed payload.
- Failure reason is captured and auditable.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and relationships are documented in [data-model.md](data-model.md).
