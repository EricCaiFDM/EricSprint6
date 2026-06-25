# Quickstart: Authentication Feature Validation

## Prerequisites
- Java 17+
- Maven or Gradle
- React 18 toolchain (Vite or CRA) with TypeScript support
- PostgreSQL 15+
- Postman and Prism mock server available for API validation and mocking
- Environment variables configured for Spring Boot backend DB connection, token secrets, and frontend API integration

## Setup
1. Install dependencies.
2. Initialize database schema/migrations.
3. Start the authentication API service.

## Validation Scenarios

### Scenario 1: Registration + Login
1. Submit valid registration request.
2. Verify account is created.
3. Submit login with same credentials.
4. Verify access + refresh tokens are returned.

Expected outcome:
- Registration succeeds once per unique email.
- Login succeeds for valid active account.

### Scenario 2: Duplicate Registration and Invalid Login
1. Attempt registration with an existing email.
2. Attempt login with wrong password.

Expected outcome:
- Duplicate registration fails with conflict response.
- Invalid login fails with non-sensitive error response.

### Scenario 3: Password Reset Request Enumeration Safety
1. Submit reset request for existing identity.
2. Submit reset request for non-existing identity.

Expected outcome:
- Both return generic acknowledgment.
- Internal event/audit entries differ appropriately.

### Scenario 4: Token Refresh Rotation
1. Login to obtain refresh token.
2. Refresh token once and capture newly returned refresh token.
3. Attempt reusing old refresh token.

Expected outcome:
- First refresh succeeds and rotates token.
- Reuse attempt fails and is audited as failure.

## Contract Validation
- Validate endpoints and response schemas against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity behaviors and transitions are defined in [data-model.md](data-model.md).
