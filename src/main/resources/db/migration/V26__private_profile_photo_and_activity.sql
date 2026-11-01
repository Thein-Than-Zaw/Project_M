-- CHAT-03: private normalized profile photos and observed foreground activity.
ALTER TABLE app_user ADD COLUMN avatar_jpeg bytea,
    ADD COLUMN avatar_version bigint NOT NULL DEFAULT 0,
    ADD COLUMN last_active_at timestamptz;
ALTER TABLE app_user ADD CONSTRAINT avatar_size CHECK (avatar_jpeg IS NULL OR octet_length(avatar_jpeg)<=262144);
