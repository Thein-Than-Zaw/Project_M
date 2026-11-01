-- DEV-05 CHAT-02: allocation row locks serialize the couple's stable message order.
CREATE TABLE chat_conversation (
    space_id UUID PRIMARY KEY REFERENCES couple_space(id),
    last_sequence BIGINT NOT NULL DEFAULT 0 CHECK (last_sequence >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
