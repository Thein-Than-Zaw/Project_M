# CHAT-05 — Local Chat media and mobile interaction

2 November 2026, Myanmar time (+06:30). Existing Spring Boot/PostgreSQL application; base main `fd58d918befb2ca0171c3f05a6f82ff0bcc0da21`. Current changes are uncommitted. Owner explicitly requested **NO SERVER DEPLOYMENT**. No push, deployment, online database migration, credential change or customer-data export is authorized by this checkpoint.

## Owner scope and coverage

| Clause | Entry / implementation | Verification |
|---|---|---|
| C1 Mobile tap does not open reactions; long press opens emoji + Reply/More | `/chat.html`, `bindMessageInteractions`, persistent reaction toolbar | Node gesture regressions; physical touch review pending |
| C2 Laptop partner card has no inner scroll | `chat.css`, desktop fixed flex card and compact short-height styling | Source review; graphical review pending |
| C3 Reading incoming Chat automatically reads matching notification | Existing visible/focused per-message read ACK → V27 transaction trigger | Native subset / delivery-only / rollback tests |
| C4 Telegram-like composer; left paperclip and one Emoji/Stickers icon | `/chat.html`, `chat.js`, `chat-media.js`; common picker with Emoji tab | Source / module tests; graphical review pending |
| C5 Photo, document, short video, voice inside Chat bubbles | Private upload → FILE send → protected resource/range endpoint; inline HTML media controls | Native upload/ACL/range/video/audio tests; device playback pending |
| C6 Cancel video during upload or Sending | Abort local XHR, persist cancellation intent, serialized server cancel/send | Native before-upload / after-commit / concurrent cancellation tests |
| C7 Voice can cancel immediately; stop → preview → send | Generation-guarded MediaRecorder; immediately stop all microphone tracks, discard draft | Node delayed-permission / track-release / preview / size-duration tests |

All seven clauses are UI_REQUIRED. Backend security and lifecycle checks support C3/C5/C6/C7. Previous account/pairing, private-space authorization, text limits, reply/delete/reaction rules, calls and notification settings remain in scope for regression, without product changes.

## Contract and schema

- POST `/api/chat/uploads` multipart `file`, optional `voice=true`, CSRF header and UUID `Idempotency-Key`. Same key is the FILE message `clientMessageId`. Returns attachment metadata, never a public storage URL.
- Existing `/app/chat.send` accepts `{clientMessageId, type:"FILE", attachmentId, body, replyToId?}`. Optional caption uses the existing 4,000-character limit. REST history/snapshots and STOMP created/sent messages include attachment metadata.
- DELETE `/api/chat/uploads/intents/{clientMessageId}` records durable cancellation, even before upload completes. If the send already committed, cancellation deletes the owner's message for everyone, leaving the existing tombstone. It cannot guarantee the partner never saw an already committed message.
- GET `/api/chat/assets/{id}` supports browser range playback; `thumbnail=true` for image/video, `download=true` for downloads. Active membership and per-user hidden/deleted visibility are enforced server-side, cache disabled. Documents download; image/video/audio stay inline in the bubble.
- V27 syncs matching MESSAGE/GIFT notifications to actual incoming-message read receipts, including existing read rows. Notification-list read does not imply Chat read. Unseen older messages remain unread.
- V28 cancellation intents; V29 private attachments with scoped ownership/lifecycle; V30 FILE payload/FK and request-fingerprint compatibility. Previously applied migrations remain unchanged. Old TEXT/GIFT fingerprint format remains valid.
- Shared Memory byte storage/validators are reused, but Chat owns independent metadata/lifecycle. Memory orphan maintenance protects Chat references too. Unsent uploads older than 24 hours expire; deleted bytes are retried by cleanup. This is temporary upload cleanup, not a change to Memory's 3-day trash policy.

## Limits and decision provenance

Image JPG/PNG/WebP 10 MB; PDF/DOCX/XLSX/PPTX 20 MB; one attachment per message are the proposed Chat defaults discussed with the owner. Owner requested short TikTok-like video and voice cancellation. Existing Memory MP4 H.264/AAC 3-minute/100-MB validation is reused for Chat. Voice uses the discussed 5-minute/10-MB ceiling; AAC normalization allows 0.5 seconds of container/encoder padding. Numeric Chat video/voice limits were proposed, not separately explicitly confirmed; do not label them owner-approved. No external media service or stack change is introduced.

