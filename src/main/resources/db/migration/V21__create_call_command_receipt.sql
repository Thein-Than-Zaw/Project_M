CREATE TABLE call_command_receipt (
 actor_id UUID NOT NULL REFERENCES app_user(id),
 event_id UUID NOT NULL,
 call_id UUID NOT NULL REFERENCES call_session(id) ON DELETE CASCADE,
 operation VARCHAR(10) NOT NULL CHECK(operation IN ('invite','accept','decline','cancel','end')),
 request_hash CHAR(64) NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(actor_id,event_id)
);
