ALTER TABLE clients ADD COLUMN cpf_encrypted VARCHAR(128);
ALTER TABLE clients ADD COLUMN cpf_hash CHAR(64);
ALTER TABLE clients ADD CONSTRAINT uq_clients_cpf_hash UNIQUE (cpf_hash);

ALTER TABLE pix_keys DROP CONSTRAINT ck_pix_keys_type;
ALTER TABLE pix_keys
    ADD CONSTRAINT ck_pix_keys_type CHECK (key_type IN ('EMAIL', 'PHONE', 'CPF', 'RANDOM'));
ALTER TABLE pix_keys ALTER COLUMN key_value TYPE VARCHAR(512);
