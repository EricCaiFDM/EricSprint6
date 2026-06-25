# Tasks: Account Management

**Input**: Design documents from `/specs/004-account-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize account-management module scaffolding and baseline configuration

- [ ] T001 Create account API module entrypoint in backend/src/api/accounts/index.ts
- [ ] T002 Add account module configuration schema in backend/src/lib/config/account-config.ts
- [ ] T003 [P] Add account module dependency wiring in backend/src/lib/container.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core data, authorization, policy, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [ ] T004 Define account entities and enums in backend/prisma/schema.prisma
- [ ] T005 Create account-management migration in backend/prisma/migrations/20260625_account_mgmt_init/migration.sql
- [ ] T006 [P] Implement hybrid RBAC scope policy utility in backend/src/lib/security/account-access-policy.ts
- [ ] T007 [P] Implement account field-level masking utility in backend/src/lib/security/account-field-masker.ts
- [ ] T008 Implement account eligibility check service in backend/src/services/account-eligibility-service.ts
- [ ] T009 Implement account lifecycle audit service in backend/src/services/account-lifecycle-audit-service.ts
- [ ] T010 Implement account-management error mapping utilities in backend/src/lib/errors/account-errors.ts
- [ ] T011 Implement account repository abstractions in backend/src/services/account-repository.ts
- [ ] T012 Implement account deletion policy-check service in backend/src/services/account-deletion-policy-service.ts

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Create and Retrieve Accounts (Priority: P1) 🎯 MVP

**Goal**: Allow authorized callers to create checking/savings accounts and retrieve account details

**Independent Test**: Create checking and savings accounts and retrieve authorized account details

### Implementation for User Story 1

- [ ] T013 [P] [US1] Implement account domain model in backend/src/models/account.ts
- [ ] T014 [P] [US1] Implement create-account request schema in backend/src/api/accounts/schemas/create-account-schema.ts
- [ ] T015 [P] [US1] Implement get-account request schema in backend/src/api/accounts/schemas/get-account-schema.ts
- [ ] T016 [US1] Implement create-account service workflow in backend/src/services/create-account-service.ts
- [ ] T017 [US1] Implement get-account-details service workflow in backend/src/services/get-account-details-service.ts
- [ ] T018 [US1] Implement POST /accounts route handler in backend/src/api/accounts/routes/create-account-route.ts
- [ ] T019 [US1] Implement GET /accounts/{accountId} route handler in backend/src/api/accounts/routes/get-account-route.ts
- [ ] T020 [US1] Register create/get routes in backend/src/api/accounts/index.ts

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - List and Update Accounts (Priority: P2)

**Goal**: Allow authorized callers to list customer accounts with pagination/filters and update editable fields

**Independent Test**: List accounts with pagination and filters, then update valid editable fields

### Implementation for User Story 2

- [ ] T021 [P] [US2] Implement list-accounts request schema in backend/src/api/accounts/schemas/list-accounts-schema.ts
- [ ] T022 [P] [US2] Implement update-account request schema in backend/src/api/accounts/schemas/update-account-schema.ts
- [ ] T023 [US2] Implement list-accounts service workflow in backend/src/services/list-accounts-service.ts
- [ ] T024 [US2] Implement update-account service workflow in backend/src/services/update-account-service.ts
- [ ] T025 [US2] Implement GET /accounts route handler in backend/src/api/accounts/routes/list-accounts-route.ts
- [ ] T026 [US2] Implement PATCH /accounts/{accountId} route handler in backend/src/api/accounts/routes/update-account-route.ts
- [ ] T027 [US2] Apply account masking to list/retrieve responses in backend/src/services/account-response-policy-service.ts
- [ ] T028 [US2] Register list/update routes in backend/src/api/accounts/index.ts

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: User Story 3 - Delete Account (Priority: P2)

**Goal**: Allow authorized deletion or closure when policy permits, and reject blocked operations

**Independent Test**: Delete eligible account and verify blocked delete for dependency/retention policy cases

### Implementation for User Story 3

- [ ] T029 [P] [US3] Implement delete-account request schema in backend/src/api/accounts/schemas/delete-account-schema.ts
- [ ] T030 [US3] Implement delete-account service workflow in backend/src/services/delete-account-service.ts
- [ ] T031 [US3] Integrate dependency and retention policy checks in backend/src/services/delete-account-service.ts
- [ ] T032 [US3] Implement DELETE /accounts/{accountId} route handler in backend/src/api/accounts/routes/delete-account-route.ts
- [ ] T033 [US3] Register delete route in backend/src/api/accounts/index.ts

**Checkpoint**: User Story 3 is independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, docs, and validation scenarios are aligned

- [ ] T034 [P] Align account endpoint payloads with contract in specs/004-account-management/contracts/openapi.yaml
- [ ] T035 Update executable verification steps in specs/004-account-management/quickstart.md
- [ ] T036 Run quickstart scenario validation and record outcomes in specs/004-account-management/quickstart.md

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
- **US2 (P2)**: Starts after foundational completion; independent testability preserved
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
- **US1**: T014 and T015 can run in parallel with T013
- **US2**: T021 and T022 can run in parallel
- **US3**: T029 can run in parallel with delete-service preparation
- **Polish**: T034 can run in parallel with implementation hardening

---

## Parallel Example: User Story 1

```bash
# Parallelizable US1 work
T013 [US1] Implement account domain model in backend/src/models/account.ts
T014 [US1] Implement create-account request schema in backend/src/api/accounts/schemas/create-account-schema.ts
T015 [US1] Implement get-account request schema in backend/src/api/accounts/schemas/get-account-schema.ts
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T021 [US2] Implement list-accounts request schema in backend/src/api/accounts/schemas/list-accounts-schema.ts
T022 [US2] Implement update-account request schema in backend/src/api/accounts/schemas/update-account-schema.ts
```

## Parallel Example: User Story 3

```bash
# Parallelizable US3 work
T029 [US3] Implement delete-account request schema in backend/src/api/accounts/schemas/delete-account-schema.ts
T031 [US3] Integrate dependency and retention policy checks in backend/src/services/delete-account-service.ts
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate create and retrieve behavior independently

### Incremental Delivery

1. Deliver US1 (create + retrieve)
2. Add US2 (list + update)
3. Add US3 (delete with policy checks)
4. Complete polish and quickstart validation

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then split by story:
   - Engineer A: US1
   - Engineer B: US2
   - Engineer C: US3

---

## Notes

- All tasks follow strict checklist format: `- [ ] T### [P?] [US?] Description with file path`
- [P] indicates tasks that can run in parallel with no direct dependency conflict
- Story labels are included only in user-story phases
- Standalone test tasks were not added because explicit TDD/test-first tasking was not requested in the feature specification
