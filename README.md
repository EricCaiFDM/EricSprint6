# EricSprint6 Banking Platform

Full-stack banking project with a Spring Boot backend and a React frontend.

## Overview

EricSprint6 implements core banking workflows across multiple feature slices, including:

- Authentication and user access flows
- Customer lifecycle management
- Account management
- Transaction operations (deposit, withdrawal, transfer)
- Standing orders
- Notifications
- Monthly statements
- Spending insights

Feature specifications, plans, and OpenAPI contracts are maintained under `specs/`.

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.3.x
- Spring Web, Validation, Security, Data JPA
- Springdoc OpenAPI (Swagger UI)
- MySQL (runtime default)
- H2 (local/test usage)

### Frontend

- React 18
- TypeScript
- Vite
- Axios
- React Query
- Jest + React Testing Library

## Repository Structure

```text
.
|-- backend/
|   |-- pom.xml
|   |-- src/
|   |   |-- app/
|   |   `-- test/
|   `-- bin/
|-- frontend/
|   |-- package.json
|   `-- src/
|-- specs/
|   |-- 002-authentication/
|   |-- 003-customer-management/
|   |-- 004-account-management/
|   |-- 005-transaction-operations/
|   |-- 006-standing-orders/
|   |-- 007-notifications/
|   |-- 008-monthly-statements/
|   `-- 009-spending-insights/
|-- DEFINITION-OF-DONE.md
|-- TOOL-STACK.md
`-- README.md
```

## Prerequisites

Install the following before running locally:

- Java 21+
- Node.js 20+ and npm
- MySQL 8+ (or configure local profile for H2 where applicable)

## Local Setup

### 1. Clone and enter the project

```bash
git clone <your-repo-url>
cd EricSprint6
```

### 2. Configure backend environment

Backend configuration is in `backend/src/app/resources/application.yml`.

Important environment variables:

- `DB_URL` (default: `jdbc:mysql://localhost:3306/bankingdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USERNAME` (default: `banking_app`)
- `DB_PASSWORD` (default: `banking_app`)
- `JWT_SECRET` (default demo value exists; set your own in non-local environments)

### 3. Install frontend dependencies

```bash
cd frontend
npm install
cd ..
```

## Running the Application

### Run backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

Backend base URL: `http://localhost:8080`

### Run frontend (Vite dev server)

```bash
cd frontend
npm run dev
```

Frontend URL is shown by Vite at startup (commonly `http://localhost:5173`).

## API Documentation

With backend running:

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Testing

### Frontend

```bash
cd frontend
npm test
npm run test:coverage
npm run test:prism
```

`npm run test:prism` executes integration-style contract checks against Prism mock servers for all feature OpenAPI contracts under `specs/*/contracts/openapi.yaml`.

### Backend

```bash
cd backend
mvn test
```

## Build

### Frontend production build

```bash
cd frontend
npm run build
```

### Backend package

```bash
cd backend
mvn clean package
```

## Specifications and Contracts

Each feature folder in `specs/` contains implementation artifacts:

- `spec.md` for feature requirements
- `plan.md` for implementation planning
- `tasks.md` for execution checklist
- `contracts/openapi.yaml` for API contracts
- `quickstart.md` for validation scenarios

See `specs/README.md` for standards and usage guidance.

## Definition of Done

Project completion criteria are documented in:

- `DEFINITION-OF-DONE.md`

## Notes

- Backend and frontend are expected to evolve spec-first, with OpenAPI contracts as the API source of truth.
- Do not use production secrets in local files; prefer environment variables.
