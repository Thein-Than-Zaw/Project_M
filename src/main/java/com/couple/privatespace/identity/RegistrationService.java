package com.couple.privatespace.identity;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.Email;

@Service
public class RegistrationService {
    private final JdbcTemplate jdbc;
    private final AccountStore accounts;
    private final PasswordEncoder encoder;
    private final Duration tokenLifetime;
    private final jakarta.validation.Validator validator;

    public RegistrationService(JdbcTemplate jdbc, AccountStore accounts, PasswordEncoder encoder,
            jakarta.validation.Validator validator, @Value("${app.auth.registration-token-hours:24}") long hours) {
        this.jdbc = jdbc; this.accounts = accounts; this.encoder = encoder; this.validator = validator;
        this.tokenLifetime = Duration.ofHours(hours);
    }
    public record Signup(String displayName, String email, String password, String confirmPassword,
                         LocalDate relationshipDate, String registrationToken) { }
    private record EmailValue(@Email String email) { }
    public record Registered(UUID id, String displayName, String email) { }
    public record IssuedCode(String code, Instant expiresAt) { }

    /** AUTH-01/D01/D02: validate before consuming code, serialize capacity+consume+account insert. */
    @Transactional
    public Registered register(Signup form) {
        Map<String,String> errors = new LinkedHashMap<>();
        String name = form.displayName() == null ? "" : form.displayName().strip();
        String email = AccountStore.normalizeEmail(form.email());
        if (name.isBlank() || length(name) > 60) errors.put("displayName", "Enter a name between 1 and 60 characters.");
        if (email.isBlank() || email.length() > 254 || !validator.validate(new EmailValue(email)).isEmpty()) errors.put("email", "Enter a valid email address.");
        if (form.password() == null || length(form.password()) < 12 || length(form.password()) > 128) errors.put("password", "Use between 12 and 128 characters.");
        if (form.confirmPassword() == null || !form.confirmPassword().equals(form.password())) errors.put("confirmPassword", "Passwords must match.");
        if (form.relationshipDate() == null) errors.put("relationshipDate", "Choose your relationship start date.");
        else if (form.relationshipDate().getYear() < 1 || form.relationshipDate().getYear() > 9999)
            errors.put("relationshipDate", "Choose a valid date with a four-digit year.");
        if (form.registrationToken() == null || !form.registrationToken().matches("[A-Za-z0-9_-]{43}")) errors.put("registrationToken", "Enter your setup or partner invitation code.");
        if (!errors.isEmpty()) throw new ApiFailure(400, "VALIDATION_FAILED", "Check the highlighted fields.", errors);

        String hash = encoder.encode(form.password()); // Never trim a password; do expensive hashing outside DB lock.
        lockGate();
        int count = jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class);
        if (count >= 2) throw new ApiFailure(409, "SPACE_FULL", "Both accounts have already been created. Please sign in.");
        String requiredKind = count == 0 ? "SETUP" : "PARTNER";
        var codes = jdbc.query("SELECT id FROM registration_token WHERE token_hash=? AND kind=? AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP",
            (row, n) -> row.getObject("id", UUID.class), digest(form.registrationToken()), requiredKind);
        if (codes.isEmpty()) throw new ApiFailure(400, "INVALID_INVITATION", "This code is invalid, expired, or already used.");
        if (accounts.byEmail(email).isPresent()) throw new ApiFailure(409, "REGISTRATION_CONFLICT", "This registration could not be completed. Check your invitation or sign in.");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES (?,?,?,?,?,?)",
            id, count + 1, name, email, hash, form.relationshipDate());
        jdbc.update("UPDATE registration_token SET consumed_at=CURRENT_TIMESTAMP WHERE id=?", codes.getFirst());
        return new Registered(id, name, email);
    }

    /** Registration invitation only; full couple-space pairing belongs to DEV-03. */
    @Transactional
    public IssuedCode issuePartnerCode(UUID actor) {
        lockGate();
        var first = accounts.byId(actor).filter(account -> account.slot() == 1 && account.enabled())
            .orElseThrow(() -> new ApiFailure(403, "FORBIDDEN", "Only the first account can invite the second account."));
        if (jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class) >= 2)
            throw new ApiFailure(409, "SPACE_FULL", "Both accounts have already been created.");
        jdbc.update("UPDATE registration_token SET revoked_at=CURRENT_TIMESTAMP WHERE kind='PARTNER' AND consumed_at IS NULL AND revoked_at IS NULL");
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = Instant.now().plus(tokenLifetime);
        jdbc.update("INSERT INTO registration_token(id,kind,token_hash,issued_by,expires_at) VALUES (?,'PARTNER',?,?,?)",
            UUID.randomUUID(), digest(code), first.id(), java.sql.Timestamp.from(expiry));
        return new IssuedCode(code, expiry);
    }
    private void lockGate() { jdbc.queryForObject("SELECT id FROM registration_gate WHERE id=1 FOR UPDATE", Integer.class); }
    public static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static int length(String value) { return value.codePointCount(0, value.length()); }
}
