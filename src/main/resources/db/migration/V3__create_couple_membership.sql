-- DEV-01; D01; foundation only. PAIR-01/02 transaction services arrive in DEV-03.
CREATE TABLE couple_membership (
    space_id UUID NOT NULL REFERENCES couple_space(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE RESTRICT,
    member_slot SMALLINT NOT NULL CHECK (member_slot IN (1, 2)),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (space_id, member_slot)
);
