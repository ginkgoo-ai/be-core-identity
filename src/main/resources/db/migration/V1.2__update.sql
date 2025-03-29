ALTER TABLE identity.user_info
    ADD COLUMN IF NOT EXISTS created_by VARCHAR (255);

ALTER TABLE identity.user_info
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR (255);