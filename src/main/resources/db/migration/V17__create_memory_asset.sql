-- Files live outside the static root; storage paths derive exclusively from the server UUID.
CREATE TABLE memory_asset (
 id UUID PRIMARY KEY,
 space_id UUID NOT NULL REFERENCES couple_space(id),
 memory_id UUID,
 uploaded_by UUID NOT NULL,
 original_name VARCHAR(180) NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK(kind IN ('IMAGE','VIDEO','DOCUMENT')),
 mime VARCHAR(120) NOT NULL,
 byte_size BIGINT NOT NULL CHECK(byte_size>0),
 sha256 CHAR(64) NOT NULL CHECK(sha256 ~ '^[0-9a-f]{64}$'),
 duration DOUBLE PRECISION,
 width INTEGER,
 height INTEGER,
 position INTEGER NOT NULL DEFAULT 0 CHECK(position BETWEEN 0 AND 9),
 state VARCHAR(10) NOT NULL DEFAULT 'READY' CHECK(state IN ('READY','DISCARDED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(space_id,memory_id) REFERENCES memory(space_id,id) ON DELETE CASCADE,
 FOREIGN KEY(space_id,uploaded_by) REFERENCES couple_membership(space_id,user_id),
 UNIQUE(memory_id,position) DEFERRABLE INITIALLY DEFERRED,
 CHECK(memory_id IS NULL OR state='READY'),
 CHECK((kind='IMAGE' AND byte_size<=10000000 AND duration IS NULL AND width IS NOT NULL AND height IS NOT NULL AND width>0 AND height>0 AND mime IN ('image/jpeg','image/png','image/webp')) OR
       (kind='VIDEO' AND byte_size<=100000000 AND duration IS NOT NULL AND duration>0 AND duration<=180 AND width IS NOT NULL AND height IS NOT NULL AND width>0 AND height>0 AND mime='video/mp4') OR
       (kind='DOCUMENT' AND byte_size<=20000000 AND duration IS NULL AND width IS NULL AND height IS NULL AND mime IN ('application/pdf','application/vnd.openxmlformats-officedocument.wordprocessingml.document','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet','application/vnd.openxmlformats-officedocument.presentationml.presentation')))
);
CREATE INDEX ix_memory_asset_parent ON memory_asset(memory_id,position) WHERE state='READY';
CREATE INDEX ix_memory_asset_staged ON memory_asset(created_at) WHERE memory_id IS NULL;
