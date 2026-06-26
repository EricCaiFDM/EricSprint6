# Implementation Plan: Account Management

**Branch**: `feature/account` | **Date**: 2026-06-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-account-management/spec.md`

## Summary

Deliver an account lifecycle management service for checking and savings accounts that supports create, retrieve, list, update, and delete operations with hybrid RBAC (Customer owned scope, Admin global scope), eligibility validation, policy-based deletion checks, and auditable lifecycle events.

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
| Identity Mapping | JWT `sub` is user identity; `customerId` is profile UUID; ownership checks resolve through `customers.owner_user_id` |
| Project Type | Web service (backend) + Web application (frontend) |

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
├── src/app/java/com/example/banking/
│   ├── api/account/
│   ├── models/
│   ├── services/
│   └── lib/
└── src/test/java/com/example/banking/
    ├── api/account/
    ├── api/customer/
    └── api/auth/

frontend/
└── src/
    ├── pages/
    ├── services/
    └── components/
```

**Structure Decision**: Use the existing account module under `backend/src/app/java/com/example/banking/api/account` with supporting domain services in `backend/src/app/java/com/example/banking/services` and role-aware UI/service integration in `frontend/src`.

## Post-Design Constitution Check

- Phase 1 artifacts align with the current placeholder constitution and contain no explicit violations.
- Gate result (post-design): PASS.

## Complexity Tracking

No constitution violations requiring justification.
