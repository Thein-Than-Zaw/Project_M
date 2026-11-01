package com.couple.privatespace;

import com.couple.privatespace.chat.ChatService;
import com.couple.privatespace.memory.MemoryService;
import com.couple.privatespace.notification.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** DEV-09A: actual local PostgreSQL, authenticated cookies/CSRF and private STOMP. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class NotificationIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;
 @Autowired ChatService chat;@Autowired NotificationService inbox;@Autowired ApplicationEventPublisher events;@Autowired PlatformTransactionManager manager;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;jdbc.update("DELETE FROM memory WHERE space_id=?",f.space);f.cleanup();}
 JsonNode list(PairingIntegrationTest.Browser browser,String suffix)throws Exception{var response=browser.get("/api/notifications"+suffix);assertEquals(200,response.statusCode(),response.body());return f.transport.tree(response);}
 HttpResponse<String> patch(PairingIntegrationTest.Browser browser,String id,Object body,boolean csrf)throws Exception{
  var request=HttpRequest.newBuilder(f.transport.uri("/api/notifications/"+id)).header("Content-Type","application/json");if(csrf)request.header("X-CSRF-TOKEN",browser.csrf());
  return browser.client.send(request.method("PATCH",HttpRequest.BodyPublishers.ofString(f.transport.json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
 }
 ChatService.Message send(UUID key,String body){return chat.send(f.aId,new ChatService.Send(key,body,"TEXT",null,null));}
 @Test void offlineMessageAndGiftCatchUpPersistAfterLoginWithoutSensitivePreviews()throws Exception{
  try(var sender=f.socket(f.a,f.prefix+"a@example.invalid")){f.send(sender,UUID.randomUUID(),"PRIVATE-preview-never-expose 🌸");}
  var catalogue=chat.gifts(f.aId,null,null,0);var gift=catalogue.items().getFirst();
  chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"private gift note","GIFT",gift.code(),null));
  assertEquals(0,list(f.a,"").get("unreadCount").asInt());var page=list(f.b,"");assertEquals(2,page.get("unreadCount").asInt());assertEquals(2,page.get("items").size());assertFalse(page.toString().contains("PRIVATE-preview"));assertFalse(page.toString().contains("private gift note"));
  assertEquals("GIFT",page.get("items").get(0).get("type").asText());
  assertEquals(204,f.b.post("/api/auth/logout",Map.of(),null).statusCode());assertEquals(200,f.b.login(f.prefix+"b@example.invalid").statusCode());assertEquals(2,list(f.b,"").get("unreadCount").asInt());
 }
 @Test void recipientAclCsrfUnpairedAndGuessedTargetsAreDenied()throws Exception{
  send(UUID.randomUUID(),"safe");String id=list(f.b,"").get("items").get(0).get("id").asText();
  assertEquals(401,f.transport.new Browser().get("/api/notifications").statusCode());
  assertEquals(404,patch(f.a,id,Map.of("read",true,"version",0),true).statusCode());assertEquals(404,f.a.get("/api/notifications/"+id+"/target").statusCode());
  assertEquals(404,patch(f.b,UUID.randomUUID().toString(),Map.of("read",true,"version",0),true).statusCode());assertEquals(403,patch(f.b,id,Map.of("read",true,"version",0),false).statusCode());
  assertEquals(400,f.b.get("/api/notifications?before=0").statusCode());assertEquals(400,patch(f.b,id,Map.of("version",0),true).statusCode());
  jdbc.update("DELETE FROM couple_membership WHERE user_id=?",f.bId);assertEquals(403,f.a.get("/api/notifications").statusCode());
 }
 @Test void readUnreadPersistAndConflictingVersionDoesNotOverwriteOrReadChat()throws Exception{
  var message=send(UUID.randomUUID(),"not a chat read receipt");String id=list(f.b,"").get("items").get(0).get("id").asText();
  var read=patch(f.b,id,Map.of("read",true,"version",0),true);assertEquals(200,read.statusCode());assertEquals(1,f.transport.tree(read).get("version").asInt());assertEquals(200,patch(f.b,id,Map.of("read",true,"version",0),true).statusCode());
  assertEquals(0,list(f.b,"?unreadOnly=true").get("items").size());assertEquals(409,patch(f.b,id,Map.of("read",false,"version",0),true).statusCode());assertEquals(200,patch(f.b,id,Map.of("read",false,"version",1),true).statusCode());
  assertEquals(1,list(f.b,"").get("unreadCount").asInt());assertNull(jdbc.queryForObject("SELECT read_at FROM chat_message WHERE id=?",java.sql.Timestamp.class,message.id()));assertEquals("/chat.html",f.transport.tree(f.b.get("/api/notifications/"+id+"/target")).get("url").asText());
 }
 @Test void paginationIsBoundedWithoutDuplicateOrSkippedRows()throws Exception{
  for(int i=0;i<30;i++)send(UUID.randomUUID(),"page "+i);var first=list(f.b,"");assertEquals(25,first.get("items").size());assertEquals(30,first.get("unreadCount").asInt());
  var second=list(f.b,"?before="+first.get("nextCursor").asLong());assertEquals(5,second.get("items").size());assertTrue(second.get("nextCursor").isNull());var ids=new HashSet<String>();for(var row:first.get("items"))assertTrue(ids.add(row.get("id").asText()));for(var row:second.get("items"))assertTrue(ids.add(row.get("id").asText()));assertEquals(30,ids.size());
 }
 @Test void duplicateEventAndSourceRetryCreateExactlyOneInboxRow(){
  UUID key=UUID.randomUUID();var message=send(key,"idempotent");send(key,"idempotent");
  new TransactionTemplate(manager).executeWithoutResult(tx->{for(int i=0;i<3;i++)events.publishEvent(new ChatService.Changed(List.of(f.aId,f.bId),"message.created",message,List.of()));});
  chat.acknowledge(f.bId,new ChatService.Ack(List.of(message.id())),false);
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=?",Integer.class,f.bId));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=?",Integer.class,f.aId));
 }
 @Test void inboxFailureRollsBackBusinessMessageAndRolledBackEventsNeverAppear(){
  jdbc.execute("ALTER TABLE notification_inbox ADD CONSTRAINT dev09_failure CHECK(type<>'MESSAGE')");
  try{assertThrows(RuntimeException.class,()->send(UUID.randomUUID(),"must roll back"));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=?",Integer.class,f.space));}finally{jdbc.execute("ALTER TABLE notification_inbox DROP CONSTRAINT dev09_failure");}
  new TransactionTemplate(manager).executeWithoutResult(tx->{send(UUID.randomUUID(),"rolled back intent");tx.setRollbackOnly();});assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=?",Integer.class,f.bId));
 }
 @Test void memoryPublicationDeduplicatesAndPurgedTargetStaysGenericAndUnavailable()throws Exception{
  UUID memory=UUID.randomUUID();new TransactionTemplate(manager).executeWithoutResult(tx->{jdbc.update("INSERT INTO memory(id,space_id,title,memory_date,created_by,updated_by) VALUES (?,?,'private title',CURRENT_DATE,?,?)",memory,f.space,f.aId,f.aId);events.publishEvent(new MemoryService.Published(f.space,f.aId,memory));events.publishEvent(new MemoryService.Published(f.space,f.aId,memory));});
  var page=list(f.b,"");assertEquals(1,page.get("items").size());String id=page.get("items").get(0).get("id").asText();assertFalse(page.toString().contains("private title"));assertTrue(f.transport.tree(f.b.get("/api/notifications/"+id+"/target")).get("available").asBoolean());
  jdbc.update("DELETE FROM memory WHERE id=?",memory);var target=f.transport.tree(f.b.get("/api/notifications/"+id+"/target"));assertFalse(target.get("available").asBoolean());assertTrue(target.get("url").isNull());assertEquals(1,list(f.b,"").get("items").size());
 }
 @Test void privateSocketInvalidatesOnlyRecipientAfterCommitAndReadChangesSync()throws Exception{
  try(var a=f.a.socket(true,"http://127.0.0.1:18080");var b=f.b.socket(true,"http://127.0.0.1:18080")){
   a.await("CONNECTED");b.await("CONNECTED");for(var socket:List.of(a,b))socket.send("SUBSCRIBE\nid:inbox\ndestination:/user/queue/pairing\n\n\0");
   long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<until){boolean ready=true;for(String email:List.of(f.prefix+"a@example.invalid",f.prefix+"b@example.invalid")){var user=registry.getUser(email);ready&=user!=null&&user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getId().equals("inbox")));}if(ready)break;Thread.sleep(10);}
   send(UUID.randomUUID(),"socket private");assertTrue(b.await("notifications.updated").contains("ACTIVE"));assertFalse(a.frames.stream().anyMatch(frame->frame.contains("notifications.updated")));
   String id=list(f.b,"").get("items").get(0).get("id").asText();b.frames.clear();assertEquals(200,patch(f.b,id,Map.of("read",true,"version",0),true).statusCode());assertTrue(b.await("notifications.updated").contains("ACTIVE"));
   assertEquals(204,f.b.post("/api/auth/logout",Map.of(),null).statusCode());assertNotNull(b.closed.get(5,TimeUnit.SECONDS));assertEquals(401,f.b.get("/api/notifications").statusCode());
  }
 }
}
