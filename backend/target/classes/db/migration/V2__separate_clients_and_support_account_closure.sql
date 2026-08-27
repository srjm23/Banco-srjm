CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE accounts ADD COLUMN client_id BIGINT;
ALTER TABLE accounts ADD COLUMN closed_at TIMESTAMPTZ;

DO $$
DECLARE
    account_row RECORD;
    new_client_id BIGINT;
BEGIN
    FOR account_row IN SELECT id, holder_name, created_at FROM accounts ORDER BY id LOOP
        INSERT INTO clients (full_name, created_at)
        VALUES (account_row.holder_name, account_row.created_at)
        RETURNING id INTO new_client_id;

        UPDATE accounts
        SET client_id = new_client_id
        WHERE id = account_row.id;
    END LOOP;
END $$;

ALTER TABLE accounts ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_client
    FOREIGN KEY (client_id) REFERENCES clients(id);
ALTER TABLE accounts DROP COLUMN holder_name;

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_status;
ALTER TABLE accounts
    ADD CONSTRAINT ck_accounts_status
    CHECK (status IN ('ATIVA', 'BLOQUEADA', 'ENCERRADA'));
ALTER TABLE accounts
    ADD CONSTRAINT ck_accounts_close_state
    CHECK (
        (status = 'ENCERRADA' AND closed_at IS NOT NULL)
        OR (status <> 'ENCERRADA' AND closed_at IS NULL)
    );

CREATE INDEX idx_accounts_client_id ON accounts (client_id);
