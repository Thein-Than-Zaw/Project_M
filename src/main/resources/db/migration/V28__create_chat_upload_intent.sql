-- CHAT-05: cancellation wins over delayed uploads/sends for the same actor intent.
CREATE TABLE chat_upload_intent (
 space_id UUID NOT NULL,
 uploader_id UUID NOT NULL,
 client_message_id UUID NOT NULL,
 cancelled_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(uploader_id,client_message_id),
 UNIQUE(space_id,uploader_id,client_message_id),
 FOREIGN KEY(space_id,uploader_id) REFERENCES couple_membership(space_id,user_id)
);
