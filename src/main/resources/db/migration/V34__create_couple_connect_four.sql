-- GAME-01: one durable current board per private couple; no chat/call media data.
CREATE TABLE couple_connect_four (
 space_id UUID PRIMARY KEY REFERENCES couple_space(id),
 id UUID NOT NULL UNIQUE,
 inviter_id UUID NOT NULL,
 red_id UUID NOT NULL,
 yellow_id UUID NOT NULL,
 last_first_id UUID,
 status VARCHAR(10) NOT NULL CHECK(status IN ('WAITING','ACTIVE','WON','DRAW','RESIGNED','CANCELLED','DECLINED')),
 board VARCHAR(42) NOT NULL CHECK(board ~ '^[.RY]{42}$'),
 moves INTEGER NOT NULL DEFAULT 0 CHECK(moves BETWEEN 0 AND 42),
 winner_id UUID,
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(space_id,inviter_id) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,red_id) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,yellow_id) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,last_first_id) REFERENCES couple_membership(space_id,user_id),
 FOREIGN KEY(space_id,winner_id) REFERENCES couple_membership(space_id,user_id),
 CHECK(red_id<>yellow_id),
 CHECK(inviter_id IN(red_id,yellow_id)),
 CHECK(last_first_id IS NULL OR last_first_id IN(red_id,yellow_id)),
 CHECK((status IN ('WON','RESIGNED') AND winner_id IS NOT NULL AND winner_id IN(red_id,yellow_id)) OR (status NOT IN ('WON','RESIGNED') AND winner_id IS NULL))
);
