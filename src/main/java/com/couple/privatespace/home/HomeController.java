package com.couple.privatespace.home;

import com.couple.privatespace.identity.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class HomeController {
    private final HomeService home;
    public HomeController(HomeService home) {this.home=home;}
    @GetMapping("/api/home") public HomeService.Home home(@AuthenticationPrincipal AccountPrincipal actor) {return home.home(actor.accountId());}
    @GetMapping("/api/dates") public HomeService.Dates dates(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(defaultValue="0") int page) {return home.dates(actor.accountId(),page);}
    @PatchMapping("/api/profile") public HomeService.Profile profile(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody HomeService.ProfileEdit body) {return home.editProfile(actor.accountId(),body);}
    @PatchMapping("/api/relationship-date") public HomeService.Home relationship(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody HomeService.RelationshipEdit body) {return home.editRelationship(actor.accountId(),body);}
    @PostMapping("/api/dates") @ResponseStatus(HttpStatus.CREATED)
    public HomeService.SpecialDate create(@AuthenticationPrincipal AccountPrincipal actor,@RequestHeader(value="Idempotency-Key",required=false) String rawKey,@RequestBody HomeService.EventEdit body) {
        UUID key;try{key=UUID.fromString(rawKey);}catch(Exception error){throw new ApiFailure(400,"IDEMPOTENCY_KEY_REQUIRED","A valid save identifier is required.");}
        return home.create(actor.accountId(),key,body);
    }
    @PatchMapping("/api/dates/{id}") public HomeService.SpecialDate edit(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody HomeService.EventEdit body) {return home.edit(actor.accountId(),id,body);}
    @DeleteMapping("/api/dates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestParam Long version) {home.delete(actor.accountId(),id,version);}
}
