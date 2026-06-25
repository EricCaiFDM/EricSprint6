# Implementation Plan: Customer Management

**Branch**: `[unassigned]` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-customer-management/spec.md`

## Summary

Deliver a customer profile lifecycle service that supports create, update, get details, and delete operations with hybrid RBAC enforcement (Customer owned scope, Admin global scope), uniqueness validation, policy-based deletion safeguards, and auditable lifecycle events.

## Technical Context

**Language/Version**: TypeScript 5.x on Node.js 22 LTS

**Primary Dependencies**: Fastify (HTTP API), Zod (validation), Prisma (ORM/data access), pino (structured logging)

**Storage**: PostgreSQL for customer records and lifecycle audit events

**Testing**: Vitest for unit/integration tests, Supertest (or Fastify inject) for API contract/integration coverage

**Target Platform**: Linux containerized backend API

**Project Type**: Backend web-service

**Performance Goals**: p95 create/update latency < 400ms; p95 get-details latency < 200ms under standard load

**Constraints**: Hybrid RBAC mandatory, sensitive field masking, deletion blocked by dependency/retention policy, support-agent role out of scope

**Scale/Scope**: Initial release for up to 100k customer profiles with moderate CRUD throughput

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Constitution file currently contains placeholders only and no enforceable project principles.
- Gate result (pre-research): PASS.

## Project Structure

### Documentation (this feature)

```text
specs/003-customer-management/
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
│   │   └── customers/
│   ├── models/
│   ├── services/
│   └── lib/
└── tests/
    ├── contract/
    ├── integration/
    └── unit/
```

**Structure Decision**: Use backend customer module under `backend/src/api/customers` with service and model layers for business rules and authorization checks, plus contract/integration/unit tests under `backend/tests`.

## Post-Design Constitution Check

- Phase 1 artifacts align with the current placeholder constitution and contain no explicit violations.
- Gate result (post-design): PASS.

## Complexity Tracking

No constitution violations requiring justification.
