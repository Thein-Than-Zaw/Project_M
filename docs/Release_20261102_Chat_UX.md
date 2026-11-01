# Main deployment — 2 November 2026

Owner instruction: push the completed local changes to `main`, then deploy the existing Render application. This explicitly supersedes the earlier deployment HOLD; it does not claim human/device UAT acceptance.

## Scope and target

Preserve existing Java/Spring Boot/Security, PostgreSQL/Flyway, STOMP, private S3 media and managed ExpressTURN configuration. Publish the previously reviewed UI02, DEV-09A/B, CHAT-02A/B and CHAT-03 increments. No new hosting service, paid resource, database import or local-to-Neon user migration.

Repository: `Thein-Than-Zaw/Project_M`. Existing feature branch `codex/render-hosting`; remote main is an ancestor, so use a normal fast-forward without force-push. Target Render service `couple-private-space` (`srv-dbjisdqjnfac7384lr50`), workspace `tea-d18r9jjuibrs73dvc0i0`, existing Project_M Production environment. Change its tracked Git branch to main; preserve auto-deploy OFF, free plan, Docker runtime, health path and all secrets.

## Migration and recovery

Read-only production catalog inspection confirms predecessor V21 in `couple_private_space`. Pending V22–V26 add the notification inbox, chat reply/reaction/hide metadata and private photo/activity fields. V23 backfills request hashes and preserves message text; no account/chat/memory migration or deletion is part of this deployment. Native predecessor/fresh tests are linked in the previous checkpoints.

Automatic approval review rejected exporting a private Neon backup because deployment authorization did not explicitly include copying account/chat data to this machine. The owner then explicitly selected **deploy without a backup**. No production backup was exported; only read-only version/aggregate checks are permitted. Do not retry that export or copy live data by another route.

Recovery is a forward fix by default. PostgreSQL transactional migration failures roll back the failing migration, but earlier completed migrations may remain. Do not drop new tables, erase new actions, restore a database or silently run an old chat projection after users have begun using hide/reply/delete. Any destructive recovery needs a separate authorized decision. Existing provider restore availability has not been verified; no restoration promise is made.

## Verification and known limits

App source/build identity and graphical results: [CHAT-03 record](CHAT-03_Mobile_Chat_UX.md). Final release review adds a complete Maven suite. Initial full-suite run found a stale Foundation assertion expecting V21 (now V26) and four Windows temporary-directory cleanup errors in isolated S3 adapter tests. Fix the version assertion; run test temporary directories inside ignored `target/test-tmp`. Do not suppress or skip failures. The initial failing log is retained privately, and the corrected final outcome is recorded below.

The Foundation assertion is a test-only correction; application source is unchanged from the reviewed local JAR. No paid infrastructure or environment credentials are changed. Physical phone keyboard/long-press, active-call avatar/media qualification and human UAT remain NOT_TESTED/NOT_RECORDED. Browser alerts require an open page and permission; no closed-site push is claimed.

Source package: prepare a complete secret-free versioned archive locally from committed source, verify inventory/hash/integrity and exclude runtime/dependency directories, credentials, real data and backups. Existing authorized Drive destination remains unavailable: DRIVE_SYNC_PENDING, independently of Git/deploy status. This does not fabricate source delivery acceptance.

## Actual release evidence

Local release verification PASS: full Maven suite 98 tests, 0 failures/errors/skips, BUILD SUCCESS in 3:21, 2 November 2026 01:16:27 +06:30. Command: source `scripts/local-env.ps1`, create ignored `target/test-tmp`, then Maven `-B -ntp -DargLine=-Djava.io.tmpdir=C:/Projects/Project_M/target/test-tmp test`. JavaScript: `node --test src/test/javascript/*.test.mjs`, 48 PASS / 0 fail. No test was disabled or removed.

Canonical repository source identity: [release manifest](evidence/RELEASE_CHAT03_Source_Manifest.json), 521 source/config/test files, SHA256 `0730f4db17e44c5cb01181222422904eac8270a3de94a76d2b1212949e7a6b6c`. Identity uses staged Git blob bytes, so Windows checkout line ending conversions do not change its meaning. Local reviewed app JAR SHA256 remains `4e444e974ed5623ec18763719ea603fef1fc8d15396cadb64679620e82728ef2`; only test/docs changed in this release continuation. Render builds its own Docker/Java21 artifact from the pushed commit.

**Deployment LIVE.** App commit [`b2c051d76ebbdc9474b4505efd60bc714b381b25`](https://github.com/Thein-Than-Zaw/Project_M/commit/b2c051d76ebbdc9474b4505efd60bc714b381b25) was committed, main fast-forwarded and pushed without force. Render branch is now main; auto-deploy remains OFF. Deployment `dep-dbjp036tlv9c73dsnrh0` finished live at 2 November 2026 **01:49:44 Myanmar time** (`2026-11-01T19:19:44.973097Z`). URL: https://couple-private-space.onrender.com.

Cloud Maven BUILD SUCCESS; app started under Java21.0.13. Flyway log: `Successfully applied 5 migrations to schema "couple_private_space", now at version v26`. Read-only before/after aggregate checks show unchanged account/chat/memory row counts. No production user content was downloaded or seeded. No production backup was exported, as explicitly selected by the owner.

After live status: 10 HTTPS smoke checks PASS (readiness, landing page, CSRF, guest access boundaries, untrusted write denial, database-backed invalid-login lookup, HSTS and Secure/HttpOnly/SameSite session cookie); 12 public JS/CSS assets HTTP200 and normalized source equality against b2c051d PASS; 3 new private endpoints reject anonymous access with HTTP401. No real account login, live message send or media call was exercised by the agent online. The earlier pre-live network/sandbox/timeout attempts are not counted as successful checks; the final post-live run used the machine's existing proxy configuration and succeeded.

Source archive for the deployed app commit: ignored `.local/source-packages/Project_M_Source_CHAT03_b2c051d76ebb.zip`, 12,511,206 bytes, 788 files, SHA256 `6eaae6e553cf579b7fa69c9d886d1ef5bc7d58b203cce50583b2b88c61137066`. CRC, exact committed Git inventory/blob equality and known-credential exclusion PASS. Archive generation explicitly disables checkout CRLF conversion for canonical Git bytes; no global Git setting changed. Manifest is alongside the archive. Deployment documentation may be committed after the app commit without another app deployment; application source remains identical to the release manifest.

Physical-device checks, human UAT and Drive sync remain pending, independently of this completed explicit push/deployment task. Source package is PREPARED_LOCAL / DRIVE_SYNC_PENDING, not verified delivered to Drive. The existing database/storage/TURN secrets and free hosting plan were preserved.
