# CHAT-07 — Message edit, desktop activity and video call timeline

Code Update Record · 3 November 2026 · Myanmar time · LOCAL ONLY

Base: main `fb935d13bea837bc379d6870968be1836a7088a3` plus existing uncommitted HOME-05. Existing Java/Spring/PostgreSQL/STOMP stack retained. Owner approved unlimited-age edits to own text/sticker note/file caption, explicit **Edited** marker, desktop Chat shared totals, and all terminal video outcomes (ended/missed/declined/cancelled, with failed connections represented too).

## Acceptance and behavior

- C1: Desktop partner card shows the same private shared message count, connected-video seconds and connected-video count as Home. Message totals include nondeleted TEXT/GIFT/FILE; CALL entries do not inflate them. Delete-for-me does not change shared totals.
- C2: Owner-only Edit in existing message options. Text stays at 4,000 Unicode code points; sticker notes at 500; file caption at 4,000 and may be empty. No age cutoff. Save/Cancel; failures keep the draft; explicit Reload latest replaces it for review. No-op saves add no marker.
- C3: Actual changes persist edited_at and display `Edited` beside the original timestamp. Original creation time, order, attachment and receipt state stay unchanged. Deletion clears the marker. Version checks prevent stale overwrites. Original send fingerprint remains immutable, so retrying an old send returns its latest edited state without restoring old text.
- C4: From this feature onward, each completed video call appends exactly one immutable system CALL entry, showing direction/outcome and connected duration when present. Existing retained calls still contribute to totals and remain in Recent calls; this change does **not** backfill old timeline entries or resequence old messages. Voice call timeline entries are outside this request.
- C5: Call log is not a normal incoming message: no MESSAGE notification, message chime or unread/read status. It cannot be edited, replied to, reacted to, cancelled as a pending send or deleted for everyone. Existing Delete-for-me API/desktop context action can hide its entry locally. Never return call client/socket identifiers, negotiation IDs or SDP in Chat DTOs.
- C6: Call completion enqueues a durable outbox row in the call transaction. An after-commit, separate transaction publishes the chat entry; 5s retries recover failed appends. Pending-row lock + unique call_id prevent duplicate publication. Chat history sequence and change_version advance together; failures roll back allocation. No acquisition of chat's lock inside the original call space-lock transaction.

## Source and contract changes

`SharedActivityService` centralizes HOME-05 aggregates, called inside existing repeatable-read Home/Chat response transactions. Chat Page adds `activity`. Chat Message adds nullable `editedAt`, `callId`, and `call: {id,state,connectedAt,endedAt,durationSeconds}`.

Cookie/CSRF-protected `PATCH /api/chat/messages/{id}` accepts `{body,version}` and returns the latest scoped Message. 400 invalid Unicode/length/version/blank text; 403 other sender; 404 unavailable/hidden/foreign message; 409 stale/deleted/system log. Private `message.edited` and `call.logged` events carry `message:null`, invalidating REST snapshots without broadcasting actor-specific quotes. Existing paging and mutation reconciliation remain in place.

V31 adds chat_message.edited_at. V32 creates call_chat_event outbox and pending index. V33 adds unique call_id and scoped FK to call_session, scoped call key and CALL payload constraint. Original migration files are unchanged. Call metadata removal cascades its outbox/log rows; no production metadata purge is added. No user rows or content is seeded, exported, backed up or deleted by these migrations.

UI: chat.html/js/css add compact stats, edit dialog, Edited metadata and terminal call card. Existing hover/long-press reactions, inline media playback, responsive composer and optimistic sends remain. An edited original confirmed during a send retry also clears the matching pending draft, preserving the new body.

## Verification / limits

Evidence is recorded below after execution. Test fixtures use only the guarded local `_test` database. Human visual acceptance, real mobile/laptop interaction and two-device media are NOT_TESTED here because browser use is disabled; no graphical automation bypass. This is local engineering completion, not UAT acceptance or deployment approval. Drive record destination remains unresolved: DRIVE_SYNC_PENDING. Full source package NOT_DUE for an ordinary local update.

Recovery: revert affected source/rebuild. Migrations are additive; do not edit applied migrations, reset data, remove audit markers or restore a database as a code rollback. Production remains on its previous release until owner requests deployment.

## Human review steps

Local Chat → own message long press → More (desktop right click) → Edit → Save → verify Edited and unchanged original time from both accounts. Try file caption/sticker note; Cancel must keep original. Edit from two tabs, save the older draft and verify conflict/reload. Review desktop totals against Home. End/cancel/decline/miss a new video call and verify one outcome entry on each account, no increment to message count. Refresh to confirm persistence.

## Executed checkpoint

- JS **76/76 PASS**, `.local/chat07-js.log`, including actual Edited/call rendering functions, edited-original retry reconciliation and editing-dialog read-receipt suppression.
- Native **58 distinct tests PASS across scoped runs**: Call13, Chat8, ChatActions11, ChatEdit3, ChatMedia7, Foundation3, Home10, HomeActivity2, GiftMigration1. `.local/chat07-native.log` has the first run's independent passing classes but also one **test setup error**: an intentionally rejecting CALL constraint was applied against already-existing call fixtures. Corrected the fault-injection fixture to `NOT VALID` (still rejects new rows). `.local/chat07-regression.log` then **31/31 PASS**, including durable outbox outage recovery; `.local/chat07-final-call.log` **13/13 PASS** against the final system-send UUID isolation change. No outstanding test failure; no full-project suite claim. Expected fault-injection warning remains in test log only.
- Native coverage includes own/other sender, cookie/CSRF PATCH, old-message age, Unicode limits, empty sticker/file captions, marker persistence/deletion clearing, same-body no-op, stale version, immutable original retry hash, refreshed reply quotes, exact call outcomes, dedup, no unread/message notification/count inflation, call log immutability, terminal-call transaction surviving chat insert failure, and successful deferred append without a sequence gap. GiftMigration rehearses earlier V13 through V33 without resetting normal schema.
- `npm.cmd run build` **BUILD SUCCESS**, 13.585s, at **00:21:53 Myanmar, 3 November 2026**, `.local/chat07-build.log`. Packaging skips execution; native results above are separate. JAR SHA256 `69587cfd8a5b2bca271b2ed38371691889268f3c9dfe00080de4e8469c1dbb92`.
- Source/test/scripts manifest `.local/chat07-source-manifest.json`: **532 files**, SHA256 `f1958472bb9d8236f20dbbb9027004a2c3fc824d9ef655a643e47897e6a2d234`. Uncommitted HOME-05 + CHAT-07, no Git push.
- Existing local PostgreSQL app migrated **V30→V33** and running **PID6340** on **http://127.0.0.1:18083/**, `.local/chat07-app.log`. Existing local users/content retained; JVM restart may require sign-in. **7 HTTP checks PASS**: root/health, Chat JS+CSS source equality, protected Chat HTML302 and guest Chat/Home API401 (`.local/chat07-http.log`). Unique Chat IDs, labelled edit dialog and metric semantics PASS (`.local/chat07-identity.log`). `git diff --check` PASS.
- Current local checkpoint supersedes HOME-05's earlier run identity. Production stays at application `cd4376f`; no Render operations, push, production migrations or database backup/export in this update. IMPLEMENTED LOCALLY / scoped tests PASS / device visual UAT PENDING / DRIVE_SYNC_PENDING.
