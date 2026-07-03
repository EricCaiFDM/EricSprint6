# Banking API Feature Specification: Customer Management

## Business Context
Customer Management provides the canonical API surface for customer profile lifecycle operations in the retail banking platform. It supports profile creation, profile maintenance, profile retrieval, and policy-aware deletion with hybrid RBAC controls and immutable lifecycle auditing.

## Purpose and Scope
This specification is the source of truth for contract and implementation alignment for customer lifecycle operations only.

This document covers the following operations:

1. Create Customer
2. Update Customer
3. Retrieve Customer Details
4. Delete Customer

### Development Perspective
- Target implementation stack:
	- Backend: Java 21 with Spring Boot 3.x (Web, Data JPA, Validation, Security)
	- Frontend: React 18 with JavaScript ES2022, React Query v5, Axios, and Vite
	- Runtime persistence: MySQL
	- Local development and test persistence: H2
	- Backend testing: JUnit 5 and Mockito
	- Frontend testing: Jest and React Testing Library
- Statements containing MUST, MUST NOT, REQUIRED, and NOT ALLOWED are normative.
- Operation-specific rules override shared rules where they are more specific.

### QA Perspective
- Use this document as a traceable test baseline, not only as a business summary.
- For every operation, verify request and response contract, authorization outcome, state mutation or non-mutation, and required audit side effects.

## Clarifications

### Session 2026-06-25

- Q: What authorization model governs customer operations? -> A: Hybrid RBAC with CUSTOMER in owned scope and ADMIN in global scope.
- Q: Which uniqueness constraints apply to customer creation? -> A: `externalCustomerKey` and normalized `primaryEmail` are unique.
- Q: What blocks customer deletion? -> A: Dependency blockers and retention or legal-hold blockers.
- Q: Are customer lifecycle actions auditable? -> A: Yes, all create, update, get, and delete attempts are lifecycle-audited.
- Q: How is sensitive data handled on reads? -> A: Response fields are masked according to requester role and scope.

## Shared Definitions

### Domain Objects

#### Customer
- `customerId` (UUID string)
- `externalCustomerKey` (string, unique)
- `legalName` (string)
- `primaryEmail` (string, normalized)
- `phoneNumber` (string, nullable)
- `status` (`ACTIVE`, `SUSPENDED`, `CLOSED`)
- `createdAtUtc` (ISO-8601 UTC string)
- `updatedAtUtc` (ISO-8601 UTC string)
- `createdByUserId` (UUID string)
- `ownerUserId` (UUID string)

#### CustomerLifecycleEvent
- Immutable audit record for create, update, get, and delete attempts.
- `eventId` (UUID string)
- `customerId` (UUID string, nullable for failed create attempts)
- `eventType` (`CREATE`, `UPDATE`, `GET`, `DELETE_ATTEMPT`, `DELETE_BLOCKED`, `DELETE_SUCCESS`)
- `actorUserId` (UUID string)
- `actorRole` (`CUSTOMER`, `ADMIN`)
- `occurredAtUtc` (ISO-8601 UTC string)
- `outcome` (`SUCCESS`, `FAILURE`)
- `reasonCode` (string, nullable)
- `metadata` (JSON, redacted)

#### DeletionPolicyCheck
- Evaluation artifact used before delete execution.
- `checkId` (UUID string)
- `customerId` (UUID string)
- `evaluatedAtUtc` (ISO-8601 UTC string)
- `hasDependencyBlocker` (boolean)
- `hasRetentionBlocker` (boolean)
- `blockerReasons` (array of strings)
- `decision` (`ALLOW_DELETE`, `BLOCK_DELETE`)

### Enums

#### CustomerStatus
- `ACTIVE`
- `SUSPENDED`
- `CLOSED`

#### CustomerLifecycleEventType
- `CREATE`
- `UPDATE`
- `GET`
- `DELETE_ATTEMPT`
- `DELETE_BLOCKED`
- `DELETE_SUCCESS`

#### LifecycleOutcome
- `SUCCESS`
- `FAILURE`

#### DeletionDecision
- `ALLOW_DELETE`
- `BLOCK_DELETE`

### ErrorResponse Schema
```json
{
	"code": "string",
	"message": "string",
	"field": "string"
}
```
- `ErrorResponse` is returned for operation errors in this feature.
- In this feature version, that includes `400`, `403`, `404`, and `409`.
- `field` is optional and used when the failure maps to a specific request field.

### Actors and Roles
- `CUSTOMER`
	- May create and manage owned customer profiles only.
	- Must not operate outside owned scope.
- `ADMIN`
	- May operate across all customer resources.
	- Bypasses ownership checks but remains subject to deletion policy blockers.

