# Tasks: Standing Orders

**Input**: Design documents from `/specs/006-standing-orders/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize standing-orders module scaffolding and baseline configuration

- [ ] T001 Create standing-orders API module entrypoint in backend/src/api/standing-orders/index.ts
- [ ] T002 Add standing-orders module configuration schema in backend/src/lib/config/standing-orders-config.ts
- [ ] T003 [P] Add standing-orders module dependency wiring in backend/src/lib/container.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core scheduling, lifecycle, authorization, retry, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [ ] T004 Define standing-order entities and enums in backend/prisma/schema.prisma
- [ ] T005 Create standing-orders migration in backend/prisma/migrations/20260625_standing_orders_init/migration.sql
- [ ] T006 [P] Implement standing-order RBAC scope policy utility in backend/src/lib/security/standing-order-access-policy.ts
- [ ] T007 [P] Implement UTC schedule calculation utility in backend/src/lib/scheduling/standing-order-schedule-calculator.ts
- [ ] T008 Implement standing-order lifecycle transition policy service in backend/src/services/standing-order-lifecycle-policy-service.ts
- [ ] T009 Implement standing-order repository abstractions in backend/src/services/standing-order-repository.ts
- [ ] T010 Implement execution event repository abstractions in backend/src/services/standing-order-execution-event-repository.ts
- [ ] T011 Implement retry policy evaluator service in backend/src/services/standing-order-retry-policy-service.ts
- [ ] T012 Implement standing-order error mapping utilities in backend/src/lib/errors/standing-order-errors.ts
- [ ] T013 Implement standing-order lifecycle audit service in backend/src/services/standing-order-lifecycle-audit-service.ts

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Configure Standing Order (Priority: P1) 🎯 MVP

**Goal**: Allow authorized callers to create, update, pause, resume, and cancel recurring standing orders

**Independent Test**: Create a valid standing order, perform lifecycle transitions, and verify persisted state and audit trail

### Implementation for User Story 1

- [ ] T014 [P] [US1] Implement standing-order domain model in backend/src/models/standing-order.ts
- [ ] T015 [P] [US1] Implement create-standing-order request schema in backend/src/api/standing-orders/schemas/create-standing-order-schema.ts
- [ ] T016 [P] [US1] Implement update-standing-order request schema in backend/src/api/standing-orders/schemas/update-standing-order-schema.ts
- [ ] T017 [US1] Implement create-standing-order service workflow in backend/src/services/create-standing-order-service.ts
- [ ] T018 [US1] Implement update-standing-order service workflow in backend/src/services/update-standing-order-service.ts
- [ ] T019 [US1] Implement pause/resume/cancel standing-order service workflow in backend/src/services/standing-order-lifecycle-service.ts
- [ ] T020 [US1] Implement POST /standing-orders route handler in backend/src/api/standing-orders/routes/create-standing-order-route.ts
- [ ] T021 [US1] Implement PATCH /standing-orders/{standingOrderId} route handler in backend/src/api/standing-orders/routes/update-standing-order-route.ts
- [ ] T022 [US1] Implement pause/resume/cancel route handlers in backend/src/api/standing-orders/routes/lifecycle-routes.ts
- [ ] T023 [US1] Register standing-order lifecycle routes in backend/src/api/standing-orders/index.ts

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Scheduled Execution (Priority: P1)

**Goal**: Execute active standing orders at due windows and record success/failure outcomes with retry behavior

**Independent Test**: Trigger a scheduler run for due standing orders and verify success/failure outcome recording and bounded retry behavior

### Implementation for User Story 2

- [ ] T024 [P] [US2] Implement scheduler due-window query utility in backend/src/jobs/standing-orders/due-window-query.ts
- [ ] T025 [P] [US2] Implement scheduler claim cursor service in backend/src/jobs/standing-orders/schedule-cursor-service.ts
- [ ] T026 [US2] Implement standing-order execution orchestrator in backend/src/services/standing-order-execution-orchestrator.ts
- [ ] T027 [US2] Integrate transfer eligibility and funds-availability checks in backend/src/services/standing-order-execution-orchestrator.ts
- [ ] T028 [US2] Implement retry scheduling logic for failed executions in backend/src/services/standing-order-retry-policy-service.ts
- [ ] T029 [US2] Persist execution outcome events in backend/src/services/standing-order-execution-event-repository.ts
- [ ] T030 [US2] Implement scheduler runner job in backend/src/jobs/standing-orders/standing-order-scheduler-job.ts
- [ ] T031 [US2] Implement GET /standing-orders/{standingOrderId}/executions route handler in backend/src/api/standing-orders/routes/list-executions-route.ts
- [ ] T032 [US2] Register execution outcomes route in backend/src/api/standing-orders/index.ts

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, docs, and observability are aligned across lifecycle and execution flows

- [ ] T033 [P] Align standing-order endpoint payloads with contract in specs/006-standing-orders/contracts/openapi.yaml
- [ ] T034 Add structured execution telemetry fields in backend/src/lib/observability/standing-order-log-fields.ts
- [ ] T035 Update executable verification steps in specs/006-standing-orders/quickstart.md
- [ ] T036 Run quickstart scenario validation and record outcomes in specs/006-standing-orders/quickstart.md

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
T014 [US1] Implement standing-order domain model in backend/src/models/standing-order.ts
T015 [US1] Implement create-standing-order request schema in backend/src/api/standing-orders/schemas/create-standing-order-schema.ts
T016 [US1] Implement update-standing-order request schema in backend/src/api/standing-orders/schemas/update-standing-order-schema.ts
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T024 [US2] Implement scheduler due-window query utility in backend/src/jobs/standing-orders/due-window-query.ts
T025 [US2] Implement scheduler claim cursor service in backend/src/jobs/standing-orders/schedule-cursor-service.ts
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

## Notes

- All tasks follow strict checklist format: `- [ ] T### [P?] [US?] Description with file path`
- [P] indicates tasks that can run in parallel with no direct dependency conflict
- Story labels are included only in user-story phases
- Standalone test tasks were not added because explicit TDD/test-first tasking was not requested in the feature specification
