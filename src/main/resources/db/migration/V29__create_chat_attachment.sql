CREATE TABLE chat_attachment (
 id UUID PRIMARY KEY,
 space_id UUID NOT NULL,
 uploader_id UUID NOT NULL,
 client_message_id UUID NOT NULL,
 message_id UUID,
 name varchar(180) NOT NULL,
 kind varchar(10) NOT NULL CHECK(kind IN ('IMAGE','VIDEO','DOCUMENT','VOICE')),
 mime varchar(120) NOT NULL,
 bytes bigint NOT NULL CHECK(bytes>0 AND bytes<=100000000),
 sha256 char(64) NOT NULL,
 duration double precision,
 width integer,
 height integer,
 state varchar(12) NOT NULL DEFAULT 'UPLOADED' CHECK(state IN ('UPLOADED','ATTACHED','DELETED','PURGED')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(space_id,id),
 UNIQUE(uploader_id,client_message_id),
 UNIQUE(message_id),
 FOREIGN KEY(space_id,uploader_id,client_message_id) REFERENCES chat_upload_intent(space_id,uploader_id,client_message_id),
 FOREIGN KEY(space_id,message_id) REFERENCES chat_message(space_id,id),
 CHECK((state='UPLOADED' AND message_id IS NULL) OR (state='ATTACHED' AND message_id IS NOT NULL) OR state IN ('DELETED','PURGED'))
);
CREATE INDEX ix_chat_attachment_cleanup ON chat_attachment(state,created_at);
