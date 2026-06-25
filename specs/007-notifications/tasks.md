# Tasks: Notifications

**Input**: Design documents from `/specs/007-notifications/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize notifications module scaffolding and baseline configuration

- [ ] T001 Create notifications API module entrypoint in backend/src/api/notifications/index.ts
- [ ] T002 Add notifications module configuration schema in backend/src/lib/config/notifications-config.ts
- [ ] T003 [P] Add notifications module dependency wiring in backend/src/lib/container.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build core event, preference, channel policy, retry, and error infrastructure required by all user stories

**⚠️ CRITICAL**: No user story work should begin until this phase is complete

- [ ] T004 Define notification entities and enums in backend/prisma/schema.prisma
- [ ] T005 Create notifications migration in backend/prisma/migrations/20260625_notifications_init/migration.sql
- [ ] T006 [P] Implement consent and channel-preference policy evaluator in backend/src/lib/security/notification-preference-policy.ts
- [ ] T007 [P] Implement template context sanitization utility in backend/src/lib/security/notification-template-sanitizer.ts
- [ ] T008 Implement notification event repository abstractions in backend/src/services/notification-event-repository.ts
- [ ] T009 Implement dispatch attempt repository abstractions in backend/src/services/notification-dispatch-attempt-repository.ts
- [ ] T010 Implement delivery outcome repository abstractions in backend/src/services/notification-delivery-outcome-repository.ts
- [ ] T011 Implement retry and fallback policy service in backend/src/services/notification-retry-fallback-policy-service.ts
- [ ] T012 Implement notifications error mapping utilities in backend/src/lib/errors/notification-errors.ts

**Checkpoint**: Foundation complete - user story implementation can begin

---

## Phase 3: User Story 1 - Event Notifications (Priority: P1) 🎯 MVP

**Goal**: Trigger and process notification events, dispatch through channels, and record outcome details

**Independent Test**: Produce trigger events and verify dispatch attempts and outcome records

### Implementation for User Story 1

- [ ] T013 [P] [US1] Implement notification-event domain model in backend/src/models/notification-event.ts
- [ ] T014 [P] [US1] Implement trigger-notification request schema in backend/src/api/notifications/schemas/trigger-notification-schema.ts
- [ ] T015 [P] [US1] Implement event-status query schema in backend/src/api/notifications/schemas/get-notification-event-schema.ts
- [ ] T016 [US1] Implement trigger-notification ingestion service in backend/src/services/trigger-notification-service.ts
- [ ] T017 [US1] Implement dispatch worker orchestration service in backend/src/workers/notifications/notification-dispatch-worker.ts
- [ ] T018 [US1] Implement channel dispatch adapter interface and default adapter in backend/src/services/channel-dispatch-adapter.ts
- [ ] T019 [US1] Implement POST /notifications/events route handler in backend/src/api/notifications/routes/trigger-notification-route.ts
- [ ] T020 [US1] Implement GET /notifications/events/{notificationEventId} route handler in backend/src/api/notifications/routes/get-notification-event-route.ts
- [ ] T021 [US1] Register trigger/status routes in backend/src/api/notifications/index.ts

**Checkpoint**: User Story 1 is independently functional

---

## Phase 4: User Story 2 - Preference and Channel Handling (Priority: P2)

**Goal**: Enforce consent restrictions and channel preferences with retry/fallback behavior and auditable blocked delivery outcomes

**Independent Test**: Validate allowed and restricted deliveries according to preference policy and verify blocked attempts are logged

### Implementation for User Story 2

- [ ] T022 [P] [US2] Implement preference snapshot domain model in backend/src/models/notification-preference-snapshot.ts
- [ ] T023 [P] [US2] Implement attempts-list query schema in backend/src/api/notifications/schemas/list-notification-attempts-schema.ts
- [ ] T024 [US2] Integrate consent and channel-preference checks in backend/src/workers/notifications/notification-dispatch-worker.ts
- [ ] T025 [US2] Implement restricted-delivery blocking and reason-code mapping in backend/src/services/notification-preference-enforcement-service.ts
- [ ] T026 [US2] Implement retry/fallback attempt scheduler in backend/src/services/notification-retry-fallback-policy-service.ts
- [ ] T027 [US2] Persist blocked/failure outcomes in backend/src/services/notification-delivery-outcome-repository.ts
- [ ] T028 [US2] Implement GET /notifications/events/{notificationEventId}/attempts route handler in backend/src/api/notifications/routes/list-notification-attempts-route.ts
- [ ] T029 [US2] Register attempts route in backend/src/api/notifications/index.ts

**Checkpoint**: User Story 2 is independently functional

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Ensure contract, observability, and validation documentation are aligned

- [ ] T030 [P] Align notification endpoint payloads with contract in specs/007-notifications/contracts/openapi.yaml
- [ ] T031 Add structured notification telemetry fields in backend/src/lib/observability/notification-log-fields.ts
- [ ] T032 Update executable verification steps in specs/007-notifications/quickstart.md
- [ ] T033 Run quickstart scenario validation and record outcomes in specs/007-notifications/quickstart.md

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
T013 [US1] Implement notification-event domain model in backend/src/models/notification-event.ts
T014 [US1] Implement trigger-notification request schema in backend/src/api/notifications/schemas/trigger-notification-schema.ts
T015 [US1] Implement event-status query schema in backend/src/api/notifications/schemas/get-notification-event-schema.ts
```

## Parallel Example: User Story 2

```bash
# Parallelizable US2 work
T022 [US2] Implement preference snapshot domain model in backend/src/models/notification-preference-snapshot.ts
T023 [US2] Implement attempts-list query schema in backend/src/api/notifications/schemas/list-notification-attempts-schema.ts
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

## Notes

- All tasks follow strict checklist format: `- [ ] T### [P?] [US?] Description with file path`
- [P] indicates tasks that can run in parallel with no direct dependency conflict
- Story labels are included only in user-story phases
- Standalone test tasks were not added because explicit TDD/test-first tasking was not requested in the feature specification
