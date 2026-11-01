-- DEV-09A / NOTIFY-01–03: recipient-only durable event metadata, never message previews.
CREATE TABLE notification_inbox (
 sequence BIGSERIAL PRIMARY KEY,
 id UUID NOT NULL UNIQUE,
 space_id UUID NOT NULL,
 recipient_id UUID NOT NULL,
 source_event_id UUID NOT NULL,
 type VARCHAR(20) NOT NULL CHECK(type IN ('MESSAGE','GIFT','MEMORY','MISSED_CALL','PARTNER_JOINED')),
 target_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 read_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 FOREIGN KEY(space_id,recipient_id) REFERENCES couple_membership(space_id,user_id) ON DELETE CASCADE,
 UNIQUE(source_event_id,recipient_id,type)
);
CREATE INDEX ix_notification_recipient_page ON notification_inbox(recipient_id,space_id,sequence DESC);
CREATE INDEX ix_notification_unread ON notification_inbox(recipient_id,space_id,sequence DESC) WHERE read_at IS NULL;
