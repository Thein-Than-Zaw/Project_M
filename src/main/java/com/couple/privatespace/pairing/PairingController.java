package com.couple.privatespace.pairing;

import com.couple.privatespace.identity.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class PairingController {
    private final PairingService service;
    public PairingController(PairingService service) { this.service=service; }
    @GetMapping("/api/space")
    public PairingService.Space current(@AuthenticationPrincipal AccountPrincipal actor) { return service.current(actor.accountId()); }
    @PostMapping("/api/spaces") @ResponseStatus(HttpStatus.CREATED)
    public PairingService.Space create(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody PairingService.Create body) { return service.create(actor.accountId(),body); }
    @PostMapping("/api/invitations") @ResponseStatus(HttpStatus.CREATED)
    public PairingService.Invitation invite(@AuthenticationPrincipal AccountPrincipal actor) { return service.invite(actor.accountId()); }
    @PostMapping("/api/invitations/preview")
    public PairingService.Preview preview(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody PairingService.Code body) { return service.preview(actor.accountId(),body.code()); }
    @PostMapping("/api/invitations/join")
    public PairingService.Space join(@AuthenticationPrincipal AccountPrincipal actor,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody PairingService.Join body) {
        UUID id;
        try { id=UUID.fromString(key); } catch(Exception error) { throw new ApiFailure(400,"IDEMPOTENCY_KEY_REQUIRED","A valid request identifier is required. Review and try again."); }
        return service.join(actor.accountId(),id,body);
    }
}
