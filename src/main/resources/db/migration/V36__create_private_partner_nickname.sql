-- ACCOUNT-01: personal label, never a partner profile rename or shared nickname.
CREATE TABLE partner_nickname (
 space_id UUID NOT NULL,
 owner_id UUID PRIMARY KEY,
 partner_id UUID NOT NULL,
 nickname VARCHAR(60),
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(space_id,owner_id) REFERENCES couple_membership(space_id,user_id) ON DELETE CASCADE,
 FOREIGN KEY(space_id,partner_id) REFERENCES couple_membership(space_id,user_id) ON DELETE CASCADE,
 CHECK(owner_id<>partner_id),
 CHECK(nickname IS NULL OR (length(nickname) BETWEEN 1 AND 60 AND length(btrim(nickname))>0))
);
