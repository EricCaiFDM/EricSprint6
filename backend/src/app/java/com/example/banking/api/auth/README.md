# Auth API (src/app)

This package contains the runtime auth HTTP endpoints used by the frontend.

## Endpoints

- `POST /auth/register` creates a user in `auth_users`
- `POST /auth/login` validates credentials against `auth_users` and issues JWT tokens
- `POST /auth/password-reset/request` returns a generic reset acknowledgment
- `POST /auth/token/refresh` validates refresh token and rotates session tokens

## Persistence

- Backed by MySQL configured in `src/app/resources/application.yml`
- Schema is initialized from `src/app/resources/schema.sql`
- Auth lifecycle events are persisted in `auth_events`
