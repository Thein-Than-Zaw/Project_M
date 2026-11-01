-- DEV-06 GIFT-02: retain TEXT history and the original dedup/receipt/sequence constraints.
ALTER TABLE chat_message ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'TEXT';
ALTER TABLE chat_message ADD COLUMN gift_code VARCHAR(120) REFERENCES gift_catalog(code);
ALTER TABLE chat_message ADD COLUMN gift_name VARCHAR(180);
ALTER TABLE chat_message ADD COLUMN gift_asset_path VARCHAR(240);
ALTER TABLE chat_message DROP CONSTRAINT chat_message_body_check;
ALTER TABLE chat_message ADD CONSTRAINT chat_message_payload_check CHECK (
    (type='TEXT' AND gift_code IS NULL AND gift_name IS NULL AND gift_asset_path IS NULL
      AND length(body) BETWEEN 1 AND 4000 AND length(btrim(body))>0)
    OR (type='GIFT' AND gift_code IS NOT NULL AND gift_name IS NOT NULL AND gift_asset_path IS NOT NULL
      AND gift_asset_path ~ '^/stickers/[a-z0-9_-]+/[a-zA-Z0-9_-]+[.]png$' AND length(body)<=500)
);
CREATE INDEX ix_chat_message_gifts ON chat_message(space_id,sequence DESC) WHERE type='GIFT';
