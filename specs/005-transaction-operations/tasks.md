# Tasks: Transaction Operations

**Input**: Design documents from `/specs/005-transaction-operations/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize transaction-operations module scaffolding and baseline configuration

- [X] T001 Create transaction API module entrypoint in backend/src/main/java/com/example/banking/api/transactions/index.java
- [X] T002 Add transaction module configuration schema in backend/src/main/java/com/example/banking/lib/config/transaction-config.java
- [X] T003 [P] Add transaction module dependency wiring in backend/src/main/java/com/example/banking/lib/container.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core ledger, consistency, idempotency, authorization, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [X] T004 Define transaction entities and enums in backend/src/main/resources/db/changelog/db.changelog-master.yaml
- [X] T005 Create transaction-operations migration in backend/src/main/resources/db/changelog/changes/20260625_transaction_ops_init.xml
- [X] T006 [P] Implement transaction RBAC scope policy utility in backend/src/main/java/com/example/banking/lib/security/transaction-access-policy.java
- [X] T007 [P] Implement monetary precision and rounding utility in backend/src/main/java/com/example/banking/lib/finance/money-policy.java
- [X] T008 Implement balance consistency and locking utility in backend/src/main/java/com/example/banking/services/balance-consistency-service.java
- [X] T009 Implement idempotency record service in backend/src/main/java/com/example/banking/services/idempotency-service.java
- [X] T010 Implement immutable transaction repository abstractions in backend/src/main/java/com/example/banking/services/transaction-repository.java
- [X] T011 Implement transfer-link persistence service in backend/src/main/java/com/example/banking/services/transfer-link-service.java
- [X] T012 Implement transaction error mapping utilities in backend/src/main/java/com/example/banking/lib/errors/transaction-errors.java
- [X] T013 Implement transaction lifecycle audit service in backend/src/main/java/com/example/banking/services/transaction-lifecycle-audit-service.java

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Deposit and Withdraw (Priority: P1) 🎯 MVP

**Goal**: Allow authorized callers to post deposits and withdrawals with no-overdraft enforcement and immutable records

**Independent Test**: Post valid deposits and withdrawals, validate balance updates and transaction records, and verify insufficient-funds blocking

### Implementation for User Story 1

- [X] T014 [P] [US1] Implement transaction domain model in backend/src/main/java/com/example/banking/models/transaction.java
- [X] T015 [P] [US1] Implement deposit request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/deposit-schema.java
- [X] T016 [P] [US1] Implement withdrawal request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/withdrawal-schema.java
- [X] T017 [US1] Implement deposit service workflow in backend/src/main/java/com/example/banking/services/deposit-service.java
- [X] T018 [US1] Implement withdrawal service workflow in backend/src/main/java/com/example/banking/services/withdrawal-service.java
- [X] T019 [US1] Integrate idempotency handling in backend/src/main/java/com/example/banking/services/monetary-idempotency-orchestrator.java
- [X] T020 [US1] Implement POST /transactions/deposit route handler in backend/src/main/java/com/example/banking/api/transactions/routes/deposit-route.java
- [X] T021 [US1] Implement POST /transactions/withdrawal route handler in backend/src/main/java/com/example/banking/api/transactions/routes/withdrawal-route.java
- [X] T022 [US1] Register deposit and withdrawal routes in backend/src/main/java/com/example/banking/api/transactions/index.java

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Transfer Funds (Priority: P1)

**Goal**: Allow authorized callers to execute atomic transfers with linked debit and credit transaction records

**Independent Test**: Execute valid transfer and verify atomic debit/credit posting with deterministic idempotent replay behavior

### Implementation for User Story 2

- [X] T023 [P] [US2] Implement transfer request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/transfer-schema.java
- [X] T024 [P] [US2] Implement transfer response mapper in backend/src/main/java/com/example/banking/api/transactions/mappers/transfer-response-mapper.java
- [X] T025 [US2] Implement atomic transfer orchestration service in backend/src/main/java/com/example/banking/services/transfer-service.java
- [X] T026 [US2] Integrate no-overdraft debit guardrails in backend/src/main/java/com/example/banking/services/transfer-service.java
- [X] T027 [US2] Persist transfer debit-credit linkage in backend/src/main/java/com/example/banking/services/transfer-link-service.java
- [X] T028 [US2] Implement POST /transactions/transfer route handler in backend/src/main/java/com/example/banking/api/transactions/routes/transfer-route.java
- [X] T029 [US2] Register transfer route in backend/src/main/java/com/example/banking/api/transactions/index.java

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: User Story 3 - Retrieve Transaction History (Priority: P2)

**Goal**: Allow authorized callers to retrieve transaction history with filters, pagination, and deterministic ordering

**Independent Test**: Query account and customer transaction history by date/type with paging and deterministic ordering guarantees

### Implementation for User Story 3

- [X] T030 [P] [US3] Implement transaction history query schema in backend/src/main/java/com/example/banking/api/transactions/schemas/history-schema.java
- [X] T031 [P] [US3] Implement transaction history response mapper in backend/src/main/java/com/example/banking/api/transactions/mappers/history-response-mapper.java
- [X] T032 [US3] Implement transaction history query service in backend/src/main/java/com/example/banking/services/transaction-history-service.java
- [X] T033 [US3] Implement deterministic sort and paging policy in backend/src/main/java/com/example/banking/services/transaction-history-query-policy.java
- [X] T034 [US3] Implement GET /transactions/history route handler in backend/src/main/java/com/example/banking/api/transactions/routes/history-route.java
- [X] T035 [US3] Register history route in backend/src/main/java/com/example/banking/api/transactions/index.java

**Checkpoint**: User Story 3 is independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, docs, observability, and validation scenarios are aligned across all transaction flows

- [X] T036 [P] Align transaction endpoint payloads with contract in specs/005-transaction-operations/contracts/openapi.yaml
- [X] T037 Add structured transaction telemetry fields in backend/src/main/java/com/example/banking/lib/observability/transaction-log-fields.java
- [X] T038 Update executable verification steps in specs/005-transaction-operations/quickstart.md
- [X] T039 Run quickstart scenario validation and record outcomes in specs/005-transaction-operations/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 and foundational transfer/idempotency services
- **Phase 5 (US3)**: Depends on Phase 2 and transaction repository abstractions
- **Phase 6 (Polish)**: Depends on all targeted user stories

### User Story Dependencies

- **US1 (P1)**: Starts immediately after foundational completion
- **US2 (P1)**: Starts after foundational completion; independent testability preserved
- **US3 (P2)**: Starts after foundational completion; independent query validation preserved

### Within Each User Story

- Schemas before service workflows
- Service workflows before route handlers
- Route registration after handlers are implemented
- Story checkpoint validation before phase closeout

---

## Parallel Opportunities

- **Setup**: T003 can run in parallel with T001-T002
- **Foundational**: T006 and T007 can run in parallel after T004-T005
- **US1**: T015 and T016 can run in parallel with T014
- **US2**: T023 and T024 can run in parallel
- **US3**: T030 and T031 can run in parallel
- **Polish**: T036 can run in parallel with T037

---

## Parallel Example: User Story 1

```bash
# Parallelizable US1 work
T014 [US1] Implement transaction domain model in backend/src/main/java/com/example/banking/models/transaction.java
T015 [US1] Implement deposit request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/deposit-schema.java
T016 [US1] Implement withdrawal request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/withdrawal-schema.java
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T023 [US2] Implement transfer request schema in backend/src/main/java/com/example/banking/api/transactions/schemas/transfer-schema.java
T024 [US2] Implement transfer response mapper in backend/src/main/java/com/example/banking/api/transactions/mappers/transfer-response-mapper.java
```

## Parallel Example: User Story 3

```bash
# Parallelizable US3 work
T030 [US3] Implement transaction history query schema in backend/src/main/java/com/example/banking/api/transactions/schemas/history-schema.java
T031 [US3] Implement transaction history response mapper in backend/src/main/java/com/example/banking/api/transactions/mappers/history-response-mapper.java
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate deposit and withdrawal behavior independently

