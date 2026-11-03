package com.couple.privatespace.chat;

import com.couple.privatespace.call.CallService;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.*;
import org.springframework.transaction.support.TransactionTemplate;

/** CHAT-07: terminal video metadata only, durable retry and exactly one timeline row. */
@Service
public class ChatCallHistory {
 private final JdbcTemplate jdbc;
 private final ApplicationEventPublisher events;
 private final TransactionTemplate tx;
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ChatCallHistory.class);
 public ChatCallHistory(JdbcTemplate jdbc,ApplicationEventPublisher events,PlatformTransactionManager manager){
  this.jdbc=jdbc;this.events=events;tx=new TransactionTemplate(manager);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void ended(CallService.State event){if(event.call().mode().equals("VIDEO")&&event.call().endedAt()!=null)publishSafely(event.call().id());}
 @Scheduled(initialDelayString="${app.calls.maintenance-initial-ms:5000}",fixedDelay=5000)
 public void retryPending(){for(var id:jdbc.queryForList("SELECT call_id FROM call_chat_event WHERE published_at IS NULL ORDER BY created_at LIMIT 50",UUID.class))publishSafely(id);}
 private void publishSafely(UUID id){try{publish(id);}catch(RuntimeException failure){LOG.warn("Video call history deferred; durable retry remains pending ({})",failure.getClass().getSimpleName());}}
 public void publish(UUID id){tx.executeWithoutResult(status->{
  var pending=jdbc.queryForList("SELECT call_id FROM call_chat_event WHERE call_id=? AND published_at IS NULL FOR UPDATE",id);if(pending.isEmpty())return;
  var rows=jdbc.queryForList("SELECT * FROM call_session WHERE id=? AND mode='VIDEO' AND ended_at IS NOT NULL",id);if(rows.isEmpty())return;
  var call=rows.getFirst();var space=(UUID)call.get("space_id");var caller=(UUID)call.get("caller_id");var callee=(UUID)call.get("callee_id");
  jdbc.update("INSERT INTO chat_conversation(space_id) VALUES (?) ON CONFLICT DO NOTHING",space);
  jdbc.queryForObject("SELECT last_sequence FROM chat_conversation WHERE space_id=? FOR UPDATE",Long.class,space);
  long sequence=jdbc.queryForObject("UPDATE chat_conversation SET last_sequence=last_sequence+1,change_version=change_version+1,updated_at=clock_timestamp() WHERE space_id=? RETURNING last_sequence",Long.class,space);
  jdbc.update("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body,type,call_id,created_at,delivered_at,read_at) VALUES (?,?,?,?,?,'','CALL',?,?,?,?)",UUID.randomUUID(),space,sequence,caller,UUID.randomUUID(),id,call.get("ended_at"),call.get("ended_at"),call.get("ended_at"));
  jdbc.update("UPDATE call_chat_event SET published_at=clock_timestamp() WHERE call_id=?",id);
  // Invalidation only: no ordinary message chime, unread receipt or MESSAGE notification.
  events.publishEvent(new ChatService.Changed(List.of(caller,callee),"call.logged",null,List.of()));
 });}
}
