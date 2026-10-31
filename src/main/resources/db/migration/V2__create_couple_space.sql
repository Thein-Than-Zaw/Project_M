-- DEV-01; D01/D02; shared date has no assumed timezone or signup-conflict default.
CREATE TABLE couple_space (
    id UUID PRIMARY KEY,
    deployment_slot SMALLINT NOT NULL UNIQUE CHECK (deployment_slot = 1),
    relationship_date DATE NOT NULL,
    timezone TEXT NOT NULL CHECK (length(btrim(timezone)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
