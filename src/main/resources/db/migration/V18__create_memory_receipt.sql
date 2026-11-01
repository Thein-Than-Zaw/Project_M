-- Minimal retry identity survives purge: no title, caption, filename or file content retained.
CREATE TABLE memory_receipt (
 actor_id UUID NOT NULL REFERENCES app_user(id),
 request_key UUID NOT NULL,
 operation VARCHAR(6) NOT NULL CHECK(operation IN ('UPLOAD','CREATE')),
 request_hash CHAR(64) NOT NULL,
 target_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(actor_id,request_key,operation)
);
