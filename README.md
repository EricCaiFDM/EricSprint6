# EricSprint6 Banking Platform

Full-stack banking project with a Spring Boot backend and a React frontend.

## What is included

Core feature areas implemented in this repo:

- Authentication and user access flows
- Customer lifecycle management
- Account management
- Transaction operations (deposit, withdrawal, transfer)
- Standing orders
- Notifications
- Monthly statements
- Spending insights

Feature specifications, plans, task checklists, and OpenAPI contracts are under `specs/`.

## Quick Start (Run Full Stack)

1. Clone the repository:

```bash
git clone <your-repo-url>
cd EricSprint6
```

2. Start backend (Terminal 1):

```bash
cd backend
mvn spring-boot:run
```

3. Start frontend (Terminal 2):

```bash
cd frontend
npm install
npm run dev
```

4. Open the app at `http://localhost:5173`.

The frontend proxies `/api` requests to backend `http://localhost:8080`.

---

## Backend Setup (Spring Boot)

### Backend prerequisites

- Java 21+
- Maven 3.9+
- Optional: MySQL 8+ (for MySQL runtime profile)

### Backend dependency installation

All backend libraries are managed in `backend/pom.xml` and downloaded automatically by Maven.

```bash
cd backend
mvn dependency:resolve
```

### Run backend with H2 (default local profile)

Default profile is `local`, which uses in-memory H2.

```bash
cd backend
mvn spring-boot:run
```

Backend URLs:

- API base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 console (local profile): `http://localhost:8080/h2-console`

### Run backend with MySQL (optional)

1. Create a MySQL database and user (example):

```sql
CREATE DATABASE bankingdb;
CREATE USER 'banking_app'@'%' IDENTIFIED BY 'banking_app';
GRANT ALL PRIVILEGES ON bankingdb.* TO 'banking_app'@'%';
FLUSH PRIVILEGES;
```

2. Set environment variables and run with a non-local active profile:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL="jdbc:mysql://localhost:3306/bankingdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="banking_app"
export DB_PASSWORD="banking_app"
export JWT_SECRET="change-this-to-a-long-random-value"

cd backend
mvn spring-boot:run
```

### Backend key libraries

Defined in `backend/pom.xml`:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`
- `springdoc-openapi-starter-webmvc-ui`
- `mysql-connector-j` (runtime)
- `h2` (runtime)
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- Test: `spring-boot-starter-test`, `mockito-junit-jupiter`, `spring-security-test`

### Backend test and build

```bash
cd backend
mvn test
mvn clean package
```

---

## Frontend Setup (React + Vite)

### Frontend prerequisites

- Node.js 20+
- npm 10+

### Frontend dependency installation

All frontend libraries are managed in `frontend/package.json`.

```bash
cd frontend
npm install
```

### Run frontend

```bash
cd frontend
npm run dev
```

Default frontend URL: `http://localhost:5173`

### Frontend key libraries

Runtime dependencies:

- `react`
- `react-dom`
- `react-router-dom`
- `@tanstack/react-query`
- `axios`

Development and test dependencies:

- `vite`, `@vitejs/plugin-react`
- `typescript`
- `jest`, `ts-jest`, `jest-environment-jsdom`
- `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`
- `@stoplight/prism-cli`

### Frontend test and build

```bash
cd frontend
npm test
npm run test:coverage
npm run test:prism
npm run build
```

`npm run test:prism` executes contract tests using Prism mock servers for specs in `specs/*/contracts/openapi.yaml`.

---

## Repository Structure

```text
.
|-- backend/
|   |-- pom.xml
|   `-- src/
|-- frontend/
|   |-- package.json
|   `-- src/
|-- specs/
|-- DEFINITION-OF-DONE.md
|-- GUARDRAILS.md
|-- TOOL-STACK.md
`-- README.md
```

## Specs, Guardrails, and Quality

- Specs and feature artifacts: `specs/README.md`
- Definition of Done: `DEFINITION-OF-DONE.md`
- Guardrail governance and enforcement map: `GUARDRAILS.md`

## Notes

- Use environment variables for secrets (especially `JWT_SECRET`).
- Prefer spec-first changes and keep OpenAPI contracts aligned with implementation.
