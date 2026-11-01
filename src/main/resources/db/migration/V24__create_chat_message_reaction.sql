-- CHAT-02A: one editable reaction per member/message, scoped by composite foreign keys.
CREATE TABLE chat_message_reaction (
 space_id UUID NOT NULL,
 message_id UUID NOT NULL,
 user_id UUID NOT NULL,
 emoji VARCHAR(8) NOT NULL CHECK(emoji IN ('❤️','😂','😮','😢','👍','🥰')),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(message_id,user_id),
 FOREIGN KEY(space_id,message_id) REFERENCES chat_message(space_id,id) ON DELETE CASCADE,
 FOREIGN KEY(space_id,user_id) REFERENCES couple_membership(space_id,user_id) ON DELETE CASCADE
);
