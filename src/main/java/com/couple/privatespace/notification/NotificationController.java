package com.couple.privatespace.notification;

import com.couple.privatespace.identity.AccountPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {
 private final NotificationService service;
 public NotificationController(NotificationService service){this.service=service;}
 @GetMapping("/api/notifications") public NotificationService.Page list(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(required=false) Long before,@RequestParam(defaultValue="false") boolean unreadOnly){return service.list(actor.accountId(),before,unreadOnly);}
 @PatchMapping("/api/notifications/{id}") public NotificationService.Item edit(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody NotificationService.Edit request){return service.edit(actor.accountId(),id,request);}
 @GetMapping("/api/notifications/{id}/target") public NotificationService.Target target(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id){return service.target(actor.accountId(),id);}
}