### Glossary
- **Owned scope**: Resources where `ownerUserId` matches the authenticated caller's local user identity.
- **Normal operational access**: API-visible state that excludes resources intentionally removed from standard read paths by delete semantics.

### Development Perspective
- Shared definitions are canonical and must map consistently across DTOs, persistence models, and frontend API types.
- Status transitions and audit semantics in this section are normative for all operations.

### QA Perspective
- Validate field presence, enum values, and role-driven behavior against these shared definitions.
- Use these definitions to derive boundary and negative tests, not only happy-path tests.

## Role-Based Access Control (RBAC)

### Overview
- Users are assigned one or more roles.
- Endpoints define required permissions.
- Access is granted when `required_permissions ⊆ user.permissions`.
- For `CUSTOMER`, ownership checks are REQUIRED in addition to permission checks.

### Permissions
- `CUSTOMER:CREATE`
- `CUSTOMER:READ`
- `CUSTOMER:UPDATE`
- `CUSTOMER:DELETE`

### Default Roles

#### CUSTOMER
- Permissions:
	- `CUSTOMER:CREATE` (owned scope)
	- `CUSTOMER:READ` (owned scope)
	- `CUSTOMER:UPDATE` (owned scope)
	- `CUSTOMER:DELETE` (owned scope)

#### ADMIN
- Permissions:
	- All customer-management permissions
- Notes:
	- No ownership restriction applies.

### Authorization Rules

#### Permission-Based Access
- Each endpoint MUST define required permissions.
- If permissions are missing, API returns `403`.

#### Ownership Constraint
- For `CUSTOMER`, required permissions do not bypass ownership validation.
- Ownership failure returns `403`.

#### Admin Override
- `ADMIN` callers bypass ownership constraints.

### Endpoint Permission Matrix
- `POST /customers` -> requires `CUSTOMER:CREATE`
- `GET /customers/{customerId}` -> requires `CUSTOMER:READ`
- `PATCH /customers/{customerId}` -> requires `CUSTOMER:UPDATE`
- `DELETE /customers/{customerId}` -> requires `CUSTOMER:DELETE`

### Development Perspective
- Permission and ownership checks are separate obligations and should be enforced in backend authorization components, not frontend-only logic.

### QA Perspective
- For each endpoint, verify: authorized owned-scope access, out-of-scope access denial, and admin override behavior.

## Assumptions
- A valid JWT bearer token is present on every API request.
- Caller identity resolves to a local user identity used for ownership and audit linkage.
- `customerId` is represented as a UUID string.
- Customer creation does not imply account creation.

## Out of Scope
- Account lifecycle operations
- Monetary transactions
- Role-grant and role-revoke APIs
- Bulk customer search and pagination
- Customer-to-customer delegation model

## Non-Functional Requirements
- API responses MUST use `application/json`.
- Lifecycle events MUST be persisted for both success and failure outcomes.
- Customer reads MUST apply role and scope-based masking rules.
- 95% of valid create or update requests complete in under 4 seconds.
- 98% of valid get requests complete in under 2 seconds.
- 100% of out-of-scope requests are denied.

### Development Perspective
- Non-functional requirements are contract obligations and must be visible through endpoint behavior.

### QA Perspective
- Treat performance, authorization, and audit persistence as recurring regression gates.

## Data Storage Principles
- Customer records MUST be stored in durable server-side storage.
- Lifecycle audit events MUST be append-only and durable.
- Deletion-policy check artifacts MUST be persisted for traceability.
- Runtime operational data targets MySQL; local and test execution may use H2.
- Deletion removes records from normal operational access while retained records remain available for compliance and support retrieval.

## Data Retention and Audit Policy
- Customer and lifecycle audit records MUST be retained for at least 7 years.
- Delete operations MUST preserve retained records needed for compliance and audit.
- Lifecycle audit records MUST capture actor, role, action, target resource, timestamp, and outcome.
- Lifecycle audit records MUST NOT be modified during the retention period.

### Development Perspective
- Delete semantics must be implemented as operational removal plus retained-record preservation.

### QA Perspective
- Validate that successful and failed operations both leave auditable evidence.

## API Operations

---

## 1. Create Customer

### Description
Creates a customer profile with uniqueness validation and lifecycle audit capture.

### HTTP Contract
- Method: `POST`
- Path: `/customers`
- Request Body:
```json
{
	"externalCustomerKey": "string",
	"legalName": "string",
	"primaryEmail": "string",
	"phoneNumber": "string"
}
```
- Expected Response Codes:
	- `201` Customer created successfully
	- `400` Invalid or malformed request
	- `403` Caller not authorized for requested scope
	- `409` Duplicate customer conflict

