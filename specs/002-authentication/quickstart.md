# Quickstart: Authentication Feature Validation

## Prerequisites
- Java 21
- Maven (pom.xml)
- React 18 + React Query v5 + Axios + Vite (JavaScript ES2022 frontend)
- MySQL (runtime) and H2 (local development/test execution)
- Postman and Prism mock server available for API validation and mocking
- Environment variables configured for Spring Boot backend DB connection, token secrets, and frontend API integration

## Setup
1. Install dependencies.
2. Initialize database schema/migrations.
3. Start the authentication API service.
4. Use Prism or Swagger mock endpoints to validate contract payload shape before backend integration.

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
- Suggested command (Prism): `prism mock specs/002-authentication/contracts/openapi.yaml`

## Data Model References
- Entity behaviors and transitions are defined in [data-model.md](data-model.md).

## Executable Verification Steps
1. Run registration request and verify HTTP 201 + `status=CREATED` and `userId`.
2. Run login request and verify HTTP 200 + tokens and `expiresIn`.
3. Run reset request for existing/non-existing identities and verify identical HTTP 202 payload.
4. Run refresh request with valid token and verify rotation payload.
5. Reuse prior refresh token and verify HTTP 401 error payload.

## Validation Outcomes (Latest Run)
- Registration + Login: PASS (payload shape and status semantics validated)
- Duplicate/Invalid flows: PASS (error contract validated)
- Reset enumeration safety: PASS (generic response preserved)
- Refresh rotation/replay protection: PASS (rotation + replay rejection logic validated)
