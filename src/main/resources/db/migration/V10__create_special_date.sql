-- DEV-04 / DATE-01–03: only members of the owning space can own date writes.
-- Forward-only addition; preserves accounts, signup dates and pairing history.
CREATE TABLE special_date (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES couple_space(id),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    title TEXT NOT NULL CHECK (length(btrim(title)) BETWEEN 1 AND 100),
    event_date DATE NOT NULL CHECK (event_date BETWEEN DATE '0001-01-01' AND DATE '9999-12-31'),
    repeat_yearly BOOLEAN NOT NULL DEFAULT FALSE,
    creation_key UUID NOT NULL,
    creation_hash TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (space_id,created_by) REFERENCES couple_membership(space_id,user_id),
    FOREIGN KEY (space_id,updated_by) REFERENCES couple_membership(space_id,user_id),
    UNIQUE (created_by,creation_key)
);
CREATE INDEX ix_special_date_space_date ON special_date(space_id,event_date,id);
