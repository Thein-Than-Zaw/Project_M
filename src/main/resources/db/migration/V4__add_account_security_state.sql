-- DEV-02 / AUTH-02 / PRIV-02. Forward-only addition; no account lifecycle API.
ALTER TABLE app_user ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_user ADD COLUMN security_revision BIGINT NOT NULL DEFAULT 0 CHECK (security_revision >= 0);
