package com.couple.privatespace.notification;

import com.couple.privatespace.chat.ChatService;
import com.couple.privatespace.call.CallService;
import com.couple.privatespace.home.HomeService;
import com.couple.privatespace.memory.MemoryService;
import com.couple.privatespace.pairing.PairingService;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

/** NOTIFY-02: inbox insertion shares the business transaction; WS is only invalidation. */
@Component
public class NotificationEvents {
 private final JdbcTemplate jdbc;private final ApplicationEventPublisher events;
 public NotificationEvents(JdbcTemplate jdbc,ApplicationEventPublisher events){this.jdbc=jdbc;this.events=events;}
 private void save(UUID space,UUID recipient,UUID source,String type,UUID target){
  int added=jdbc.update("INSERT INTO notification_inbox(id,space_id,recipient_id,source_event_id,type,target_id) VALUES (?,?,?,?,?,?) ON CONFLICT(source_event_id,recipient_id,type) DO NOTHING",UUID.randomUUID(),space,recipient,source,type,target);
  if(added>0)events.publishEvent(new HomeService.Changed(List.of(recipient),"notifications.updated"));
 }
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void message(ChatService.Changed event){
  if(event.message()==null||!"message.created".equals(event.type()))return;
  var message=event.message();UUID space=jdbc.queryForObject("SELECT space_id FROM chat_message WHERE id=?",UUID.class,message.id());
  for(UUID recipient:event.members())if(!recipient.equals(message.senderId()))save(space,recipient,message.id(),"GIFT".equals(message.type())?"GIFT":"MESSAGE",message.id());
 }
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void memory(MemoryService.Published event){for(UUID recipient:jdbc.queryForList("SELECT user_id FROM couple_membership WHERE space_id=? AND user_id<>?",UUID.class,event.space(),event.actor()))save(event.space(),recipient,event.memoryId(),"MEMORY",event.memoryId());}
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void missed(CallService.State event){if("MISSED".equals(event.call().state())){UUID id=event.call().id();UUID space=jdbc.queryForObject("SELECT space_id FROM call_session WHERE id=?",UUID.class,id);save(space,event.call().calleeId(),id,"MISSED_CALL",id);}}
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
 public void paired(PairingService.Changed event){
  // The inviter is member_slot1; the joining partner does not receive their own join event.
  for(UUID recipient:event.members())for(UUID space:jdbc.queryForList("SELECT space_id FROM couple_membership WHERE user_id=? AND member_slot=1",UUID.class,recipient))save(space,recipient,space,"PARTNER_JOINED",space);
 }
}
