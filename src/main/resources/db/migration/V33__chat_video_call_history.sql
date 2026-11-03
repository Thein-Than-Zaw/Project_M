ALTER TABLE call_session ADD CONSTRAINT uq_call_session_scope UNIQUE(space_id,id);
ALTER TABLE chat_message ADD COLUMN call_id UUID UNIQUE;
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_call_scope FOREIGN KEY(space_id,call_id) REFERENCES call_session(space_id,id) ON DELETE CASCADE;
ALTER TABLE chat_message DROP CONSTRAINT chat_message_payload_check;
ALTER TABLE chat_message ADD CONSTRAINT chat_message_payload_check CHECK (
 (deleted_at IS NOT NULL AND type IN ('TEXT','GIFT','FILE') AND body='' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND attachment_id IS NULL)
 OR (type='CALL' AND call_id IS NOT NULL AND deleted_at IS NULL AND body='' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND attachment_id IS NULL AND reply_to_id IS NULL AND edited_at IS NULL AND read_at IS NOT NULL AND delivered_at IS NOT NULL)
 OR (deleted_at IS NULL AND call_id IS NULL AND (
 (type='TEXT' AND attachment_id IS NULL AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND length(body) BETWEEN 1 AND 4000 AND length(btrim(body))>0)
 OR (type='GIFT' AND attachment_id IS NULL AND gift_code IS NOT NULL AND gift_name IS NOT NULL AND gift_asset_path IS NOT NULL AND gift_asset_path ~ '^/stickers/[a-z0-9_-]+/[a-zA-Z0-9_-]+[.]png$' AND length(body)<=500)
 OR (type='FILE' AND attachment_id IS NOT NULL AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND length(body)<=4000)))
);
