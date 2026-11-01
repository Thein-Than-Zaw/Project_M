-- CHAT-02A: immutable reply linkage, sender deletion tombstones and mutation recovery.
ALTER TABLE chat_conversation ADD COLUMN change_version BIGINT NOT NULL DEFAULT 0 CHECK(change_version>=0);
UPDATE chat_conversation SET change_version=last_sequence;
ALTER TABLE chat_message ADD COLUMN reply_to_id UUID;
ALTER TABLE chat_message ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE chat_message ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0);
ALTER TABLE chat_message ADD COLUMN request_hash BYTEA;
ALTER TABLE chat_message ADD CONSTRAINT uq_chat_message_space_id UNIQUE(space_id,id);
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_reply_scope FOREIGN KEY(space_id,reply_to_id) REFERENCES chat_message(space_id,id);
ALTER TABLE chat_message ADD CONSTRAINT ck_chat_reply_not_self CHECK(reply_to_id IS NULL OR reply_to_id<>id);
UPDATE chat_message SET request_hash=sha256(convert_to(jsonb_build_array(body,type,gift_code,reply_to_id)::text,'UTF8'));
ALTER TABLE chat_message ALTER COLUMN request_hash SET NOT NULL;
ALTER TABLE chat_message ADD CONSTRAINT ck_chat_request_hash CHECK(octet_length(request_hash)=32);
CREATE FUNCTION set_chat_message_request_hash() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 NEW.request_hash:=sha256(convert_to(jsonb_build_array(NEW.body,NEW.type,NEW.gift_code,NEW.reply_to_id)::text,'UTF8'));
 RETURN NEW;
END;
$$;
CREATE TRIGGER tr_chat_message_request_hash BEFORE INSERT ON chat_message FOR EACH ROW EXECUTE FUNCTION set_chat_message_request_hash();
ALTER TABLE chat_message DROP CONSTRAINT chat_message_payload_check;
ALTER TABLE chat_message ADD CONSTRAINT chat_message_payload_check CHECK (
 (deleted_at IS NOT NULL AND type IN ('TEXT','GIFT') AND body='' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL)
 OR (deleted_at IS NULL AND (
 (type='TEXT' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND length(body) BETWEEN 1 AND 4000 AND length(btrim(body))>0)
 OR (type='GIFT' AND gift_code IS NOT NULL AND gift_name IS NOT NULL AND gift_asset_path IS NOT NULL AND gift_asset_path ~ '^/stickers/[a-z0-9_-]+/[a-zA-Z0-9_-]+[.]png$' AND length(body)<=500)))
);
CREATE INDEX ix_chat_reply_target ON chat_message(space_id,reply_to_id) WHERE reply_to_id IS NOT NULL;
