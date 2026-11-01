-- DEV-04 / DATE-03: preserve retry identity even after either member deletes the event.
-- Event id intentionally has no FK: deleting an event must not erase its receipt.
CREATE TABLE special_date_creation_receipt (
    created_by UUID NOT NULL REFERENCES app_user(id),
    creation_key UUID NOT NULL,
    request_hash TEXT NOT NULL,
    event_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (created_by,creation_key)
);
INSERT INTO special_date_creation_receipt(created_by,creation_key,request_hash,event_id)
    SELECT created_by,creation_key,creation_hash,id FROM special_date;
