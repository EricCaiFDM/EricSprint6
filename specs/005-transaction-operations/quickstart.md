# Quickstart: Transaction Operations Feature Validation

## Prerequisites
- Node.js 22+
- PostgreSQL 15+
- Environment configured for API, DB connection, and RBAC seed data

## Setup
1. Install dependencies.
2. Apply database migrations.
3. Seed role/scope test fixtures (Customer and Admin).
4. Start the backend API service.

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
