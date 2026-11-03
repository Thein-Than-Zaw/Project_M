package com.couple.privatespace;

import com.couple.privatespace.chat.*;
import com.couple.privatespace.home.HomeService;
import com.couple.privatespace.memory.MemoryFiles;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** HOME-05: native shared counters from committed messages and connected call metadata. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class HomeActivityIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;
 @Autowired HomeService home;@Autowired ChatService chat;@Autowired ChatAttachmentService media;@Autowired MemoryFiles files;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeAll static void prepare()throws Exception{MemoryFixtures.prepare();}
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup()throws Exception{
  if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
  jdbc.update("DELETE FROM call_session WHERE space_id=?",f.space);
  for(UUID id:jdbc.queryForList("SELECT id FROM chat_attachment WHERE space_id=?",UUID.class,f.space))files.delete(id);
  jdbc.update("UPDATE chat_message SET attachment_id=NULL,body='',gift_code=NULL,gift_name=NULL,gift_asset_path=NULL,deleted_at=COALESCE(deleted_at,clock_timestamp()) WHERE space_id=?",f.space);
  jdbc.update("DELETE FROM chat_attachment WHERE space_id=?",f.space);jdbc.update("DELETE FROM chat_upload_intent WHERE space_id=?",f.space);f.cleanup();
 }
 ChatService.Attachment upload(UUID key)throws Exception{
  var checked=files.validate(new MockMultipartFile("file","moment.png","image/png",Files.readAllBytes(MemoryFixtures.root.resolve("moment.png"))));try{return media.register(f.aId,key,checked);}finally{files.discardStaged(checked.id());}
 }
 @Test void sharedMessageCounterIncludesSavedTypesButNotRetriesUnsentUploadsOrDeletedForEveryone()throws Exception{
  assertEquals(0,home.home(f.aId).activity().messageCount());assertEquals(401,f.transport.new Browser().get("/api/home").statusCode());
  var command=new ChatService.Send(UUID.randomUUID(),"one saved text","TEXT",null,null);var text=chat.send(f.aId,command);chat.send(f.aId,command);
  String gift=chat.gifts(f.bId,null,null,0).items().getFirst().code();var sticker=chat.send(f.bId,new ChatService.Send(UUID.randomUUID(),"","GIFT",gift,null));
  UUID key=UUID.randomUUID();var file=upload(key);var sentFile=chat.send(f.aId,new ChatService.Send(key,"","FILE",null,null,null,file.id()));upload(UUID.randomUUID());
  var editedFile=chat.edit(f.aId,sentFile.id(),new ChatService.Edit("new caption",0L));assertNotNull(editedFile.editedAt());assertEquals(file.id(),editedFile.attachmentId());assertEquals("new caption",chat.snapshots(f.bId,List.of(sentFile.id())).items().getFirst().body());
  assertEquals(3,home.home(f.aId).activity().messageCount());assertEquals(home.home(f.aId).activity(),home.home(f.bId).activity());
  chat.hide(f.aId,text.id());assertEquals(3,home.home(f.aId).activity().messageCount());assertEquals(3,home.home(f.bId).activity().messageCount());
  chat.delete(f.bId,sticker.id());assertEquals(2,home.home(f.aId).activity().messageCount());assertEquals(2,f.transport.tree(f.b.get("/api/home")).path("activity").path("messageCount").asLong());
 }
 @Test void videoTotalsCountOneCallForTheCoupleExcludeVoiceAndUnansweredAndUseConnectedElapsedTime(){
  var empty=home.home(f.aId).activity();assertEquals(0,empty.videoCallSeconds());assertEquals(0,empty.videoCallCount());
  Instant start=Instant.parse("2026-01-01T00:00:00Z");insertCall("VIDEO","ENDED",start,start.plusSeconds(3661));insertCall("VIDEO","FAILED",start,start.plusSeconds(59));insertCall("VOICE","ENDED",start,start.plusSeconds(7200));insertCall("VIDEO","MISSED",null,start.plusSeconds(30));
  var total=home.home(f.aId).activity();assertEquals(3720,total.videoCallSeconds());assertEquals(2,total.videoCallCount());assertEquals(total,home.home(f.bId).activity());
  insertCall("VIDEO","ACTIVE",Instant.now().minusSeconds(121),null);var ongoing=home.home(f.aId).activity();assertEquals(3,ongoing.videoCallCount());assertTrue(ongoing.videoCallSeconds()>=3840&&ongoing.videoCallSeconds()<=3845);
 }
 void insertCall(String mode,String state,Instant connected,Instant ended){
  Instant created=connected==null?Instant.parse("2026-01-01T00:00:00Z"):connected.minusSeconds(10);
  jdbc.update("INSERT INTO call_session(id,space_id,caller_id,callee_id,mode,state,caller_client,callee_client,caller_socket,callee_socket,caller_connected,callee_connected,created_at,ring_deadline,accepted_at,connected_at,ended_at,end_reason) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),f.space,f.aId,f.bId,mode,state,UUID.randomUUID(),UUID.randomUUID(),"synthetic-home-a","synthetic-home-b",state.equals("ACTIVE"),state.equals("ACTIVE"),Timestamp.from(created),Timestamp.from(created.plusSeconds(30)),connected==null?null:Timestamp.from(connected.minusSeconds(1)),connected==null?null:Timestamp.from(connected),ended==null?null:Timestamp.from(ended),ended==null?null:"SYNTHETIC_END");
 }
}
