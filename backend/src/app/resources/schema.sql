CREATE TABLE IF NOT EXISTS auth_users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_events (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    identity VARCHAR(255) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    reason_code VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(36) PRIMARY KEY,
    external_customer_key VARCHAR(120) NOT NULL,
    legal_name VARCHAR(160) NOT NULL,
    primary_email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32),
    status VARCHAR(16) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_external_key
    ON customers(external_customer_key);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_primary_email
    ON customers(primary_email);

CREATE TABLE IF NOT EXISTS customer_lifecycle_events (
    event_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36),
    event_type VARCHAR(32) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS deletion_policy_checks (
    check_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    evaluated_at_utc TIMESTAMP NOT NULL,
    has_dependency_blocker BOOLEAN NOT NULL,
    has_retention_blocker BOOLEAN NOT NULL,
    blocker_reasons TEXT,
    decision VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    balance DECIMAL(18, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
