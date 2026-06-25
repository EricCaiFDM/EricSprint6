# Implementation Plan: Standing Orders

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-standing-orders/spec.md`

## Summary

Deliver standing order management and scheduled execution capabilities for recurring transfers, including lifecycle controls (create/update/pause/resume/cancel), schedule validation, UTC-driven due-window processing, execution-time funds and eligibility checks, and auditable outcome recording under hybrid RBAC.

## Technical Context

**Language/Version**: TypeScript 5.x on Node.js 22 LTS

**Primary Dependencies**: Fastify (HTTP API), Zod (validation), Prisma (ORM/data access), pino (structured logging), node-cron (or equivalent scheduler)

**Storage**: PostgreSQL for standing-order definitions, next-run schedules, and immutable execution event records

**Testing**: Vitest for unit/integration tests, Supertest (or Fastify inject) for API contract/integration coverage

**Target Platform**: Linux containerized backend API with scheduled worker process

**Project Type**: Backend web-service

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
