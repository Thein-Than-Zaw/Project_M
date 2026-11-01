package com.couple.privatespace.notification;

import com.couple.privatespace.identity.ApiFailure;
import com.couple.privatespace.pairing.PairingService;
import com.couple.privatespace.home.HomeService;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** DEV-09A / NOTIFY-01–03: all ownership and targets are resolved on the server. */
@Service
public class NotificationService {
 public record Item(UUID id,long sequence,String type,Instant createdAt,Instant readAt,long version) { }
 public record Page(List<Item> items,Long nextCursor,long unreadCount) { }
 public record Edit(Boolean read,Long version) { }
 public record Target(boolean available,String url) { }
 private final JdbcTemplate jdbc;private final PairingService pairing;private final ApplicationEventPublisher events;
 public NotificationService(JdbcTemplate jdbc,PairingService pairing,ApplicationEventPublisher events){this.jdbc=jdbc;this.pairing=pairing;this.events=events;}
 private UUID space(UUID actor){var current=pairing.current(actor);if(!current.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return current.id();}
 private Item item(java.sql.ResultSet row)throws java.sql.SQLException{return new Item(row.getObject("id",UUID.class),row.getLong("sequence"),row.getString("type"),row.getTimestamp("created_at").toInstant(),row.getTimestamp("read_at")==null?null:row.getTimestamp("read_at").toInstant(),row.getLong("version"));}
 private Item own(UUID actor,UUID space,UUID id){return jdbc.query("SELECT * FROM notification_inbox WHERE id=? AND recipient_id=? AND space_id=?",(r,n)->item(r),id,actor,space).stream().findFirst().orElseThrow(()->new ApiFailure(404,"NOTIFICATION_NOT_FOUND","This notification is not available."));}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Page list(UUID actor,Long before,boolean unreadOnly){
  UUID space=space(actor);if(before!=null&&before<=0)throw new ApiFailure(400,"INVALID_CURSOR","Reload your notifications and try again.");
  var rows=jdbc.query("SELECT * FROM notification_inbox WHERE recipient_id=? AND space_id=? AND sequence<?"+(unreadOnly?" AND read_at IS NULL":"")+" ORDER BY sequence DESC LIMIT 26",(r,n)->item(r),actor,space,before==null?Long.MAX_VALUE:before);
  long unread=jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=? AND space_id=? AND read_at IS NULL",Long.class,actor,space);
  boolean more=rows.size()>25;var items=List.copyOf(rows.subList(0,Math.min(25,rows.size())));return new Page(items,more?items.getLast().sequence():null,unread);
 }
 @Transactional
 public Item edit(UUID actor,UUID id,Edit request){
  UUID space=space(actor);if(request==null||request.read()==null||request.version()==null||request.version()<0)throw new ApiFailure(400,"INVALID_NOTIFICATION_UPDATE","Choose read or unread and reload the current notification.");
  var current=own(actor,space,id);if((current.readAt()!=null)==request.read())return current;
  int updated=jdbc.update("UPDATE notification_inbox SET read_at=CASE WHEN ? THEN clock_timestamp() ELSE NULL END,version=version+1 WHERE id=? AND recipient_id=? AND space_id=? AND version=?",request.read(),id,actor,space,request.version());
  if(updated==0){current=own(actor,space,id);if((current.readAt()!=null)!=request.read())throw new ApiFailure(409,"NOTIFICATION_CHANGED","This notification changed in another tab. Reload and try again.");return current;}
  events.publishEvent(new HomeService.Changed(List.of(actor),"notifications.updated"));return own(actor,space,id);
 }
 @Transactional(readOnly=true)
 public Target target(UUID actor,UUID id){
  UUID space=space(actor);var notification=own(actor,space,id);
  UUID target=jdbc.queryForObject("SELECT target_id FROM notification_inbox WHERE id=?",UUID.class,id);
  String table=switch(notification.type()){case "MESSAGE","GIFT"->"chat_message";case "MEMORY"->"memory";case "MISSED_CALL"->"call_session";default->"couple_space";};
  String condition=table.equals("couple_space")?"id=? AND id=?":"id=? AND space_id=?";
  if(table.equals("memory"))condition+=" AND state='ACTIVE'";
  var args=new ArrayList<Object>();args.add(target);args.add(space);
  if(table.equals("chat_message")){condition+=" AND deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?)";args.add(actor);}
  boolean exists=jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE "+condition,Integer.class,args.toArray())==1;
  String url=switch(notification.type()){case "MESSAGE","GIFT"->"/chat.html";case "MEMORY"->"/memories.html#"+target;case "MISSED_CALL"->"/home.html#calls";default->"/home.html";};
  return new Target(exists,exists?url:null);
 }
}