## Run and evidence

Local command (PowerShell): load `scripts/local-env.ps1`, set `SERVER_PORT=18083`, then `java -jar target/private-space-0.1.0-SNAPSHOT.jar`. This uses installed local PostgreSQL and existing local accounts, not Neon. Rebuild with `npm.cmd run build`.

Verification is in progress; final results are appended after execution. Physical microphone/camera, two-device playback, mobile keyboard/touch and graphical layout acceptance remain separate from automated tests. Browser tool previously reported local access disabled; do not bypass it or claim a graphical journey ran.

Recovery: stop only the project-owned local preview process; retain database rows and uploaded bytes. V27–V30 are forward changes; reverting a JAR is not a database rollback. No production restore is involved.

Code status: IMPLEMENTED LOCALLY; scoped automated checks PASS with the limits below. Human UX acceptance: PENDING. Release: HOLD (owner). Drive Code Update Record sync: DRIVE_SYNC_PENDING because no existing authorized destination has been resolved. This ordinary local checkpoint is not a complete release/handoff declaration.

## Executed checkpoint — 07:37 Myanmar time

- Final `npm.cmd run build`: BUILD SUCCESS, 10.072 seconds, 07:35:51. Final JAR SHA256 `b7d6754577f54930cc59aca603e38464e9823ed2461b439a859c491c6e4e1783`.
- Final Node suite: **59 PASS**, zero failures/skips; syntax checks for `chat.js` and `chat-media.js` PASS. Evidence: ignored `.local/chat05-js-final.log`.
- First native full suite: 103 tests, 102 PASS, one newly written negative test expected HTTP422 instead of the existing validator's HTTP400. Production validation remained unchanged; test corrected to assert HTTP400 plus precise INVALID_VOICE/VIDEO_TOO_LONG codes. No assertions were removed. One additional hidden-own-send cancellation regression was added.
- Final affected native suite: **25 PASS**, zero failures/errors/skips (ChatActions11, ChatMedia6, Notification8), 56.078 seconds, 07:35:16. Includes HTTP auth/CSRF, sender/partner attachment visibility, range206, dedup, actual video/audio checking, before-upload/post-commit/racing cancellation, cancellation after sender hide, read subset and rollback. The whole suite was not rerun after that correction; don't describe these results as a single 104-test final full-suite run. Evidence: `.local/chat05-native.log`, `.local/chat05-targeted.log`.
- Existing local application database advanced V26→V30 successfully on first preview boot, with existing rows retained. Final restart validates V30; no online database operation occurred.
- Final local app runs at **http://127.0.0.1:18083/**, actual Java PID24760, launcher32448. Local health UP, root/chat HTML/JS/media module/CSS HTTP200 and two guest-only private checks HTTP401: **8 HTTP checks PASS**. Evidence: `.local/chat05-local-http.log`, `.local/chat05-app.log`.
- Browser access now works: the local login page was visibly opened and left ready for the owner. Earlier disabled-browser observation is historical. Signed-in Chat graphical/touch/device playback and physical microphone verification remain NOT_TESTED; no existing account password was accessed or reset.
- Source manifest `.local/chat05-source-manifest.json` covers source/tests/scripts/manifests; SHA256 `a71223fc55148f5ed7d1b558ae8e68261ae22e171383c8199d89ab9c5b38a34d`. Base/final Git identity remains base plus uncommitted changes; no commit/push/deploy performed. Final self-review also fixed initial composer enablement, stale Cancel draft state, hidden CSS specificity, and preserves connected media elements during receipt/reaction updates. Self-review is not independent human acceptance.

Owner review: log in locally, long press a mobile message, choose/change a reaction or Reply, send a picture/document/short MP4, cancel during upload/Sending, and record→Cancel / record→Stop→listen→Send. Verify two-device playback and laptop card fit before deciding on release. Cancellation immediately drops local microphone/XHR; server cleanup confirmation can take time, retries persist for the current tab, and already committed sends become tombstones.
