# Implementation Plan: Monthly Statements

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-monthly-statements/spec.md`

## Summary

Deliver monthly statement generation and retrieval for eligible accounts with UTC month-boundary handling, immutable statement artifacts per version, auditable generation/retrieval events, and correction support for late-posted events tied to closed periods.

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
specs/008-monthly-statements/
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
├── src/main/java/com/example/banking/
│   ├── api/statements/
│   ├── models/
│   ├── services/
│   ├── jobs/
│   └── lib/
└── src/test/
    ├── contract/
    ├── integration/
    └── unit/

frontend/
└── src/
    ├── components/
    ├── pages/
    └── services/
```

**Structure Decision**: Use a statement module under `backend/src/main/java/com/example/banking/api/statements` for generation/retrieval endpoints, scheduled month-close generation services in backend jobs, and frontend retrieval surfaces in React for authorized statement access.

## Post-Design Constitution Check

- Phase 1 artifacts align with the current placeholder constitution and contain no explicit violations.
- Gate result (post-design): PASS.

## Complexity Tracking

No constitution violations requiring justification.
