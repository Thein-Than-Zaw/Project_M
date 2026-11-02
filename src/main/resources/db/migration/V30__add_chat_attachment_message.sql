ALTER TABLE chat_message ADD COLUMN attachment_id UUID;
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_attachment_scope FOREIGN KEY(space_id,attachment_id) REFERENCES chat_attachment(space_id,id);
ALTER TABLE chat_message DROP CONSTRAINT chat_message_payload_check;
ALTER TABLE chat_message ADD CONSTRAINT chat_message_payload_check CHECK (
 (deleted_at IS NOT NULL AND type IN ('TEXT','GIFT','FILE') AND body='' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND attachment_id IS NULL)
 OR (deleted_at IS NULL AND (
 (type='TEXT' AND attachment_id IS NULL AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND length(body) BETWEEN 1 AND 4000 AND length(btrim(body))>0)
 OR (type='GIFT' AND attachment_id IS NULL AND gift_code IS NOT NULL AND gift_name IS NOT NULL AND gift_asset_path IS NOT NULL AND gift_asset_path ~ '^/stickers/[a-z0-9_-]+/[a-zA-Z0-9_-]+[.]png$' AND length(body)<=500)
 OR (type='FILE' AND attachment_id IS NOT NULL AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL AND length(body)<=4000)))
);
CREATE OR REPLACE FUNCTION set_chat_message_request_hash() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 -- Preserve fingerprints for pre-attachment TEXT/GIFT retry compatibility.
 NEW.request_hash:=sha256(convert_to(CASE WHEN NEW.attachment_id IS NULL
 THEN jsonb_build_array(NEW.body,NEW.type,NEW.gift_code,NEW.reply_to_id)::text
 ELSE jsonb_build_array(NEW.body,NEW.type,NEW.gift_code,NEW.reply_to_id,NEW.attachment_id)::text END,'UTF8'));
 RETURN NEW;
END;
$$;
