# Quickstart: Account Management Feature Validation

## Prerequisites
- Java 21
- Maven (pom.xml)
- React 18 + React Query v5 + Axios + Vite (JavaScript ES2022 frontend)
- MySQL (runtime) and H2 (local development/test execution)
- Postman and Prism mock server available for API validation and mocking
- Environment configured for Spring Boot backend, React frontend, DB connection, and RBAC seed data

## Setup
1. Backend dependency and test execution:

```bash
cd backend
mvn test
```

2. Frontend regression baseline:

```bash
cd frontend
npm test -- --runInBand
npm run build
```

3. Start backend API service (local profile):

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

4. Obtain a JWT token via auth flow and call account endpoints with bearer auth.

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

### Scenario 4: Owned Scope with Distinct Identity Keys
1. Authenticate as a customer user where JWT `sub` differs from `customerId`.
2. List accounts for the owned customer profile id.
3. Retrieve one listed account.

Expected outcome:
- List and retrieve succeed when `customers.owner_user_id` matches JWT `sub`.
- No false 403 occurs solely because `customerId` differs from JWT `sub`.

### Scenario 5: Delete with Policy Checks
1. Attempt delete/closure for account with no blockers.
2. Attempt delete/closure for account with dependency/retention blocker.

Expected outcome:
- Eligible delete/closure succeeds.
- Blocked delete/closure returns policy-compliant error reason.

## Executable API Verification

1. Create account

```bash
curl -X POST http://localhost:8080/accounts \
	-H "Authorization: Bearer <ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"customerId":"<CUSTOMER_ID>",
		"accountType":"CHECKING",
		"currencyCode":"USD",
		"nickname":"Daily Spending"
	}'
```

2. Get account

```bash
curl -X GET http://localhost:8080/accounts/<ACCOUNT_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

3. List accounts

```bash
curl -X GET "http://localhost:8080/accounts?customerId=<CUSTOMER_ID>&page=1&pageSize=20&accountType=CHECKING" \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

4. Update account

```bash
curl -X PATCH http://localhost:8080/accounts/<ACCOUNT_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"nickname":"Bills",
		"status":"SUSPENDED"
	}'
```

5. Delete account

```bash
curl -X DELETE http://localhost:8080/accounts/<ACCOUNT_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and state transitions are documented in [data-model.md](data-model.md).

## Validation Outcomes (Recorded)

| Check | Command | Result |
|---|---|---|
| Backend test suite | `cd backend && mvn test` | Not executed: `mvn` not available in environment |
| Frontend tests | `cd frontend && npm test -- --runInBand` | Pass (4 suites, 26 tests) |
| Frontend build | `cd frontend && npm run build` | Pass |
| Backend static diagnostics | VS Code Java diagnostics | Pass (no errors in account/customer/auth modules) |
