# DEV-07 paused checkpoint — 1 November 2026

> Historical pause snapshot. Owner subsequently resumed DEV-07; use [DEV-07_Checkpoint.md](DEV-07_Checkpoint.md) and current source/evidence for present status. Preserve the original failure/account-handoff facts below as history.

Status: USER-PAUSED for account handoff. DEV-07 is incomplete. The subsequent instruction to continue was interpreted as completing this interrupted handoff, not resuming feature development.

## Preserve this workspace

- Project: `C:\Projects\Project_M`; branch `main`; HEAD `658251c9c12dcf10092d9f0cabfe6ca4efa34a69`.
- DEV-06, CHAT-UI-01 and partial DEV-07 changes are local and uncommitted. Do not reset, clean, replace or assume the remote contains them. No new commit or push was performed for this handoff.
- Keep the existing local PostgreSQL databases and both real accounts. Never reset/reseed the development database to reproduce tests.
- Credentials are in ignored `.local/credentials.json`; load through `scripts/local-env.ps1`. Do not print credentials or put them into source packages or chat. Local dependencies and test logs also live in ignored `.tools`, `.local`, and `target`; keep these on this computer.
- Source files stay in this folder when using another account on this computer. This checkpoint supplies context for the next chat; it is not a transfer of chat history or account access.

## Scope already accepted

- One couple, two accounts; persistent one-time pairing. Server-side private access, no E2EE; local development first; in-app notifications only.
- Myanmar time `Asia/Yangon`; relationship start is Day 1; leap-day anniversary uses February 28 in non-leap years.
- Both partners can edit/delete shared memories and Special Dates. Chat limit 4,000 Unicode code points; message edit/delete deferred.
- DEV-07: JPG/JPEG, PNG, WebP photos <=10 MB; short videos <=180 seconds and <=100 MB; PDF/DOCX/XLSX/PPTX documents <=20 MB. Maximum 10 mixed attachments **per memory**, not across the application. Decimal MB.
- Shared Trash retains deleted memories for 72 elapsed hours. Restore within that time; automatic permanent purge after expiry. Confirmed Empty Trash permanently deletes its current trash batch and attachments, keeping active memories.
- Source working defaults, not additional owner confirmations: title 1–120, optional caption <=2,000 code points, nonfuture Myanmar date, >=1 attachment, per-user favorites, optimistic version conflicts. Current playback baseline is MP4/H.264 8-bit with optional AAC, rejecting unsupported encodings without silently transcoding originals.
- Total storage quota, avatar policy and disposition of historical backup copies remain undecided. Do not reopen already accepted file limits.

## Existing completed local work

Read `DEV-06_Checkpoint.md` and `CHAT-UI-01_Checkpoint.md`. DEV-06 has 363 licensed bundled stickers and chat gift snapshots; CHAT-UI-01 enlarges the conversation viewport. Earlier regression evidence is historical, not validation of current DEV-07 source. The actual hidden/unfocused-browser read-receipt prerequisite remains unverified. General notification inbox is deferred to DEV-09.

## Partial DEV-07 source

- New `memory` Java package: private file validation/storage, upload and create receipts, memory CRUD/favorites, optimistic edits, shared Trash/Restore/Empty Trash, durable PURGING state, maintenance and notification/controller code.
- New Flyway V16–V19: memory, memory_asset, memory_receipt, memory_favorite. These have not been packaged/applied to the real development database by this DEV-07 work.
- New `memories.html`, `memories.css`, `memories.js`: list/filter, upload editor, private media/document detail, favorites and Trash dialogs. Home/chat/navigation/socket integration changed. GUI verification is pending.
- `pom.xml` adds PDFBox 3.0.5. `scripts/setup-media-tools.ps1` pins FFmpeg/ffprobe 8.1.2. Local binary under `.tools/ffmpeg/ffmpeg-8.1.2-essentials_build/bin`; downloaded archive SHA-256 `e25b682664025d49034c981afb4bae36238a40f29a3cc1c713ad9a8b5b3528f6`.
- Multipart limits, private storage config and test profile changed. New `MemoryFixtures` and `MemoryIntegrationTest`; Foundation migration count updated to 19. This is implementation in progress, not an acceptance or release claim.

## Exact verification status at pause

- Compile checkpoints succeeded earlier; latest test command compiled current test source and completed with **BUILD FAILURE: 8 tests, 4 failures, 0 errors, 0 skipped**, ending 2026-11-01 12:58:42 +06:30.
- Failing tests: `realFormatsPersistPrivatelyAndSupportRangesAndDownloadIdentity`, `bothMembersEditButCreatorAndFavoritePreferencesStayCorrectAndStaleWritesReject`, `emptyTrashSnapshotDoesNotSilentlyIncludeConcurrentDeletionOrActiveMemories`, `malformedSpoofedAndMacroDocumentsRejectAndFileNamesNeverChooseStoragePaths`.
- Failures expect upload HTTP 201 but receive 400 `INVALID_FILE`. Office validation/fixture compatibility is an investigation lead, not an established root cause. Do not weaken validation to make fixtures pass without checking valid document behavior.
- Full log: `.local/dev07-memory-tests.log`; Surefire: `target/surefire-reports/com.couple.privatespace.MemoryIntegrationTest.txt` and XML. A compact copy is saved under `docs/evidence/DEV-07_paused-test-summary.txt`.
- Test run used PostgreSQL test database `couple_private_space_test`, isolated schema `dev07_fresh_20261101`, HTTP port 18080. `.local/Dev07Prepare.java` already created that schema; do not blindly rerun its CREATE SCHEMA. `.local/dev07-tests.ps1` reuses it. Inspect test cleanup/database state before another run; no fresh post-test row-count audit was performed for this handoff.
- Test command session 10388 completed with exit code 1. No listener on 8080 or 18080 was reported by the checkpoint checks; application was not restarted during handoff.
- Existing packaged JAR `target/private-space-0.1.0-SNAPSHOT.jar` is the earlier CHAT-UI-01 build, SHA-256 `BC8AD1F6391DE621004FF2BB03B0EFE92AED9EA7DC5B4E32381F974CFC1A5B5B`. It does **not** contain DEV-07. `target/classes` has newer source compilation. Do not confuse these artifacts.

## Next work when explicitly resumed

1. Read requirements v1.1 in full, this checkpoint, working spec, prior checkpoints and repository instructions. Inspect actual diff, logs and test/schema state rather than treating this summary as proof.
2. Investigate the four rejected valid-file uploads. Check real Office documents and fixture package validity; preserve content validation, XML/archive limits and private access checks.
3. Complete meaningful verification: mixed 10/11 attachment cap, concurrent retry/notification deduplication, purge failure/retry/restart behavior, orphan expiry, pagination, exact photo/document size boundaries, malformed archives/XML and access isolation. Review resource limits before multipart spooling; current validation semaphore alone does not cap that earlier stage.
4. Run appropriate full regression and V13-to-latest migration rehearsal. Prior DEV-06 test results cannot establish DEV-07 compatibility. Test only synthetic owned data in the test database.
5. Verify real GUI journeys and responsive layout, upload/retry/cancel/edit conflicts, both-member permissions, private downloads/range playback, Trash/Restore and confirmed Empty Trash. No GUI evidence exists for DEV-07 yet.
6. Reconcile API/schema/RTM/plan and working-spec historical planning text with actual implementation and evidence. Only after verification prepare a reviewed build, protect real data and apply migrations/run locally. No production release or human UAT approval is implied.

Drive destination is unknown; previous Drive sync remains pending. This handoff is local. No account credentials, local secrets, database dump or binary upload data were copied into the checkpoint.
