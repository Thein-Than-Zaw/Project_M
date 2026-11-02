package com.couple.privatespace;

import com.couple.privatespace.chat.*;
import com.couple.privatespace.memory.*;
import com.couple.privatespace.identity.ApiFailure;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

/** CHAT-05: actual private HTTP storage, PostgreSQL cancellation/receipts, native audio validation. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class ChatMediaIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired ChatService chat;@Autowired ChatAttachmentService media;@Autowired MemoryFiles files;
 @Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;@Autowired PlatformTransactionManager transactions;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeAll static void nativeFixtures()throws Exception{MemoryFixtures.prepare();MemoryFixtures.ff("-f","lavfi","-i","sine=frequency=440:sample_rate=48000","-t","1","-c:a","libopus",MemoryFixtures.root.resolve("voice.webm").toString());}
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup()throws Exception{
  if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
  for(UUID id:jdbc.queryForList("SELECT id FROM chat_attachment WHERE space_id=?",UUID.class,f.space))files.delete(id);
  jdbc.update("UPDATE chat_message SET attachment_id=NULL,body='',gift_code=NULL,gift_name=NULL,gift_asset_path=NULL,deleted_at=COALESCE(deleted_at,clock_timestamp()) WHERE space_id=?",f.space);
  jdbc.update("DELETE FROM chat_attachment WHERE space_id=?",f.space);jdbc.update("DELETE FROM chat_upload_intent WHERE space_id=?",f.space);f.cleanup();
 }
 ChatService.Attachment register(UUID actor,UUID key,String filename)throws Exception{
  var checked=files.validate(new MockMultipartFile("file",filename,"application/octet-stream",Files.readAllBytes(MemoryFixtures.root.resolve(filename))));try{return media.register(actor,key,checked);}finally{files.discardStaged(checked.id());}
 }
 ChatService.Send command(UUID key,UUID attachment){return new ChatService.Send(key,"inline caption","FILE",null,null,null,attachment);}
 HttpResponse<String> upload(PairingIntegrationTest.Browser browser,UUID key,String filename,boolean csrf,boolean voice)throws Exception{
  String boundary="chatMedia"+UUID.randomUUID().toString().replace("-","");String header="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+filename+"\"\r\nContent-Type: application/octet-stream\r\n\r\n";
  var body=HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofString(header,StandardCharsets.UTF_8),HttpRequest.BodyPublishers.ofFile(MemoryFixtures.root.resolve(filename)),HttpRequest.BodyPublishers.ofString("\r\n--"+boundary+"--\r\n"));
  var request=HttpRequest.newBuilder(f.transport.uri("/api/chat/uploads"+(voice?"?voice=true":""))).header("Idempotency-Key",key.toString()).header("Content-Type","multipart/form-data; boundary="+boundary);if(csrf)request.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(request.POST(body).build(),HttpResponse.BodyHandlers.ofString());
 }
 @Test void privateHttpUploadRequiresCsrfAndPartnerOnlySeesCommittedFileWithRangeSupport()throws Exception{
  UUID key=UUID.randomUUID();assertEquals(401,upload(f.transport.new Browser(),key,"moment.png",false,false).statusCode());assertEquals(403,upload(f.a,key,"moment.png",false,false).statusCode());
  var response=upload(f.a,key,"moment.png",true,false);assertEquals(201,response.statusCode(),response.body());UUID id=UUID.fromString(f.transport.tree(response).get("id").asText());String path="/api/chat/assets/"+id;
  assertEquals(401,f.transport.new Browser().get(path).statusCode());assertEquals(404,f.b.get(path).statusCode());assertEquals(200,f.a.get(path).statusCode());
  var message=chat.send(f.aId,command(key,id));assertEquals(id,message.attachment().id());assertEquals(message.id(),chat.send(f.aId,command(key,id)).id());assertEquals(200,f.b.get(path).statusCode());
  var partial=f.b.client.send(HttpRequest.newBuilder(f.transport.uri(path)).header("Range","bytes=0-15").GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(206,partial.statusCode());assertEquals(16,partial.body().length);assertTrue(partial.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
  chat.hide(f.bId,message.id());assertEquals(404,f.b.get(path).statusCode());assertEquals(200,f.a.get(path).statusCode());chat.delete(f.aId,message.id());assertEquals(404,f.a.get(path).statusCode());media.cleanup();assertFalse(Files.exists(files.path(id,false)));
 }
 @Test void cancellationBeforeDelayedUploadAndAfterCommitIsDurableAndIdempotent()throws Exception{
  UUID cancelled=UUID.randomUUID();media.cancel(f.aId,cancelled);media.cancel(f.aId,cancelled);assertEquals("UPLOAD_CANCELLED",assertThrows(ApiFailure.class,()->register(f.aId,cancelled,"moment.png")).code());
  UUID key=UUID.randomUUID();var attachment=register(f.aId,key,"moment.png");var request=command(key,attachment.id());var sent=chat.send(f.aId,request);assertEquals("REMOVED",media.cancel(f.aId,key).state());media.cancel(f.aId,key);assertNotNull(chat.snapshots(f.bId,List.of(sent.id())).items().getFirst().deletedAt());assertEquals("UPLOAD_CANCELLED",assertThrows(ApiFailure.class,()->chat.send(f.aId,request)).code());assertThrows(ApiFailure.class,()->media.download(f.bId,attachment.id(),false,false));media.cleanup();assertEquals("PURGED",jdbc.queryForObject("SELECT state FROM chat_attachment WHERE id=?",String.class,attachment.id()));
 }
 @Test void racingSendAndCancelAlwaysEndsWithNoAccessibleAttachment()throws Exception{
  UUID key=UUID.randomUUID();var attachment=register(f.aId,key,"moment.png");var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){
   var sending=pool.submit(()->{start.await();try{chat.send(f.aId,command(key,attachment.id()));}catch(ApiFailure failure){assertEquals("UPLOAD_CANCELLED",failure.code());}return null;});
   var cancelling=pool.submit(()->{start.await();return media.cancel(f.aId,key);});start.countDown();sending.get(10,TimeUnit.SECONDS);cancelling.get(10,TimeUnit.SECONDS);
  }assertThrows(ApiFailure.class,()->media.download(f.bId,attachment.id(),false,false));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND deleted_at IS NULL",Integer.class,f.space));
 }
 @Test void cancellingHiddenOwnSendStillRevokesPartnerFileAccess()throws Exception{
  UUID key=UUID.randomUUID();var attachment=register(f.aId,key,"moment.png");var sent=chat.send(f.aId,command(key,attachment.id()));chat.hide(f.aId,sent.id());media.cancel(f.aId,key);assertThrows(ApiFailure.class,()->media.download(f.bId,attachment.id(),false,false));assertTrue(chat.snapshots(f.aId,List.of(sent.id())).items().isEmpty());assertNotNull(chat.snapshots(f.bId,List.of(sent.id())).items().getFirst().deletedAt());
 }
 @Test void metadataDedupScopeAndNativeVideoAndVoiceAreValidated()throws Exception{
  UUID key=UUID.randomUUID();var video=register(f.aId,key,"short.mp4");assertEquals("VIDEO",video.kind());assertEquals(video.id(),register(f.aId,key,"short.mp4").id());assertThrows(ApiFailure.class,()->chat.send(f.bId,command(key,video.id())));
  var message=chat.send(f.aId,command(key,video.id()));assertNotNull(chat.snapshots(f.bId,List.of(message.id())).items().getFirst().attachment().thumbnail());
  var voice=upload(f.a,UUID.randomUUID(),"voice.webm",true,true);assertEquals(201,voice.statusCode(),voice.body());var audio=f.transport.tree(voice);assertEquals("VOICE",audio.get("kind").asText());assertEquals("audio/mp4",audio.get("mime").asText());assertTrue(audio.get("duration").asDouble()>0);
  var invalidVoice=upload(f.a,UUID.randomUUID(),"short.mp4",true,true);assertEquals(400,invalidVoice.statusCode());assertEquals("INVALID_VOICE",f.transport.tree(invalidVoice).get("code").asText());
  var longVideo=upload(f.a,UUID.randomUUID(),"181-seconds.mp4",true,false);assertEquals(400,longVideo.statusCode());assertEquals("VIDEO_TOO_LONG",f.transport.tree(longVideo).get("code").asText());
 }
 @Test void actualReadReceiptsOnlyReadCorrespondingInboxEntriesAndRollbackTogether(){
  var first=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"unseen","TEXT",null,null));var second=chat.send(f.aId,new ChatService.Send(UUID.randomUUID(),"visible","TEXT",null,null));
  chat.acknowledge(f.bId,new ChatService.Ack(List.of(first.id(),second.id())),false);assertEquals(2,unread());
  new TransactionTemplate(transactions).executeWithoutResult(tx->{chat.acknowledge(f.bId,new ChatService.Ack(List.of(second.id())),true);assertEquals(1,unread());tx.setRollbackOnly();});assertEquals(2,unread());
  chat.acknowledge(f.bId,new ChatService.Ack(List.of(second.id())),true);assertEquals(1,unread());assertEquals(first.id(),jdbc.queryForObject("SELECT target_id FROM notification_inbox WHERE space_id=? AND read_at IS NULL",UUID.class,f.space));chat.acknowledge(f.bId,new ChatService.Ack(List.of(second.id())),true);assertEquals(1,unread());
 }
 int unread(){return jdbc.queryForObject("SELECT count(*) FROM notification_inbox WHERE space_id=? AND type='MESSAGE' AND read_at IS NULL",Integer.class,f.space);}
 @Test void textAndStickerSendsShareDurableCancellationAndCannotResurrectOnRetry(){
  for(String type:List.of("TEXT","GIFT")){
   UUID key=UUID.randomUUID();String gift=type.equals("GIFT")?chat.gifts(f.aId,null,null,0).items().getFirst().code():null;
   var command=new ChatService.Send(key,"pending",type,gift,null);media.cancel(f.aId,key);assertEquals("UPLOAD_CANCELLED",assertThrows(ApiFailure.class,()->chat.send(f.aId,command)).code());
   UUID committedKey=UUID.randomUUID();var committed=new ChatService.Send(committedKey,"pending",type,gift,null);var message=chat.send(f.aId,committed);media.cancel(f.aId,committedKey);assertNotNull(chat.snapshots(f.bId,List.of(message.id())).items().getFirst().deletedAt());assertEquals("UPLOAD_CANCELLED",assertThrows(ApiFailure.class,()->chat.send(f.aId,committed)).code());
  }
 }
}
