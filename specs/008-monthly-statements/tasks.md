# Tasks: Monthly Statements

**Input**: Design documents from `/specs/008-monthly-statements/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Automated test tasks are not included because the specification does not explicitly require a TDD workflow. Independent test criteria are included per user story.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize monthly statements feature scaffolding and baseline configuration.

- [ ] T001 Create monthly statement feature package structure in backend/src/main/java/com/example/banking/api/statements
- [ ] T002 [P] Add statement module configuration and scheduler properties in backend/src/main/resources/application.yml
- [ ] T003 [P] Add frontend statement service and page scaffolding in frontend/src/services/statements.ts and frontend/src/pages/StatementsPage.tsx

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared infrastructure that blocks all user story delivery until complete.

**CRITICAL**: No user story implementation should start until this phase is complete.

- [ ] T004 Create statement persistence migrations for metadata and versioning tables in backend/src/main/resources/db/changelog/008-monthly-statements.xml
- [ ] T005 [P] Create audit event persistence migrations for generation and retrieval events in backend/src/main/resources/db/changelog/009-statement-audit-events.xml
- [ ] T006 [P] Implement shared RBAC access guard utilities for statement scope checks in backend/src/main/java/com/example/banking/lib/security/StatementAccessGuard.java
- [ ] T007 Implement shared statement exception mapping and error responses in backend/src/main/java/com/example/banking/api/statements/StatementExceptionHandler.java
- [ ] T008 Configure statement repositories and base domain models in backend/src/main/java/com/example/banking/models/statement

**Checkpoint**: Foundation complete. User story implementation can begin.

---

## Phase 3: User Story 1 - Generate Statements (Priority: P1) MVP

**Goal**: Generate monthly statements with opening/closing balances, event-time activity summaries, and immutable correction versions.

**Independent Test**: Generate statement for active period and validate totals and UTC boundaries.

### Implementation for User Story 1

- [ ] T009 [P] [US1] Implement MonthlyStatement aggregate model and validation rules in backend/src/main/java/com/example/banking/models/statement/MonthlyStatement.java
- [ ] T010 [P] [US1] Implement StatementActivitySummary and mapping model in backend/src/main/java/com/example/banking/models/statement/StatementActivitySummary.java
- [ ] T011 [P] [US1] Implement StatementGenerationEvent model and persistence adapter in backend/src/main/java/com/example/banking/models/statement/StatementGenerationEvent.java and backend/src/main/java/com/example/banking/services/statement/StatementGenerationEventRepository.java
- [ ] T012 [US1] Implement UTC boundary and event-time ledger aggregation service in backend/src/main/java/com/example/banking/services/statement/StatementComputationService.java
- [ ] T013 [US1] Implement statement generation orchestration with correction-version flow in backend/src/main/java/com/example/banking/services/statement/StatementGenerationService.java
- [ ] T014 [US1] Implement generation endpoint contract for POST /statements/generate in backend/src/main/java/com/example/banking/api/statements/StatementGenerationController.java
- [ ] T015 [US1] Implement scheduled monthly generation job with account partitioning in backend/src/main/java/com/example/banking/jobs/MonthlyStatementGenerationJob.java

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Retrieve Statements (Priority: P2)

**Goal**: Retrieve generated statements only for authorized customer/admin scopes with auditable outcomes.

**Independent Test**: Retrieve statement within owned scope and admin scope, then verify unauthorized scope is denied.

### Implementation for User Story 2

- [ ] T016 [P] [US2] Implement StatementRetrievalEvent model and persistence adapter in backend/src/main/java/com/example/banking/models/statement/StatementRetrievalEvent.java and backend/src/main/java/com/example/banking/services/statement/StatementRetrievalEventRepository.java
- [ ] T017 [P] [US2] Implement StatementAccessPolicy model and resolver in backend/src/main/java/com/example/banking/models/statement/StatementAccessPolicy.java and backend/src/main/java/com/example/banking/services/statement/StatementAccessPolicyService.java
- [ ] T018 [US2] Implement retrieval authorization service using hybrid RBAC checks in backend/src/main/java/com/example/banking/services/statement/StatementAuthorizationService.java
- [ ] T019 [US2] Implement retrieval endpoint contract for GET /statements/{statementId} in backend/src/main/java/com/example/banking/api/statements/StatementRetrievalController.java
- [ ] T020 [US2] Implement listing endpoint contract for GET /statements with account and period filters in backend/src/main/java/com/example/banking/api/statements/StatementQueryController.java
- [ ] T021 [US2] Implement frontend statement retrieval/listing integration and access error handling in frontend/src/services/statements.ts and frontend/src/pages/StatementsPage.tsx

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Finalize consistency, observability, and readiness across delivered stories.

- [ ] T022 [P] Update OpenAPI examples and response schemas for generation and retrieval edge cases in specs/008-monthly-statements/contracts/openapi.yaml
- [ ] T023 [P] Update quickstart validation steps for correction-version and unauthorized retrieval checks in specs/008-monthly-statements/quickstart.md
- [ ] T024 Run full feature validation checklist and document outcomes in specs/008-monthly-statements/checklists/quality.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 (Setup): No dependencies.
- Phase 2 (Foundational): Depends on Phase 1 and blocks all user stories.
- Phase 3 (US1): Depends on Phase 2.
- Phase 4 (US2): Depends on Phase 2 and can proceed after or alongside US1 when shared foundations are stable.
- Phase 5 (Polish): Depends on completion of US1 and US2.

### User Story Dependencies

- US1 (P1): No dependency on other user stories.
- US2 (P2): No strict dependency on US1, but uses shared statement entities and access infrastructure from Phase 2.

### Task-Level Dependency Highlights

- T012 depends on T009 and T010.
- T013 depends on T011 and T012.
- T014 depends on T013.
- T015 depends on T013.
- T018 depends on T016, T017, and T006.
- T019 and T020 depend on T018.
- T021 depends on T019 and T020.

---

## Parallel Opportunities

- Setup: T002 and T003 can run in parallel after T001.
- Foundational: T005 and T006 can run in parallel after T004 starts.
- US1: T009, T010, and T011 can run in parallel before service wiring.
- US2: T016 and T017 can run in parallel before authorization and controllers.
- Polish: T022 and T023 can run in parallel before T024.

## Parallel Example: User Story 1

- Task: T009 Implement MonthlyStatement aggregate model in backend/src/main/java/com/example/banking/models/statement/MonthlyStatement.java
- Task: T010 Implement StatementActivitySummary model in backend/src/main/java/com/example/banking/models/statement/StatementActivitySummary.java
- Task: T011 Implement StatementGenerationEvent model and repository adapter in backend/src/main/java/com/example/banking/models/statement/StatementGenerationEvent.java and backend/src/main/java/com/example/banking/services/statement/StatementGenerationEventRepository.java

## Parallel Example: User Story 2

- Task: T016 Implement StatementRetrievalEvent model and repository adapter in backend/src/main/java/com/example/banking/models/statement/StatementRetrievalEvent.java and backend/src/main/java/com/example/banking/services/statement/StatementRetrievalEventRepository.java
- Task: T017 Implement StatementAccessPolicy model and resolver in backend/src/main/java/com/example/banking/models/statement/StatementAccessPolicy.java and backend/src/main/java/com/example/banking/services/statement/StatementAccessPolicyService.java

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete all US1 tasks in Phase 3.
3. Validate US1 independently using statement generation and boundary/totals checks.
4. Demo or release MVP increment.

### Incremental Delivery

1. Build shared setup and foundations.
2. Deliver US1 generation increment.
3. Deliver US2 retrieval and access-control increment.
4. Complete polish tasks and final validation.

### Parallel Team Strategy

1. Team aligns on setup and foundational tasks.
2. One developer implements US1 while another prepares US2 domain/repository tasks.
3. Integrate controllers and frontend retrieval after authorization service is stable.

---

## Notes

- All tasks follow checklist format with Task ID, optional parallel marker, optional story label, and explicit file paths.
- Story labels are only used for user story phases.
- Each user story has independent test criteria and can be validated separately.
