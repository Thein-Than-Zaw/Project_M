package com.couple.privatespace;

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

/** DEV-04 HOME/DATE: real two-member cookies, SQL persistence, scope and concurrent versions. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class HomeIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SimpUserRegistry userRegistry;
    final JsonMapper json=new JsonMapper();
    final UUID aId=UUID.randomUUID(),bId=UUID.randomUUID(),space=UUID.randomUUID();
    final String prefix="dev04-"+UUID.randomUUID(), password="Synthetic Home password 2026!";
    Browser a,b;
    @BeforeEach void fixtures()throws Exception {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
        var hash=encoder.encode(password);
        jdbc.update("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES (?,1,'Creator',?,?,DATE '2024-02-29'),(?,2,'Partner',?,?,DATE '2024-02-29')",aId,prefix+"a@example.invalid",hash,bId,prefix+"b@example.invalid",hash);
        jdbc.update("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES (?,1,DATE '2024-02-29','Asia/Yangon')",space);
        jdbc.update("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES (?,?,1),(?,?,2)",space,aId,space,bId);
        a=new Browser();b=new Browser();a.login(prefix+"a@example.invalid");b.login(prefix+"b@example.invalid");
    }
    @AfterEach void cleanup() {
        if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
        jdbc.update("DELETE FROM special_date WHERE space_id=?",space);jdbc.update("DELETE FROM special_date_creation_receipt WHERE created_by IN (?,?)",aId,bId);jdbc.update("DELETE FROM couple_membership WHERE space_id=?",space);jdbc.update("DELETE FROM couple_space WHERE id=?",space);jdbc.update("DELETE FROM app_user WHERE id IN (?,?)",aId,bId);
    }
    @Test void ownUnicodeProfilePersistsAndCannotTargetPartner()throws Exception {
        assertEquals(401,new Browser().get("/api/home").statusCode());
        assertEquals(403,a.write("/api/profile","PATCH",Map.of("displayName","No CSRF","version",0),null,false).statusCode());
        var result=a.write("/api/profile","PATCH",Map.of("displayName","ချစ်သူ 🌸","version",0,"userId",bId,"email","spoof@example.invalid"),null,true);
        assertEquals(200,result.statusCode(),result.body());assertEquals("ချစ်သူ 🌸",tree(result).get("displayName").asText());
        assertEquals("Partner",jdbc.queryForObject("SELECT display_name FROM app_user WHERE id=?",String.class,bId));
        assertEquals(prefix+"a@example.invalid",jdbc.queryForObject("SELECT email FROM app_user WHERE id=?",String.class,aId));
        assertEquals("ချစ်သူ 🌸",tree(b.get("/api/home")).get("members").get(0).get("displayName").asText());
        assertEquals(409,a.write("/api/profile","PATCH",Map.of("displayName","Stale","version",0),null,true).statusCode());
        assertEquals(400,a.write("/api/profile","PATCH",Map.of("displayName"," ","version",1),null,true).statusCode());
        assertEquals(400,a.write("/api/profile","PATCH",Map.of("displayName","🌸".repeat(61),"version",1),null,true).statusCode());
        assertEquals(403,a.write("/api/profile/"+bId,"PATCH",Map.of(),null,true).statusCode());
    }
    @Test void sharedStartDateRequiresConfirmationAndRefreshesSameCalendar()throws Exception {
        var today=LocalDate.now(ZoneId.of("Asia/Yangon"));
        var change=new HashMap<String,Object>(Map.of("relationshipDate",today.toString(),"version",0,"confirmed",false));
        assertEquals(400,a.write("/api/relationship-date","PATCH",change,null,true).statusCode());
        change.put("confirmed",true);change.put("relationshipDate",today.plusDays(1).toString());
        assertEquals(400,a.write("/api/relationship-date","PATCH",change,null,true).statusCode());
        change.put("relationshipDate",today.toString());assertEquals(200,b.write("/api/relationship-date","PATCH",change,null,true).statusCode());
        var home=tree(a.get("/api/home"));var dates=tree(b.get("/api/dates"));
        assertEquals(1,home.get("calendar").get("dayNumber").asInt());assertEquals(home.get("calendar"),dates.get("calendar"));
        assertEquals(409,a.write("/api/relationship-date","PATCH",change,null,true).statusCode());
        assertEquals("2024-02-29",jdbc.queryForObject("SELECT signup_relationship_date::text FROM app_user WHERE id=?",String.class,aId));
    }
    @Test void bothMembersEditDeleteIdempotentCreationAndNativeLeapOccurrence()throws Exception {
        UUID key=UUID.randomUUID();var body=event("Our leap day 🌸","2024-02-29",true,0);
        var created=a.write("/api/dates","POST",body,key,true);assertEquals(201,created.statusCode(),created.body());
        var date=tree(created);String id=date.get("id").asText();
        assertEquals(id,tree(a.write("/api/dates","POST",body,key,true)).get("id").asText());
        assertEquals(409,a.write("/api/dates","POST",event("Different","2024-02-29",true,0),key,true).statusCode());
        assertEquals(400,a.write("/api/dates","POST",body,null,true).statusCode());
        // PostgreSQL oracle: original date + target-year offset restores Feb29.
        assertEquals("2027-02-28",jdbc.queryForObject("SELECT (event_date+make_interval(years=>3))::date::text FROM special_date WHERE id=?",String.class,UUID.fromString(id)));
        assertEquals("2028-02-29",jdbc.queryForObject("SELECT (event_date+make_interval(years=>4))::date::text FROM special_date WHERE id=?",String.class,UUID.fromString(id)));
        assertEquals(200,b.write("/api/dates/"+id,"PATCH",event("Edited by partner","2024-03-01",true,0),null,true).statusCode());
        assertEquals(bId,jdbc.queryForObject("SELECT updated_by FROM special_date WHERE id=?",UUID.class,UUID.fromString(id)));
        assertEquals(aId,jdbc.queryForObject("SELECT created_by FROM special_date WHERE id=?",UUID.class,UUID.fromString(id)));
        assertEquals(409,a.write("/api/dates/"+id+"?version=0","DELETE",null,null,true).statusCode());
        assertEquals(204,b.write("/api/dates/"+id+"?version=1","DELETE",null,null,true).statusCode());
        assertEquals(404,a.write("/api/dates","POST",body,key,true).statusCode(),"A delayed creation retry must not resurrect a deleted date");
        assertTrue(tree(a.get("/api/dates")).get("items").isEmpty());
        assertEquals(404,a.write("/api/dates/"+UUID.randomUUID(),"PATCH",body,null,true).statusCode());
    }
    @Test void concurrentEditsHaveOneWinnerAndUnpairedCannotWrite()throws Exception {
        var created=tree(a.write("/api/dates","POST",event("Original","2026-12-31",false,0),UUID.randomUUID(),true));var id=created.get("id").asText();
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{var first=pool.submit(()->{start.await();return a.write("/api/dates/"+id,"PATCH",event("A","2026-12-31",false,0),null,true).statusCode();});var second=pool.submit(()->{start.await();return b.write("/api/dates/"+id,"PATCH",event("B","2026-12-31",false,0),null,true).statusCode();});start.countDown();assertEquals(Set.of(200,409),Set.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS)));}finally{pool.shutdownNow();}
        // The non-member cannot read, create, edit or delete the member's data.
        jdbc.update("DELETE FROM special_date WHERE space_id=?",space);
        jdbc.update("DELETE FROM couple_membership WHERE user_id=?",bId);
        assertEquals(403,b.get("/api/dates").statusCode());assertEquals(403,b.write("/api/dates","POST",event("Spoof","2026-12-31",false,0),UUID.randomUUID(),true).statusCode());
        assertEquals(403,b.write("/api/dates/"+id,"PATCH",event("Spoof","2026-12-31",false,1),null,true).statusCode());
        assertEquals(403,b.write("/api/dates/"+id+"?version=1","DELETE",null,null,true).statusCode());
    }
    @Test void paginatedListsAndHistoryNeverBecomeNegativeUpcoming()throws Exception {
        for(int i=0;i<51;i++)jdbc.update("INSERT INTO special_date(id,space_id,created_by,updated_by,title,event_date,repeat_yearly,creation_key,creation_hash) VALUES (?,?,?,?,?,DATE '2000-01-01',false,?,'synthetic')",UUID.randomUUID(),space,aId,aId,"History "+i,UUID.randomUUID());
        var first=tree(a.get("/api/dates"));assertEquals(50,first.get("items").size());assertTrue(first.get("hasMore").asBoolean());assertFalse(first.get("items").get(0).get("upcoming").asBoolean());
        assertEquals(1,tree(b.get("/api/dates?page=1")).get("items").size());assertTrue(tree(a.get("/api/home")).get("nextEvent").isNull());
        assertEquals(400,a.get("/api/dates?page=-1").statusCode());
        assertEquals(400,a.write("/api/dates","POST",event(" ","2026-01-01",false,0),UUID.randomUUID(),true).statusCode());
    }
    @Test void creationRaceCommitsOneDateAndFutureYearlyDateDoesNotStartEarly()throws Exception {
        UUID key=UUID.randomUUID();var body=event("<img src=x onerror=alert(1)> 🌸","2030-02-28",true,0);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{var first=pool.submit(()->{start.await();return a.write("/api/dates","POST",body,key,true);});var second=pool.submit(()->{start.await();return a.write("/api/dates","POST",body,key,true);});start.countDown();var x=first.get(10,TimeUnit.SECONDS);var y=second.get(10,TimeUnit.SECONDS);assertEquals(201,x.statusCode(),x.body());assertEquals(201,y.statusCode(),y.body());assertEquals(tree(x).get("id"),tree(y).get("id"));assertEquals("2030-02-28",tree(x).get("nextDate").asText());}finally{pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM special_date WHERE space_id=?",Integer.class,space));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM special_date_creation_receipt WHERE created_by=?",Integer.class,aId));
    }
    @Test void profileInvalidationIsStateOnlyAfterCommitAndPartnerCatchesUp()throws Exception {
        var socket=new PairingIntegrationTest().new Socket();
        try(socket){
            socket.socket=b.client.newWebSocketBuilder().header("Origin","http://127.0.0.1:18080").subprotocols("v12.stomp").buildAsync(URI.create("ws://127.0.0.1:18080/ws"),socket).get(5,TimeUnit.SECONDS);
            socket.send("CONNECT\naccept-version:1.2\nhost:127.0.0.1\nheart-beat:0,0\nX-CSRF-TOKEN:"+b.csrf()+"\n\n\0");socket.await("CONNECTED");
            socket.send("SUBSCRIBE\nid:home\ndestination:/user/queue/pairing\n\n\0");
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean subscribed=false;
            while(System.nanoTime()<end){var user=userRegistry.getUser(prefix+"b@example.invalid");if(user!=null&&user.getSessions().stream().anyMatch(s->!s.getSubscriptions().isEmpty())){subscribed=true;break;}Thread.sleep(20);}assertTrue(subscribed);
            var response=a.write("/api/profile","PATCH",Map.of("displayName","Committed private name","version",0),null,true);assertEquals(200,response.statusCode());
            var frame=socket.await("profile.updated");assertFalse(frame.contains("Committed private name"));assertTrue(tree(b.get("/api/home")).get("members").get(0).get("displayName").asText().contains("Committed private name"));
            assertEquals(409,a.write("/api/profile","PATCH",Map.of("displayName","Rejected name","version",0),null,true).statusCode());
            assertNull(socket.frames.poll(300,TimeUnit.MILLISECONDS),"Failed stale write must not emit invalidation");
        }
    }
    // CHAT-03: actual HTTP, CSRF, normalized persisted photo and same-space access.
    @Test void privatePhotoNormalizesAndCannotTargetAnotherAccount()throws Exception {
        var image=new java.awt.image.BufferedImage(64,32,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"PNG",bytes);
        var raw=bytes.toByteArray();
        assertEquals(401,new Browser().get("/api/profiles/"+aId+"/photo").statusCode());
        assertEquals(403,photo(a,raw,false).statusCode());
        var saved=photo(a,raw,true);assertEquals(200,saved.statusCode(),saved.body());
        String url=tree(saved).get("avatarUrl").asText();assertTrue(url.startsWith("/api/profiles/"+aId+"/photo?v="));
        var response=b.client.send(HttpRequest.newBuilder(b.uri(url)).build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/jpeg"));
        var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(response.body()));assertEquals(256,decoded.getWidth());assertEquals(256,decoded.getHeight());
        assertEquals(url,tree(a.get("/api/home")).get("self").get("avatarUrl").asText());
        assertEquals(url,tree(b.get("/api/chat")).get("partner").get("avatarUrl").asText());
        assertNull(jdbc.queryForObject("SELECT avatar_jpeg FROM app_user WHERE id=?",byte[].class,bId));
        assertEquals(404,b.get("/api/profiles/"+UUID.randomUUID()+"/photo").statusCode());
        jdbc.update("DELETE FROM couple_membership WHERE user_id=?",bId);
        assertEquals(403,b.get(url).statusCode());assertEquals(403,photo(b,raw,true).statusCode());
    }
    @Test void malformedAndOversizedPhotosNeverOverwriteSavedImage()throws Exception {
        assertEquals(400,photo(a,"<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8),true).statusCode());
        assertEquals(413,photo(a,new byte[2097153],true).statusCode());
        assertNull(jdbc.queryForObject("SELECT avatar_jpeg FROM app_user WHERE id=?",byte[].class,aId));
        assertEquals(0,jdbc.queryForObject("SELECT avatar_version FROM app_user WHERE id=?",Long.class,aId));
    }
    @Test void activityUsesPartnerHeartbeatAndExpiresWithoutFabricatingLoginTime()throws Exception {
        assertEquals(401,new Browser().get("/api/presence").statusCode());
        var initial=tree(a.get("/api/presence"));assertFalse(initial.get("active").asBoolean());assertTrue(initial.get("lastActiveAt").isNull());
        assertEquals(403,b.write("/api/presence","POST",null,null,false).statusCode());
        assertEquals(204,b.write("/api/presence","POST",null,null,true).statusCode());assertTrue(tree(a.get("/api/presence")).get("active").asBoolean());
        assertFalse(tree(b.get("/api/presence")).get("active").asBoolean(),"Must report partner, not actor");
        jdbc.update("UPDATE app_user SET last_active_at=CURRENT_TIMESTAMP-interval '2 minutes' WHERE id=?",bId);
        var idle=tree(a.get("/api/presence"));assertFalse(idle.get("active").asBoolean());assertFalse(idle.get("lastActiveAt").isNull());
    }
    HttpResponse<String> photo(Browser browser,byte[] data,boolean token)throws Exception {
        var request=HttpRequest.newBuilder(browser.uri("/api/profile/photo")).header("Content-Type","application/octet-stream");if(token)request.header("X-CSRF-TOKEN",browser.csrf());
        return browser.client.send(request.PUT(HttpRequest.BodyPublishers.ofByteArray(data)).build(),HttpResponse.BodyHandlers.ofString());
    }
    Map<String,Object> event(String title,String date,boolean repeat,long version){return Map.of("title",title,"eventDate",date,"repeatYearly",repeat,"version",version);}
    JsonNode tree(HttpResponse<String> response){return json.readTree(response.body());}
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        URI uri(String path){return URI.create("http://127.0.0.1:18080"+path);}
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(uri(path)).build(),HttpResponse.BodyHandlers.ofString());}
        String csrf()throws Exception{return tree(get("/api/csrf")).get("token").asText();}
        void login(String email)throws Exception{var response=client.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("X-CSRF-TOKEN",csrf()).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());}
        HttpResponse<String> write(String path,String method,Object body,UUID key,boolean token)throws Exception{var req=HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json");if(token)req.header("X-CSRF-TOKEN",csrf());if(key!=null)req.header("Idempotency-Key",key.toString());return client.send(req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
    }
}
