-- CHAT-02A: Delete for me is a view choice, separate from sender deletion for everyone.
CREATE TABLE chat_message_hidden (
 space_id UUID NOT NULL,
 message_id UUID NOT NULL,
 user_id UUID NOT NULL,
 hidden_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(message_id,user_id),
 FOREIGN KEY(space_id,message_id) REFERENCES chat_message(space_id,id) ON DELETE CASCADE,
 FOREIGN KEY(space_id,user_id) REFERENCES couple_membership(space_id,user_id) ON DELETE CASCADE
);
