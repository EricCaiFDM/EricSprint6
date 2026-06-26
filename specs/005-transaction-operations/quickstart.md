# Quickstart: Transaction Operations Feature Validation

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

### Example Backend Commands
```bash
cd backend
mvn test
mvn spring-boot:run
```

### Example API Calls
```bash
# Deposit
curl -i -X POST "http://localhost:8080/transactions/deposit" \
	-H "Authorization: Bearer <access-token>" \
	-H "Idempotency-Key: dep-001-unique" \
	-H "Content-Type: application/json" \
	-d '{"accountId":"<account-uuid>","amount":"100.00"}'

# Withdrawal
curl -i -X POST "http://localhost:8080/transactions/withdrawal" \
	-H "Authorization: Bearer <access-token>" \
	-H "Idempotency-Key: wd-001-unique" \
	-H "Content-Type: application/json" \
	-d '{"accountId":"<account-uuid>","amount":"20.00"}'

# Transfer
curl -i -X POST "http://localhost:8080/transactions/transfer" \
	-H "Authorization: Bearer <access-token>" \
	-H "Idempotency-Key: tr-001-unique" \
	-H "Content-Type: application/json" \
	-d '{"sourceAccountId":"<source-uuid>","destinationAccountId":"<destination-uuid>","amount":"15.00"}'

# History
curl -i "http://localhost:8080/transactions/history?scopeType=ACCOUNT&scopeId=<account-uuid>&page=1&pageSize=20" \
	-H "Authorization: Bearer <access-token>"
```

## Validation Scenarios

### Scenario 1: Deposit and Withdraw
1. Post valid deposit for an eligible account with idempotency key.
2. Post valid withdrawal within available balance with idempotency key.
3. Retry either request with same idempotency key.

Expected outcome:
- Deposit increases account balance and creates immutable transaction record.
- Withdrawal decreases account balance and creates immutable transaction record.
- Retry returns deterministic idempotent response without duplicate posting.

### Scenario 2: Transfer Funds
1. Post valid transfer between eligible accounts with sufficient source funds.
2. Verify linked debit and credit records are created.
3. Verify source and destination balances reflect one atomic transfer.

Expected outcome:
- Transfer posts atomically as paired debit/credit entries.
- No partial posting remains on failure.

### Scenario 3: Overdraft and Permission Controls
1. Attempt withdrawal above available balance.
2. Attempt transfer above available balance.
3. Attempt transaction operation outside authorized scope.

Expected outcome:
- Insufficient-funds requests are rejected.
- Unauthorized operations are denied.
- No forbidden balance mutation occurs.

### Scenario 4: Transaction History Retrieval
1. Query transaction history by date range and transaction type.
2. Request first page and second page with deterministic sort.
3. Verify authorization by account/customer scope.

Expected outcome:
- History returns filtered, paginated results in deterministic order.
- Paging metadata and boundaries are correct.

## Contract Validation
- Validate API requests/responses against [contracts/openapi.yaml](contracts/openapi.yaml).

## Data Model References
- Entity definitions and relationships are documented in [data-model.md](data-model.md).

## Validation Outcomes (2026-06-26)

| Validation Item | Result | Notes |
|---|---|---|
| Static compile diagnostics (`src/app/java`, `src/test/java`) | PASS | No Java diagnostics in changed transaction implementation and tests. |
| Automated transaction integration tests (`TransactionControllerIntegrationTest`) | BLOCKED | Maven is unavailable in this shell (`mvn: command not found`), so runtime execution could not be performed here. |
| Frontend transaction operation validation (`frontend`: `npm run build`, `npm test -- --runInBand`) | PASS | Build succeeded and all test suites passed (5/5, 43 tests), including deposit/withdraw/transfer/history service coverage and Payments page UI operation coverage. |
| Quickstart scenario execution (deposit/withdraw/transfer/history) | PARTIAL | Endpoints and flows are implemented and covered by integration tests, but end-to-end command execution is pending in an environment with Maven installed. |
