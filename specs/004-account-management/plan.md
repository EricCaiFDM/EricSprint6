# Implementation Plan: Account Management

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-account-management/spec.md`

## Summary

Deliver an account lifecycle management service for checking and savings accounts that supports create, retrieve, list, update, and delete operations with hybrid RBAC (Customer owned scope, Admin global scope), eligibility validation, policy-based deletion checks, and auditable lifecycle events.

## Technical Context

**Language/Version**: Backend Java 17+; Frontend TypeScript (React 18)

**Backend Stack**: Java 17+, Spring Boot (Web, Security, Validation), Maven/Gradle, JUnit + MockMvc, OpenAPI Generator, GitHub Spec Kit, Git, GitHub

**Frontend Stack**: React 18, Vite / CRA, TypeScript, Axios, Jest / React Testing Library, Postman, Prism mock server

**Cross-cutting Stack**: GitHub Copilot, ESLint / Checkstyle / SpotBugs / SonarQube, Dependency scanners (npm audit, OWASP), GitHub Actions, Swagger UI

**Storage**: PostgreSQL for account records and account lifecycle audit events

**Testing**: JUnit + MockMvc for backend; Jest / React Testing Library for frontend

**Target Platform**: Linux containerized Spring Boot backend + browser-based React frontend

**Project Type**: Full-stack web application

**Performance Goals**: p95 create/update latency < 400ms; p95 retrieve latency < 200ms; p95 list-first-page latency < 300ms

**Constraints**: Hybrid RBAC mandatory, only checking/savings in scope, field-level visibility controls required, bulk import/export out of scope

**Scale/Scope**: Initial release for up to 500k active accounts with moderate CRUD and listing throughput

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Constitution file currently contains placeholders only and no enforceable project principles.
- Gate result (pre-research): PASS.

## Project Structure

### Documentation (this feature)

```text
specs/004-account-management/
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
│   │   └── accounts/
│   ├── models/
│   ├── services/
│   └── lib/
└── tests/
    ├── contract/
    ├── integration/
    └── unit/
```

**Structure Decision**: Use a dedicated account module under `backend/src/api/accounts` with domain services and model abstractions for eligibility, RBAC enforcement, list pagination/filtering, and lifecycle policy checks.

## Post-Design Constitution Check

- Phase 1 artifacts align with the current placeholder constitution and contain no explicit violations.
- Gate result (post-design): PASS.

## Complexity Tracking

No constitution violations requiring justification.
