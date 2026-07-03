# Phase 0 Research: Authentication

## Decision 1: Stateless Access Token + Stateful Refresh Token
- Decision: Use short-lived stateless JWT access tokens and stateful refresh tokens persisted in MySQL (runtime) with H2 for local development and test execution.
- Rationale: Keeps authorization checks fast for access tokens while allowing revocation, rotation, and replay controls through persisted refresh token records.
- Alternatives considered:
  - Fully stateless refresh tokens: rejected due to weak revocation and replay control.
  - Fully stateful sessions only: rejected due to unnecessary lookup overhead for every authenticated request.

## Decision 2: Password Hashing Policy
- Decision: Use Argon2id (preferred) or bcrypt with strong cost settings and per-password salt.
- Rationale: Modern resistant hashing strategy with tunable work factors protects stored credentials against offline attacks.
- Alternatives considered:
  - SHA-based fast hashing: rejected as insecure for password storage.
  - External identity provider only: rejected because scope explicitly includes native registration/login.

## Decision 3: Generic Password Reset Request Responses
- Decision: Always return a generic reset acknowledgment regardless of account existence.
- Rationale: Prevents account enumeration while keeping UX clear.
- Alternatives considered:
  - Explicit "account not found" response: rejected for security exposure.

## Decision 4: Token Refresh Rotation and Replay Detection
- Decision: Rotate refresh token on each successful refresh and invalidate prior token family nodes on detected replay.
- Rationale: Limits attacker persistence window and supports strong session security controls.
- Alternatives considered:
  - Reusable long-lived refresh token: rejected due to elevated compromise risk.

## Decision 5: Transport and Secret Handling
- Decision: Enforce HTTPS/TLS for all auth endpoints; redact secrets from logs and error payloads.
- Rationale: Satisfies core security constraints and reduces sensitive-data leakage risk.
- Alternatives considered:
  - Mixed HTTP/HTTPS environments: rejected due to credential exposure risk.

## Decision 6: Service Topology for This Feature
- Decision: Implement as a backend API service with clear auth module boundaries (API, service, model, token utility, audit logging).
- Rationale: Aligns with split feature scope and enables independent testing and delivery.
- Alternatives considered:
  - Coupling auth logic directly to UI: rejected due to reuse/testing limitations.

## Resolved Clarifications
- Auth method: Email + password (native auth) with registration and login.
- Session model: Access + refresh token pattern with rotation.
- Reset behavior: Request initiation only in this release; completion flow out of scope.
- Scope exclusions: Social login and MFA remain out of scope for this feature release.
