# Run and review DEV-03 Pairing

From `C:\Projects\Project_M`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
# In another terminal when needed:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

Local URL [http://localhost:8080](http://localhost:8080). Current database is installed PostgreSQL18.6/5432 after owner-authorized transfer. Existing dev state/history/setup code preserved; old portable17.7 data retained, server stopped. Same start/test commands now verify installed DBs without admin credentials or automatic portable fallback. Current connection/evidence: Installed_PostgreSQL_Setup.md and PostgreSQL_18_Checkpoint.md. Original DEV-03 checkpoint's17.7 results remain historical. README's pre-existing merge conflict is preserved; this guide supplies current steps without overwriting unrelated merge work.

1. Sign up/sign in with your real own accounts. If no account exists, obtain local private setup code with `scripts/issue-setup-token.ps1` as described in DEV-02. No demo account/password is created in the development DB.
2. One account selects date (prefilled signup date), confirms Myanmar timezone/date, and **Create our space**. This creates one waiting membership.
3. If partner has no account yet, use the separate **account registration code** first. After signup/sign-in, partner uses the **pairing invitation** to join the shared space. Labels distinguish the two codes.
4. In Waiting, choose **Create pairing invitation** and privately share that code. Replacement revokes the prior unused pairing code. Code24h lifetime, cannot retrieve raw code after reload; replace it when necessary.
5. Partner enters pairing code→Review invitation→sees inviter, original signup date and canonical shared date in Myanmar timezone. Explicitly confirm the shared date→Join our space. Preview10min. Dates are not silently overwritten.
6. Creator's waiting screen receives live STOMP pairing event and both users go Home. If live transport drops, background REST check/reconnect reconciles committed DB state. Home shows actual members/shared date/timezone, with no fabricated day counter or unfinished chat/memory/call actions.
7. Sign out and sign in again: paired accounts go directly Home. Partner offline, browser closure, application restart or logout does not undo the pairing. There is no unpair/delete/replacement action in this increment.

All POSTs require CSRF, own-user pairing queue only, unauthorized destinations/SEND denied, sockets close on logout/expiry/disabled/revised identity. Tokens never go in URL, source, logs or browser storage. This is local development only; future HTTPS needs secure cookies/WSS and configured explicit origins. Automated tests use isolated `_test` DB, never wipe dev accounts. Test profile idle timeout1m and larger per-IP limits are isolated from production defaults. Tests include actual cookies, native PostgreSQL and STOMP frames, not an in-memory DB substitute.

Current drafts/decisions: DEV-03_Working_Spec.md; final evidence/status: DEV-03_Checkpoint.md. Counters/profile/full dashboard are DEV-04; chat/memory/call remain later. Full authentication recovery/change/logout-all remain separate pending work. No UAT/production readiness or human UI approval is implied by this increment.
