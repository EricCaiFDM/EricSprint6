# Implementation Plan: Monthly Statements

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-monthly-statements/spec.md`

## Summary

Deliver monthly statement generation and retrieval for eligible accounts with UTC month-boundary handling, immutable statement artifacts per version, auditable generation/retrieval events, and correction support for late-posted events tied to closed periods.

## Technical Context

**Language/Version**: Backend Java 17+; Frontend TypeScript (React 18)

**Backend Stack**: Java 17+, Spring Boot (Web, Security, Validation), Maven/Gradle, JUnit + MockMvc, OpenAPI Generator, GitHub Spec Kit, Git, GitHub

**Frontend Stack**: React 18, Vite / CRA, TypeScript, Axios, Jest / React Testing Library, Postman, Prism mock server

**Cross-cutting Stack**: GitHub Copilot, ESLint / Checkstyle / SpotBugs / SonarQube, Dependency scanners (npm audit, OWASP), GitHub Actions, Swagger UI

**Storage**: PostgreSQL for statement metadata, artifact version records, and statement audit events

**Testing**: JUnit + MockMvc for backend; Jest / React Testing Library for frontend

**Target Platform**: Linux containerized Spring Boot backend + browser-based React frontend

**Project Type**: Full-stack web application

**Performance Goals**: 98% of standard-volume statement generations complete within 5 minutes; retrieval authorization checks complete within service SLAs

**Constraints**: UTC period boundaries, no real-time full recomputation, hybrid RBAC scope enforcement, immutable statement artifact versions

**Scale/Scope**: Initial release for monthly generation across all eligible active accounts with retained version history and correction outputs

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
