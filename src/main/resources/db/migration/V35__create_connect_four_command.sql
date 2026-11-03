-- Stable action keys make uncertain HTTP retries harmless, including new-round invitations.
CREATE TABLE connect_four_command (
 space_id UUID NOT NULL REFERENCES couple_space(id),
 actor_id UUID NOT NULL,
 action_id UUID NOT NULL,
 request_hash CHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(actor_id,action_id),
 FOREIGN KEY(space_id,actor_id) REFERENCES couple_membership(space_id,user_id)
);
CREATE INDEX ix_connect_four_command_rate ON connect_four_command(actor_id,created_at);
