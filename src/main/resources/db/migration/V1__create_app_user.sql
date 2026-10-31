-- DEV-01; D01, D02; foundation for AUTH-01 / AC-F01-07/08.
-- Two unique slots enforce the deployment-wide cap even under concurrent writes.
-- No user is seeded. Slot reuse/account lifecycle remains Q03; no deletion API exists.
CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    account_slot SMALLINT NOT NULL UNIQUE CHECK (account_slot IN (1, 2)),
    display_name TEXT NOT NULL CHECK (length(btrim(display_name)) > 0),
    email TEXT NOT NULL CHECK (length(btrim(email)) > 0),
    password_hash TEXT NOT NULL CHECK (length(password_hash) > 0),
    signup_relationship_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE UNIQUE INDEX uq_app_user_email_normalized ON app_user (lower(btrim(email)));
