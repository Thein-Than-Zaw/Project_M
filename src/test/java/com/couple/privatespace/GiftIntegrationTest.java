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

/** DEV-06 GIFT-01–03 / AC-F05-01–05: native PostgreSQL + real authenticated STOMP. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class GiftIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SimpUserRegistry registry;
    final ChatIntegrationTest f=new ChatIntegrationTest();
    String code;
    @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();code=jdbc.queryForObject("SELECT code FROM gift_catalog WHERE active AND code LIKE '%love%' ORDER BY code LIMIT 1",String.class);}
    @AfterEach void cleanup(){f.cleanup();}
    void gift(PairingIntegrationTest.Socket socket,UUID key,String note){f.command(socket,"chat.send",Map.of("clientMessageId",key,"type","GIFT","giftCode",code,"body",note));}
    @Test void catalogueContainsEveryBundledStickerIsPrivatePagedSearchableAndCancelIsReadOnly()throws Exception{
        assertEquals(401,f.transport.new Browser().get("/api/gifts").statusCode());
        assertEquals(401,f.transport.new Browser().get("/stickers/blobfox/blobfox.png").statusCode());
        var response=f.a.get("/api/gifts");assertEquals(200,response.statusCode());var page=f.transport.tree(response);
        assertEquals(363,page.get("total").asInt());assertEquals(60,page.get("items").size());assertTrue(page.get("hasMore").asBoolean());assertEquals(2,page.get("packs").size());
        var second=f.transport.tree(f.a.get("/api/gifts?offset=60"));assertNotEquals(page.get("items").get(0).get("code"),second.get("items").get(0).get("code"));
        var found=f.transport.tree(f.b.get("/api/gifts?pack=bunhd&q=heart"));assertTrue(found.get("items").size()>0);for(var item:found.get("items")){assertEquals("bunhd",item.get("pack").asText());assertTrue(item.get("name").asText().toLowerCase(Locale.ROOT).contains("heart"));assertEquals(200,f.b.get(item.get("assetPath").asText()).statusCode());}
        assertEquals(400,f.a.get("/api/gifts?offset=-1").statusCode());assertEquals(400,f.a.get("/api/gifts?q="+"x".repeat(81)).statusCode());
        assertEquals(0,f.history(f.b,"").get("unreadCount").asInt());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message",Integer.class));
        jdbc.update("DELETE FROM couple_membership WHERE user_id=?",f.bId);assertEquals(403,f.b.get("/api/gifts").statusCode());assertEquals(403,f.b.get("/api/gifts/history").statusCode());
    }
    @Test void selectedGiftNoteSnapshotNotificationsReplayAndUnicodeSurviveRelogin()throws Exception{
        String note="ချစ်တယ် ❤️ 👩🏽‍❤️‍👨🏻 <img src=x onerror=alert(1)>";UUID key=UUID.randomUUID();
        var original=jdbc.queryForMap("SELECT name,asset_path FROM gift_catalog WHERE code=?",code);
        try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
            gift(a,key,note);var sent=f.frame(a,"chat.sent").get("message");var received=f.frame(b,"message.created");
            assertEquals("You received a little gift.",received.get("notification").asText());assertEquals(sent,received.get("message"));assertEquals(note,sent.get("body").asText());assertEquals(code,sent.get("giftCode").asText());assertEquals(original.get("name"),sent.get("giftName").asText());assertEquals(original.get("asset_path"),sent.get("giftAssetPath").asText());
            jdbc.update("UPDATE gift_catalog SET active=false,name='Changed catalogue label' WHERE code=?",code);
            try{for(int i=0;i<3;i++){gift(a,key,note);assertEquals(sent,f.frame(a,"chat.sent").get("message"));}gift(a,UUID.randomUUID(),note);assertEquals("INVALID_GIFT",f.frame(a,"chat.error").get("code").asText());}
            finally{jdbc.update("UPDATE gift_catalog SET active=true,name=? WHERE code=?",original.get("name"),code);}
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM chat_message",Integer.class));assertEquals(1,f.history(f.b,"").get("unreadCount").asInt());
            var sentHistory=f.transport.tree(f.a.get("/api/gifts/history?direction=sent"));assertEquals(sent,sentHistory.get("items").get(0));assertEquals(0,f.transport.tree(f.a.get("/api/gifts/history?direction=received")).get("items").size());assertEquals(1,f.transport.tree(f.b.get("/api/gifts/history?direction=received")).get("items").size());
        }
        var again=f.transport.new Browser();assertEquals(200,again.login(f.prefix+"b@example.invalid").statusCode());assertEquals(note,f.history(again,"").get("items").get(0).get("body").asText());
    }
    @Test void invalidCodeRecipientSpoofUnicodeLimitsAndChangedIntentRejectWithoutUnread()throws Exception{
        try(var a=f.socket(f.a,f.prefix+"a@example.invalid")){
            f.command(a,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"type","GIFT","giftCode","not-real","body",""));assertEquals("INVALID_GIFT",f.frame(a,"chat.error").get("code").asText());
            f.command(a,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"type","GIFT","giftCode",code,"recipientId",UUID.randomUUID()));assertEquals("INVALID_RECIPIENT",f.frame(a,"chat.error").get("code").asText());
            gift(a,UUID.randomUUID(),"🌸".repeat(501));assertEquals("GIFT_NOTE_TOO_LONG",f.frame(a,"chat.error").get("code").asText());assertEquals(0,f.history(f.b,"").get("unreadCount").asInt());
            UUID key=UUID.randomUUID();gift(a,key,"🌸".repeat(500));assertEquals(1000,f.frame(a,"chat.sent").get("message").get("body").asText().length());
            gift(a,key,"Changed note");assertEquals("MESSAGE_CONFLICT",f.frame(a,"chat.error").get("code").asText());
            f.command(a,"chat.send",Map.of("clientMessageId",key,"body","A text message"));assertEquals("MESSAGE_CONFLICT",f.frame(a,"chat.error").get("code").asText());
            f.command(a,"chat.send",Map.of("clientMessageId",UUID.randomUUID(),"type","GIFT","giftCode",code));assertEquals("",f.frame(a,"chat.sent").get("message").get("body").asText());
        }
    }
    @Test void concurrentGiftReplayCreatesOneSequenceAndNoRepeatedCreationNotification()throws Exception{
        var other=f.transport.new Browser();other.login(f.prefix+"a@example.invalid");
        try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var a2=f.socket(other,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
            UUID key=UUID.randomUUID();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try{var one=pool.submit(()->{start.await();gift(a,key,"");return f.frame(a,"chat.sent").get("message");});var two=pool.submit(()->{start.await();gift(a2,key,"");return f.frame(a2,"chat.sent").get("message");});start.countDown();assertEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));}
            finally{pool.shutdownNow();}
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM chat_message",Integer.class));assertEquals(1,f.history(f.b,"").get("latestSequence").asInt());
            f.frame(b,"message.created");assertNull(b.frames.poll(750,TimeUnit.MILLISECONDS),"No duplicate creation/notification event");
        }
    }
    @Test void failedGiftTransactionNeverSendsAndSameKeyRetryRecovers()throws Exception{
        UUID key=UUID.randomUUID();
        try(var a=f.socket(f.a,f.prefix+"a@example.invalid")){
            jdbc.execute("ALTER TABLE chat_message ADD CONSTRAINT dev06_failure_fixture CHECK (type<>'GIFT')");
            try{gift(a,key,"Retry me");assertEquals("MESSAGE_NOT_SAVED",f.frame(a,"chat.error").get("code").asText());assertEquals(0,f.history(f.a,"").get("latestSequence").asInt());}
            finally{jdbc.execute("ALTER TABLE chat_message DROP CONSTRAINT dev06_failure_fixture");}
            gift(a,key,"Retry me");assertEquals(1,f.frame(a,"chat.sent").get("message").get("sequence").asInt());
        }
    }
}
