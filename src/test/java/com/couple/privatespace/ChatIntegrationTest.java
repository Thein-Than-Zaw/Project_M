package com.couple.privatespace;

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

/** DEV-05 / CHAT-01–03, AC-F04-01–06: native cookies/STOMP/PostgreSQL, never a mocked broker. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class ChatIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SimpUserRegistry registry;
    final PairingIntegrationTest transport=new PairingIntegrationTest();
    final UUID aId=UUID.randomUUID(),bId=UUID.randomUUID(),space=UUID.randomUUID();
    final String prefix="dev05-"+UUID.randomUUID();
    PairingIntegrationTest.Browser a,b;
    @BeforeEach void fixtures()throws Exception {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class),"Dedicated empty test DB required");
        var hash=encoder.encode(transport.password);
        jdbc.update("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES (?,1,'Sender',?,?,DATE '2025-01-01'),(?,2,'Recipient',?,?,DATE '2025-01-01')",aId,prefix+"a@example.invalid",hash,bId,prefix+"b@example.invalid",hash);
        jdbc.update("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES (?,1,DATE '2025-01-01','Asia/Yangon')",space);
        jdbc.update("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES (?,?,1),(?,?,2)",space,aId,space,bId);
        a=transport.new Browser();b=transport.new Browser();assertEquals(200,a.login(prefix+"a@example.invalid").statusCode());assertEquals(200,b.login(prefix+"b@example.invalid").statusCode());
    }
    @AfterEach void cleanup(){
        if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
        jdbc.update("DELETE FROM chat_message WHERE space_id=?",space);jdbc.update("DELETE FROM chat_conversation WHERE space_id=?",space);jdbc.update("DELETE FROM couple_membership WHERE space_id=?",space);jdbc.update("DELETE FROM couple_space WHERE id=?",space);jdbc.update("DELETE FROM app_user WHERE id IN (?,?)",aId,bId);
    }
    PairingIntegrationTest.Socket socket(PairingIntegrationTest.Browser browser,String email)throws Exception {
        var socket=browser.socket(true,"http://127.0.0.1:18080");socket.await("CONNECTED");socket.send("SUBSCRIBE\nid:chat\ndestination:/user/queue/chat\n\n\0");
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end){var user=registry.getUser(email);if(user!=null&&user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getDestination().equals("/user/queue/chat"))))return socket;Thread.sleep(10);}
        socket.close();throw new AssertionError("Chat subscription not registered");
    }
    void command(PairingIntegrationTest.Socket socket,String destination,Object body){socket.send("SEND\ndestination:/app/"+destination+"\ncontent-type:application/json\n\n"+transport.json.writeValueAsString(body)+"\0");}
    JsonNode frame(PairingIntegrationTest.Socket socket,String marker)throws Exception {var frame=socket.await(marker);return transport.json.readTree(frame.substring(frame.indexOf("\n\n")+2,frame.lastIndexOf('\0')));}
    JsonNode history(PairingIntegrationTest.Browser browser,String suffix)throws Exception {var response=browser.get("/api/chat"+suffix);assertEquals(200,response.statusCode(),response.body());return transport.tree(response);}
    JsonNode send(PairingIntegrationTest.Socket socket,UUID key,String body)throws Exception {command(socket,"chat.send",Map.of("clientMessageId",key,"body",body));var result=frame(socket,"chat.sent");assertEquals(key.toString(),result.get("clientMessageId").asText());return result.get("message");}
    @Test void realMessageCommitPersistenceThreeRetriesAndSpoofedSenderIgnored()throws Exception {
        assertEquals(401,transport.new Browser().get("/api/chat").statusCode());assertEquals(200,a.get("/chat.html").statusCode());
        try(var sa=socket(a,prefix+"a@example.invalid");var sb=socket(b,prefix+"b@example.invalid")){
            UUID key=UUID.randomUUID();String body="မင်္ဂလာပါ 🌸 <script>alert(1)</script>\nA real message";
            command(sa,"chat.send",Map.of("clientMessageId",key,"body",body,"senderId",bId,"spaceId",UUID.randomUUID()));
            var first=frame(sa,"chat.sent").get("message");var received=frame(sb,"message.created").get("message");
            assertEquals(first.get("id"),received.get("id"));assertEquals(body,received.get("body").asText());assertEquals(aId.toString(),received.get("senderId").asText());
            assertTrue(first.get("deliveredAt").isNull(),"Broker send alone is not delivery");
            for(int i=0;i<3;i++)assertEquals(first.get("id"),send(sa,key,body).get("id"));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=?",Integer.class,space));
            command(sa,"chat.send",Map.of("clientMessageId",key,"body","Changed intent"));assertEquals("MESSAGE_CONFLICT",frame(sa,"chat.error").get("code").asText());
            assertEquals(body,history(b,"").get("items").get(0).get("body").asText());assertEquals(1,history(b,"").get("unreadCount").asInt());
        }
        var again=transport.new Browser();again.login(prefix+"b@example.invalid");assertEquals(1,history(again,"").get("items").size());
    }
    @Test void recipientDeliveryIsNotReadAndReadingOneMessageDoesNotReadAnother()throws Exception {
        try(var sa=socket(a,prefix+"a@example.invalid");var sb=socket(b,prefix+"b@example.invalid")){
            var one=send(sa,UUID.randomUUID(),"First not visible");var two=send(sa,UUID.randomUUID(),"Second visible");
            String id1=one.get("id").asText(),id2=two.get("id").asText();
            command(sa,"chat.read",Map.of("messageIds",List.of(id1)));assertEquals("INVALID_RECIPIENT",frame(sa,"chat.error").get("code").asText());
            command(sb,"chat.delivered",Map.of("messageIds",List.of(id1,id2),"userId",aId));var delivered=frame(sb,"chat.ack");assertEquals(2,delivered.get("receipts").size());
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND read_at IS NOT NULL",Integer.class,space));
            command(sb,"chat.read",Map.of("messageIds",List.of(id2)));frame(sb,"chat.ack");
            assertEquals(1,history(b,"").get("unreadCount").asInt());assertEquals(1,transport.tree(b.get("/api/home")).get("unreadMessages").asInt());
            var lookup=a.get("/api/chat/receipts?ids="+id1+","+id2);assertEquals(200,lookup.statusCode());var rows=transport.tree(lookup);assertEquals(2,rows.size());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND read_at IS NOT NULL",Integer.class,space));
            command(sb,"chat.read",Map.of("messageIds",List.of(id2)));frame(sb,"chat.ack");
            assertNotNull(jdbc.queryForObject("SELECT read_at FROM chat_message WHERE id=?",java.sql.Timestamp.class,UUID.fromString(id2)));
            assertEquals(403,a.get("/api/chat/receipts?ids="+UUID.randomUUID()).statusCode());
        }
    }
    @Test void databaseFailureNeverAcknowledgesSentAndRetryDoesNotSkipSequence()throws Exception {
        try(var sa=socket(a,prefix+"a@example.invalid")){
            UUID key=UUID.randomUUID();String marker="native_failure_"+UUID.randomUUID();
            jdbc.execute("ALTER TABLE chat_message ADD CONSTRAINT dev05_failure_fixture CHECK (body <> '"+marker+"')");
            try{command(sa,"chat.send",Map.of("clientMessageId",key,"body",marker));var error=frame(sa,"chat.error");assertEquals("MESSAGE_NOT_SAVED",error.get("code").asText());assertFalse(error.toString().contains(marker));assertEquals(0,history(a,"").get("items").size());assertEquals(0,history(a,"").get("latestSequence").asInt());}
            finally{jdbc.execute("ALTER TABLE chat_message DROP CONSTRAINT dev05_failure_fixture");}
            assertEquals(1,send(sa,key,marker).get("sequence").asInt());
        }
    }
    @Test void concurrentSameIdCreatesOnceAndDifferentIdsHaveStableOrder()throws Exception {
        var other=transport.new Browser();other.login(prefix+"a@example.invalid");
        try(var one=socket(a,prefix+"a@example.invalid");var two=socket(other,prefix+"a@example.invalid")){
            UUID key=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
            try{var x=pool.submit(()->{start.await();return send(one,key,"Same intent");});var y=pool.submit(()->{start.await();return send(two,key,"Same intent");});start.countDown();assertEquals(x.get(10,TimeUnit.SECONDS).get("id"),y.get(10,TimeUnit.SECONDS).get("id"));}
            finally{pool.shutdownNow();}
            assertEquals(2,send(one,UUID.randomUUID(),"Next").get("sequence").asInt());
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=?",Integer.class,space));
        }
    }
    @Test void unicode4000EmojiFitsRealTransportAndBlankOrOversizedRejects()throws Exception {
        try(var sa=socket(a,prefix+"a@example.invalid")){
            var saved=send(sa,UUID.randomUUID(),"🌸".repeat(4000));assertEquals(8000,saved.get("body").asText().length());
            command(sa,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"body","🌸".repeat(4001)));assertEquals("MESSAGE_TOO_LONG",frame(sa,"chat.error").get("code").asText());
            command(sa,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"body"," \n\t"));assertEquals("EMPTY_MESSAGE",frame(sa,"chat.error").get("code").asText());
            command(sa,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"body","\u00a0\u2007\ufeff"));assertEquals("EMPTY_MESSAGE",frame(sa,"chat.error").get("code").asText());
            assertEquals(1,history(a,"").get("items").size());
        }
    }
    @Test void cursorPagesRecoverMissingMessagesAndOldStatusesWithoutDuplication()throws Exception {
        jdbc.update("INSERT INTO chat_conversation(space_id,last_sequence) VALUES (?,103)",space);
        for(int i=1;i<=103;i++)jdbc.update("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),space,i,aId,UUID.randomUUID(),"History "+i);
        var latest=history(b,"");assertEquals(50,latest.get("items").size());assertEquals(54,latest.get("items").get(0).get("sequence").asInt());assertTrue(latest.get("hasMore").asBoolean());
        var older=history(b,"?before=54");assertEquals(4,older.get("items").get(0).get("sequence").asInt());
        var catchup=history(b,"?after=0");assertEquals(1,catchup.get("items").get(0).get("sequence").asInt());assertEquals(50,catchup.get("items").get(49).get("sequence").asInt());
        var last=history(b,"?after=100");assertEquals(3,last.get("items").size());assertFalse(last.get("hasMore").asBoolean());assertEquals(103,last.get("unreadCount").asInt());
        assertEquals(400,b.get("/api/chat?before=2&after=0").statusCode());assertEquals(400,b.get("/api/chat?after=-1").statusCode());
    }
    @Test void unpairedChatForbiddenAndUnknownOrForeignDestinationsCannotPublish()throws Exception {
        for(String destination:List.of("/topic/chat","/queue/chat","/user/"+bId+"/queue/chat"))try(var ws=a.socket(true,"http://127.0.0.1:18080")){ws.await("CONNECTED");ws.send("SUBSCRIBE\nid:foreign\ndestination:"+destination+"\n\n\0");ws.await("ERROR");}
        try(var ws=a.socket(true,"http://127.0.0.1:18080")){ws.await("CONNECTED");ws.send("SEND\ndestination:/app/chat.delete\n\n{}\0");ws.await("ERROR");}
        jdbc.update("DELETE FROM couple_membership WHERE user_id=?",bId);
        assertEquals(403,b.get("/api/chat").statusCode());assertEquals(403,b.get("/chat.html").statusCode());
        try(var ws=b.socket(true,"http://127.0.0.1:18080")){ws.await("CONNECTED");command(ws,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"body","Forbidden"));assertNotNull(ws.closed.get(5,TimeUnit.SECONDS));}
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=?",Integer.class,space));
    }
    @Test void disconnectedRecipientCatchesUpAndRevokedSessionCannotReceive()throws Exception {
        var recipient=socket(b,prefix+"b@example.invalid");recipient.close();
        try(var sender=socket(a,prefix+"a@example.invalid")){
            send(sender,UUID.randomUUID(),"While disconnected");assertEquals(1,history(b,"?after=0").get("items").size());
            try(var again=socket(b,prefix+"b@example.invalid")){
                jdbc.update("UPDATE app_user SET security_revision=security_revision+1 WHERE id=?",bId);
                assertNotNull(again.closed.get(6,TimeUnit.SECONDS));
                assertEquals(401,b.get("/api/chat").statusCode());
                assertEquals(2,send(sender,UUID.randomUUID(),"Still saved for next login").get("sequence").asInt());
            }
        }
    }
}
