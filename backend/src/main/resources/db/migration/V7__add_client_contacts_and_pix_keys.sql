ALTER TABLE clients ADD COLUMN email VARCHAR(254);
ALTER TABLE clients ADD COLUMN phone VARCHAR(11);

ALTER TABLE clients
    ADD CONSTRAINT uq_clients_email UNIQUE (email),
    ADD CONSTRAINT uq_clients_phone UNIQUE (phone),
    ADD CONSTRAINT ck_clients_phone CHECK (phone IS NULL OR phone ~ '^[0-9]{11}$');

CREATE TABLE pix_keys (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    key_type VARCHAR(10) NOT NULL,
    key_value VARCHAR(254) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pix_keys_account_type UNIQUE (account_id, key_type),
    CONSTRAINT ck_pix_keys_type CHECK (key_type IN ('EMAIL', 'PHONE', 'RANDOM'))
);

CREATE INDEX idx_pix_keys_account_id ON pix_keys (account_id);
