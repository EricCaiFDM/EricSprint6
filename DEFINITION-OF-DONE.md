# Definition of Done (DoD)

A story is DONE only when all criteria below are satisfied.

## 1. Specification Complete
- Feature spec written (BA)
- Business rules defined
- Acceptance criteria written in Given/When/Then format
- Negative scenarios defined (QA)
- Error codes defined
- OpenAPI contract complete
- Allowed/forbidden libraries documented
- Guardrail rules documented
- Spec reviewed and approved before development

## 2. Implementation Complete
- Spring Boot: required libraries used and forbidden patterns avoided
- React: spec-first client usage and proper error handling
- Frontend implementation exists for every approved feature spec, not only backend endpoints
- Frontend routes/pages are wired into the application shell for each delivered spec
- Implementation matches OpenAPI exactly
- No extra fields or deviations from spec

## 3. Testing Complete
- 70%+ code coverage
- Contract tests passing
- Negative tests implemented
- Integration tests executed against mock server

## 4. Guardrails Enforced
- Prompt references spec
- No insecure AI-suggested dependencies
- Static analysis passed
- Secret scanning passed
- Dependency audit passed

## 5. Git Workflow Complete
- Feature branches only -> PR -> develop
- 2 peer approvals required
- Passing CI
- Meaningful commit messages
- No direct commits to protected branches

## 6. Documentation Complete
- OpenAPI updated
- Swagger UI renders correctly
- README updated
- Request/response examples included
- Frontend behavior and page-to-endpoint mapping documented for each feature

## 8. UX Coherence
- Shared design tokens, typography, spacing, and component patterns are reused across feature pages
- New feature pages match existing navigation and interaction patterns unless an approved design change is documented

## 7. Demo Ready
- API running
- Positive and negative paths demonstrated
- Contract tests shown
- Documentation presented
- Guardrails explained
