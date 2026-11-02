-- CHAT-05: viewing actual incoming messages also reads their own inbox entries.
CREATE INDEX ix_notification_chat_target_unread ON notification_inbox(space_id,target_id,recipient_id)
 WHERE read_at IS NULL AND type IN ('MESSAGE','GIFT');

UPDATE notification_inbox n SET read_at=m.read_at,version=n.version+1
 FROM chat_message m WHERE n.space_id=m.space_id AND n.target_id=m.id
 AND n.recipient_id<>m.sender_id AND n.type IN ('MESSAGE','GIFT')
 AND n.read_at IS NULL AND m.read_at IS NOT NULL;

CREATE FUNCTION sync_chat_notification_read() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 UPDATE notification_inbox SET read_at=NEW.read_at,version=version+1
 WHERE space_id=NEW.space_id AND target_id=NEW.id AND recipient_id<>NEW.sender_id
 AND type IN ('MESSAGE','GIFT') AND read_at IS NULL;
 RETURN NEW;
END;
$$;
CREATE TRIGGER tr_chat_notification_read AFTER UPDATE OF read_at ON chat_message
 FOR EACH ROW WHEN (NEW.read_at IS NOT NULL AND OLD.read_at IS DISTINCT FROM NEW.read_at)
 EXECUTE FUNCTION sync_chat_notification_read();
