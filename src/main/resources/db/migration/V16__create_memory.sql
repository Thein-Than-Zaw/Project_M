-- DEV-07 MEM-01–03 / owner: shared edit/delete, 72-hour Trash and immediate Empty Trash.
CREATE TABLE memory (
 id UUID PRIMARY KEY,
 space_id UUID NOT NULL REFERENCES couple_space(id),
 title VARCHAR(120) NOT NULL CHECK(length(btrim(title))>0),
 caption TEXT NOT NULL DEFAULT '' CHECK(length(caption)<=2000),
 memory_date DATE NOT NULL,
 state VARCHAR(12) NOT NULL DEFAULT 'ACTIVE' CHECK(state IN ('ACTIVE','TRASHED','PURGING')),
 created_by UUID NOT NULL,
 updated_by UUID NOT NULL,
 deleted_by UUID,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 deleted_at TIMESTAMPTZ,
 expires_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 UNIQUE(space_id,id),
 FOREIGN KEY(space_id,created_by) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,updated_by) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,deleted_by) REFERENCES couple_membership(space_id,user_id),
 CHECK((state='ACTIVE' AND deleted_at IS NULL AND expires_at IS NULL) OR
       (state IN ('TRASHED','PURGING') AND deleted_at IS NOT NULL AND expires_at=deleted_at+INTERVAL '72 hours'))
);
CREATE INDEX ix_memory_list ON memory(space_id,state,memory_date DESC,id DESC);
CREATE INDEX ix_memory_expiry ON memory(expires_at) WHERE state='TRASHED';
