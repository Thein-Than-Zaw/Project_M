-- Durable outbox: separate call's space lock from chat's conversation lock.
CREATE TABLE call_chat_event (
 call_id UUID PRIMARY KEY REFERENCES call_session(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 published_at TIMESTAMPTZ
);
CREATE INDEX ix_call_chat_event_pending ON call_chat_event(created_at) WHERE published_at IS NULL;
