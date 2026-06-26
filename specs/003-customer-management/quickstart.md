# Quickstart: Customer Management Feature Validation

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

4. Obtain JWT token via auth flow and call customer endpoints with bearer auth.

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

## Executable API Verification

1. Create customer

```bash
curl -X POST http://localhost:8080/customers \
	-H "Authorization: Bearer <ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"externalCustomerKey":"cust-ext-001",
		"legalName":"Jane Q Customer",
		"primaryEmail":"jane.customer@example.com",
		"phoneNumber":"+27123456789"
	}'
```

2. Get customer

```bash
curl -X GET http://localhost:8080/customers/<CUSTOMER_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

3. Update customer

```bash
curl -X PATCH http://localhost:8080/customers/<CUSTOMER_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>" \
	-H "Content-Type: application/json" \
	-d '{
		"legalName":"Jane Updated",
		"status":"SUSPENDED"
	}'
```

4. Delete customer

```bash
curl -X DELETE http://localhost:8080/customers/<CUSTOMER_ID> \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and transitions are in [data-model.md](data-model.md).

## Validation Outcomes (Recorded)

| Check | Command | Result |
|---|---|---|
| Backend test suite | `cd backend && mvn test` | Not executed: `mvn` not available in environment |
| Frontend tests | `cd frontend && npm test -- --runInBand` | Pass (2 suites, 7 tests) |
| Frontend build | `cd frontend && npm run build` | Pass |
| Backend static diagnostics | VS Code Java diagnostics | Pass (no errors in customer/auth modules) |
