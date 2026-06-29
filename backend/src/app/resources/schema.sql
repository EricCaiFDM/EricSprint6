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
    account_number VARCHAR(24) NOT NULL,
    account_type VARCHAR(16) NOT NULL,
    interest_rate DECIMAL(8, 4) NULL,
    checking_number INT NULL,
    status VARCHAR(16) NOT NULL,
    nickname VARCHAR(64),
    balance DECIMAL(18, 2) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    opened_at_utc TIMESTAMP NOT NULL,
    closed_at_utc TIMESTAMP NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_account_number
    ON accounts(account_number);

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_customer_checking_number
    ON accounts(customer_id, checking_number);

CREATE INDEX IF NOT EXISTS ix_accounts_customer_active
    ON accounts(customer_id, deleted_at);

CREATE TABLE IF NOT EXISTS account_lifecycle_events (
    event_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36),
    event_type VARCHAR(32) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS account_eligibility_checks (
    check_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(16) NOT NULL,
    evaluated_at_utc TIMESTAMP NOT NULL,
    is_eligible BOOLEAN NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS account_deletion_policy_checks (
    check_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    evaluated_at_utc TIMESTAMP NOT NULL,
    has_dependency_blocker BOOLEAN NOT NULL,
    has_retention_blocker BOOLEAN NOT NULL,
    blocker_reasons TEXT,
    decision VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    posted_at_utc TIMESTAMP NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(36) NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    balance_before DECIMAL(18, 2) NOT NULL,
    balance_after DECIMAL(18, 2) NOT NULL,
    metadata TEXT,
    created_at_utc TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_transactions_account_posted
    ON transactions(account_id, posted_at_utc, transaction_id);

CREATE INDEX IF NOT EXISTS ix_transactions_correlation
    ON transactions(correlation_id);

CREATE TABLE IF NOT EXISTS transfer_links (
    transfer_id VARCHAR(36) PRIMARY KEY,
    debit_transaction_id VARCHAR(36) NOT NULL,
    credit_transaction_id VARCHAR(36) NOT NULL,
    source_account_id VARCHAR(36) NOT NULL,
    destination_account_id VARCHAR(36) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    CONSTRAINT ux_transfer_links_debit UNIQUE (debit_transaction_id),
    CONSTRAINT ux_transfer_links_credit UNIQUE (credit_transaction_id)
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_transaction_id VARCHAR(36),
    response_payload TEXT,
    status VARCHAR(16) NOT NULL,
    created_at_utc TIMESTAMP NOT NULL,
    expires_at_utc TIMESTAMP NOT NULL,
    failure_reason VARCHAR(128),
    CONSTRAINT ux_idempotency_operation UNIQUE (idempotency_key, operation_type)
);

CREATE TABLE IF NOT EXISTS transaction_lifecycle_events (
    event_id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36),
    event_type VARCHAR(32) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS standing_orders (
    standing_order_id VARCHAR(36) PRIMARY KEY,
    source_account_id VARCHAR(36) NOT NULL,
    destination_account_id VARCHAR(36) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    cadence VARCHAR(16) NOT NULL,
    schedule_config TEXT,
    effective_from_utc TIMESTAMP NOT NULL,
    effective_to_utc TIMESTAMP NULL,
    next_execution_at_utc TIMESTAMP NULL,
    lifecycle_state VARCHAR(16) NOT NULL,
    retry_policy_code VARCHAR(32) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_at_utc TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_standing_orders_due
    ON standing_orders(lifecycle_state, next_execution_at_utc);

CREATE INDEX IF NOT EXISTS ix_standing_orders_source
    ON standing_orders(source_account_id);

CREATE TABLE IF NOT EXISTS standing_order_lifecycle_events (
    event_id VARCHAR(36) PRIMARY KEY,
    standing_order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor_user_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS ix_so_lifecycle_by_order
    ON standing_order_lifecycle_events(standing_order_id, occurred_at_utc);

CREATE TABLE IF NOT EXISTS standing_order_execution_events (
    execution_event_id VARCHAR(36) PRIMARY KEY,
    standing_order_id VARCHAR(36) NOT NULL,
    due_at_utc TIMESTAMP NOT NULL,
    started_at_utc TIMESTAMP NOT NULL,
    completed_at_utc TIMESTAMP NULL,
    status VARCHAR(48) NOT NULL,
    transfer_reference_id VARCHAR(36) NULL,
    attempt_number INT NOT NULL,
    next_retry_at_utc TIMESTAMP NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS ix_so_execution_by_order
    ON standing_order_execution_events(standing_order_id, started_at_utc);

CREATE TABLE IF NOT EXISTS standing_order_schedule_cursors (
    cursor_id VARCHAR(36) PRIMARY KEY,
    worker_id VARCHAR(64) NOT NULL,
    window_start_utc TIMESTAMP NOT NULL,
    window_end_utc TIMESTAMP NOT NULL,
    claimed_at_utc TIMESTAMP NOT NULL,
    completed_at_utc TIMESTAMP NULL,
    status VARCHAR(16) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_so_cursor_worker_claimed
    ON standing_order_schedule_cursors(worker_id, claimed_at_utc);

CREATE TABLE IF NOT EXISTS notification_events (
    notification_event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    recipient_scope_type VARCHAR(16) NOT NULL,
    recipient_scope_id VARCHAR(36) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_context TEXT NOT NULL,
    triggered_at_utc TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    correlation_id VARCHAR(36),
    completed_at_utc TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS ix_notification_events_status_triggered
    ON notification_events(status, triggered_at_utc);

CREATE INDEX IF NOT EXISTS ix_notification_events_scope
    ON notification_events(recipient_scope_type, recipient_scope_id);

CREATE TABLE IF NOT EXISTS notification_preference_snapshots (
    snapshot_id VARCHAR(36) PRIMARY KEY,
    notification_event_id VARCHAR(36) NOT NULL UNIQUE,
    recipient_id VARCHAR(36) NOT NULL,
    consent_status VARCHAR(16) NOT NULL,
    allowed_channels VARCHAR(160) NOT NULL,
    restricted_channels VARCHAR(160) NOT NULL,
    captured_at_utc TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_dispatch_attempts (
    attempt_id VARCHAR(36) PRIMARY KEY,
    notification_event_id VARCHAR(36) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    attempt_number INT NOT NULL,
    queued_at_utc TIMESTAMP NOT NULL,
    started_at_utc TIMESTAMP NULL,
    completed_at_utc TIMESTAMP NULL,
    status VARCHAR(48) NOT NULL,
    provider_reference_id VARCHAR(128),
    reason_code VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS ix_notification_attempts_event_attempt
    ON notification_dispatch_attempts(notification_event_id, attempt_number);

CREATE TABLE IF NOT EXISTS notification_delivery_outcomes (
    outcome_id VARCHAR(36) PRIMARY KEY,
    notification_event_id VARCHAR(36) NOT NULL UNIQUE,
    final_status VARCHAR(32) NOT NULL,
    delivered_channel VARCHAR(16),
    completed_at_utc TIMESTAMP NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS monthly_statements (
    statement_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    period_year_month VARCHAR(7) NOT NULL,
    period_start_utc TIMESTAMP NOT NULL,
    period_end_utc TIMESTAMP NOT NULL,
    opening_balance DECIMAL(18, 2) NOT NULL,
    closing_balance DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    artifact_version INT NOT NULL,
    artifact_uri VARCHAR(512) NOT NULL,
    generation_mode VARCHAR(16) NOT NULL,
    generated_at_utc TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT ux_monthly_statements_account_period_version UNIQUE (account_id, period_year_month, artifact_version)
);

CREATE INDEX IF NOT EXISTS ix_monthly_statements_account_generated
    ON monthly_statements(account_id, generated_at_utc);

CREATE INDEX IF NOT EXISTS ix_monthly_statements_period
    ON monthly_statements(period_year_month, account_id);

CREATE TABLE IF NOT EXISTS statement_activity_summaries (
    activity_summary_id VARCHAR(36) PRIMARY KEY,
    statement_id VARCHAR(36) NOT NULL UNIQUE,
    debit_total DECIMAL(18, 2) NOT NULL,
    credit_total DECIMAL(18, 2) NOT NULL,
    transaction_count INT NOT NULL,
    included_event_start_utc TIMESTAMP NOT NULL,
    included_event_end_utc TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS statement_generation_events (
    generation_event_id VARCHAR(36) PRIMARY KEY,
    statement_id VARCHAR(36),
    account_id VARCHAR(36) NOT NULL,
    period_year_month VARCHAR(7) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128),
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS ix_statement_generation_events_period
    ON statement_generation_events(account_id, period_year_month);

CREATE TABLE IF NOT EXISTS statement_retrieval_events (
    retrieval_event_id VARCHAR(36) PRIMARY KEY,
    statement_id VARCHAR(36) NOT NULL,
    requester_user_id VARCHAR(36) NOT NULL,
    requester_role VARCHAR(16) NOT NULL,
    occurred_at_utc TIMESTAMP NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    reason_code VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS ix_statement_retrieval_events_statement
    ON statement_retrieval_events(statement_id, occurred_at_utc);
