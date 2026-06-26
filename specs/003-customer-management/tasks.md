# Tasks: Customer Management

**Input**: Design documents from `/specs/003-customer-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize customer-management module scaffolding and baseline configuration

- [X] T001 Create customer API module entrypoint in backend/src/main/java/com/example/banking/api/customers/index.java
- [X] T002 Add customer module configuration schema in backend/src/main/java/com/example/banking/lib/config/customer-config.java
- [X] T003 [P] Add customer module dependency wiring in backend/src/main/java/com/example/banking/lib/container.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core data, authorization, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [X] T004 Define customer entities and enums in backend/src/main/resources/db/changelog/db.changelog-master.yaml
- [X] T005 Create customer-management migration in backend/src/main/resources/db/changelog/changes/20260625_customer_mgmt_init.xml
- [X] T006 [P] Implement hybrid RBAC scope policy utility in backend/src/main/java/com/example/banking/lib/security/customer-access-policy.java
- [X] T007 [P] Implement customer payload masking utility in backend/src/main/java/com/example/banking/lib/security/customer-field-masker.java
- [X] T008 Implement customer lifecycle audit service in backend/src/main/java/com/example/banking/services/customer-lifecycle-audit-service.java
- [X] T009 Implement customer-management error mapping utilities in backend/src/main/java/com/example/banking/lib/errors/customer-errors.java
- [X] T010 Implement customer repository abstractions in backend/src/main/java/com/example/banking/services/customer-repository.java
- [X] T011 Implement deletion policy-check service in backend/src/main/java/com/example/banking/services/customer-deletion-policy-service.java

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Create Customer (Priority: P1) 🎯 MVP

**Goal**: Allow authorized callers to create customer profiles with uniqueness validation and audit logging

**Independent Test**: Submit valid and invalid create requests and verify uniqueness handling

### Implementation for User Story 1

- [X] T012 [P] [US1] Implement customer domain model in backend/src/main/java/com/example/banking/models/customer.java
- [X] T013 [P] [US1] Implement create-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/create-customer-schema.java
- [X] T014 [US1] Implement create-customer service workflow in backend/src/main/java/com/example/banking/services/create-customer-service.java
- [X] T015 [US1] Implement POST /customers route handler in backend/src/main/java/com/example/banking/api/customers/routes/create-customer-route.java
- [X] T016 [US1] Add uniqueness conflict handling and reason mapping in backend/src/main/java/com/example/banking/services/create-customer-service.java
- [X] T017 [US1] Register create-customer route in backend/src/main/java/com/example/banking/api/customers/index.java

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Maintain Customer Profile (Priority: P1)

**Goal**: Allow authorized callers to update and retrieve customer details with ownership/admin scope controls

**Independent Test**: Update existing profile, fetch details, and validate ownership/admin scope

### Implementation for User Story 2

- [X] T018 [P] [US2] Implement update-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/update-customer-schema.java
- [X] T019 [P] [US2] Implement get-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/get-customer-schema.java
- [X] T020 [US2] Implement update-customer service workflow in backend/src/main/java/com/example/banking/services/update-customer-service.java
- [X] T021 [US2] Implement get-customer-details service workflow in backend/src/main/java/com/example/banking/services/get-customer-details-service.java
- [X] T022 [US2] Implement PATCH /customers/{customerId} route handler in backend/src/main/java/com/example/banking/api/customers/routes/update-customer-route.java
- [X] T023 [US2] Implement GET /customers/{customerId} route handler in backend/src/main/java/com/example/banking/api/customers/routes/get-customer-route.java
- [X] T024 [US2] Apply field masking logic in backend/src/main/java/com/example/banking/services/get-customer-details-service.java
- [X] T025 [US2] Register update/get routes in backend/src/main/java/com/example/banking/api/customers/index.java

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: User Story 3 - Delete Customer (Priority: P2)

**Goal**: Allow authorized deletion when no policy blockers exist; deny and explain blocked attempts

**Independent Test**: Attempt delete for eligible and blocked records

### Implementation for User Story 3

- [X] T026 [P] [US3] Implement delete-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/delete-customer-schema.java
- [X] T027 [US3] Implement delete-customer service workflow in backend/src/main/java/com/example/banking/services/delete-customer-service.java
- [X] T028 [US3] Integrate dependency/retention checks in backend/src/main/java/com/example/banking/services/delete-customer-service.java
- [X] T029 [US3] Implement DELETE /customers/{customerId} route handler in backend/src/main/java/com/example/banking/api/customers/routes/delete-customer-route.java
- [X] T030 [US3] Register delete route in backend/src/main/java/com/example/banking/api/customers/index.java

**Checkpoint**: User Story 3 is independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, docs, and validation scenarios are aligned

- [X] T031 [P] Align customer endpoint payloads with contract in specs/003-customer-management/contracts/openapi.yaml
- [X] T032 Update executable verification steps in specs/003-customer-management/quickstart.md
- [X] T033 Run quickstart scenario validation and record outcomes in specs/003-customer-management/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2; can proceed after foundational completion
- **Phase 5 (US3)**: Depends on Phase 2 and uses policy services from foundational phase
- **Phase 6 (Polish)**: Depends on all targeted user stories

### User Story Dependencies

- **US1 (P1)**: Starts immediately after foundational completion
- **US2 (P1)**: Starts after foundational completion; independent testability preserved
- **US3 (P2)**: Starts after foundational completion; functionally relies on deletion policy service from foundational phase

### Within Each User Story

- Schemas before services
- Services before route handlers
- Route registration after handlers are implemented
- Story checkpoint validation before phase closeout

---

## Parallel Opportunities

- **Setup**: T003 can run in parallel with T001-T002
- **Foundational**: T006 and T007 can run in parallel after T004-T005
- **US1**: T012 and T013 can run in parallel
- **US2**: T018 and T019 can run in parallel
- **US3**: T026 can run in parallel with US3 service prep
- **Polish**: T031 can run in parallel with implementation hardening

---

## Parallel Example: User Story 1

```bash
# Parallelizable US1 work
T012 [US1] Implement customer domain model in backend/src/main/java/com/example/banking/models/customer.java
T013 [US1] Implement create-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/create-customer-schema.java
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T018 [US2] Implement update-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/update-customer-schema.java
T019 [US2] Implement get-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/get-customer-schema.java
```

## Parallel Example: User Story 3

```bash
# Parallelizable US3 work
T026 [US3] Implement delete-customer request schema in backend/src/main/java/com/example/banking/api/customers/schemas/delete-customer-schema.java
T028 [US3] Integrate dependency/retention checks in backend/src/main/java/com/example/banking/services/delete-customer-service.java
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate create-customer behavior independently

### Incremental Delivery

1. Deliver US1 (create)
2. Add US2 (update + get details)
3. Add US3 (delete with policy checks)
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
- Tests were not added as standalone tasks because explicit test-first/TDD tasking was not requested in the specification
