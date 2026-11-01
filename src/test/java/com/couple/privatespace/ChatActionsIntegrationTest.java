package com.couple.privatespace;

import com.couple.privatespace.chat.ChatService;
import com.couple.privatespace.identity.ApiFailure;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class ChatActionsIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired ChatService chat;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){f.cleanup();}
 ChatService.Message send(UUID actor,String text,UUID reply){return chat.send(actor,new ChatService.Send(UUID.randomUUID(),text,"TEXT",null,null,reply));}
 ChatService.Message view(UUID actor,UUID id){return chat.snapshots(actor,List.of(id)).items().getFirst();}
 HttpResponse<String> action(PairingIntegrationTest.Browser browser,UUID id,String op,String method,Object body,boolean csrf)throws Exception{var request=HttpRequest.newBuilder(f.transport.uri("/api/chat/messages/"+id+"/"+op)).header("Content-Type","application/json");if(csrf)request.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(f.transport.json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 @Test void repliesReferenceSameSpaceVisibleMessageAndQuotesRedactAfterDeletion(){
  var original=send(f.aId,"private original 🌸",null);var reply=send(f.bId,"a reply",original.id());assertEquals(original.id(),reply.replyToId());assertEquals("private original 🌸",reply.reply().body());assertEquals(f.aId,reply.reply().senderId());
  chat.delete(f.aId,original.id());assertTrue(view(f.bId,reply.id()).reply().unavailable());assertNull(view(f.bId,reply.id()).reply().body());
  assertEquals(409,assertThrows(ApiFailure.class,()->send(f.bId,"not allowed",original.id())).status());assertEquals(409,assertThrows(ApiFailure.class,()->send(f.bId,"not allowed",UUID.randomUUID())).status());
 }
 @Test void reactionChangesReplaceOwnChoiceAndSameEmojiRetryAndRemovalAreIdempotent(){
  var message=send(f.aId,"reaction",null);var first=chat.react(f.bId,message.id(),new ChatService.React("❤️"));assertEquals("❤️",first.reactions().getFirst().emoji());long version=first.version();assertEquals(version,chat.react(f.bId,message.id(),new ChatService.React("❤️")).version());
  var changed=chat.react(f.bId,message.id(),new ChatService.React("😂"));assertEquals(1,changed.reactions().size());assertEquals("😂",changed.reactions().getFirst().emoji());assertTrue(changed.version()>version);
  chat.react(f.aId,message.id(),new ChatService.React("🥰"));assertEquals(2,view(f.aId,message.id()).reactions().size());chat.react(f.bId,message.id(),new ChatService.React(null));assertEquals(f.aId,view(f.aId,message.id()).reactions().getFirst().userId());chat.react(f.bId,message.id(),new ChatService.React(null));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM chat_message_reaction",Integer.class));
  assertEquals(400,assertThrows(ApiFailure.class,()->chat.react(f.aId,message.id(),new ChatService.React("💰"))).status());
 }
 @Test void hideIsOnlyForActorAndUpdatesUnreadHomeSnapshotsQuotesAndNotificationTarget()throws Exception{
  var message=send(f.aId,"only partner hides",null);assertEquals(1,chat.state(f.bId).unreadCount());String notice=f.transport.tree(f.b.get("/api/notifications")).get("items").get(0).get("id").asText();
  chat.hide(f.bId,message.id());chat.hide(f.bId,message.id());assertEquals(1,chat.history(f.aId,null,null).items().size());assertTrue(chat.history(f.bId,null,null).items().isEmpty());assertEquals(List.of(message.id()),chat.snapshots(f.bId,List.of(message.id())).removedIds());assertEquals(0,chat.state(f.bId).unreadCount());assertEquals(0,f.transport.tree(f.b.get("/api/home")).get("unreadMessages").asInt());assertFalse(f.transport.tree(f.b.get("/api/notifications/"+notice+"/target")).get("available").asBoolean());
  assertEquals(409,assertThrows(ApiFailure.class,()->send(f.bId,"reply to hidden",message.id())).status());var reply=send(f.aId,"own reply",message.id());assertEquals("only partner hides",reply.reply().body());assertTrue(view(f.bId,reply.id()).reply().unavailable());
 }
 @Test void senderOnlyDeleteScrubsPayloadAndReactionsWithoutRestoringOnRetry(){
  UUID key=UUID.randomUUID();var command=new ChatService.Send(key,"erase this secret","TEXT",null,null);var message=chat.send(f.aId,command);chat.react(f.bId,message.id(),new ChatService.React("👍"));
  assertEquals(403,assertThrows(ApiFailure.class,()->chat.delete(f.bId,message.id())).status());var deleted=chat.delete(f.aId,message.id());assertNotNull(deleted.deletedAt());assertEquals("",deleted.body());assertTrue(deleted.reactions().isEmpty());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message_reaction",Integer.class));assertEquals(deleted.version(),chat.delete(f.aId,message.id()).version());
  assertEquals("",chat.send(f.aId,command).body());assertNotNull(chat.send(f.aId,command).deletedAt());assertEquals(409,assertThrows(ApiFailure.class,()->chat.send(f.aId,new ChatService.Send(key,"changed content","TEXT",null,null))).status());assertEquals(409,assertThrows(ApiFailure.class,()->chat.react(f.bId,message.id(),new ChatService.React("❤️"))).status());assertEquals(0,chat.state(f.bId).unreadCount());
 }
 @Test void hiddenSendRetryIsRedactedAndGiftPayloadIsScrubbedFromHistoryAndQuotes(){
  UUID key=UUID.randomUUID();var gift=chat.gifts(f.aId,null,null,0).items().getFirst();var command=new ChatService.Send(key,"private note","GIFT",gift.code(),null);var message=chat.send(f.aId,command);var reply=send(f.bId,"thanks",message.id());chat.hide(f.aId,message.id());var hidden=chat.send(f.aId,command);assertTrue(hidden.hidden());assertEquals("",hidden.body());assertNull(hidden.giftAssetPath());assertTrue(chat.giftHistory(f.aId,"all",null).items().isEmpty());assertEquals(1,chat.giftHistory(f.bId,"all",null).items().size());
  // A fresh visible gift verifies true sender deletion independently of the hidden-view choice.
  var second=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"another note","GIFT",gift.code(),null));var quote=send(f.bId,"quoted sticker",second.id());chat.delete(f.aId,second.id());var tombstone=view(f.bId,second.id());assertNull(tombstone.giftCode());assertNull(tombstone.giftName());assertNull(tombstone.giftAssetPath());assertTrue(view(f.bId,quote.id()).reply().unavailable());assertEquals(1,chat.giftHistory(f.bId,"all",null).items().size());
 }
 @Test void replyIntentParticipatesInDedupAndCannotBeReassigned(){
  var first=send(f.aId,"one",null);var second=send(f.aId,"two",null);UUID key=UUID.randomUUID();var command=new ChatService.Send(key,"reply","TEXT",null,null,first.id());var reply=chat.send(f.bId,command);assertEquals(reply.id(),chat.send(f.bId,command).id());assertEquals(409,assertThrows(ApiFailure.class,()->chat.send(f.bId,new ChatService.Send(key,"reply","TEXT",null,null,second.id()))).status());chat.delete(f.aId,first.id());assertTrue(chat.send(f.bId,command).reply().unavailable());
 }
 @Test void oldMessageMutationsRecoverWithoutAdvancingMessageSequence(){
  var message=send(f.aId,"old",null);long sequence=chat.state(f.bId).latestSequence(),version=chat.state(f.bId).changeVersion();chat.react(f.aId,message.id(),new ChatService.React("😮"));assertEquals(sequence,chat.state(f.bId).latestSequence());assertTrue(chat.state(f.bId).changeVersion()>version);assertTrue(chat.history(f.bId,null,sequence).items().isEmpty());assertEquals("😮",view(f.bId,message.id()).reactions().getFirst().emoji());chat.delete(f.aId,message.id());assertNotNull(view(f.bId,message.id()).deletedAt());
 }
 @Test void httpEnforcesCookieCsrfMembershipAndNeverAcceptsForeignIds()throws Exception{
  var message=send(f.aId,"HTTP",null);assertEquals(401,action(f.transport.new Browser(),message.id(),"delete","POST",null,true).statusCode());assertEquals(403,action(f.a,message.id(),"delete","POST",null,false).statusCode());assertEquals(403,action(f.b,message.id(),"delete","POST",Map.of("actorId",f.aId),true).statusCode());assertEquals(200,action(f.b,message.id(),"reaction","PUT",Map.of("emoji","😢"),true).statusCode());assertEquals(400,action(f.a,message.id(),"reaction","PUT",Map.of("emoji","no"),true).statusCode());assertEquals(404,action(f.a,UUID.randomUUID(),"hide","POST",null,true).statusCode());assertEquals(403,f.a.get("/api/chat/messages?ids="+UUID.randomUUID()).statusCode());assertEquals(204,action(f.b,message.id(),"hide","POST",null,true).statusCode());
 }
 @Test void concurrentReactionAndDeletionSerializeAndNeverLeaveAnOrphanReaction()throws Exception{
  var message=send(f.aId,"race",null);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  try{var reaction=pool.submit(()->{start.await();try{chat.react(f.bId,message.id(),new ChatService.React("😂"));}catch(ApiFailure e){assertEquals(409,e.status());}return true;});var deletion=pool.submit(()->{start.await();chat.delete(f.aId,message.id());return true;});start.countDown();assertTrue(reaction.get(10,TimeUnit.SECONDS));assertTrue(deletion.get(10,TimeUnit.SECONDS));}finally{pool.shutdownNow();}assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message_reaction",Integer.class));assertNotNull(view(f.bId,message.id()).deletedAt());
 }
 @Test void mutationFailureRollsBackPayloadReactionAndVersionAndLiveEventsArePrivate()throws Exception{
  var message=send(f.aId,"keep on failure",null);chat.react(f.bId,message.id(),new ChatService.React("❤️"));long version=chat.state(f.aId).changeVersion();jdbc.execute("ALTER TABLE chat_message ADD CONSTRAINT chat02_failure CHECK(body<>'')");try{assertThrows(RuntimeException.class,()->chat.delete(f.aId,message.id()));assertEquals("keep on failure",view(f.bId,message.id()).body());assertEquals(1,view(f.bId,message.id()).reactions().size());assertEquals(version,chat.state(f.aId).changeVersion());}finally{jdbc.execute("ALTER TABLE chat_message DROP CONSTRAINT chat02_failure");}
  try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
   chat.react(f.bId,message.id(),new ChatService.React("🥰"));assertTrue(a.await("message.reacted").contains("\"message\":null"));assertTrue(b.await("message.reacted").contains("\"message\":null"));chat.hide(f.bId,message.id());b.await("message.hidden");assertFalse(a.frames.stream().anyMatch(frame->frame.contains("message.hidden")));
  }
 }
 @Test void stompReplyUsesServerTargetAndRejectsUnavailableOriginal()throws Exception{
  var original=send(f.aId,"reply transport 🌸",null);
  try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
   UUID key=UUID.randomUUID();f.command(b,"chat.send",Map.of("clientMessageId",key,"body","actual STOMP reply","replyToId",original.id()));var ack=f.frame(b,"chat.sent");assertEquals(original.id().toString(),ack.get("message").get("replyToId").asText());assertEquals("reply transport 🌸",ack.get("message").get("reply").get("body").asText());
   var event=f.frame(a,"message.created");assertEquals(original.id().toString(),event.get("message").get("replyToId").asText());assertTrue(event.get("message").get("reply").isNull(),"Shared event must not carry actor-specific quote text");
   f.command(b,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"body","not saved","replyToId",UUID.randomUUID()));assertEquals("REPLY_UNAVAILABLE",f.frame(b,"chat.error").get("code").asText());assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=?",Integer.class,f.space));
  }
 }

}
