# Tasks: Standing Orders

**Input**: Design documents from `/specs/006-standing-orders/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize standing-orders module scaffolding and baseline configuration

- [X] T001 Create standing-orders API module entrypoint in backend/src/main/java/com/example/banking/api/standing-orders/index.java
- [X] T002 Add standing-orders module configuration schema in backend/src/main/java/com/example/banking/lib/config/standing-orders-config.java
- [X] T003 [P] Add standing-orders module dependency wiring in backend/src/main/java/com/example/banking/lib/container.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core scheduling, lifecycle, authorization, retry, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [X] T004 Define standing-order entities and enums in backend/src/main/resources/db/changelog/db.changelog-master.yaml
- [X] T005 Create standing-orders migration in backend/src/main/resources/db/changelog/changes/20260625_standing_orders_init.xml
- [X] T006 [P] Implement standing-order RBAC scope policy utility in backend/src/main/java/com/example/banking/lib/security/standing-order-access-policy.java
- [X] T007 [P] Implement UTC schedule calculation utility in backend/src/main/java/com/example/banking/lib/scheduling/standing-order-schedule-calculator.java
- [X] T008 Implement standing-order lifecycle transition policy service in backend/src/main/java/com/example/banking/services/standing-order-lifecycle-policy-service.java
- [X] T009 Implement standing-order repository abstractions in backend/src/main/java/com/example/banking/services/standing-order-repository.java
- [X] T010 Implement execution event repository abstractions in backend/src/main/java/com/example/banking/services/standing-order-execution-event-repository.java
- [X] T011 Implement retry policy evaluator service in backend/src/main/java/com/example/banking/services/standing-order-retry-policy-service.java
- [X] T012 Implement standing-order error mapping utilities in backend/src/main/java/com/example/banking/lib/errors/standing-order-errors.java
- [X] T013 Implement standing-order lifecycle audit service in backend/src/main/java/com/example/banking/services/standing-order-lifecycle-audit-service.java

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Configure Standing Order (Priority: P1) 🎯 MVP

**Goal**: Allow authorized callers to create, update, pause, resume, and cancel recurring standing orders

**Independent Test**: Create a valid standing order, perform lifecycle transitions, and verify persisted state and audit trail

### Implementation for User Story 1

- [X] T014 [P] [US1] Implement standing-order domain model in backend/src/main/java/com/example/banking/models/standing-order.java
- [X] T015 [P] [US1] Implement create-standing-order request schema in backend/src/main/java/com/example/banking/api/standing-orders/schemas/create-standing-order-schema.java
- [X] T016 [P] [US1] Implement update-standing-order request schema in backend/src/main/java/com/example/banking/api/standing-orders/schemas/update-standing-order-schema.java
- [X] T017 [US1] Implement create-standing-order service workflow in backend/src/main/java/com/example/banking/services/create-standing-order-service.java
- [X] T018 [US1] Implement update-standing-order service workflow in backend/src/main/java/com/example/banking/services/update-standing-order-service.java
- [X] T019 [US1] Implement pause/resume/cancel standing-order service workflow in backend/src/main/java/com/example/banking/services/standing-order-lifecycle-service.java
- [X] T020 [US1] Implement POST /standing-orders route handler in backend/src/main/java/com/example/banking/api/standing-orders/routes/create-standing-order-route.java
- [X] T021 [US1] Implement PATCH /standing-orders/{standingOrderId} route handler in backend/src/main/java/com/example/banking/api/standing-orders/routes/update-standing-order-route.java
- [X] T022 [US1] Implement pause/resume/cancel route handlers in backend/src/main/java/com/example/banking/api/standing-orders/routes/lifecycle-routes.java
- [X] T023 [US1] Register standing-order lifecycle routes in backend/src/main/java/com/example/banking/api/standing-orders/index.java

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Scheduled Execution (Priority: P1)

**Goal**: Execute active standing orders at due windows and record success/failure outcomes with retry behavior

**Independent Test**: Trigger a scheduler run for due standing orders and verify success/failure outcome recording and bounded retry behavior

### Implementation for User Story 2

- [X] T024 [P] [US2] Implement scheduler due-window query utility in backend/src/main/java/com/example/banking/jobs/standing-orders/due-window-query.java
- [X] T025 [P] [US2] Implement scheduler claim cursor service in backend/src/main/java/com/example/banking/jobs/standing-orders/schedule-cursor-service.java
- [X] T026 [US2] Implement standing-order execution orchestrator in backend/src/main/java/com/example/banking/services/standing-order-execution-orchestrator.java
- [X] T027 [US2] Integrate transfer eligibility and funds-availability checks in backend/src/main/java/com/example/banking/services/standing-order-execution-orchestrator.java
- [X] T028 [US2] Implement retry scheduling logic for failed executions in backend/src/main/java/com/example/banking/services/standing-order-retry-policy-service.java
- [X] T029 [US2] Persist execution outcome events in backend/src/main/java/com/example/banking/services/standing-order-execution-event-repository.java
- [X] T030 [US2] Implement scheduler runner job in backend/src/main/java/com/example/banking/jobs/standing-orders/standing-order-scheduler-job.java
- [X] T031 [US2] Implement GET /standing-orders/{standingOrderId}/executions route handler in backend/src/main/java/com/example/banking/api/standing-orders/routes/list-executions-route.java
- [X] T032 [US2] Register execution outcomes route in backend/src/main/java/com/example/banking/api/standing-orders/index.java

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, docs, and observability are aligned across lifecycle and execution flows

- [X] T033 [P] Align standing-order endpoint payloads with contract in specs/006-standing-orders/contracts/openapi.yaml
- [X] T034 Add structured execution telemetry fields in backend/src/main/java/com/example/banking/lib/observability/standing-order-log-fields.java
- [X] T035 Update executable verification steps in specs/006-standing-orders/quickstart.md
- [X] T036 Run quickstart scenario validation and record outcomes in specs/006-standing-orders/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 and uses standing-order configuration artifacts from US1 data model
- **Phase 5 (Polish)**: Depends on all targeted user stories

### User Story Dependencies

- **US1 (P1)**: Starts immediately after foundational completion
- **US2 (P1)**: Starts after foundational completion; independently testable via scheduler-triggered run and outcome inspection

### Within Each User Story

- Schemas before service workflows
- Service workflows before route handlers/jobs
- Route registration after handlers are implemented
- Story checkpoint validation before phase closeout

---

## Parallel Opportunities

- **Setup**: T003 can run in parallel with T001-T002
- **Foundational**: T006 and T007 can run in parallel after T004-T005
- **US1**: T015 and T016 can run in parallel with T014
- **US2**: T024 and T025 can run in parallel
- **Polish**: T033 can run in parallel with T034

---

## Parallel Example: User Story 1

```bash
# Parallelizable US1 work
T014 [US1] Implement standing-order domain model in backend/src/main/java/com/example/banking/models/standing-order.java
T015 [US1] Implement create-standing-order request schema in backend/src/main/java/com/example/banking/api/standing-orders/schemas/create-standing-order-schema.java
T016 [US1] Implement update-standing-order request schema in backend/src/main/java/com/example/banking/api/standing-orders/schemas/update-standing-order-schema.java
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T024 [US2] Implement scheduler due-window query utility in backend/src/main/java/com/example/banking/jobs/standing-orders/due-window-query.java
T025 [US2] Implement scheduler claim cursor service in backend/src/main/java/com/example/banking/jobs/standing-orders/schedule-cursor-service.java
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate standing-order configuration and lifecycle behavior independently

### Incremental Delivery

1. Deliver US1 (configure lifecycle)
2. Add US2 (scheduled execution and outcomes)
3. Complete polish and quickstart validation

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then split by story:
   - Engineer A: US1
   - Engineer B: US2

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
