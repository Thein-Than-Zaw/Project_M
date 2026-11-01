package com.couple.privatespace.identity;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final RegistrationService registration;
    private final AccountStore accounts;
    private final JdbcTemplate jdbc;
    public AuthController(RegistrationService registration, AccountStore accounts, JdbcTemplate jdbc) {
        this.registration = registration; this.accounts = accounts; this.jdbc = jdbc;
    }
    public record Csrf(String headerName, String token) { }
    public record Self(UUID id, String displayName, String email, LocalDate relationshipDate,
                       boolean paired, boolean canInvitePartner) { }
    @GetMapping("/api/csrf")
    public Csrf csrf(CsrfToken token) { return new Csrf(token.getHeaderName(), token.getToken()); }

    @PostMapping("/api/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public RegistrationService.Registered register(@RequestBody RegistrationService.Signup form) {
        return registration.register(form);
    }
    @GetMapping("/api/me")
    public Self me(@AuthenticationPrincipal AccountPrincipal principal) {
        var account = accounts.byId(principal.accountId()).orElseThrow(() -> new ApiFailure(401, "SESSION_EXPIRED", "Please sign in again."));
        boolean paired = jdbc.queryForObject("SELECT count(*) FROM couple_membership own WHERE own.user_id=? AND (SELECT count(*) FROM couple_membership member WHERE member.space_id=own.space_id)=2", Integer.class, account.id()) > 0;
        boolean canInvite = account.slot() == 1 && jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class) == 1;
        return new Self(account.id(), account.displayName(), account.email(), account.relationshipDate(), paired, canInvite);
    }
    @PostMapping("/api/auth/partner-registration-code")
    public RegistrationService.IssuedCode invite(@AuthenticationPrincipal AccountPrincipal principal) {
        return registration.issuePartnerCode(principal.accountId());
    }
}
