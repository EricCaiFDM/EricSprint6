# Quickstart: Spending Insights

## Prerequisites

- Backend running on `http://localhost:8080`
- Valid JWT access token
- A customer with at least one account and posted transactions

## 1. Seed Example Data

Create at least one deposit and one spending transaction (withdrawal or transfer debit) so insights have meaningful output.

## 2. Retrieve Spending Insights

Request customer-scope insights:

```bash
curl -sS "http://localhost:8080/insights/spending" \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

Request account-scope insights for a specific account:

```bash
curl -sS "http://localhost:8080/insights/spending?scopeType=ACCOUNT&scopeId=<ACCOUNT_ID>" \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

Optional filter by supported category codes:

```bash
curl -sS "http://localhost:8080/insights/spending?scopeType=ACCOUNT&scopeId=<ACCOUNT_ID>&categoryFilters=CASH_WITHDRAWAL,TRANSFER_OUT" \
	-H "Authorization: Bearer <ACCESS_TOKEN>"
```

## 3. Validate Response Semantics

- `totalSpend`: sum of posted spending transactions only (withdrawals and transfer debits).
- `categories[*].ratio`: category share of `totalSpend`.
- `categories[*].trend`: direction versus previous equivalent time window.
- `confidenceLevel` and `coverageRatio`: reliability based on available sample size.
- `status`:
	- `GENERATED`: sufficient sample size.
	- `PARTIAL`: sparse sample size.
	- `INSUFFICIENT_DATA`: no posted spending transactions.

## 4. Validate Security Boundaries

- A customer requesting insights for another customer/account must receive `403 INSIGHT_FORBIDDEN`.
- Invalid filters must return `400 INSIGHT_VALIDATION_ERROR`.

## 5. Validate Audit Persistence

After requests, verify retrieval events were recorded:

```sql
SELECT request_id, requester_user_id, outcome, reason_code
FROM insight_retrieval_events
ORDER BY occurred_at_utc DESC;
```