### Business Rules
- `externalCustomerKey` and normalized `primaryEmail` must be unique.
- New customers are created in `ACTIVE` status.
- Create operation MUST write a lifecycle event.

### Security Constraints
- Caller must be authenticated.
- Required permission: `CUSTOMER:CREATE`.
- `CUSTOMER` may create only within owned scope.
- `ADMIN` may create for any supported scope.

### Validation Rules
- `externalCustomerKey` is required and must be non-empty.
- `legalName` is required and must be non-empty.
- `primaryEmail` is required and must be a valid email format.
- `phoneNumber`, when present, must be a valid phone representation.

### Error Mapping
- Missing or invalid input field -> `400`, `field` set when identifiable
- Duplicate `externalCustomerKey` or `primaryEmail` -> `409`
- Permission or ownership violation -> `403`

### Edge Cases
- Re-submitting equivalent create payload for an existing unique identity returns `409`.

### Development Perspective
- Create must preserve uniqueness, authorization, and lifecycle auditing as one atomic observable outcome.

### QA Perspective
- Cover valid create, duplicate key conflict, duplicate email conflict, malformed input, and ownership denial.

### Acceptance Criteria

#### Positive Scenario
- Given valid customer input in permitted scope
- When a create request is submitted
- Then API returns `201` and the new customer representation

#### Negative Scenario 1
- Given an existing `externalCustomerKey`
- When a create request reuses that key
- Then API returns `409` with `ErrorResponse`

#### Negative Scenario 2
- Given missing required fields
- When create is submitted
- Then API returns `400` with `ErrorResponse`

#### Negative Scenario 3
- Given a `CUSTOMER` caller outside owned scope
- When create is submitted
- Then API returns `403` with `ErrorResponse`

---

## 2. Update Customer

### Description
Updates mutable customer profile attributes within authorization and status-transition rules.

### HTTP Contract
- Method: `PATCH`
- Path: `/customers/{customerId}`
- Request Body:
```json
{
	"legalName": "string",
	"primaryEmail": "string",
	"phoneNumber": "string",
	"status": "ACTIVE | SUSPENDED | CLOSED"
}
```
- Expected Response Codes:
	- `200` Customer updated successfully
	- `400` Invalid path or malformed request
	- `403` Caller not authorized for target customer
	- `404` Customer not found

### Business Rules
- Customer must exist.
- Mutable fields are `legalName`, `primaryEmail`, `phoneNumber`, and `status`.
- Status transitions allowed:
	- `ACTIVE -> SUSPENDED`
	- `SUSPENDED -> ACTIVE`
	- `ACTIVE|SUSPENDED -> CLOSED`
- Successful update MUST refresh `updatedAtUtc`.
- Update operation MUST write a lifecycle event.

### Security Constraints
- Caller must be authenticated.
- Required permission: `CUSTOMER:UPDATE`.
- `CUSTOMER` callers must own the target customer.
- `ADMIN` callers may update any customer.

### Validation Rules
- `customerId` must be a valid UUID.
- Request body must include at least one mutable field.
- `primaryEmail`, when provided, must be valid email format.

### Error Mapping
- Invalid `customerId` format -> `400`, `field="customerId"`
- Empty or malformed update body -> `400`
- Target customer not found -> `404`
- Permission or ownership violation -> `403`

### Edge Cases
- A no-op update with unchanged values still returns `200` with current representation.
- Transition from `CLOSED` to another status is rejected as invalid update input.

### Development Perspective
- Update must keep status-transition rules and ownership rules consistent across all callers.

### QA Perspective
- Cover valid updates, invalid transition requests, empty patch body, missing customer, and ownership denial.

### Acceptance Criteria

#### Positive Scenario
- Given existing customer in owned scope
- When patch request with valid mutable fields is submitted
- Then API returns `200` with updated representation

#### Negative Scenario 1
- Given no customer for supplied `customerId`
- When patch request is submitted
- Then API returns `404` with `ErrorResponse`

#### Negative Scenario 2
- Given invalid `customerId`
- When patch request is submitted
- Then API returns `400` with `ErrorResponse`

#### Negative Scenario 3
- Given `CUSTOMER` caller outside ownership scope
- When patch request is submitted
- Then API returns `403` with `ErrorResponse`

---

## 3. Retrieve Customer Details

### Description
Returns authorized customer profile details with role and scope-based field masking.

### HTTP Contract
- Method: `GET`
- Path: `/customers/{customerId}`
- Request Body: None
- Expected Response Codes:
	- `400` Invalid `customerId` format
	- `200` Customer retrieved successfully
	- `403` Caller not authorized for target customer
	- `404` Customer not found

