package com.couple.privatespace.identity;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountStore {
    private final JdbcTemplate jdbc;
    public AccountStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Account(UUID id, int slot, String displayName, String email, String hash,
            LocalDate relationshipDate, boolean enabled, long revision) {
        AccountPrincipal principal() { return new AccountPrincipal(id, email, hash, enabled, revision); }
    }

    public Optional<Account> byEmail(String email) {
        return find("lower(btrim(email)) = ?", normalizeEmail(email));
    }
    public Optional<Account> byId(UUID id) { return find("id = ?", id); }
    private Optional<Account> find(String condition, Object value) {
        return jdbc.query("SELECT * FROM app_user WHERE " + condition, (row, n) -> new Account(
            row.getObject("id", UUID.class), row.getInt("account_slot"), row.getString("display_name"),
            row.getString("email"), row.getString("password_hash"), row.getObject("signup_relationship_date", LocalDate.class),
            row.getBoolean("enabled"), row.getLong("security_revision")), value).stream().findFirst();
    }
    public static String normalizeEmail(String email) { return email == null ? "" : email.strip().toLowerCase(Locale.ROOT); }
}
