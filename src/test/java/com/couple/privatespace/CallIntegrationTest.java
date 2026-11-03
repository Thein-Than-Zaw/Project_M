package com.couple.privatespace;

import com.couple.privatespace.call.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** CALL-01–03: real cookies/STOMP/transactions. Synthetic SDP tests signaling, not media. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class CallIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;@Autowired CallMaintenance maintenance;
 @Autowired com.couple.privatespace.chat.ChatService chat;
 @Autowired com.couple.privatespace.chat.ChatCallHistory callHistory;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 final UUID ac=UUID.randomUUID(),bc=UUID.randomUUID();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){jdbc.update("DELETE FROM call_session WHERE space_id=?",f.space);f.cleanup();}
 PairingIntegrationTest.Socket socket(PairingIntegrationTest.Browser browser,String email)throws Exception{
  var socket=browser.socket(true,"http://127.0.0.1:18080");socket.await("CONNECTED");String subscription="calls-"+UUID.randomUUID();socket.send("SUBSCRIBE\nid:"+subscription+"\ndestination:/user/queue/calls\n\n\0");
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<end){var user=registry.getUser(email);if(user!=null&&user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getId().equals(subscription)&&sub.getDestination().equals("/user/queue/calls"))))return socket;Thread.sleep(10);}socket.close();throw new AssertionError("Call subscriber not ready");
 }
 JsonNode reply(PairingIntegrationTest.Socket socket)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);while(System.nanoTime()<end){String raw=socket.frames.poll(100,TimeUnit.MILLISECONDS);if(raw!=null&&raw.startsWith("MESSAGE")){var node=f.transport.json.readTree(raw.substring(raw.indexOf("\n\n")+2,raw.lastIndexOf('\0')));if(Set.of("call.ack","call.error").contains(node.path("type").asText()))return node;}}throw new AssertionError("No call command reply");
 }
 JsonNode command(PairingIntegrationTest.Socket socket,String operation,UUID client,UUID call,Map<String,Object> extra)throws Exception{
  var body=new HashMap<String,Object>(extra);body.putIfAbsent("eventId",UUID.randomUUID());body.put("clientId",client);if(call!=null)body.put("callId",call);if(operation.equals("connected")&&!body.containsKey("negotiationId")){var n=jdbc.queryForObject("SELECT negotiation_id FROM call_session WHERE id=?",UUID.class,call);if(n!=null)body.put("negotiationId",n);}f.command(socket,"call."+operation,body);return reply(socket);
 }
 JsonNode ok(PairingIntegrationTest.Socket socket,String op,UUID client,UUID id,Map<String,Object> extra)throws Exception{var response=command(socket,op,client,id,extra);assertEquals("call.ack",response.path("type").asText(),response.toString());return response.get("call");}
 UUID invite(PairingIntegrationTest.Socket socket,String mode)throws Exception{return UUID.fromString(ok(socket,"invite",ac,null,Map.of("mode",mode)).get("id").asText());}
 void negotiate(PairingIntegrationTest.Socket a,PairingIntegrationTest.Socket b,UUID id,UUID negotiation)throws Exception{
  ok(a,"offer",ac,id,Map.of("negotiationId",negotiation,"sdp","v=0\r\ns=synthetic-offer\r\n"));
  ok(b,"answer",bc,id,Map.of("negotiationId",negotiation,"sdp","v=0\r\ns=synthetic-answer\r\n"));
 }
 @Test void videoOutcomesAppendOnceToChatWithoutUnreadMessagesAndRetriesRecover()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID ended=invite(a,"VIDEO");ok(b,"accept",bc,ended,Map.of());negotiate(a,b,ended,UUID.randomUUID());ok(a,"connected",ac,ended,Map.of());ok(b,"connected",bc,ended,Map.of());ok(a,"end",ac,ended,Map.of());
   UUID cancelled=invite(a,"VIDEO");ok(a,"cancel",ac,cancelled,Map.of());
   UUID declined=invite(a,"VIDEO");ok(b,"decline",bc,declined,Map.of());
   UUID missed=invite(a,"VIDEO");jdbc.update("UPDATE call_session SET created_at=clock_timestamp()-INTERVAL '31 seconds',ring_deadline=clock_timestamp()-INTERVAL '1 second' WHERE id=?",missed);maintenance.runOnce();
   UUID failed=invite(a,"VIDEO");ok(b,"accept",bc,failed,Map.of());jdbc.update("UPDATE call_session SET connect_deadline=clock_timestamp()-INTERVAL '1 second' WHERE id=?",failed);maintenance.runOnce();
   var page=chat.history(f.bId,null,null);assertEquals(5,page.items().size());assertEquals(Set.of("ENDED","CANCELLED","DECLINED","MISSED","FAILED"),new HashSet<>(page.items().stream().map(m->m.call().state()).toList()));
   assertEquals(0,page.unreadCount());assertEquals(0,page.activity().messageCount());assertEquals(1,page.activity().videoCallCount());
   var entry=page.items().getFirst();assertEquals("CALL",entry.type());assertNotNull(entry.callId());assertNotNull(entry.readAt());assertNull(entry.editedAt());
   assertEquals(409,assertThrows(com.couple.privatespace.identity.ApiFailure.class,()->chat.edit(f.aId,entry.id(),new com.couple.privatespace.chat.ChatService.Edit("fake",0L))).status());
   assertEquals(409,assertThrows(com.couple.privatespace.identity.ApiFailure.class,()->chat.delete(f.aId,entry.id())).status());
   assertEquals(409,assertThrows(com.couple.privatespace.identity.ApiFailure.class,()->chat.react(f.bId,entry.id(),new com.couple.privatespace.chat.ChatService.React("❤️"))).status());
   assertEquals(409,assertThrows(com.couple.privatespace.identity.ApiFailure.class,()->chat.send(f.bId,new com.couple.privatespace.chat.ChatService.Send(UUID.randomUUID(),"reply","TEXT",null,null,entry.id()))).status());
   callHistory.publish(ended);callHistory.retryPending();assertEquals(5,chat.history(f.aId,null,null).items().size());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE type='MESSAGE'",Integer.class));
   // Simulate a chat insert outage: call completion must commit, outbox must retry later.
   jdbc.execute("ALTER TABLE chat_message ADD CONSTRAINT chat07_failure CHECK(type<>'CALL') NOT VALID");UUID deferred;
   try{deferred=invite(a,"VIDEO");assertEquals("CANCELLED",ok(a,"cancel",ac,deferred,Map.of()).path("state").asText());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM call_chat_event WHERE published_at IS NULL",Integer.class));}finally{jdbc.execute("ALTER TABLE chat_message DROP CONSTRAINT chat07_failure");}
   callHistory.retryPending();assertEquals(6,chat.history(f.aId,null,null).items().size());assertEquals(6,chat.state(f.aId).latestSequence());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM call_chat_event WHERE published_at IS NULL",Integer.class));
   String json=f.b.get("/api/chat").body();assertFalse(json.contains("callerSocket"));assertFalse(json.contains("callerClient"));assertFalse(json.contains("negotiationId"));
   chat.hide(f.bId,entry.id());assertEquals(5,chat.history(f.bId,null,null).items().size());assertEquals(6,chat.history(f.aId,null,null).items().size());
  }
 }
 @Test void voiceLifecycleOnlyActivatesAfterBothConnectedAndPreservesOutcome()throws Exception{
  assertEquals(200,f.transport.new Browser().get("/call-media.js").statusCode(),"Shared device-setup module must load before a call can be answered");
  assertEquals(401,f.transport.new Browser().get("/api/calls/current").statusCode());assertEquals(401,f.transport.new Browser().get("/api/rtc/config").statusCode());
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VOICE");var ringing=f.transport.tree(f.a.get("/api/calls/"+id));assertEquals("VOICE",ringing.get("mode").asText());assertEquals(30,Duration.between(Instant.parse(ringing.get("createdAt").asText()),Instant.parse(ringing.get("ringDeadline").asText())).toSeconds());
   assertEquals("CONNECTING",ok(b,"accept",bc,id,Map.of()).get("state").asText());assertEquals("CALL_STATE",command(a,"connected",ac,id,Map.of()).get("code").asText());
   negotiate(a,b,id,UUID.randomUUID());assertEquals("CONNECTING",ok(a,"connected",ac,id,Map.of()).get("state").asText());assertEquals("ACTIVE",ok(b,"connected",bc,id,Map.of()).get("state").asText());
   assertEquals("ENDED",ok(b,"end",bc,id,Map.of()).get("state").asText());assertTrue(f.transport.tree(f.a.get("/api/calls/current")).get("call").isNull());
   var again=f.transport.new Browser();again.login(f.prefix+"a@example.invalid");var outcome=f.transport.tree(again.get("/api/calls/"+id));assertFalse(outcome.get("connectedAt").isNull());assertFalse(outcome.get("endedAt").isNull());assertEquals("ENDED",outcome.get("endReason").asText());
  }
 }
 @Test void offlineRejectsWithoutRowAndConcurrentInvitesNeverCreateTwoLiveCalls()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid")){
   assertEquals("PARTNER_OFFLINE",command(a,"invite",ac,null,Map.of("mode","VIDEO")).get("code").asText());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM call_session",Integer.class));
   try(var b=socket(f.b,f.prefix+"b@example.invalid")){
    var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
    try{var first=pool.submit(()->{start.await();return command(a,"invite",ac,null,Map.of("mode","VIDEO"));});var second=pool.submit(()->{start.await();return command(b,"invite",bc,null,Map.of("mode","VOICE"));});start.countDown();var x=first.get(10,TimeUnit.SECONDS);var y=second.get(10,TimeUnit.SECONDS);assertEquals(1,List.of(x,y).stream().filter(n->n.path("type").asText().equals("call.ack")).count());assertEquals(1,List.of(x,y).stream().filter(n->n.path("code").asText().equals("CALL_BUSY")).count());}finally{pool.shutdownNow();}
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM call_session WHERE ended_at IS NULL",Integer.class));
   }
  }
 }
 @Test void inviteRetryHasOneRowAndChangedIntentConflictsEvenAfterTerminal()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID key=UUID.randomUUID();var intent=Map.<String,Object>of("mode","VIDEO","eventId",key,"callerId",f.bId);var first=ok(a,"invite",ac,null,intent);UUID id=UUID.fromString(first.get("id").asText());assertEquals(f.aId.toString(),first.get("callerId").asText());
   for(int i=0;i<3;i++)assertEquals(first.get("id"),ok(a,"invite",ac,null,intent).get("id"));
   assertEquals("CALL_RETRY_CONFLICT",command(a,"invite",ac,null,Map.of("mode","VOICE","eventId",key)).get("code").asText());ok(a,"cancel",ac,id,Map.of());
   assertEquals("CANCELLED",ok(a,"invite",ac,null,intent).get("state").asText());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM call_session",Integer.class));
  }
 }
 @Test void acceptedTabOnlyGetsSdpAndOtherTabsCannotSignalOrSteal()throws Exception{
  var second=f.transport.new Browser();second.login(f.prefix+"b@example.invalid");UUID otherClient=UUID.randomUUID();
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid");var other=socket(second,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO");var accepted=ok(b,"accept",bc,id,Map.of());assertNotEquals(bc.toString(),accepted.get("calleeClient").asText());assertEquals(64,accepted.get("calleeClient").asText().length());assertEquals(409,second.post("/api/calls/"+id+"/leave",Map.of("clientId",otherClient,"eventId",UUID.randomUUID()),null).statusCode());assertEquals("CALL_ELSEWHERE",command(other,"accept",otherClient,id,Map.of()).get("code").asText());
   UUID n=UUID.randomUUID();ok(a,"offer",ac,id,Map.of("negotiationId",n,"sdp","v=0\r\ns=private-selected-session\r\n"));assertTrue(b.await("call.offer").contains("private-selected-session"));assertFalse(other.frames.stream().anyMatch(frame->frame.contains("private-selected-session")));
   assertEquals("CALL_ELSEWHERE",command(other,"answer",otherClient,id,Map.of("negotiationId",n,"sdp","v=0\r\n")).get("code").asText());assertEquals("CALL_ELSEWHERE",command(other,"resume",bc,id,Map.of()).get("code").asText());
   assertEquals("CALL_ROLE",command(b,"offer",bc,id,Map.of("negotiationId",n,"sdp","v=0\r\n")).get("code").asText());
  }
 }
 @Test void wrongRoleStaleNegotiationSpoofedIdsAndOversizedSignalsReject()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO");assertEquals("CALLEE_ONLY",command(a,"accept",ac,id,Map.of()).get("code").asText());ok(b,"accept",bc,id,Map.of());UUID n=UUID.randomUUID();
   ok(a,"offer",ac,id,Map.of("negotiationId",n,"sdp","v=0\r\n"));assertEquals("STALE_NEGOTIATION",command(b,"answer",bc,id,Map.of("negotiationId",UUID.randomUUID(),"sdp","v=0\r\n")).get("code").asText());
   assertEquals("INVALID_SDP",command(a,"offer",ac,id,Map.of("negotiationId",UUID.randomUUID(),"sdp","v=0"+"x".repeat(16000))).get("code").asText());
   assertEquals("CALL_NOT_FOUND",command(b,"end",bc,UUID.randomUUID(),Map.of()).get("code").asText());assertEquals(404,f.b.get("/api/calls/"+UUID.randomUUID()).statusCode());
   assertEquals("CALL_ELSEWHERE",command(b,"end",UUID.randomUUID(),id,Map.of()).get("code").asText());
  }
 }
 @Test void ringExpiryAndRecoveryCutoffAreAuthoritativeAndDoNotExtendOnRetry()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID missed=invite(a,"VOICE");jdbc.update("UPDATE call_session SET created_at=clock_timestamp()-INTERVAL '31 seconds',ring_deadline=clock_timestamp()-INTERVAL '1 second' WHERE id=?",missed);maintenance.runOnce();assertEquals("MISSED",f.transport.tree(f.a.get("/api/calls/"+missed)).get("state").asText());maintenance.runOnce();assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=? AND type='MISSED_CALL' AND target_id=?",Integer.class,f.bId,missed));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE recipient_id=? AND type='MISSED_CALL'",Integer.class,f.aId));
   UUID id=invite(a,"VIDEO");ok(b,"accept",bc,id,Map.of());negotiate(a,b,id,UUID.randomUUID());ok(a,"connected",ac,id,Map.of());ok(b,"connected",bc,id,Map.of());
   var recovering=ok(a,"disconnected",ac,id,Map.of());assertEquals("RECONNECTING",recovering.get("state").asText());assertEquals(recovering.get("recoveryDeadline"),ok(a,"disconnected",ac,id,Map.of()).get("recoveryDeadline"));
   jdbc.update("UPDATE call_session SET recovery_deadline=clock_timestamp()-INTERVAL '1 second' WHERE id=?",id);maintenance.runOnce();assertEquals("FAILED",f.transport.tree(f.b.get("/api/calls/"+id)).get("state").asText());assertEquals("FAILED",ok(b,"connected",bc,id,Map.of()).get("state").asText());
  }
 }
 @Test void databaseFailurePublishesNoCallAndSameInviteCanRetry()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID key=UUID.randomUUID();jdbc.execute("ALTER TABLE call_session ADD CONSTRAINT dev08_failure_fixture CHECK(mode<>'VOICE')");
   try{assertEquals("CALL_NOT_CONFIRMED",command(a,"invite",ac,null,Map.of("mode","VOICE","eventId",key)).get("code").asText());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM call_session",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM call_command_receipt",Integer.class));assertFalse(b.frames.stream().anyMatch(frame->frame.contains("call.updated")));}finally{jdbc.execute("ALTER TABLE call_session DROP CONSTRAINT dev08_failure_fixture");}
   assertEquals("RINGING",ok(a,"invite",ac,null,Map.of("mode","VOICE","eventId",key)).get("state").asText());
  }
 }
 @Test void logoutRevokesSelectedCallAndUnconfiguredRtcIsHonest()throws Exception{
  var config=f.a.get("/api/rtc/config");assertEquals(200,config.statusCode());assertTrue(config.headers().firstValue("Cache-Control").orElse("").contains("no-store"));assertFalse(f.transport.tree(config).get("relayConfigured").asBoolean());assertEquals(0,f.transport.tree(config).get("iceServers").size());
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO");ok(b,"accept",bc,id,Map.of());assertEquals(204,f.b.post("/api/auth/logout",Map.of(),null).statusCode());maintenance.runOnce();var state=f.transport.tree(f.a.get("/api/calls/"+id));assertEquals("FAILED",state.get("state").asText());assertEquals("SESSION_ENDED",state.get("endReason").asText());
  }
 }
 @Test void simultaneousAnswerHasOneWinnerAndThatClientCanResumeOnlyAfterOldSocketCloses()throws Exception{
  var second=f.transport.new Browser();second.login(f.prefix+"b@example.invalid");UUID otherClient=UUID.randomUUID();
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid");var other=socket(second,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO");var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);JsonNode x,y;
   try{var first=pool.submit(()->{start.await();return command(b,"accept",bc,id,Map.of());});var next=pool.submit(()->{start.await();return command(other,"accept",otherClient,id,Map.of());});start.countDown();x=first.get(10,TimeUnit.SECONDS);y=next.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
   assertEquals(1,List.of(x,y).stream().filter(n->n.path("type").asText().equals("call.ack")).count());assertEquals(1,List.of(x,y).stream().filter(n->n.path("code").asText().equals("CALL_ELSEWHERE")).count());
   boolean firstWins=x.path("type").asText().equals("call.ack");UUID selectedClient=firstWins?bc:otherClient;String oldSocket=jdbc.queryForObject("SELECT callee_socket FROM call_session WHERE id=?",String.class,id);(firstWins?b:other).close();
   long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<until){var user=registry.getUser(f.prefix+"b@example.invalid");if(user==null||user.getSessions().stream().noneMatch(s->s.getId().equals(oldSocket)))break;Thread.sleep(10);}
   try(var resumed=socket(firstWins?f.b:second,f.prefix+"b@example.invalid")){
    assertEquals("RECONNECTING",ok(resumed,"resume",selectedClient,id,Map.of()).get("state").asText());assertNotEquals(oldSocket,jdbc.queryForObject("SELECT callee_socket FROM call_session WHERE id=?",String.class,id));
    assertEquals(200,(firstWins?f.b:second).post("/api/calls/"+id+"/leave",Map.of("clientId",selectedClient,"eventId",UUID.randomUUID()),null).statusCode());assertEquals("ENDED",f.transport.tree(f.a.get("/api/calls/"+id)).get("state").asText());
   }
  }
 }
 @Test void selectedSocketDisconnectNeverFallsBackToOtherAccountTabsForSdp()throws Exception{
  var second=f.transport.new Browser();second.login(f.prefix+"b@example.invalid");
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid");var other=socket(second,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO");ok(b,"accept",bc,id,Map.of());String selected=jdbc.queryForObject("SELECT callee_socket FROM call_session WHERE id=?",String.class,id);b.close();
   long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean removed=false;while(System.nanoTime()<until){var user=registry.getUser(f.prefix+"b@example.invalid");if(user!=null&&user.getSessions().stream().noneMatch(s->s.getId().equals(selected))){removed=true;break;}Thread.sleep(10);}assertTrue(removed);
   ok(a,"offer",ac,id,Map.of("negotiationId",UUID.randomUUID(),"sdp","v=0\r\ns=must-not-fall-back\r\n"));Thread.sleep(350);assertFalse(other.frames.stream().anyMatch(frame->frame.contains("must-not-fall-back")),"Missing selected session must never broadcast SDP to the remaining tab");
  }
 }
 @Test void reactionsAreTransientNegotiationScopedAndReachOnlyTheSelectedPartnerTab()throws Exception{
  assertEquals(200,f.transport.new Browser().get("/call-reactions.js").statusCode());
  var second=f.transport.new Browser();second.login(f.prefix+"b@example.invalid");
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid");var other=socket(second,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO"),n=UUID.randomUUID(),key=UUID.randomUUID();
   assertEquals("CALL_STATE",command(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","❤️")).path("code").asText());
   ok(b,"accept",bc,id,Map.of());negotiate(a,b,id,n);
   assertEquals("CALL_STATE",command(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","❤️")).path("code").asText());
   ok(a,"connected",ac,id,Map.of());ok(b,"connected",bc,id,Map.of());
   assertEquals("CALL_ELSEWHERE",command(other,"reaction",UUID.randomUUID(),id,Map.of("negotiationId",n,"emoji","❤️")).path("code").asText());
   assertEquals("STALE_NEGOTIATION",command(a,"reaction",ac,id,Map.of("negotiationId",UUID.randomUUID(),"emoji","❤️")).path("code").asText());
   assertEquals("INVALID_CALL_REACTION",command(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","<script>bad</script>")).path("code").asText());
   long version=jdbc.queryForObject("SELECT version FROM call_session WHERE id=?",Long.class,id);
   var data=Map.<String,Object>of("eventId",key,"negotiationId",n,"emoji","❤️");ok(a,"reaction",ac,id,data);
   var event=f.frame(b,"call.reaction");assertEquals(id.toString(),event.path("callId").asText());assertEquals(n.toString(),event.path("negotiationId").asText());assertEquals("❤️",event.path("payload").path("emoji").asText());
   ok(a,"reaction",ac,id,data);Thread.sleep(150);assertFalse(b.frames.stream().anyMatch(frame->frame.contains("call.reaction")),"Same reaction event must not replay");assertFalse(other.frames.stream().anyMatch(frame->frame.contains("call.reaction")),"Only the accepted partner tab sees the reaction");
   assertEquals("CALL_RETRY_CONFLICT",command(a,"reaction",ac,id,Map.of("eventId",key,"negotiationId",n,"emoji","😂")).path("code").asText());
   ok(b,"reaction",bc,id,Map.of("negotiationId",n,"emoji","👏"));assertEquals("👏",f.frame(a,"call.reaction").path("payload").path("emoji").asText());
   assertEquals(version,jdbc.queryForObject("SELECT version FROM call_session WHERE id=?",Long.class,id));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM call_command_receipt WHERE operation='reaction'",Integer.class));
   ok(a,"end",ac,id,Map.of());assertEquals("ENDED",ok(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","❤️")).path("state").asText());Thread.sleep(100);assertFalse(b.frames.stream().anyMatch(frame->frame.contains("call.reaction")));
  }
 }
 @Test void reactionBurstIsBoundedWithoutEndingOrMutatingTheCall()throws Exception{
  try(var a=socket(f.a,f.prefix+"a@example.invalid");var b=socket(f.b,f.prefix+"b@example.invalid")){
   UUID id=invite(a,"VIDEO"),n=UUID.randomUUID();ok(b,"accept",bc,id,Map.of());negotiate(a,b,id,n);ok(a,"connected",ac,id,Map.of());ok(b,"connected",bc,id,Map.of());
   for(int i=0;i<6;i++)ok(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","🎉"));assertEquals("CALL_REACTION_LIMIT",command(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","🎉")).path("code").asText());assertEquals("ACTIVE",jdbc.queryForObject("SELECT state FROM call_session WHERE id=?",String.class,id));
   ok(a,"disconnected",ac,id,Map.of());assertEquals("CALL_STATE",command(a,"reaction",ac,id,Map.of("negotiationId",n,"emoji","🎉")).path("code").asText());ok(a,"end",ac,id,Map.of());
  }
 }
}
