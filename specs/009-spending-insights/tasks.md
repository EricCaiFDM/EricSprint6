# Tasks: Spending Insights

**Input**: Design documents from `/specs/009-spending-insights/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Test tasks are intentionally omitted because the specification does not explicitly request TDD or test-first implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize module scaffolding and baseline insight configuration.

- [ ] T001 Create insights module package structure in backend/src/main/java/com/example/banking/api/insights
- [ ] T002 [P] Add spending insight configuration properties in backend/src/main/resources/application.yml
- [ ] T003 [P] Add frontend insights service/page scaffolding in frontend/src/services/insights.ts and frontend/src/pages/SpendingInsightsPage.tsx

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before user stories can be implemented.

**CRITICAL**: No user story work should begin until this phase is complete.

- [ ] T004 Create insight persistence migration for requests, summaries, confidence, and audit events in backend/src/main/resources/db/changelog/010-spending-insights.xml
- [ ] T005 [P] Implement taxonomy mapping repository and loader in backend/src/main/java/com/example/banking/services/insights/TaxonomyMappingRepository.java
- [ ] T006 [P] Implement shared RBAC scope guard for insights in backend/src/main/java/com/example/banking/lib/security/InsightAccessGuard.java
- [ ] T007 Implement shared insight error handling and API exception mapping in backend/src/main/java/com/example/banking/api/insights/InsightExceptionHandler.java
- [ ] T008 Configure insight domain repositories in backend/src/main/java/com/example/banking/services/insights/repository

**Checkpoint**: Foundational infrastructure is complete and user stories can start.

---

## Phase 3: User Story 1 - View Insights (Priority: P1) MVP

**Goal**: Return categorized spending summaries and trend indicators for authorized users.

**Independent Test**: Generate insights for a valid scope and period, then verify categories and trend output.

### Implementation for User Story 1

- [ ] T009 [P] [US1] Implement SpendingInsightRequest model in backend/src/main/java/com/example/banking/models/insights/SpendingInsightRequest.java
- [ ] T010 [P] [US1] Implement SpendingInsight and InsightCategorySummary models in backend/src/main/java/com/example/banking/models/insights/SpendingInsight.java and backend/src/main/java/com/example/banking/models/insights/InsightCategorySummary.java
- [ ] T011 [P] [US1] Implement insight retrieval audit model in backend/src/main/java/com/example/banking/models/insights/InsightRetrievalEvent.java
- [ ] T012 [US1] Implement posted-transaction aggregation and category summarization service in backend/src/main/java/com/example/banking/services/insights/SpendingInsightAggregationService.java
- [ ] T013 [US1] Implement trend indicator computation service in backend/src/main/java/com/example/banking/services/insights/SpendingInsightTrendService.java
- [ ] T014 [US1] Implement insight orchestration service for request validation and output assembly in backend/src/main/java/com/example/banking/services/insights/SpendingInsightService.java
- [ ] T015 [US1] Implement GET /insights/spending endpoint in backend/src/main/java/com/example/banking/api/insights/SpendingInsightController.java
- [ ] T016 [US1] Implement frontend insights retrieval and rendering for categorized summaries and trends in frontend/src/services/insights.ts and frontend/src/pages/SpendingInsightsPage.tsx

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Handle Sparse Data (Priority: P2)

**Goal**: Return confidence-aware limited-coverage insights for sparse datasets without exposing hidden records.

**Independent Test**: Request insights for sparse-data scope and verify confidence indicators and partial status behavior.

### Implementation for User Story 2

- [ ] T017 [P] [US2] Implement InsightConfidenceMetadata model in backend/src/main/java/com/example/banking/models/insights/InsightConfidenceMetadata.java
- [ ] T018 [P] [US2] Implement sparse-data coverage and confidence evaluator in backend/src/main/java/com/example/banking/services/insights/InsightConfidenceService.java
- [ ] T019 [US2] Implement hidden-record safe aggregation filter in backend/src/main/java/com/example/banking/services/insights/InsightDataVisibilityService.java
- [ ] T020 [US2] Extend spending insight orchestration for PARTIAL and INSUFFICIENT_DATA responses in backend/src/main/java/com/example/banking/services/insights/SpendingInsightService.java
- [ ] T021 [US2] Extend API response contract mapping for confidence metadata in backend/src/main/java/com/example/banking/api/insights/SpendingInsightResponseMapper.java
- [ ] T022 [US2] Update frontend insights page to display confidence level and coverage ratio in frontend/src/pages/SpendingInsightsPage.tsx

**Checkpoint**: User Story 2 should be independently functional and testable.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Finalize contracts, quickstart validation, and release-readiness checks.

- [ ] T023 [P] Update contract examples for sparse-data and permission-denied outcomes in specs/009-spending-insights/contracts/openapi.yaml
- [ ] T024 [P] Update validation steps for confidence and hidden-record protection in specs/009-spending-insights/quickstart.md
- [ ] T025 Run end-to-end feature validation and record results in specs/009-spending-insights/checklists/quality.md

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): No dependencies; starts immediately.
- Foundational (Phase 2): Depends on setup completion; blocks all user stories.
- User Stories (Phase 3+): Depend on foundational completion.
- Polish (Phase 5): Depends on user story completion.

### User Story Dependencies

- User Story 1 (P1): Can start immediately after Phase 2.
- User Story 2 (P2): Can start after Phase 2; extends US1 insight service behavior but remains independently testable for sparse-data cases.

### Within Each User Story

- Models before services.
- Services before endpoint/response mapping.
- Backend behavior before frontend integration.

---

## Parallel Opportunities

- Setup: T002 and T003 can run in parallel.
- Foundational: T005 and T006 can run in parallel.
- User Story 1: T009, T010, and T011 can run in parallel.
- User Story 2: T017 and T018 can run in parallel.
- Polish: T023 and T024 can run in parallel.

## Parallel Example: User Story 1

- Task: T009 Implement SpendingInsightRequest model in backend/src/main/java/com/example/banking/models/insights/SpendingInsightRequest.java
- Task: T010 Implement SpendingInsight and InsightCategorySummary models in backend/src/main/java/com/example/banking/models/insights/SpendingInsight.java and backend/src/main/java/com/example/banking/models/insights/InsightCategorySummary.java
- Task: T011 Implement insight retrieval audit model in backend/src/main/java/com/example/banking/models/insights/InsightRetrievalEvent.java

## Parallel Example: User Story 2

- Task: T017 Implement InsightConfidenceMetadata model in backend/src/main/java/com/example/banking/models/insights/InsightConfidenceMetadata.java
- Task: T018 Implement sparse-data coverage and confidence evaluator in backend/src/main/java/com/example/banking/services/insights/InsightConfidenceService.java

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate categorized summary and trend output independently.
4. Demo MVP increment.

### Incremental Delivery

1. Finish setup and foundations.
2. Deliver US1 insight retrieval.
3. Deliver US2 sparse-data confidence behavior.
4. Complete polish and final validation.

### Parallel Team Strategy

1. Team completes setup and foundational tasks.
2. One stream focuses on insight aggregation/trend services while another prepares sparse-data confidence services.
3. Merge through shared orchestration and frontend confidence display tasks.

---

## Notes

- Every task follows strict checklist format with ID, optional [P], optional [US#], and concrete file path(s).
- Story labels are used only for user story phases.
- Each story includes independent test criteria and can be validated separately.
