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

Status: publish in progress. Exact commits, deployment ID, online health, migration result and source package identity will be recorded after their operations succeed. Physical-device checks and Drive sync remain pending, independently of this explicit deployment authorization.
