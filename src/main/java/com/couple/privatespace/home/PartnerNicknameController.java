package com.couple.privatespace.home;
import com.couple.privatespace.identity.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class PartnerNicknameController {
 private final PartnerNicknameService nicknames;public PartnerNicknameController(PartnerNicknameService nicknames){this.nicknames=nicknames;}
 @GetMapping("/api/partner-nickname") public PartnerNicknameService.Label current(@AuthenticationPrincipal AccountPrincipal actor){return nicknames.current(actor.accountId());}
 @PutMapping("/api/partner-nickname") public PartnerNicknameService.Label edit(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody PartnerNicknameService.Edit edit){return nicknames.edit(actor.accountId(),edit);}
}
