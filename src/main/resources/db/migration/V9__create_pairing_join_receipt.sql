-- DEV-03 / PAIR-02: durable same-intent retry; receipt can only reference real membership.
CREATE TABLE pairing_join_receipt (
    user_id UUID PRIMARY KEY,
    idempotency_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    space_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (space_id, user_id) REFERENCES couple_membership(space_id, user_id) ON DELETE RESTRICT
);
