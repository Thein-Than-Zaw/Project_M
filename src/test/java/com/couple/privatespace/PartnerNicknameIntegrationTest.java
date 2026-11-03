package com.couple.privatespace;
import com.couple.privatespace.home.*;
import com.couple.privatespace.pairing.PairingService;
import com.couple.privatespace.chat.ChatService;
import com.couple.privatespace.game.GameService;
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
class PartnerNicknameIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;@Autowired PartnerNicknameService nicknames;@Autowired PairingService pairing;@Autowired HomeService home;@Autowired ChatService chat;@Autowired GameService games;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){f.cleanup();}
 @Test void ownAliasAppearsAcrossPrivateViewsAndNeverRenamesOrLeaksToPartner()throws Exception{
  String secret="My private nickname 🌸";var saved=nicknames.edit(f.aId,new PartnerNicknameService.Edit(secret,0L));assertEquals(f.bId,saved.partnerId());assertEquals("Recipient",saved.profileName());assertEquals(secret,chat.history(f.aId,null,null).partner().displayName());assertEquals(secret,pairing.current(f.aId).members().get(1).displayName());assertEquals(secret,home.home(f.aId).members().get(1).displayName());assertEquals(secret,games.current(f.aId).members().get(1).displayName());
  assertEquals("Recipient",home.home(f.bId).self().displayName());assertNull(nicknames.current(f.bId).nickname());assertFalse(f.b.get("/api/home").body().contains(secret));assertFalse(f.b.get("/api/games/connect4").body().contains(secret));assertEquals("Recipient",jdbc.queryForObject("SELECT display_name FROM app_user WHERE id=?",String.class,f.bId));
  nicknames.edit(f.bId,new PartnerNicknameService.Edit("Their private name for me",0L));assertEquals("Sender",home.home(f.aId).self().displayName());assertEquals(secret,nicknames.current(f.aId).nickname());
 }
 @Test void clearRestoresCurrentProfileNameAndVersionsPreventSilentOverwrite(){
  var first=nicknames.edit(f.aId,new PartnerNicknameService.Edit("Sweetheart",0L));assertEquals(1,first.version());assertEquals(1,nicknames.edit(f.aId,new PartnerNicknameService.Edit("Sweetheart",1L)).version());assertEquals(409,assertThrows(ApiFailure.class,()->nicknames.edit(f.aId,new PartnerNicknameService.Edit("old draft",0L))).status());
  var clear=nicknames.edit(f.aId,new PartnerNicknameService.Edit("",1L));assertNull(clear.nickname());assertEquals(2,clear.version());assertEquals("Recipient",chat.history(f.aId,null,null).partner().displayName());assertEquals(409,assertThrows(ApiFailure.class,()->nicknames.edit(f.aId,new PartnerNicknameService.Edit("stale zero",0L))).status());
  for(String invalid:List.of("🌸".repeat(61),"line\nbreak","bad\u0000name","\uD800"))assertEquals(400,assertThrows(ApiFailure.class,()->nicknames.edit(f.aId,new PartnerNicknameService.Edit(invalid,2L))).status());assertEquals("🌸".repeat(60),nicknames.edit(f.aId,new PartnerNicknameService.Edit("🌸".repeat(60),2L)).nickname());
 }
 HttpResponse<String> put(PairingIntegrationTest.Browser browser,Object body,boolean csrf)throws Exception{var r=HttpRequest.newBuilder(f.transport.uri("/api/partner-nickname")).header("Content-Type","application/json");if(csrf)r.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(r.PUT(HttpRequest.BodyPublishers.ofString(f.transport.json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 @Test void httpProtectsNicknameOwnerWithCookieCsrfAndIgnoresSpoofedOwner()throws Exception{
  assertEquals(401,f.transport.new Browser().get("/api/partner-nickname").statusCode());var body=Map.of("nickname","Private label","version",0,"ownerId",f.bId,"partnerId",f.aId);assertEquals(403,put(f.a,body,false).statusCode());assertEquals(401,put(f.transport.new Browser(),body,true).statusCode());assertEquals(200,put(f.a,body,true).statusCode());assertNull(nicknames.current(f.bId).nickname());assertEquals("Private label",f.transport.tree(f.a.get("/api/partner-nickname")).path("nickname").asText());
 }
 @Test void concurrentOwnEditsAtTheSameVersionSaveOnlyOneLabel()throws Exception{
  var pool=Executors.newFixedThreadPool(2);var latch=new CountDownLatch(1);try{var one=pool.submit(()->race(latch,"One"));var two=pool.submit(()->race(latch,"Two"));latch.countDown();assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));assertEquals(1,nicknames.current(f.aId).version());}finally{pool.shutdownNow();}
 }
 boolean race(CountDownLatch start,String name)throws Exception{start.await();try{nicknames.edit(f.aId,new PartnerNicknameService.Edit(name,0L));return true;}catch(ApiFailure error){assertEquals(409,error.status());return false;}}
 @Test void privateNicknameEventReachesOwnerOnlyAndCarriesNoLabel()throws Exception{
  try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
   a.send("SUBSCRIBE\nid:labels\ndestination:/user/queue/pairing\n\n\0");b.send("SUBSCRIBE\nid:labels\ndestination:/user/queue/pairing\n\n\0");long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<deadline){long count=registry.getUsers().stream().flatMap(u->u.getSessions().stream()).flatMap(s->s.getSubscriptions().stream()).filter(sub->sub.getId().equals("labels")).count();if(count==2)break;Thread.sleep(10);}
   nicknames.edit(f.aId,new PartnerNicknameService.Edit("Owner-only secret",0L));String event=a.await("partner.nickname.updated");assertFalse(event.contains("Owner-only secret"));assertFalse(b.frames.stream().anyMatch(frame->frame.contains("partner.nickname.updated")));
  }
 }
 @Test void unpairedAccountCannotReadOrWritePrivateLabel()throws Exception{jdbc.update("DELETE FROM couple_membership WHERE space_id=? AND user_id=?",f.space,f.bId);assertEquals(403,f.a.get("/api/partner-nickname").statusCode());assertEquals(403,put(f.a,Map.of("nickname","No partner","version",0),true).statusCode());}
}
