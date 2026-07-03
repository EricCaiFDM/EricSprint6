# Tasks: Notifications

**Input**: Design documents from `/specs/007-notifications/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize notifications module scaffolding and baseline configuration

- [X] T001 Create notifications API module entrypoint in backend/src/main/java/com/example/banking/api/notifications/index.java
- [X] T002 Add notifications module configuration schema in backend/src/main/java/com/example/banking/lib/config/notifications-config.java
- [X] T003 [P] Add notifications module dependency wiring in backend/src/main/java/com/example/banking/lib/container.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core event, preference, channel policy, retry, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [X] T004 Define notification entities and enums in backend/src/main/resources/db/changelog/db.changelog-master.yaml
- [X] T005 Create notifications migration in backend/src/main/resources/db/changelog/changes/20260625_notifications_init.xml
- [X] T006 [P] Implement consent and channel-preference policy evaluator in backend/src/main/java/com/example/banking/lib/security/notification-preference-policy.java
- [X] T007 [P] Implement template context sanitization utility in backend/src/main/java/com/example/banking/lib/security/notification-template-sanitizer.java
- [X] T008 Implement notification event repository abstractions in backend/src/main/java/com/example/banking/services/notification-event-repository.java
- [X] T009 Implement dispatch attempt repository abstractions in backend/src/main/java/com/example/banking/services/notification-dispatch-attempt-repository.java
- [X] T010 Implement delivery outcome repository abstractions in backend/src/main/java/com/example/banking/services/notification-delivery-outcome-repository.java
- [X] T011 Implement retry and fallback policy service in backend/src/main/java/com/example/banking/services/notification-retry-fallback-policy-service.java
- [X] T012 Implement notifications error mapping utilities in backend/src/main/java/com/example/banking/lib/errors/notification-errors.java

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Event Notifications (Priority: P1) 🎯 MVP

**Goal**: Trigger and process notification events, dispatch through channels, and record outcome details

**Independent Test**: Produce trigger events and verify dispatch attempts and outcome records

### Implementation for User Story 1

- [X] T013 [P] [US1] Implement notification-event domain model in backend/src/main/java/com/example/banking/models/notification-event.java
- [X] T014 [P] [US1] Implement trigger-notification request schema in backend/src/main/java/com/example/banking/api/notifications/schemas/trigger-notification-schema.java
- [X] T015 [P] [US1] Implement event-status query schema in backend/src/main/java/com/example/banking/api/notifications/schemas/get-notification-event-schema.java
- [X] T016 [US1] Implement trigger-notification ingestion service in backend/src/main/java/com/example/banking/services/trigger-notification-service.java
- [X] T017 [US1] Implement dispatch worker orchestration service in backend/src/main/java/com/example/banking/workers/notifications/notification-dispatch-worker.java
- [X] T018 [US1] Implement channel dispatch adapter interface and default adapter in backend/src/main/java/com/example/banking/services/channel-dispatch-adapter.java
- [X] T019 [US1] Implement POST /notifications/events route handler in backend/src/main/java/com/example/banking/api/notifications/routes/trigger-notification-route.java
- [X] T020 [US1] Implement GET /notifications/events/{notificationEventId} route handler in backend/src/main/java/com/example/banking/api/notifications/routes/get-notification-event-route.java
- [X] T021 [US1] Register trigger/status routes in backend/src/main/java/com/example/banking/api/notifications/index.java

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Preference and Channel Handling (Priority: P2)

**Goal**: Enforce consent restrictions and channel preferences with retry/fallback behavior and auditable blocked delivery outcomes

**Independent Test**: Validate allowed and restricted deliveries according to preference policy and verify blocked attempts are logged

### Implementation for User Story 2

- [X] T022 [P] [US2] Implement preference snapshot domain model in backend/src/main/java/com/example/banking/models/notification-preference-snapshot.java
- [X] T023 [P] [US2] Implement attempts-list query schema in backend/src/main/java/com/example/banking/api/notifications/schemas/list-notification-attempts-schema.java
- [X] T024 [US2] Integrate consent and channel-preference checks in backend/src/main/java/com/example/banking/workers/notifications/notification-dispatch-worker.java
- [X] T025 [US2] Implement restricted-delivery blocking and reason-code mapping in backend/src/main/java/com/example/banking/services/notification-preference-enforcement-service.java
- [X] T026 [US2] Implement retry/fallback attempt scheduler in backend/src/main/java/com/example/banking/services/notification-retry-fallback-policy-service.java
- [X] T027 [US2] Persist blocked/failure outcomes in backend/src/main/java/com/example/banking/services/notification-delivery-outcome-repository.java
- [X] T028 [US2] Implement GET /notifications/events/{notificationEventId}/attempts route handler in backend/src/main/java/com/example/banking/api/notifications/routes/list-notification-attempts-route.java
- [X] T029 [US2] Register attempts route in backend/src/main/java/com/example/banking/api/notifications/index.java

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, observability, and validation documentation are aligned

- [X] T030 [P] Align notification endpoint payloads with contract in specs/007-notifications/contracts/openapi.yaml
- [X] T031 Add structured notification telemetry fields in backend/src/main/java/com/example/banking/lib/observability/notification-log-fields.java
- [X] T032 Update executable verification steps in specs/007-notifications/quickstart.md
- [X] T033 Run quickstart scenario validation and record outcomes in specs/007-notifications/quickstart.md
- [X] T034 Add GET /notifications/events feed endpoint and repository query support in backend/src/app/java/com/example/banking/api/notifications/routes/ListRecentNotificationsRoute.java
- [X] T035 Add frontend live-alert banner behavior and polling in frontend/src/pages/NotificationsPage.tsx
- [X] T036 Add backend/frontend regression tests for notifications feed and live-alert updates in backend/src/test/java/com/example/banking/api/notifications/NotificationControllerIntegrationTest.java
- [X] T037 Add global snackbar-style notification alert in app shell with regression coverage in frontend/src/App.tsx and frontend/src/App.test.tsx
- [X] T038 Add tablet/mobile burger-menu navigation that stays fixed on the left while scrolling and expands/collapses in frontend/src/App.tsx, frontend/src/styles.css, and frontend/src/App.test.tsx
- [X] T039 Add dashboard welcome section using the signed-in customer name with regression coverage in frontend/src/pages/DashboardPage.tsx and frontend/src/pages/DashboardPage.test.tsx
- [X] T040 Add Swagger/OpenAPI backend documentation with public docs endpoints and regression coverage in backend/pom.xml, backend/src/app/java/com/example/banking/lib/config/SecurityConfig.java, and backend/src/test/java/com/example/banking/api/common/ApiDocumentationIntegrationTest.java
- [X] T041 Add per-endpoint Swagger summaries and descriptions for backend APIs in backend/src/app/java/com/example/banking/lib/config/OpenApiConfig.java with docs contract checks in backend/src/test/java/com/example/banking/api/common/ApiDocumentationIntegrationTest.java

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 and extends US1 dispatch orchestration behavior
- **Phase 5 (Polish)**: Depends on all targeted user stories

### User Story Dependencies

- **US1 (P1)**: Starts immediately after foundational completion
- **US2 (P2)**: Starts after foundational completion and integrates with dispatch pipeline from US1

### Within Each User Story

- Schemas before service workflows
- Service workflows before route handlers
- Route registration after handlers are implemented
- Story checkpoint validation before phase closeout

---

## Parallel Opportunities

- **Setup**: T003 can run in parallel with T001-T002
- **Foundational**: T006 and T007 can run in parallel after T004-T005
- **US1**: T014 and T015 can run in parallel with T013
- **US2**: T022 and T023 can run in parallel
- **Polish**: T030 can run in parallel with T031

---

## Parallel Example: User Story 1

```bash
# Parallelizable US1 work
T013 [US1] Implement notification-event domain model in backend/src/main/java/com/example/banking/models/notification-event.java
T014 [US1] Implement trigger-notification request schema in backend/src/main/java/com/example/banking/api/notifications/schemas/trigger-notification-schema.java
T015 [US1] Implement event-status query schema in backend/src/main/java/com/example/banking/api/notifications/schemas/get-notification-event-schema.java
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T022 [US2] Implement preference snapshot domain model in backend/src/main/java/com/example/banking/models/notification-preference-snapshot.java
T023 [US2] Implement attempts-list query schema in backend/src/main/java/com/example/banking/api/notifications/schemas/list-notification-attempts-schema.java
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate event trigger and dispatch outcome behavior independently

### Incremental Delivery

1. Deliver US1 (event trigger and dispatch)
2. Add US2 (preference/channel enforcement with retry/fallback)
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