### Incremental Delivery

1. Deliver US1 (deposit + withdrawal)
2. Add US2 (atomic transfer)
3. Add US3 (transaction history retrieval)
4. Complete polish and quickstart validation

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then split by story:
   - Engineer A: US1
   - Engineer B: US2
   - Engineer C: US3

---

### Definition of Done (MANDATORY)

- [ ] TXXX Verify specification completeness (business rules, acceptance criteria, negative scenarios, error codes, OpenAPI, allowed/forbidden libraries, guardrails)
- [ ] TXXX Verify implementation matches OpenAPI exactly (no undocumented fields or behavior)
- [ ] TXXX Verify test obligations (>=70% coverage, contract tests, negative tests, mock-server integration tests)
- [ ] TXXX Verify guardrails (spec reference, secure dependencies, static analysis, secret scan, dependency audit)
- [ ] TXXX Verify git workflow evidence (feature branch, PR to develop, 2 approvals, passing CI, meaningful commits)
- [ ] TXXX Verify documentation/demo readiness (OpenAPI, Swagger UI, README, examples, positive/negative demo)

## Notes

- All tasks follow strict checklist format: `- [ ] T### [P?] [US?] Description with file path`
- [P] indicates tasks that can run in parallel with no direct dependency conflict
- Story labels are included only in user-story phases
- Standalone test tasks were not added because explicit TDD/test-first tasking was not requested in the feature specification
