# CHAT-06 — Sending state and compact media

2 November 2026, Myanmar time (+06:30). Owner reviewed local CHAT-05 image/video/voice successfully and requested the following corrections. Base Git remains `fd58d918befb2ca0171c3f05a6f82ff0bcc0da21` plus uncommitted CHAT-05/06 changes. Deployment HOLD remains explicit: no push, Render deploy, Neon migration, credential change or real-data export.

| Owner clause | Before → after | Source / evidence |
|---|---|---|
| C1 Send media in the conversation with a small spinner | Composer progress bar until upload completes → local preview bubble immediately, quiet Sending… + small spinner at the bottom corner | chat.html removes progress; chat.js outbox renderer; chat-media.js queue-before-upload; mounted real-module test |
| C2 Wait up to five minutes; then Failed | 180-second upload and 15-second send timeout → one five-minute deadline covering upload/offline/confirmation for text, sticker and media | transmit/checkSendTimeouts; deadline regression; XHR remaining-budget regression |
| C3 Cancel sending, including slow video | Per-upload composer lock → per-bubble Cancel × and Retry after failure; durable cancellation for all own send types | upload abort/revision guard + existing intent API generalized for TEXT/GIFT/FILE; native durable cancellation/race tests |
| C4 Consistent text/media statuses | Queued / Not confirmed mixed with upload panel → Sending… / Failed / existing server-confirmed Sent, Delivered, Read | shared outbox status; late ACK/reconnect/deadline tests |
| C5 Remove media filename and visible Download row | Extra labels around image/video/voice → subtle ⋯ menu containing filename and Download | appendMedia; safe protected relative URLs and textContent retained; documents keep identifying filename |
| C6 Reduce large media padding | Large media bubble spacing → 4px media padding, compact caption spacing and audio controls, responsive width | chat.css media overrides; actual-device visual review pending |

All six clauses are UI_REQUIRED, with backend cancellation/authorization supporting C3. The composer becomes available immediately after queuing a media send; each selected attachment remains one message. No file formats, size/duration limits, private-storage policy, reaction rules or Memory trash behavior changed. No new migration; schema remains V30.

## State and recovery details

- Send queues the preview before any upload starts. No artificial Sent state or fake delivery receipt is generated: Sent requires actual server confirmation. On a fast connection that confirmation can arrive quickly; no arbitrary display delay is added.
- A reconnect never extends the original five-minute deadline. Explicit Retry starts a fresh five-minute budget using the same request/upload key to avoid duplicates. Timer checks also run when the tab becomes visible/focused.
- Failed means confirmation was not obtained, not proof that a server transaction never committed. A late real ACK/snapshot resolves it to the actual saved message. Cancel records the same durable marker used in CHAT-05 and tombstones an already committed own message if necessary.
- Local blob previews/files stay only in this tab's memory; blob URLs are excluded from sessionStorage. After a reload, a not-yet-uploaded file cannot be recreated silently; Retry explains that it must be selected again. Already uploaded attachment metadata and text intents retain the existing outbox recovery.
- Native upload XHR can be aborted immediately. An old async result cannot send a cancelled/failed/outdated item. Recording Cancel still immediately releases microphone tracks.
- ⋯ remains keyboard accessible through native details/summary. Download still uses the existing authenticated endpoint; no public link is introduced.

## Verification and return

Final Node suite: **64 PASS**, no failures/skips. Includes new five-minute/reconnect/failure/retry checks for all types, pending media before upload, immediate abort, independent queued file, late completion suppression and late real ACK recovery. Existing gesture, reply, sent sound and recording-cancel tests remain passing.

Native affected suite: **31 PASS**, no failures/errors/skips (ChatActions11, Chat8, ChatMedia7, Gift5), Maven BUILD SUCCESS, 1:06 at 07:57:36. Added TEXT/GIFT cancellation-before-send and after-commit retry-resurrection negatives, alongside existing file cancellation races/ACL/native media tests. No whole-project final full-suite claim is made. Final subsequent backend edit only clarified a cancellation comment and unavailable-message error text.

Local final build/start/HTTP identity is appended below after execution. Physical slow-network/mobile visual acceptance remains pending. Owner's preceding media-success report applies to the prior CHAT-05 build, not automatically to this revision. AI self-review is not independent human acceptance.

Code: IMPLEMENTED LOCALLY; affected automated checks PASS. Human visual/device acceptance: PENDING. Release: HOLD. Drive Code Update Record sync: DRIVE_SYNC_PENDING (existing authorized destination unresolved). Ordinary local checkpoint, not a complete release/handoff declaration. Recovery is a scoped source revert followed by local rebuild; no database rollback is needed for CHAT-06.

Final build: `npm.cmd run build` BUILD SUCCESS, 20.767s at 07:59:15 Myanmar time. JAR SHA256 `7f40ecee3ecd08e8d42600ca6b70ed0eb24fe4dd22798b4303ff3d1b971e2fe6`. Final source/scripts/tests manifest `.local/chat06-source-manifest.json` SHA256 `b03b5ecaba0149277203a0a63a5ab3aabb8dd6e4694611d21a12622a129a0ba6` (documentation excluded). Final Node64 PASS and syntax/diff checks PASS. Logs: `.local/chat06-build-final.log`, `.local/chat06-js-final.log`, `.local/chat06-native.log`.

At 08:00 the final application is running on http://127.0.0.1:18083/ with existing local accounts/PostgreSQL, Java PID32536 (launcher13940). Six HTTP checks PASS: health UP; chat HTML/module/media module/CSS200; unauthenticated Chat401; served markup has no upload progress bar. Evidence `.local/chat06-http.log` and `.local/chat06-app.log`. Browser inventory shows the login page, so no signed-in visual/physical slow-network journey is claimed for CHAT-06. Local owner can refresh and sign in to review. No push or deployment was performed.
