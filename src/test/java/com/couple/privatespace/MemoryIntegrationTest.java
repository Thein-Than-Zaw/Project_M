package com.couple.privatespace;

import com.couple.privatespace.memory.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class MemoryIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;@Autowired MemoryFiles files;@Autowired MemoryMaintenance maintenance;
 final ChatIntegrationTest f=new ChatIntegrationTest();
 @BeforeAll static void nativeFixtures()throws Exception{MemoryFixtures.prepare();}
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup()throws Exception{
  if(!jdbc.queryForObject("SELECT current_database()",String.class).endsWith("_test"))return;
  for(UUID id:jdbc.queryForList("SELECT id FROM memory_asset WHERE space_id=?",UUID.class,f.space))files.delete(id);
  jdbc.update("DELETE FROM memory_favorite WHERE memory_id IN (SELECT id FROM memory WHERE space_id=?)",f.space);jdbc.update("DELETE FROM memory_asset WHERE space_id=?",f.space);jdbc.update("DELETE FROM memory WHERE space_id=?",f.space);jdbc.update("DELETE FROM memory_receipt WHERE actor_id IN (?,?)",f.aId,f.bId);f.cleanup();
 }
 HttpResponse<String> upload(PairingIntegrationTest.Browser browser,UUID key,Path file,String filename,String mime)throws Exception{
  String boundary="memoryTest"+UUID.randomUUID().toString().replace("-","");String header="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+filename+"\"\r\nContent-Type: "+mime+"\r\n\r\n";
  var body=HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(header.getBytes(StandardCharsets.UTF_8)),HttpRequest.BodyPublishers.ofFile(file),HttpRequest.BodyPublishers.ofString("\r\n--"+boundary+"--\r\n"));
  return browser.client.send(HttpRequest.newBuilder(f.transport.uri("/api/memory-uploads")).header("X-CSRF-TOKEN",browser.csrf()).header("Idempotency-Key",key.toString()).header("Content-Type","multipart/form-data; boundary="+boundary).POST(body).build(),HttpResponse.BodyHandlers.ofString());
 }
 JsonNode asset(PairingIntegrationTest.Browser browser,String file)throws Exception{String ext=file.substring(file.lastIndexOf('.')+1);var response=upload(browser,UUID.randomUUID(),MemoryFixtures.root.resolve(file),file,MemoryFiles.TYPES.get(ext));assertEquals(201,response.statusCode(),response.body());return f.transport.tree(response);}
 Map<String,Object> body(List<JsonNode> assets,String title,Long version){var map=new HashMap<String,Object>();map.put("title",title);map.put("caption","မြန်မာစာ ❤️ <script>plain text</script>");map.put("memoryDate","2025-01-01");map.put("assetIds",assets.stream().map(a->a.get("id").asText()).toList());if(version!=null)map.put("version",version);return map;}
 JsonNode create(PairingIntegrationTest.Browser browser,List<JsonNode> assets,String title)throws Exception{var response=browser.post("/api/memories",body(assets,title,null),UUID.randomUUID());assertEquals(201,response.statusCode(),response.body());return f.transport.tree(response);}
 HttpResponse<String> change(PairingIntegrationTest.Browser b,String method,String path,Object data)throws Exception{return b.client.send(HttpRequest.newBuilder(f.transport.uri(path)).header("X-CSRF-TOKEN",b.csrf()).header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(data==null?"":f.transport.json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());}
 HttpResponse<byte[]> bytes(PairingIntegrationTest.Browser b,String path,String range)throws Exception{var request=HttpRequest.newBuilder(f.transport.uri(path));if(range!=null)request.header("Range",range);return b.client.send(request.build(),HttpResponse.BodyHandlers.ofByteArray());}
 @Test void realFormatsPersistPrivatelyAndSupportRangesAndDownloadIdentity()throws Exception{
  assertEquals(401,f.transport.new Browser().get("/api/memories").statusCode());assertEquals(200,f.a.get("/memories.html").statusCode());
  var assets=new ArrayList<JsonNode>();for(String file:List.of("moment.jpg","moment.png","moment.webp","short.mp4","note.pdf","note.docx","note.xlsx","note.pptx"))assets.add(asset(f.a,file));
  assertEquals(404,f.b.get(assets.getFirst().get("url").asText()).statusCode(),"Partner cannot borrow unpublished drafts");var memory=create(f.a,assets,"A real shared memory");
  assertEquals(f.aId.toString(),memory.get("createdBy").asText());assertEquals(8,memory.get("assets").size());
  for(int i=0;i<assets.size();i++){var a=assets.get(i);String url=a.get("url").asText();var response=bytes(f.b,url,null);assertEquals(200,response.statusCode());assertEquals(a.get("size").asLong(),response.body().length);String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(response.body()));assertEquals(a.get("sha256").asText(),hash);assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));if(a.get("kind").asText().equals("DOCUMENT"))assertTrue(response.headers().firstValue("Content-Disposition").orElseThrow().startsWith("attachment"));else{var thumb=bytes(f.b,a.get("thumbnail").asText(),null);assertEquals(200,thumb.statusCode());assertEquals("image/jpeg",thumb.headers().firstValue("Content-Type").orElseThrow());}}
  var range=bytes(f.b,assets.get(3).get("url").asText(),"bytes=0-15");assertEquals(206,range.statusCode());assertEquals(16,range.body().length);assertTrue(range.headers().firstValue("Content-Range").orElseThrow().startsWith("bytes 0-15/"));
  assertEquals(1,f.transport.tree(f.b.get("/api/memories?filter=DOCUMENT")).get("items").size());var again=f.transport.new Browser();again.login(f.prefix+"b@example.invalid");assertEquals(8,f.transport.tree(again.get("/api/memories/"+memory.get("id").asText())).get("assets").size());
  jdbc.update("DELETE FROM couple_membership WHERE user_id=?",f.bId);assertEquals(403,f.b.get(assets.getFirst().get("url").asText()).statusCode());
 }
 @Test void bothMembersEditButCreatorAndFavoritePreferencesStayCorrectAndStaleWritesReject()throws Exception{
  var photo=asset(f.a,"moment.png");var pdf=asset(f.a,"note.pdf");var original=create(f.a,List.of(photo,pdf),"Original");String id=original.get("id").asText();var extra=asset(f.b,"note.docx");
  assertEquals(204,change(f.b,"PUT","/api/memories/"+id+"/favorite",Map.of("favorite",true)).statusCode());assertEquals(0,f.transport.tree(f.a.get("/api/memories?filter=FAVORITES")).get("items").size());assertEquals(1,f.transport.tree(f.b.get("/api/memories?filter=FAVORITES")).get("items").size());
  var response=change(f.b,"PATCH","/api/memories/"+id,body(List.of(pdf,extra),"Partner edited",0L));assertEquals(200,response.statusCode(),response.body());var edited=f.transport.tree(response);assertEquals(f.aId.toString(),edited.get("createdBy").asText());assertEquals(f.bId.toString(),edited.get("updatedBy").asText());assertEquals(1,edited.get("version").asInt());assertEquals(404,f.a.get(photo.get("url").asText()).statusCode());
  assertEquals(409,change(f.a,"PATCH","/api/memories/"+id,body(List.of(photo,pdf),"Stale overwrite",0L)).statusCode());maintenance.runOnce();assertFalse(Files.exists(files.path(UUID.fromString(photo.get("id").asText()),false)));assertEquals("Partner edited",f.transport.tree(f.a.get("/api/memories/"+id)).get("title").asText());
 }
 @Test void trashHidesFilesForBothAndRestoreKeepsBytesThen72HourExpiryPurges()throws Exception{
  var photo=asset(f.a,"moment.png");var m=create(f.a,List.of(photo),"Trash lifecycle");String id=m.get("id").asText();var removed=change(f.b,"DELETE","/api/memories/"+id+"?version=0",null);assertEquals(200,removed.statusCode());var trash=f.transport.tree(removed);assertEquals(Duration.ofHours(72),Duration.between(Instant.parse(trash.get("deletedAt").asText()),Instant.parse(trash.get("expiresAt").asText())));assertEquals(f.bId.toString(),trash.get("deletedBy").asText());
  assertEquals(0,f.transport.tree(f.a.get("/api/memories")).get("items").size());assertEquals(1,f.transport.tree(f.a.get("/api/memories?filter=TRASH")).get("items").size());assertEquals(404,f.a.get("/api/memories/"+id).statusCode());assertEquals(404,f.b.get(photo.get("url").asText()).statusCode());
  var restore=f.a.post("/api/memories/"+id+"/restore",Map.of("version",1),null);assertEquals(200,restore.statusCode());assertEquals(200,f.b.get(photo.get("url").asText()).statusCode());assertEquals(photo.get("sha256"),f.transport.tree(restore).get("assets").get(0).get("sha256"));
  assertEquals(200,change(f.a,"DELETE","/api/memories/"+id+"?version=2",null).statusCode());jdbc.update("UPDATE memory SET deleted_at=statement_timestamp()-INTERVAL '73 hours',expires_at=statement_timestamp()-INTERVAL '1 hour' WHERE id=?",UUID.fromString(id));
  assertEquals(404,f.b.post("/api/memories/"+id+"/restore",Map.of("version",3),null).statusCode());assertEquals(0,f.transport.tree(f.b.get("/api/memories?filter=TRASH")).get("items").size());maintenance.runOnce();assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory WHERE id=?",Integer.class,UUID.fromString(id)));assertFalse(Files.exists(files.path(UUID.fromString(photo.get("id").asText()),false)));
 }
 @Test void emptyTrashSnapshotDoesNotSilentlyIncludeConcurrentDeletionOrActiveMemories()throws Exception{
  var a=asset(f.a,"moment.png");var b=asset(f.a,"note.pdf");var c=asset(f.a,"note.docx");var one=create(f.a,List.of(a),"One");var two=create(f.a,List.of(b),"Two");var active=create(f.a,List.of(c),"Keep active");String id1=one.get("id").asText(),id2=two.get("id").asText();
  change(f.a,"DELETE","/api/memories/"+id1+"?version=0",null);var preview=f.transport.tree(f.a.get("/api/memories/trash/preview"));change(f.b,"DELETE","/api/memories/"+id2+"?version=0",null);
  assertEquals(409,f.a.post("/api/memories/trash/empty",Map.of("snapshot",preview.get("snapshot").asText(),"confirmed",true),null).statusCode());preview=f.transport.tree(f.b.get("/api/memories/trash/preview"));assertEquals(2,preview.get("count").asInt());assertEquals(400,f.b.post("/api/memories/trash/empty",Map.of("snapshot",preview.get("snapshot").asText(),"confirmed",false),null).statusCode());
  var response=f.b.post("/api/memories/trash/empty",Map.of("snapshot",preview.get("snapshot").asText(),"confirmed",true),null);assertEquals(200,response.statusCode(),response.body());assertEquals(2,f.transport.tree(response).get("deleted").asInt());assertEquals(200,f.a.get("/api/memories/"+active.get("id").asText()).statusCode());assertFalse(Files.exists(files.path(UUID.fromString(a.get("id").asText()),false)));assertFalse(Files.exists(files.path(UUID.fromString(b.get("id").asText()),false)));
 }
 @Test void databaseRejectionKeepsUnpublishedUploadAndSameCreateKeyCanRetryWithoutDuplication()throws Exception{
  var a=asset(f.a,"note.pdf");UUID key=UUID.randomUUID();var body=body(List.of(a),"native_publish_failure",null);jdbc.execute("ALTER TABLE memory ADD CONSTRAINT dev07_failure_fixture CHECK(title<>'native_publish_failure')");
  try{assertEquals(503,f.a.post("/api/memories",body,key).statusCode());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory",Integer.class));assertEquals(200,f.a.get(a.get("url").asText()).statusCode());assertEquals(404,f.b.get(a.get("url").asText()).statusCode());}finally{jdbc.execute("ALTER TABLE memory DROP CONSTRAINT dev07_failure_fixture");}
  var first=f.a.post("/api/memories",body,key);assertEquals(201,first.statusCode());for(int i=0;i<3;i++)assertEquals(f.transport.tree(first).get("id"),f.transport.tree(f.a.post("/api/memories",body,key)).get("id"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM memory",Integer.class));body.put("title","Changed intent");assertEquals(409,f.a.post("/api/memories",body,key).statusCode());
 }
 @Test void realUploadRetryDoesNotBorrowAnotherDraftAndPurgeNeverResurrectsOldKeys()throws Exception{
  UUID uploadKey=UUID.randomUUID();var response=upload(f.a,uploadKey,MemoryFixtures.root.resolve("note.pdf"),"note.pdf","application/pdf");assertEquals(201,response.statusCode());var a=f.transport.tree(response);assertEquals(a.get("id"),f.transport.tree(upload(f.a,uploadKey,MemoryFixtures.root.resolve("note.pdf"),"note.pdf","application/pdf")).get("id"));assertEquals(400,f.b.post("/api/memories",body(List.of(a),"Borrow draft",null),UUID.randomUUID()).statusCode());
  UUID createKey=UUID.randomUUID();var command=body(List.of(a),"Never resurrect",null);var m=f.transport.tree(f.a.post("/api/memories",command,createKey));change(f.a,"DELETE","/api/memories/"+m.get("id").asText()+"?version=0",null);var preview=f.transport.tree(f.a.get("/api/memories/trash/preview"));f.a.post("/api/memories/trash/empty",Map.of("snapshot",preview.get("snapshot").asText(),"confirmed",true),null);
  assertEquals(404,f.a.post("/api/memories",command,createKey).statusCode());assertEquals(410,upload(f.a,uploadKey,MemoryFixtures.root.resolve("note.pdf"),"note.pdf","application/pdf").statusCode());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory_asset",Integer.class));
 }
 @Test void realVideoDurationAndHundredMegabyteBoundaryAreEnforcedServerSide()throws Exception{
  var exact=asset(f.a,"180-seconds.mp4");assertEquals(180.0,exact.get("duration").asDouble());assertEquals(400,upload(f.a,UUID.randomUUID(),MemoryFixtures.root.resolve("181-seconds.mp4"),"long.mp4","video/mp4").statusCode());
  var full=MemoryFixtures.paddedVideo(100000000);var response=upload(f.a,UUID.randomUUID(),full,"full.mp4","video/mp4");assertEquals(201,response.statusCode(),response.body());assertEquals(100000000,f.transport.tree(response).get("size").asLong());
  var over=MemoryFixtures.paddedVideo(100000001);assertEquals(413,upload(f.a,UUID.randomUUID(),over,"over.mp4","video/mp4").statusCode());Files.delete(full);Files.delete(over);
 }
 @Test void malformedSpoofedAndMacroDocumentsRejectAndFileNamesNeverChooseStoragePaths()throws Exception{
  Path bad=MemoryFixtures.root.resolve("bad.docx");Files.write(bad,MemoryFixtures.office("docx",true));assertEquals(400,upload(f.a,UUID.randomUUID(),bad,"macro.docx",MemoryFiles.TYPES.get("docx")).statusCode());
  assertEquals(400,upload(f.a,UUID.randomUUID(),MemoryFixtures.root.resolve("moment.png"),"fake.pdf","application/pdf").statusCode());assertEquals(400,upload(f.a,UUID.randomUUID(),MemoryFixtures.root.resolve("moment.jpg"),"fake.png","image/png").statusCode());assertEquals(400,upload(f.a,UUID.randomUUID(),MemoryFixtures.root.resolve("note.pdf"),"run.exe","application/octet-stream").statusCode());
  Path playlist=MemoryFixtures.root.resolve("indirect.png");Files.writeString(playlist,"ffconcat version 1.0\nfile '"+MemoryFixtures.root.resolve("moment.png").toAbsolutePath().toString().replace('\\','/')+"'\n");var indirect=upload(f.a,UUID.randomUUID(),playlist,"indirect.png","image/png");assertEquals(400,indirect.statusCode());assertEquals("FILE_TYPE_MISMATCH",f.transport.tree(indirect).get("code").asText());
  var ok=upload(f.a,UUID.randomUUID(),MemoryFixtures.root.resolve("note.docx"),"../../private.docx",MemoryFiles.TYPES.get("docx"));assertEquals(201,ok.statusCode(),ok.body());var a=f.transport.tree(ok);assertEquals("private.docx",a.get("name").asText());assertTrue(Files.exists(files.path(UUID.fromString(a.get("id").asText()),false)));assertEquals(0,f.transport.tree(f.a.get("/api/memories")).get("items").size());
 }

 @Test void tenMixedAttachmentsPersistReorderedButElevenAndStaleDeleteReject()throws Exception{
  var uploads=new ArrayList<JsonNode>();
  for(int i=0;i<11;i++)uploads.add(asset(f.a,i%2==0?"moment.png":"note.pdf"));
  assertEquals(400,f.a.post("/api/memories",body(uploads,"Too many",null),UUID.randomUUID()).statusCode());
  var kept=new ArrayList<>(uploads.subList(0,10));var memory=create(f.a,kept,"Ten files together");String id=memory.get("id").asText();
  assertEquals(10,memory.get("assets").size());Collections.reverse(kept);
  var edited=change(f.b,"PATCH","/api/memories/"+id,body(kept,"Reordered by partner",0L));assertEquals(200,edited.statusCode(),edited.body());
  for(int i=0;i<10;i++)assertEquals(kept.get(i).get("id"),f.transport.tree(edited).get("assets").get(i).get("id"));
  assertEquals(409,change(f.a,"DELETE","/api/memories/"+id+"?version=0",null).statusCode());
  assertEquals(200,f.a.get("/api/memories/"+id).statusCode());
 }

 @Test void concurrentCreateRetriesCommitOneMemory()throws Exception{
  var a=asset(f.a,"note.pdf");var command=body(List.of(a),"Concurrent same save",null);UUID key=UUID.randomUUID();
  var other=f.transport.new Browser();other.login(f.prefix+"a@example.invalid");
  var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  try{
   var first=pool.submit(()->{start.await();return f.a.post("/api/memories",command,key);});
   var second=pool.submit(()->{start.await();return other.post("/api/memories",command,key);});start.countDown();
   var x=first.get(15,TimeUnit.SECONDS);var y=second.get(15,TimeUnit.SECONDS);assertEquals(201,x.statusCode(),x.body());assertEquals(201,y.statusCode(),y.body());assertEquals(f.transport.tree(x).get("id"),f.transport.tree(y).get("id"));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM memory WHERE space_id=?",Integer.class,f.space));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM memory_receipt WHERE actor_id=? AND operation='CREATE'",Integer.class,f.aId));
  }finally{pool.shutdownNow();}
 }

 @Test void storageFailureKeepsDurableUnrestorablePurgeAndMaintenanceRetries()throws Exception{
  var a=asset(f.a,"note.pdf");var memory=create(f.a,List.of(a),"Retry physical removal");String id=memory.get("id").asText();
  UUID assetId=UUID.fromString(a.get("id").asText());Path original=files.path(assetId,false),held=original.resolveSibling(assetId+".held");
  Files.move(original,held);Files.createDirectory(original);Path blocker=original.resolve("test-owned-blocker");Files.writeString(blocker,"Synthetic deletion failure");
  try{
   assertEquals(200,change(f.b,"DELETE","/api/memories/"+id+"?version=0",null).statusCode());var preview=f.transport.tree(f.a.get("/api/memories/trash/preview"));
   var result=f.a.post("/api/memories/trash/empty",Map.of("snapshot",preview.get("snapshot").asText(),"confirmed",true),null);assertEquals(202,result.statusCode(),result.body());assertEquals(1,f.transport.tree(result).get("pending").asInt());
   assertEquals("PURGING",jdbc.queryForObject("SELECT state FROM memory WHERE id=?",String.class,UUID.fromString(id)));
   assertEquals(404,f.b.get(a.get("url").asText()).statusCode());assertEquals(404,f.a.post("/api/memories/"+id+"/restore",Map.of("version",2),null).statusCode());
  }finally{Files.deleteIfExists(blocker);Files.deleteIfExists(original);Files.move(held,original);}
  maintenance.runOnce();assertFalse(Files.exists(original));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory WHERE id=?",Integer.class,UUID.fromString(id)));
 }

 @Test void abandonedUploadsAndUnreferencedPrivateFilesExpireAfter24Hours()throws Exception{
  var old=asset(f.a,"moment.png");UUID assetId=UUID.fromString(old.get("id").asText());
  jdbc.update("UPDATE memory_asset SET created_at=clock_timestamp()-INTERVAL '25 hours' WHERE id=?",assetId);
  assertEquals(404,f.a.get(old.get("url").asText()).statusCode());
  Path orphan=files.path(UUID.randomUUID(),false);Files.writeString(orphan,"Synthetic unreferenced object");Files.setLastModifiedTime(orphan,java.nio.file.attribute.FileTime.from(Instant.now().minus(Duration.ofHours(25))));
  maintenance.runOnce();assertFalse(Files.exists(orphan));assertFalse(Files.exists(files.path(assetId,false)));assertFalse(Files.exists(files.path(assetId,true)));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory_asset WHERE id=?",Integer.class,assetId));
 }

 @Test void officeNamespaceSpoofAndExternalEntityAreRejected()throws Exception{
  for(String xml:List.of("<document xmlns='https://invalid.example/wordprocessingml/2006/main'/>","<!DOCTYPE document [<!ENTITY external SYSTEM 'file:///not-readable-by-test'>]><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>&external;</w:body></w:document>")){
   Path bad=MemoryFixtures.root.resolve("unsafe.docx");
   try(var source=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(MemoryFixtures.office("docx",false)));var output=new java.util.zip.ZipOutputStream(Files.newOutputStream(bad))){
    java.util.zip.ZipEntry entry;while((entry=source.getNextEntry())!=null){output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));output.write(entry.getName().equals("word/document.xml")?xml.getBytes(StandardCharsets.UTF_8):source.readAllBytes());output.closeEntry();}
   }
   assertEquals(400,upload(f.a,UUID.randomUUID(),bad,"unsafe.docx",MemoryFiles.TYPES.get("docx")).statusCode());
  }
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory_asset WHERE space_id=?",Integer.class,f.space));
 }

 @Test void cursorPaginationHasNoDuplicatesAndDoesNotExposeOtherDrafts()throws Exception{
  var ids=new HashSet<String>();for(int i=0;i<25;i++)ids.add(create(f.a,List.of(asset(f.a,"note.pdf")),"Page memory "+i).get("id").asText());
  var first=f.transport.tree(f.b.get("/api/memories"));assertEquals(24,first.get("items").size());assertFalse(first.get("nextCursor").isNull());
  var seen=new HashSet<String>();for(var m:first.get("items"))assertTrue(seen.add(m.get("id").asText()));
  var second=f.transport.tree(f.b.get("/api/memories?before="+first.get("nextCursor").asText()));assertEquals(1,second.get("items").size());assertTrue(second.get("nextCursor").isNull());
  assertTrue(seen.add(second.get("items").get(0).get("id").asText()));assertEquals(ids,seen);
  assertEquals(400,f.b.get("/api/memories?before="+UUID.randomUUID()).statusCode());
 }

 @Test void onlyCommittedNewPublicationNotifiesPartnerAndRetriesDoNotNotifyTwice()throws Exception{
  try(var receiver=f.b.socket(true,"http://127.0.0.1:18080")){
   receiver.await("CONNECTED");receiver.send("SUBSCRIBE\nid:memory\ndestination:/user/queue/pairing\n\n\0");
   long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean subscribed=false;
   while(System.nanoTime()<until){var user=registry.getUser(f.prefix+"b@example.invalid");if(user!=null&&user.getSessions().stream().anyMatch(s->s.getSubscriptions().stream().anyMatch(sub->sub.getDestination().equals("/user/queue/pairing")))){subscribed=true;break;}Thread.sleep(10);}assertTrue(subscribed);
   var a=asset(f.a,"note.pdf");assertFalse(receiver.frames.stream().anyMatch(frame->frame.contains("memory.published")),"Drafts must not notify");
   UUID key=UUID.randomUUID();var command=body(List.of(a),"One publication notice",null);var saved=f.a.post("/api/memories",command,key);assertEquals(201,saved.statusCode());
   String first=receiver.await("memory.published");assertTrue(first.contains(f.transport.tree(saved).get("id").asText()));
   for(int i=0;i<3;i++)assertEquals(201,f.a.post("/api/memories",command,key).statusCode());
   assertNull(receiver.frames.poll(300,TimeUnit.MILLISECONDS),"No further event is emitted by create replay");
  }
 }
}
