ALTER TABLE accounts
    ALTER COLUMN check_digit TYPE VARCHAR(1);

ALTER TABLE bank_transactions
    ALTER COLUMN direction TYPE VARCHAR(1);
