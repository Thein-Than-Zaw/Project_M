-- DEV-03 / PAIR-01–03: pair invitation differs from account-registration eligibility.
ALTER TABLE couple_membership ADD CONSTRAINT uq_membership_space_user UNIQUE (space_id, user_id);
CREATE TABLE pairing_invitation (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES couple_space(id) ON DELETE RESTRICT,
    created_by UUID NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    consumed_by UUID REFERENCES app_user(id) ON DELETE RESTRICT,
    revoked_at TIMESTAMPTZ,
    FOREIGN KEY (space_id, created_by) REFERENCES couple_membership(space_id, user_id) ON DELETE RESTRICT,
    CHECK (expires_at > created_at),
    CHECK ((consumed_at IS NULL) = (consumed_by IS NULL))
);
CREATE UNIQUE INDEX uq_pairing_invitation_live_space ON pairing_invitation(space_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
