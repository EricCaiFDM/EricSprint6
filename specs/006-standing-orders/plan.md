# Implementation Plan: Standing Orders

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-standing-orders/spec.md`

## Summary

Deliver standing order management and scheduled execution capabilities for recurring transfers, including lifecycle controls (create/update/pause/resume/cancel), schedule validation, UTC-driven due-window processing, execution-time funds and eligibility checks, and auditable outcome recording under hybrid RBAC.

## Technical Context

**Language/Version**: Backend Java 17+; Frontend TypeScript (React 18)

**Backend Stack**: Java 17+, Spring Boot (Web, Security, Validation), Maven/Gradle, JUnit + MockMvc, OpenAPI Generator, GitHub Spec Kit, Git, GitHub

**Frontend Stack**: React 18, Vite / CRA, TypeScript, Axios, Jest / React Testing Library, Postman, Prism mock server

**Cross-cutting Stack**: GitHub Copilot, ESLint / Checkstyle / SpotBugs / SonarQube, Dependency scanners (npm audit, OWASP), GitHub Actions, Swagger UI

**Storage**: PostgreSQL for standing-order definitions, next-run schedules, and immutable execution event records

**Testing**: JUnit + MockMvc for backend; Jest / React Testing Library for frontend

**Target Platform**: Linux containerized Spring Boot backend + browser-based React frontend

**Project Type**: Full-stack web application

**Performance Goals**: p95 setup/update lifecycle latency < 400ms; 99% of eligible executions started within configured schedule window

**Constraints**: UTC canonical scheduling, execution follows transfer and insufficient-funds policy, retry behavior follows predefined policy, delegated cross-scope setup out of scope

**Scale/Scope**: Initial release for up to 100k active standing orders with minute-level scheduler windows

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