### Business Rules
- Customer must exist and be available for normal operational access.
- Response applies masking according to requester role and scope policy.
- Read operation MUST write lifecycle audit event.

### Security Constraints
- Caller must be authenticated.
- Required permission: `CUSTOMER:READ`.
- `CUSTOMER` callers must own the target customer.
- `ADMIN` callers may retrieve any customer.

### Validation Rules
- `customerId` must be a valid UUID.

### Error Mapping
- Invalid `customerId` format -> `400`, `field="customerId"`
- Invalid or unauthorized scope access -> `403`
- Customer not found or already removed from normal operational access -> `404`

### Edge Cases
- Repeated reads return consistent masked field behavior for the same caller role and scope.

### Development Perspective
- Retrieval is read-only but still has mandatory authorization and audit side effects.

### QA Perspective
- Verify customer-role ownership success and denial, admin override, not-found behavior, and masking differences by role.

### Acceptance Criteria

#### Positive Scenario
- Given existing customer in authorized scope
- When details request is submitted
- Then API returns `200` with authorized customer profile data

#### Negative Scenario 1
- Given missing customer
- When details request is submitted
- Then API returns `404` with `ErrorResponse`

#### Negative Scenario 2
- Given invalid `customerId` format
- When details request is submitted
- Then API returns `400` with `ErrorResponse`

#### Negative Scenario 3
- Given `CUSTOMER` caller outside ownership scope
- When details request is submitted
- Then API returns `403` with `ErrorResponse`

---

## 4. Delete Customer

### Description
Deletes a customer from normal operational access when policy checks allow deletion.

### HTTP Contract
- Method: `DELETE`
- Path: `/customers/{customerId}`
- Request Body: None
- Expected Response Codes:
	- `400` Invalid `customerId` format
	- `200` Customer deleted successfully
	- `403` Caller not authorized for target customer
	- `404` Customer not found
	- `409` Deletion blocked by policy

### Business Rules
- Customer must exist.
- Deletion requires policy decision `ALLOW_DELETE`.
- Deletion blockers include dependency and retention blockers.
- Successful delete removes customer from normal operational access.
- Delete attempts MUST write lifecycle events including blocked and successful outcomes.

### Security Constraints
- Caller must be authenticated.
- Required permission: `CUSTOMER:DELETE`.
- `CUSTOMER` callers may delete only owned customers when policy allows.
- `ADMIN` callers may delete any customer when policy allows.

### Validation Rules
- `customerId` must be a valid UUID.

### Error Mapping
- Invalid `customerId` format -> `400`, `field="customerId"`
- Permission or ownership violation -> `403`
- Missing customer -> `404`
- Policy blocker present -> `409`

### Edge Cases
- Repeated delete on the same customer after successful deletion returns `404`.
- Concurrent delete requests: first successful request returns `200`, subsequent requests return `404`.

### Development Perspective
- Delete must preserve retention and lifecycle-audit obligations while removing normal operational visibility.

### QA Perspective
- Cover successful delete, dependency-blocked delete, retention-blocked delete, repeated delete, and scope denial.

### Acceptance Criteria

#### Positive Scenario
- Given existing customer with no blockers in authorized scope
- When delete request is submitted
- Then API returns `200` and customer is removed from normal operational access

#### Negative Scenario 1
- Given no customer for supplied `customerId`
- When delete request is submitted
- Then API returns `404` with `ErrorResponse`

#### Negative Scenario 2
- Given dependency or retention blocker exists
- When delete request is submitted
- Then API returns `409` with `ErrorResponse`

#### Negative Scenario 3
- Given invalid `customerId` format
- When delete request is submitted
- Then API returns `400` with `ErrorResponse`

#### Negative Scenario 4
- Given `CUSTOMER` caller outside ownership scope
- When delete request is submitted
- Then API returns `403` with `ErrorResponse`

---

## Cross-Cutting Validation and Error Semantics

- All customer-management endpoints require authenticated callers.
- `CUSTOMER` callers are restricted to owned scope.
- `ADMIN` callers bypass ownership checks.
- Error responses use shared `ErrorResponse` shape.
- `400` is used for malformed payloads and invalid field formats.
- `403` is used for missing permissions or ownership violations.
- `404` is used for missing resources or resources unavailable in normal operational access.
- `409` is used for business-state conflicts such as uniqueness conflicts and deletion blockers.
- Successful create returns `201`; successful read, update, and delete return `200`.

### Development Perspective
- Keep status-code semantics consistent across all customer operations to avoid endpoint-specific divergence.

### QA Perspective
- Build endpoint-consistency tests for status usage, field-level errors, ownership behavior, and lifecycle auditing side effects.