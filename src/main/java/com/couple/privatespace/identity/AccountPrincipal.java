package com.couple.privatespace.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public final class AccountPrincipal extends User {
    private final UUID accountId;
    private final long securityRevision;

    public AccountPrincipal(UUID id, String email, String hash, boolean enabled, long revision) {
        super(email, hash, enabled, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT")));
        this.accountId = id;
        this.securityRevision = revision;
    }
    public UUID accountId() { return accountId; }
    public long securityRevision() { return securityRevision; }
}
