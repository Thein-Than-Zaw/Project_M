package com.couple.privatespace.memory;

import com.couple.privatespace.identity.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

@RestController
public class MemoryController {
    private final MemoryService memories;private final MemoryFiles files;private final MemoryMaintenance maintenance;
    public MemoryController(MemoryService memories,MemoryFiles files,MemoryMaintenance maintenance){this.memories=memories;this.files=files;this.maintenance=maintenance;}
    private UUID key(String raw){try{return UUID.fromString(raw);}catch(Exception error){throw new ApiFailure(400,"SAVE_KEY_REQUIRED","A valid retry identifier is required.");}}
    @GetMapping("/api/memories") public MemoryService.Page list(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(defaultValue="ALL") String filter,@RequestParam(required=false) UUID before){return memories.list(actor.accountId(),filter,before);}
    @GetMapping("/api/memories/{id}") public MemoryService.Entry get(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestParam(defaultValue="false") boolean trash){return memories.get(actor.accountId(),id,trash);}
    @PostMapping("/api/memory-uploads") @ResponseStatus(HttpStatus.CREATED)
    public MemoryService.Asset upload(@AuthenticationPrincipal AccountPrincipal actor,@RequestHeader(value="Idempotency-Key",required=false) String raw,@RequestParam MultipartFile file){
        UUID key=key(raw);var checked=files.validate(file);try{return memories.registerUpload(actor.accountId(),key,checked);}finally{files.discardStaged(checked.id());}
    }
    @PostMapping("/api/memories") @ResponseStatus(HttpStatus.CREATED)
    public MemoryService.Entry create(@AuthenticationPrincipal AccountPrincipal actor,@RequestHeader(value="Idempotency-Key",required=false) String raw,@RequestBody MemoryService.Edit body){return memories.create(actor.accountId(),key(raw),body);}
    @DeleteMapping("/api/memory-uploads/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id){memories.discardUpload(actor.accountId(),id);maintenance.runOnce();}
    @PatchMapping("/api/memories/{id}") public MemoryService.Entry edit(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody MemoryService.Edit body){return memories.edit(actor.accountId(),id,body);}
    @PutMapping("/api/memories/{id}/favorite") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody MemoryService.Favorite body){memories.favorite(actor.accountId(),id,body.favorite());}
    @DeleteMapping("/api/memories/{id}") public MemoryService.Entry trash(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestParam Long version){return memories.trash(actor.accountId(),id,version);}
    @PostMapping("/api/memories/{id}/restore") public MemoryService.Entry restore(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody MemoryService.Version body){return memories.restore(actor.accountId(),id,body.version());}
    @GetMapping("/api/memories/trash/preview") public MemoryService.TrashPreview preview(@AuthenticationPrincipal AccountPrincipal actor){return memories.previewTrash(actor.accountId());}
    @PostMapping("/api/memories/trash/empty") public ResponseEntity<MemoryMaintenance.Result> empty(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody MemoryService.EmptyTrash body){var result=maintenance.purgeIds(memories.emptyTrash(actor.accountId(),body));return ResponseEntity.status(result.pending()>0?202:200).body(result);}
    @GetMapping("/api/memory-assets/{id}") public ResponseEntity<Resource> download(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestParam(defaultValue="false") boolean thumbnail,@RequestParam(defaultValue="false") boolean download){
        var file=memories.download(actor.accountId(),id,thumbnail,download);
        var disposition=(file.attachment()?ContentDisposition.attachment():ContentDisposition.inline()).filename(file.name(),StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header(HttpHeaders.CACHE_CONTROL,"no-store, private").contentType(MediaType.parseMediaType(file.mime())).contentLength(file.size()).body(new FileSystemResource(file.path()));
    }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<Map<String,String>> databaseFailure(){return ResponseEntity.status(503).body(Map.of("code","MEMORY_NOT_SAVED","message","We could not confirm this change. Keep your draft and retry with the same save identifier."));}
    @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.status(413).body(Map.of("code","FILE_TOO_LARGE","message","This file exceeds its upload limit."));}
}
