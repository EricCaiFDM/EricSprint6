# Auth API (src/app)

This package contains the runtime auth HTTP endpoints used by the frontend.

## Endpoints

- `POST /auth/register` creates a user in `auth_users`
- `POST /auth/login` validates credentials against `auth_users`

## Persistence

- Backed by MySQL configured in `src/app/resources/application.yml`
- Schema is initialized from `src/app/resources/schema.sql`
