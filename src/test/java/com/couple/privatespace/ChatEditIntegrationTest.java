package com.couple.privatespace;

import com.couple.privatespace.chat.*;
import com.couple.privatespace.home.HomeService;
import com.couple.privatespace.identity.ApiFailure;
import java.net.http.*;
import java.util.*;
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
class ChatEditIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;
 @Autowired ChatService chat;@Autowired HomeService home;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){f.cleanup();}
 ChatService.Message view(UUID actor,UUID id){return chat.snapshots(actor,List.of(id)).items().getFirst();}
 @Test void editOldOwnMessageKeepsOrderReceiptsAndRetryIdentityAndRefreshesQuotes()throws Exception{
  var command=new ChatService.Send(UUID.randomUUID(),"original","TEXT",null,null);var original=chat.send(f.aId,command);
  jdbc.update("UPDATE chat_message SET created_at=created_at-INTERVAL '400 days' WHERE id=?",original.id());original=view(f.aId,original.id());
  chat.acknowledge(f.bId,new ChatService.Ack(List.of(original.id())),true);
  var reply=chat.send(f.bId,new ChatService.Send(UUID.randomUUID(),"reply","TEXT",null,null,original.id()));
  UUID id=original.id();var before=chat.state(f.aId);var saved=chat.edit(f.aId,id,new ChatService.Edit("မြန်မာ 🌸 <script>plain text</script>",original.version()));
  assertNotNull(saved.editedAt());assertEquals(original.createdAt(),saved.createdAt());assertEquals(original.sequence(),saved.sequence());assertNotNull(saved.readAt());
  assertEquals(before.latestSequence(),chat.state(f.aId).latestSequence());assertTrue(chat.state(f.aId).changeVersion()>before.changeVersion());
  assertEquals(saved.body(),view(f.bId,reply.id()).reply().body());assertEquals(saved.body(),chat.send(f.aId,command).body());
  var same=chat.edit(f.aId,id,new ChatService.Edit(saved.body(),saved.version()));assertEquals(saved.version(),same.version());assertEquals(saved.editedAt(),same.editedAt());
  assertEquals(409,assertThrows(ApiFailure.class,()->chat.edit(f.aId,id,new ChatService.Edit("stale",0L))).status());
  assertEquals(2,home.home(f.aId).activity().messageCount());assertEquals(home.home(f.aId).activity(),chat.history(f.bId,null,null).activity());
  chat.delete(f.aId,id);assertNull(view(f.bId,id).editedAt());assertEquals(409,assertThrows(ApiFailure.class,()->chat.edit(f.aId,id,new ChatService.Edit("restore",saved.version()))).status());
 }
 @Test void ownershipScopeUnicodeAndGiftNoteLimitsAreEnforced(){
  var text=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"text","TEXT",null,null));
  assertEquals(403,assertThrows(ApiFailure.class,()->chat.edit(f.bId,text.id(),new ChatService.Edit("spoof",0L))).status());
  assertEquals(404,assertThrows(ApiFailure.class,()->chat.edit(f.aId,UUID.randomUUID(),new ChatService.Edit("foreign",0L))).status());
  for(String invalid:List.of("\u00a0\uFEFF","a".repeat(4001),"bad\0text","\uD800"))assertEquals(400,assertThrows(ApiFailure.class,()->chat.edit(f.aId,text.id(),new ChatService.Edit(invalid,0L))).status());
  var max=chat.edit(f.aId,text.id(),new ChatService.Edit("🌸".repeat(4000),0L));assertEquals(4000,max.body().codePointCount(0,max.body().length()));
  var gift=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"note","GIFT",chat.gifts(f.aId,null,null,0).items().getFirst().code(),null));
  assertEquals(400,assertThrows(ApiFailure.class,()->chat.edit(f.aId,gift.id(),new ChatService.Edit("a".repeat(501),0L))).status());assertEquals("",chat.edit(f.aId,gift.id(),new ChatService.Edit("",0L)).body());
 }
 HttpResponse<String> patch(PairingIntegrationTest.Browser browser,UUID id,boolean csrf)throws Exception{
  var request=HttpRequest.newBuilder(f.transport.uri("/api/chat/messages/"+id)).header("Content-Type","application/json");if(csrf)request.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(request.method("PATCH",HttpRequest.BodyPublishers.ofString("{\"body\":\"HTTP edit\",\"version\":0}")).build(),HttpResponse.BodyHandlers.ofString());
 }
 @Test void realHttpCsrfAndPrivateStompInvalidation()throws Exception{
  var message=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"before","TEXT",null,null));
  assertEquals(401,patch(f.transport.new Browser(),message.id(),true).statusCode());assertEquals(403,patch(f.a,message.id(),false).statusCode());assertEquals(403,patch(f.b,message.id(),true).statusCode());
  try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
   var result=patch(f.a,message.id(),true);assertEquals(200,result.statusCode());assertFalse(f.transport.tree(result).path("editedAt").isNull());
   assertTrue(a.await("message.edited").contains("\"message\":null"));assertTrue(b.await("message.edited").contains("\"message\":null"));
  }
 }
}
