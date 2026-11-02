package com.couple.privatespace.chat;

import com.couple.privatespace.identity.ApiFailure;
import com.couple.privatespace.memory.MemoryFiles;
import com.couple.privatespace.pairing.PairingService;
import java.io.IOException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

/** CHAT-05: shared private storage, independent Chat ownership/lifecycle, cancellable intents. */
@Service
public class ChatAttachmentService {
 private final JdbcTemplate jdbc;private final PairingService pairing;private final MemoryFiles files;private final ChatService chat;
 public ChatAttachmentService(JdbcTemplate jdbc,PairingService pairing,MemoryFiles files,ChatService chat){this.jdbc=jdbc;this.pairing=pairing;this.files=files;this.chat=chat;}
 private UUID space(UUID actor){var space=pairing.current(actor);if(!space.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return space.id();}
 @Transactional
 public ChatService.Attachment register(UUID actor,UUID key,MemoryFiles.Validated checked){
  UUID space=space(actor);ChatService.lockUpload(jdbc,actor,key);intent(space,actor,key);ChatService.ensureNotCancelled(jdbc,actor,key);
  var old=jdbc.queryForList("SELECT id,sha256,name,state FROM chat_attachment WHERE uploader_id=? AND client_message_id=?",actor,key);
  if(!old.isEmpty()){var row=old.getFirst();if(!checked.sha().equals(row.get("sha256"))||!checked.name().equals(row.get("name")))throw new ApiFailure(409,"UPLOAD_CONFLICT","This upload identifier was used for another file.");if(Set.of("DELETED","PURGED").contains(row.get("state")))throw new ApiFailure(409,"UPLOAD_CANCELLED","This upload was removed.");return metadata((UUID)row.get("id"));}
  if(jdbc.queryForObject("SELECT count(*) FROM chat_attachment WHERE uploader_id=? AND state='UPLOADED'",Integer.class,actor)>=20)throw new ApiFailure(429,"UPLOAD_LIMIT","Send or cancel your pending uploads first.");
  try{files.promote(checked);}catch(IOException error){throw new ApiFailure(503,"UPLOAD_NOT_SAVED","The private file could not be saved. Retry with the same identifier.");}
  jdbc.update("INSERT INTO chat_attachment(id,space_id,uploader_id,client_message_id,name,kind,mime,bytes,sha256,duration,width,height) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",checked.id(),space,actor,key,checked.name(),checked.kind(),checked.mime(),checked.size(),checked.sha(),checked.duration(),checked.width(),checked.height());return metadata(checked.id());
 }
 private void intent(UUID space,UUID actor,UUID key){jdbc.update("INSERT INTO chat_upload_intent(space_id,uploader_id,client_message_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",space,actor,key);}
 private ChatService.Attachment metadata(UUID id){return jdbc.queryForObject("SELECT * FROM chat_attachment WHERE id=?",(r,n)->ChatService.attachment(r),id);}
 public record Cancellation(String state,UUID messageId){}
 @Transactional
 public Cancellation cancel(UUID actor,UUID key){
  UUID space=space(actor);ChatService.lockUpload(jdbc,actor,key);intent(space,actor,key);
  jdbc.update("UPDATE chat_upload_intent SET cancelled_at=COALESCE(cancelled_at,clock_timestamp()) WHERE uploader_id=? AND client_message_id=?",actor,key);
  var messages=jdbc.queryForList("SELECT id FROM chat_message WHERE space_id=? AND sender_id=? AND client_message_id=?",UUID.class,space,actor,key);
  if(!messages.isEmpty())chat.cancelSend(actor,messages.getFirst());
  jdbc.update("UPDATE chat_attachment SET state='DELETED' WHERE space_id=? AND uploader_id=? AND client_message_id=? AND state IN ('UPLOADED','ATTACHED')",space,actor,key);
  return new Cancellation(messages.isEmpty()?"CANCELLED":"REMOVED",messages.isEmpty()?null:messages.getFirst());
 }
 public record Download(org.springframework.core.io.Resource resource,String name,String mime,long size,boolean attachment){}
 @Transactional(readOnly=true)
 public Download download(UUID actor,UUID id,boolean thumb,boolean download){
  UUID space=space(actor);var rows=jdbc.queryForList("SELECT a.* FROM chat_attachment a LEFT JOIN chat_message m ON m.id=a.message_id WHERE a.id=? AND a.space_id=? AND ((a.state='UPLOADED' AND a.uploader_id=?) OR (a.state='ATTACHED' AND m.deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=m.id AND h.user_id=?)))",id,space,actor,actor);
  if(rows.isEmpty())throw new ApiFailure(404,"ATTACHMENT_UNAVAILABLE","This file is no longer available.");var row=rows.getFirst();String kind=(String)row.get("kind");if(thumb&&!Set.of("IMAGE","VIDEO").contains(kind))throw new ApiFailure(404,"ATTACHMENT_UNAVAILABLE","No preview is available.");
  try{var resource=files.resource(id,thumb);return new Download(resource,thumb?"Preview.jpg":(String)row.get("name"),thumb?"image/jpeg":(String)row.get("mime"),resource.contentLength(),!thumb&&(download||kind.equals("DOCUMENT")));}catch(IOException error){throw new ApiFailure(503,"FILE_TEMPORARILY_UNAVAILABLE","This file could not be loaded. Retry shortly.");}
 }
 @Scheduled(fixedDelayString="${app.chat.media-maintenance-ms:60000}",initialDelayString="${app.memories.maintenance-initial-ms:1000}")
 public void cleanup(){
  try{jdbc.update("UPDATE chat_attachment SET state='DELETED' WHERE state='UPLOADED' AND created_at<clock_timestamp()-interval '24 hours'");
   for(UUID id:jdbc.queryForList("SELECT id FROM chat_attachment WHERE state='DELETED' LIMIT 50",UUID.class))try{files.delete(id);jdbc.update("UPDATE chat_attachment SET state='PURGED' WHERE id=? AND state='DELETED'",id);}catch(IOException ignored){/* durable retry; never expose a provider response */}
  }catch(org.springframework.dao.DataAccessException ignored){/* transaction/startup recovery retries */}
 }
}
