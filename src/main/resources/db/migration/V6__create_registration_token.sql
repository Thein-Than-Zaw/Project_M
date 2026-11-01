-- DEV-02 / AUTH-03: raw registration codes never persisted or logged by the app.
CREATE TABLE registration_token (
    id UUID PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('SETUP', 'PARTNER')),
    token_hash CHAR(64) NOT NULL UNIQUE,
    issued_by UUID REFERENCES app_user(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CHECK (expires_at > created_at),
    CHECK ((kind = 'SETUP' AND issued_by IS NULL) OR (kind = 'PARTNER' AND issued_by IS NOT NULL))
);
CREATE UNIQUE INDEX uq_registration_token_live_kind ON registration_token(kind)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
