package com.couple.privatespace.identity;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AUTH-01: bounded, single-instance local rate limiter; no forwarded-IP trust. */
@Component
public class AuthRateLimiter {
    private record Window(Instant end, int count) { }
    private final Map<String,Window> windows = new HashMap<>();
    private final int accountLimit, ipLimit, registerLimit,pairingLimit;
    private final Duration duration;
    public AuthRateLimiter(@Value("${app.auth.login-account-limit:10}") int accountLimit,
            @Value("${app.auth.login-ip-limit:30}") int ipLimit,
            @Value("${app.auth.registration-ip-limit:10}") int registerLimit,
            @Value("${app.pairing.request-limit:30}") int pairingLimit,
            @Value("${app.auth.rate-window-seconds:900}") long seconds) {
        this.accountLimit = accountLimit; this.ipLimit = ipLimit; this.registerLimit = registerLimit;
        this.pairingLimit=pairingLimit;
        this.duration = Duration.ofSeconds(seconds);
    }
    public synchronized long login(String ip, String email) {
        return Math.max(hit("login-ip:" + ip, ipLimit),
            hit("login-account:" + RegistrationService.digest(AccountStore.normalizeEmail(email)), accountLimit));
    }
    public synchronized long registration(String ip) { return hit("register:" + ip, registerLimit); }
    public synchronized long pairing(String ip) { return hit("pairing:"+ip,pairingLimit); }
    private long hit(String key, int limit) {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> !entry.getValue().end().isAfter(now));
        Window previous = windows.get(key);
        if (previous == null && windows.size() >= 10000) return Math.max(1, duration.toSeconds());
        Window current = previous == null ? new Window(now.plus(duration), 1)
            : new Window(previous.end(), Math.min(previous.count() + 1, limit + 1));
        windows.put(key, current);
        return current.count() > limit ? Math.max(1, Duration.between(now, current.end()).toSeconds() + 1) : 0;
    }
}
