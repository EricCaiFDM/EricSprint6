# Implementation Plan: Standing Orders

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-standing-orders/spec.md`

## Summary

Deliver standing order management and scheduled execution capabilities for recurring transfers, including lifecycle controls (create/update/pause/resume/cancel), schedule validation, UTC-driven due-window processing, execution-time funds and eligibility checks, and auditable outcome recording under hybrid RBAC.

## Technical Context

| Dimension | Decision |
|---|---|
| Language/Version | Java 21 (backend), JavaScript ES2022 (frontend) |
| Backend Framework | Spring Boot 3.x (Web, Data JPA, Validation, Security) |
| Frontend Framework | React 18 + React Query v5 + Axios + Vite |
| Storage | Runtime: MySQL; local development and test execution: H2 |
| Testing (backend) | JUnit 5, Mockito |
| Testing (frontend) | Jest, React Testing Library |
| Build (backend) | Maven (pom.xml) |
| Build (frontend) | Vite + npm / package.json |
| Target Platform | Linux server (backend), modern browser (frontend) |
| Monetary Precision | BigDecimal scale=2, single system currency |
| Security | JWT Bearer token on all endpoints, RBAC permission enforcement, ownership authorization per resource |
| Project Type | Web service (backend) + Web application (frontend) |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Constitution file currently contains placeholders only and no enforceable project principles.
- Gate result (pre-research): PASS.

## Project Structure

### Documentation (this feature)

```text
specs/006-standing-orders/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── api/
│   │   └── standing-orders/
│   ├── models/
│   ├── services/
│   ├── jobs/
│   └── lib/
└── tests/
    ├── contract/
    ├── integration/
    └── unit/
```

**Structure Decision**: Use a standing-order module under `backend/src/api/standing-orders` for lifecycle APIs, with scheduler/job orchestration in `backend/src/jobs` and service-layer execution pipelines shared with transfer policy checks.

## Post-Design Constitution Check

- Phase 1 artifacts align with the current placeholder constitution and contain no explicit violations.
- Gate result (post-design): PASS.

## Complexity Tracking

No constitution violations requiring justification.
