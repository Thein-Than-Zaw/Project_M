package com.couple.privatespace;

import com.couple.privatespace.identity.RegistrationService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
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
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** PAIR-01–03/AC-F02-01–06: real PostgreSQL, independent cookie clients and native STOMP sockets. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class PairingIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SimpUserRegistry userRegistry;
    final JsonMapper json=new JsonMapper();
    final UUID aId=UUID.randomUUID(),bId=UUID.randomUUID();
    final String prefix="dev03-"+UUID.randomUUID();
    final String password="Synthetic pairing password 2026!";
    Browser a,b;
    @BeforeEach void fixtures()throws Exception{
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class),"Empty dedicated test database required");
        String hash=encoder.encode(password);
        jdbc.update("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES (?,1,'Creator',?,?,DATE '2020-01-01'),(?,2,'Partner',?,?,DATE '2020-02-01')",aId,prefix+"a@example.invalid",hash,bId,prefix+"b@example.invalid",hash);
        a=new Browser();b=new Browser();
        assertEquals(200,a.login(prefix+"a@example.invalid").statusCode());
        assertEquals(200,b.login(prefix+"b@example.invalid").statusCode());
    }
    @AfterEach void cleanup(){
        if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
        var spaces=jdbc.query("SELECT DISTINCT space_id FROM couple_membership WHERE user_id IN (?,?)",(r,n)->r.getObject(1,UUID.class),aId,bId);
        jdbc.update("DELETE FROM pairing_join_receipt WHERE user_id IN (?,?)",aId,bId);
        jdbc.update("DELETE FROM pairing_confirmation WHERE user_id IN (?,?)",aId,bId);
        for(var space:spaces){jdbc.update("DELETE FROM pairing_invitation WHERE space_id=?",space);jdbc.update("DELETE FROM couple_membership WHERE space_id=?",space);jdbc.update("DELETE FROM couple_space WHERE id=?",space);}
        jdbc.update("DELETE FROM registration_token WHERE issued_by IN (?,?)",aId,bId);
        jdbc.update("DELETE FROM app_user WHERE id IN (?,?)",aId,bId);
    }
    @Test void oneTimePairingPreviewEventLogoutAndRelogin()throws Exception{
        assertEquals(401,new Browser().get("/api/space").statusCode());
        assertEquals(403,b.get("/api/home").statusCode());
        assertEquals(403,b.get("/home.html").statusCode());
        assertEquals("UNPAIRED",tree(a.get("/api/space")).get("state").asText());
        assertEquals(400,a.post("/api/spaces",Map.of("relationshipDate",LocalDate.now(ZoneId.of("Asia/Yangon")).plusDays(1).toString(),"confirmed",true),null).statusCode());
        var created=tree(a.post("/api/spaces",createBody(),null));
        assertEquals("WAITING",created.get("state").asText());assertEquals("Asia/Yangon",created.get("timezone").asText());
        String spaceId=created.get("id").asText();
        assertEquals(spaceId,tree(a.post("/api/spaces",createBody(),null)).get("id").asText());
        assertEquals(409,b.post("/api/spaces",createBody(),null).statusCode());
        String code=code(a);var preview=preview(b,code);assertEquals("2020-02-01",preview.get("signupRelationshipDate").asText());assertEquals("2020-01-01",preview.get("relationshipDate").asText());
        assertEquals(401,new Browser().post("/api/invitations/join",Map.of(),UUID.randomUUID()).statusCode());
        try(var socket=a.socket(true,"http://127.0.0.1:18080")){
            socket.await("CONNECTED");socket.send("SUBSCRIBE\nid:own\ndestination:/user/queue/pairing\n\n\0");
            awaitSubscription(prefix+"a@example.invalid");
            Map<String,Object> join=joinBody(code,preview);UUID key=UUID.randomUUID();
            var wrong=new HashMap<>(join);wrong.put("confirmedRelationshipDate","2020-02-01");
            assertEquals(409,b.post("/api/invitations/join",wrong,UUID.randomUUID()).statusCode());
            var unconfirmed=new HashMap<>(join);unconfirmed.put("confirmed",false);
            assertEquals(400,b.post("/api/invitations/join",unconfirmed,UUID.randomUUID()).statusCode());
            var joined=b.post("/api/invitations/join",join,key);assertEquals(200,joined.statusCode(),joined.body());
            assertEquals("ACTIVE",tree(joined).get("state").asText());
            assertTrue(socket.await("pairing.updated").contains("ACTIVE"),"Actual AFTER_COMMIT STOMP event, not REST poll");
            assertEquals(200,b.post("/api/invitations/join",join,key).statusCode());
            assertEquals(409,b.post("/api/invitations/join",join,UUID.randomUUID()).statusCode());
            var changed=new HashMap<>(join);changed.put("confirmedRelationshipDate","2020-01-02");
            assertEquals(409,b.post("/api/invitations/join",changed,key).statusCode());
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM couple_membership",Integer.class));
            assertEquals("2020-02-01",jdbc.queryForObject("SELECT signup_relationship_date::text FROM app_user WHERE id=?",String.class,bId));
            assertEquals(409,a.post("/api/invitations",Map.of(),null).statusCode());
            assertEquals(204,a.post("/api/auth/logout",Map.of(),null).statusCode());
            assertNotNull(socket.closed.get(5,TimeUnit.SECONDS),"Logout must close already-subscribed socket");
        }
        var relogin=a.login(prefix+"a@example.invalid");assertTrue(relogin.body().contains("/home.html"));
        assertEquals(spaceId,tree(a.get("/api/home")).get("id").asText());assertEquals(200,a.get("/home.html").statusCode());
        assertEquals("ACTIVE",tree(b.get("/api/space")).get("state").asText());
    }
    @Test void regenerationExpiryStalePreviewAndServerScope()throws Exception{
        a.post("/api/spaces",createBody(),null);String old=code(a);var oldPreview=preview(b,old);String fresh=code(a);
        assertEquals(400,b.post("/api/invitations/preview",Map.of("code",old),null).statusCode());
        assertEquals(400,b.post("/api/invitations/join",joinBody(old,oldPreview),UUID.randomUUID()).statusCode());
        assertEquals(409,a.post("/api/invitations/preview",Map.of("code",fresh),null).statusCode());
        assertEquals(403,b.get("/api/spaces/"+UUID.randomUUID()).statusCode());
        var unpaired=tree(b.get("/api/space?spaceId="+UUID.randomUUID()));assertTrue(unpaired.get("members").isEmpty());assertTrue(unpaired.get("id").isNull());
        jdbc.update("UPDATE pairing_invitation SET created_at=CURRENT_TIMESTAMP-INTERVAL '2 days',expires_at=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE token_hash=?",RegistrationService.digest(fresh));
        assertEquals(400,b.post("/api/invitations/preview",Map.of("code",fresh),null).statusCode());
        String current=code(a);var first=preview(b,current);var second=preview(b,current);
        assertEquals(409,b.post("/api/invitations/join",joinBody(current,first),UUID.randomUUID()).statusCode());
        jdbc.update("UPDATE couple_space SET version=version+1,relationship_date=DATE '2020-01-02'");
        assertEquals(409,b.post("/api/invitations/join",joinBody(current,second),UUID.randomUUID()).statusCode());
        var spoof=joinBody(current,preview(b,current));spoof.put("userId",aId.toString());spoof.put("memberSlot",1);
        assertEquals(200,b.post("/api/invitations/join",spoof,UUID.randomUUID()).statusCode());
        assertEquals(bId,jdbc.queryForObject("SELECT user_id FROM couple_membership WHERE member_slot=2",UUID.class));
    }
    @Test void concurrentJoinHasOneEffectAndSameIntentReplayIsDurable()throws Exception{
        a.post("/api/spaces",createBody(),null);String code=code(a);var join=joinBody(code,preview(b,code));
        Browser other=new Browser();other.login(prefix+"b@example.invalid");UUID key=UUID.randomUUID();
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{
            var first=pool.submit(()->{start.await();return b.post("/api/invitations/join",join,key);});
            var second=pool.submit(()->{start.await();return other.post("/api/invitations/join",join,key);});start.countDown();
            assertEquals(200,first.get(20,TimeUnit.SECONDS).statusCode());assertEquals(200,second.get(20,TimeUnit.SECONDS).statusCode());
        }finally{pool.shutdownNow();}
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM couple_membership",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pairing_join_receipt",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pairing_invitation WHERE consumed_at IS NOT NULL",Integer.class));
        assertEquals(409,other.post("/api/invitations/join",join,UUID.randomUUID()).statusCode());
    }
    @Test void invitationExpiringWhileWaitingForLockCannotBeConsumed()throws Exception{
        a.post("/api/spaces",createBody(),null);String code=code(a);var body=joinBody(code,preview(b,code));
        var pool=Executors.newSingleThreadExecutor();
        try(var blocker=jdbc.getDataSource().getConnection()){
            blocker.setAutoCommit(false);blocker.createStatement().executeQuery("SELECT id FROM registration_gate WHERE id=1 FOR UPDATE").close();
            jdbc.update("UPDATE pairing_invitation SET expires_at=clock_timestamp()+INTERVAL '1 second' WHERE token_hash=?",RegistrationService.digest(code));
            var attempt=pool.submit(()->b.post("/api/invitations/join",body,UUID.randomUUID()));
            Thread.sleep(1400);blocker.commit();
            assertEquals(400,attempt.get(10,TimeUnit.SECONDS).statusCode());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM couple_membership",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM pairing_join_receipt",Integer.class));
        }finally{pool.shutdownNow();}
    }
    @Test void websocketRequiresSessionOriginCsrfAndExactOwnSubscription()throws Exception{
        a.post("/api/spaces",createBody(),null);
        assertThrows(Exception.class,()->new Browser().socket(true,"http://127.0.0.1:18080"));
        assertThrows(Exception.class,()->a.socket(true,"https://evil.invalid"));
        try(var noCsrf=a.socket(false,"http://127.0.0.1:18080")){assertTrue(noCsrf.await("ERROR").contains("ERROR"));}
        try(var nonmember=b.socket(true,"http://127.0.0.1:18080")){
            nonmember.await("CONNECTED");nonmember.send("SUBSCRIBE\nid:no\ndestination:/user/queue/pairing\n\n\0");
            assertNotNull(nonmember.closed.get(5,TimeUnit.SECONDS),"Non-member subscription must close the socket");
            assertFalse(nonmember.frames.stream().anyMatch(frame->frame.startsWith("MESSAGE")),"No private message before close");
        }
        for(String destination:List.of("/topic/space/guessed","/queue/pairing","/user/"+bId+"/queue/pairing")){
            try(var forged=a.socket(true,"http://127.0.0.1:18080")){forged.await("CONNECTED");forged.send("SUBSCRIBE\nid:bad\ndestination:"+destination+"\n\n\0");forged.await("ERROR");}
        }
        try(var publishing=a.socket(true,"http://127.0.0.1:18080")){publishing.await("CONNECTED");publishing.send("SEND\ndestination:/queue/pairing\n\nspoof\0");publishing.await("ERROR");}
        try(var disabled=a.socket(true,"http://127.0.0.1:18080")){disabled.await("CONNECTED");jdbc.update("UPDATE app_user SET enabled=false WHERE id=?",aId);assertNotNull(disabled.closed.get(6,TimeUnit.SECONDS));}
    }
    Map<String,Object> createBody(){return Map.of("relationshipDate","2020-01-01","confirmed",true);}
    String code(Browser browser)throws Exception{var response=browser.post("/api/invitations",Map.of(),null);assertEquals(201,response.statusCode(),response.body());return tree(response).get("code").asText();}
    JsonNode preview(Browser browser,String code)throws Exception{var response=browser.post("/api/invitations/preview",Map.of("code",code),null);assertEquals(200,response.statusCode(),response.body());return tree(response);}
    Map<String,Object> joinBody(String code,JsonNode preview){return new HashMap<>(Map.of("code",code,"confirmationToken",preview.get("confirmationToken").asText(),"confirmedRelationshipDate",preview.get("relationshipDate").asText(),"confirmed",true));}
    JsonNode tree(HttpResponse<String> response){return json.readTree(response.body());}
    URI uri(String path){return URI.create("http://127.0.0.1:18080"+path);}
    void awaitSubscription(String email)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end){var user=userRegistry.getUser(email);if(user!=null&&user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getDestination().equals("/user/queue/pairing"))))return;Thread.sleep(20);}
        fail("Pairing subscription was not registered");
    }
    class Browser{
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(uri(path)).build(),HttpResponse.BodyHandlers.ofString());}
        String csrf()throws Exception{return tree(get("/api/csrf")).get("token").asText();}
        HttpResponse<String> post(String path,Object body,UUID key)throws Exception{
            var request=HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf());if(key!=null)request.header("Idempotency-Key",key.toString());
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> login(String email)throws Exception{return client.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("X-CSRF-TOKEN",csrf()).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());}
        Socket socket(boolean csrf,String origin)throws Exception{
            String token=csrf?csrf():null;var listener=new Socket();
            listener.socket=client.newWebSocketBuilder().header("Origin",origin).subprotocols("v12.stomp").buildAsync(URI.create("ws://127.0.0.1:18080/ws"),listener).get(5,TimeUnit.SECONDS);
            listener.send("CONNECT\naccept-version:1.2\nhost:127.0.0.1\nheart-beat:0,0\n"+(token==null?"":"X-CSRF-TOKEN:"+token+"\n")+"\n\0");return listener;
        }
    }
    class Socket implements WebSocket.Listener,AutoCloseable{
        WebSocket socket;final BlockingQueue<String> frames=new LinkedBlockingQueue<>();final CompletableFuture<Integer> closed=new CompletableFuture<>();final StringBuilder buffer=new StringBuilder();
        public void onOpen(WebSocket webSocket){webSocket.request(1);}
        public CompletionStage<?> onText(WebSocket webSocket,CharSequence data,boolean last){buffer.append(data);if(last){frames.offer(buffer.toString());buffer.setLength(0);}webSocket.request(1);return null;}
        public CompletionStage<?> onClose(WebSocket ws,int status,String reason){closed.complete(status);return null;}
        public void onError(WebSocket ws,Throwable error){closed.complete(-1);}
        void send(String frame){socket.sendText(frame,true).join();}
        String await(String text)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);while(System.nanoTime()<end){String frame=frames.poll(100,TimeUnit.MILLISECONDS);if(frame!=null&&frame.contains(text))return frame;}throw new AssertionError("Missing STOMP frame: "+text);}
        public void close(){if(socket!=null)socket.abort();}
    }
}
