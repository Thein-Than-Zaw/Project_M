-- Favorites are private per-account preferences; they never change memory authorship/version.
CREATE TABLE memory_favorite (
 memory_id UUID NOT NULL REFERENCES memory(id) ON DELETE CASCADE,
 user_id UUID NOT NULL REFERENCES app_user(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(memory_id,user_id)
);
