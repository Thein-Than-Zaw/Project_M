-- DEV-05 CHAT-01–03 / AC-F04-01–06. No seed, edit/delete or attachment behavior.
CREATE TABLE chat_message (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES chat_conversation(space_id),
    sequence BIGINT NOT NULL CHECK (sequence > 0),
    sender_id UUID NOT NULL,
    client_message_id UUID NOT NULL,
    body TEXT NOT NULL CHECK (length(body) BETWEEN 1 AND 4000 AND length(btrim(body)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    FOREIGN KEY (space_id,sender_id) REFERENCES couple_membership(space_id,user_id),
    UNIQUE (space_id,sequence),
    UNIQUE (sender_id,client_message_id),
    CHECK (read_at IS NULL OR delivered_at IS NOT NULL)
);
CREATE INDEX ix_chat_message_unread ON chat_message(space_id,sender_id,sequence) WHERE read_at IS NULL;
