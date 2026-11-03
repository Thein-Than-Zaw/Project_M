package com.couple.privatespace;

import com.couple.privatespace.game.*;
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
class GameIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;@Autowired GameService games;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;jdbc.update("DELETE FROM connect_four_command WHERE space_id=?",f.space);jdbc.update("DELETE FROM couple_connect_four WHERE space_id=?",f.space);f.cleanup();}
 GameService.Game command(UUID actor,String op,GameService.Game g,Integer column){return games.command(actor,op,new GameService.Command(UUID.randomUUID(),g==null?null:g.id(),g==null?null:g.version(),column)).game();}
 GameService.Game start(){var invited=command(f.aId,"invite",null,null);return command(f.bId,"accept",invited,null);}
 @Test void invitationRequiresPartnerAcceptAndRetriesAreIdempotent(){
  assertNull(games.current(f.aId).game());UUID key=UUID.randomUUID();var c=new GameService.Command(key,null,null,null);var invited=games.command(f.aId,"invite",c).game();assertEquals("WAITING",invited.status());assertEquals(invited.id(),games.command(f.aId,"invite",c).game().id());assertEquals(403,assertThrows(ApiFailure.class,()->command(f.aId,"accept",invited,null)).status());assertEquals(403,assertThrows(ApiFailure.class,()->command(f.bId,"cancel",invited,null)).status());
  var active=command(f.bId,"accept",invited,null);assertEquals("ACTIVE",active.status());assertEquals(active.redId(),active.nextPlayerId());assertEquals(games.current(f.aId).game(),games.current(f.bId).game());assertEquals(409,assertThrows(ApiFailure.class,()->command(f.aId,"invite",active,null)).status());
  UUID moveId=UUID.randomUUID();var move=new GameService.Command(moveId,active.id(),active.version(),2);var after=games.command(active.nextPlayerId(),"move",move).game();assertEquals(1,after.moves());assertEquals('R',after.board().charAt(37));assertEquals(after,games.command(active.nextPlayerId(),"move",move).game());assertEquals(409,assertThrows(ApiFailure.class,()->games.command(active.nextPlayerId(),"move",new GameService.Command(moveId,active.id(),active.version(),3))).status());
 }
 @Test void winFreezesBoardAndAcceptedRematchesAlternateFirstPlayer(){
  var game=start();UUID first=game.redId();for(int i=0;i<7;i++)game=command(game.nextPlayerId(),"move",game,i%2==0?0:1);assertEquals("WON",game.status());assertEquals(first,game.winnerId());assertEquals(4,game.winningCells().size());var won=game;
  assertEquals(409,assertThrows(ApiFailure.class,()->command(f.aId,"move",won,2)).status());var rematch=command(f.bId,"invite",won,null);assertNotEquals(first,rematch.redId());var active=command(f.aId,"accept",rematch,null);assertEquals(rematch.redId(),active.nextPlayerId());var resigned=command(active.redId(),"resign",active,null);assertEquals("RESIGNED",resigned.status());assertEquals(active.yellowId(),resigned.winnerId());
 }
 @Test void cancelledAndDeclinedInvitationsDoNotAdvanceAcceptedFirstPlayer(){
  var active=start();UUID first=active.redId();var ended=command(f.aId,"resign",active,null);var waiting=command(f.aId,"invite",ended,null);var cancelled=command(f.aId,"cancel",waiting,null);var second=command(f.aId,"invite",cancelled,null);assertEquals(waiting.redId(),second.redId());assertNotEquals(first,second.redId());var declined=command(f.bId,"decline",second,null);assertEquals("DECLINED",declined.status());
 }
 @Test void serverEnforcesTurnColumnScopeVersionAndFullColumns(){
  var g=start();UUID other=g.nextPlayerId().equals(f.aId)?f.bId:f.aId;assertEquals(409,assertThrows(ApiFailure.class,()->command(other,"move",g,1)).status());assertEquals(400,assertThrows(ApiFailure.class,()->command(g.nextPlayerId(),"move",g,7)).status());
  assertEquals(409,assertThrows(ApiFailure.class,()->games.command(g.nextPlayerId(),"move",new GameService.Command(UUID.randomUUID(),UUID.randomUUID(),g.version(),0))).status());
  var current=g;for(int i=0;i<6;i++)current=command(current.nextPlayerId(),"move",current,0);var full=current;assertEquals(409,assertThrows(ApiFailure.class,()->command(full.nextPlayerId(),"move",full,0)).status());assertEquals(6,games.current(f.aId).game().moves());assertEquals(409,assertThrows(ApiFailure.class,()->command(g.nextPlayerId(),"move",g,2)).status());
 }
 @Test void concurrentTabMovesAtSameVersionCommitOnlyOneDisc()throws Exception{
  var game=start();var pool=Executors.newFixedThreadPool(2);var latch=new CountDownLatch(1);var results=new ArrayList<Future<Boolean>>();
  try{for(int c:new int[]{2,3})results.add(pool.submit(()->{latch.await();try{command(game.nextPlayerId(),"move",game,c);return true;}catch(ApiFailure error){assertEquals(409,error.status());return false;}}));latch.countDown();int committed=0;for(var result:results)if(result.get(10,TimeUnit.SECONDS))committed++;assertEquals(1,committed);}finally{pool.shutdownNow();}assertEquals(1,games.current(f.aId).game().moves());
 }
 @Test void nativeFullBoardDrawSurvivesReloadWithoutWinningCells(){
  int[] seq={3,4,3,0,4,0,2,4,5,3,5,6,4,2,1,5,3,3,2,4,4,5,3,5,5,0,2,6,2,1,1,2,0,0,6,6,0,1,1,1,6,6};var g=start();
  for(int i=0;i<42;i++){if(i%10==0)jdbc.update("UPDATE connect_four_command SET created_at=clock_timestamp()-INTERVAL '1 minute' WHERE space_id=?",f.space);g=command(g.nextPlayerId(),"move",g,seq[i]);}assertEquals("DRAW",g.status());assertNull(g.winnerId());assertNull(g.nextPlayerId());assertTrue(g.winningCells().isEmpty());assertEquals(g,games.current(f.bId).game());
 }
 HttpResponse<String> post(PairingIntegrationTest.Browser browser,String action,Object body,boolean csrf)throws Exception{var r=HttpRequest.newBuilder(f.transport.uri("/api/games/connect4/"+action)).header("Content-Type","application/json");if(csrf)r.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(r.POST(HttpRequest.BodyPublishers.ofString(f.transport.json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 @Test void httpCookieCsrfAndPrivateWebsocketInvalidation()throws Exception{
  assertEquals(401,f.transport.new Browser().get("/api/games/connect4").statusCode());var intent=Map.of("actionId",UUID.randomUUID());assertEquals(403,post(f.a,"invite",intent,false).statusCode());
  try(var socket=f.socket(f.b,f.prefix+"b@example.invalid")){
   socket.send("SUBSCRIBE\nid:games\ndestination:/user/queue/pairing\n\n\0");long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<deadline){var user=registry.getUser(f.prefix+"b@example.invalid");if(user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getId().equals("games"))))break;Thread.sleep(10);}
   assertEquals(200,post(f.a,"invite",intent,true).statusCode());String event=socket.await("game.updated");assertFalse(event.contains("board"));assertEquals(200,f.b.get("/api/games/connect4").statusCode());
   var waiting=games.current(f.aId).game();var accept=Map.of("actionId",UUID.randomUUID(),"gameId",waiting.id(),"version",waiting.version(),"actorId",f.bId);assertEquals(403,post(f.a,"accept",accept,true).statusCode());assertEquals(200,post(f.b,"accept",accept,true).statusCode());
   var active=games.current(f.aId).game();var browser=active.nextPlayerId().equals(f.aId)?f.a:f.b;assertEquals(200,post(browser,"move",Map.of("actionId",UUID.randomUUID(),"gameId",active.id(),"version",active.version(),"column",4),true).statusCode());assertEquals(1,f.transport.tree(f.b.get("/api/games/connect4")).path("game").path("moves").asInt());
  }
 }
 @Test void unpairedAccountCannotReadOrStartGameEvenWithAValidSession()throws Exception{
  jdbc.update("DELETE FROM couple_membership WHERE space_id=? AND user_id=?",f.space,f.bId);assertEquals(403,f.a.get("/api/games/connect4").statusCode());assertEquals(403,post(f.a,"invite",Map.of("actionId",UUID.randomUUID()),true).statusCode());assertNull(jdbc.queryForObject("SELECT max(version) FROM couple_connect_four WHERE space_id=?",Long.class,f.space));
 }
 @Test void failedCommitRollsBackBoardReceiptAndEventAndDurableRateGuardApplies(){
  var g=start();long version=g.version();jdbc.execute("ALTER TABLE couple_connect_four ADD CONSTRAINT game01_failure CHECK(moves=0)");try{assertThrows(RuntimeException.class,()->command(g.nextPlayerId(),"move",g,0));assertEquals(version,games.current(f.aId).game().version());}finally{jdbc.execute("ALTER TABLE couple_connect_four DROP CONSTRAINT game01_failure");}
  assertEquals(0,games.current(f.aId).game().moves());for(int i=0;i<20;i++)jdbc.update("INSERT INTO connect_four_command(space_id,actor_id,action_id,request_hash) VALUES (?,?,?,?)",f.space,g.nextPlayerId(),UUID.randomUUID(),"0".repeat(64));assertEquals(429,assertThrows(ApiFailure.class,()->command(g.nextPlayerId(),"move",g,0)).status());
 }
}
