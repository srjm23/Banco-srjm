CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(7) NOT NULL UNIQUE,
    check_digit CHAR(1) NOT NULL,
    holder_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ATIVA',
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_accounts_number_format CHECK (account_number ~ '^[0-9]{3,7}$'),
    CONSTRAINT ck_accounts_check_digit_format CHECK (check_digit ~ '^[0-9]$'),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ATIVA', 'BLOQUEADA')),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0)
);

CREATE TABLE bank_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    direction CHAR(1) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    balance_after NUMERIC(15, 2) NOT NULL,
    counterparty_account VARCHAR(8),
    transfer_id UUID,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_transactions_direction CHECK (direction IN ('C', 'D')),
    CONSTRAINT ck_transactions_type CHECK (transaction_type IN ('DEPOSITO', 'SAQUE', 'PIX')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_balance CHECK (balance_after >= 0)
);

CREATE INDEX idx_transactions_account_created
    ON bank_transactions (account_id, created_at DESC, id DESC);

CREATE INDEX idx_transactions_transfer_id
    ON bank_transactions (transfer_id);

