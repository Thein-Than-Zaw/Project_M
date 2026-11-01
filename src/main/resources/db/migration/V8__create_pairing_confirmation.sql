-- DEV-03 / AC-F02-06: confirmation binds preview to the authenticated joining actor.
CREATE TABLE pairing_confirmation (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE RESTRICT,
    invitation_id UUID NOT NULL REFERENCES pairing_invitation(id) ON DELETE RESTRICT,
    token_hash CHAR(64) NOT NULL UNIQUE,
    space_version BIGINT NOT NULL CHECK (space_version >= 0),
    relationship_date DATE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
