package com.couple.privatespace.chat;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.memory.MemoryFiles;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

@RestController
public class ChatAttachmentController {
 private final ChatAttachmentService media;private final MemoryFiles files;
 public ChatAttachmentController(ChatAttachmentService media,MemoryFiles files){this.media=media;this.files=files;}
 @PostMapping("/api/chat/uploads") @ResponseStatus(HttpStatus.CREATED)
 public ChatService.Attachment upload(@AuthenticationPrincipal AccountPrincipal actor,@RequestHeader("Idempotency-Key") UUID key,@RequestParam MultipartFile file,@RequestParam(defaultValue="false") boolean voice){
  var checked=voice?files.validateVoice(file):files.validate(file);try{return media.register(actor.accountId(),key,checked);}finally{files.discardStaged(checked.id());}
 }
 @DeleteMapping("/api/chat/uploads/intents/{key}")
 public ChatAttachmentService.Cancellation cancel(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID key){return media.cancel(actor.accountId(),key);}
 @GetMapping("/api/chat/assets/{id}")
 public ResponseEntity<Resource> download(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestParam(defaultValue="false") boolean thumbnail,@RequestParam(defaultValue="false") boolean download){
  var file=media.download(actor.accountId(),id,thumbnail,download);var disposition=(file.attachment()?ContentDisposition.attachment():ContentDisposition.inline()).filename(file.name(),StandardCharsets.UTF_8).build();return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(file.mime())).contentLength(file.size()).body(file.resource());
 }
 @ExceptionHandler(org.springframework.dao.DataAccessException.class)
 public ResponseEntity<Map<String,String>> databaseFailure(){return ResponseEntity.status(503).body(Map.of("code","CHAT_MEDIA_NOT_SAVED","message","We could not confirm this change. Retry with the same upload identifier or cancel sending."));}
}
