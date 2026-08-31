ALTER TABLE bank_transactions
    DROP CONSTRAINT ck_transactions_type;

ALTER TABLE bank_transactions
    ADD CONSTRAINT ck_transactions_type
    CHECK (transaction_type IN ('DEPOSITO', 'SAQUE', 'PAGAMENTO', 'PIX'));
