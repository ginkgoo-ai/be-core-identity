ALTER TABLE identity.user_info
    ADD COLUMN IF NOT EXISTS login_methods varchar [] DEFAULT '{}';

UPDATE identity.user_info
SET login_methods = ARRAY['PASSWORD']
WHERE login_methods IS NULL
   OR cardinality(login_methods) = 0;

ALTER TABLE identity.user_info
    ADD COLUMN IF NOT EXISTS roles varchar [] DEFAULT '{}';

UPDATE identity.user_info
SET roles = ARRAY['ROLE_USER']
WHERE roles IS NULL
   OR cardinality(roles) = 0;


DROP TABLE IF EXISTS identity.role;

DROP TABLE IF EXISTS identity.user_role;