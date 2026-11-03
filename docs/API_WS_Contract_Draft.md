> Current status (1 November 2026): DEV-08 native call increment and V21 local upgrade are recorded in DEV-08_Checkpoint.md. Latest DEV-08/DEV-07 addenda supersede historical foundation/planning descriptions. Actual browser/media/relay verification remains pending.

# API / WebSocket contract v0.1 — DRAFT

Source: requirements v1.1 §§5.11.4–5.11.5, AUTH/PAIR/HOME/CHAT/GIFT/CALL/MEM/DATE/NOTIFY/PRIV. UI reference is not behavior authority. DEV-02 identity and DEV-03 pairing subsets are implemented as documented below; wider feature inventory remains draft. Health200/UP or503/DOWN, no secrets. `/ws` now supports only authenticated pairing signals; chat/call destinations are still disabled. Other feature routes deny guest401/authenticated403.

## DEV-03 implemented pairing subset

See DEV-03_Working_Spec.md for exact request/response, lifecycle and DB mapping. `/api/space` derives own UNPAIRED/WAITING/ACTIVE state from membership; `/api/spaces` creates space+creator atomically with explicit date confirmation. `/api/invitations` issues/replaces24h pairing code, distinct from account registration code. `/api/invitations/preview` creates actor-bound10min confirmation showing both signup/shared dates. `/api/invitations/join` requires code/confirmationToken/confirmedRelationshipDate/confirmed=true plus UUID Idempotency-Key; same actor/key/body retries return original persisted result, changed intent409. Invalid/expired invitation400, state/version/capacity409, unauthorized401/403. `GET /api/home` and `/home.html` require own ACTIVE membership. Minimal home is real pairing-success metadata, not full F03. Login JSON next route is `/home.html` once ACTIVE and `/space.html` otherwise; browser follows that route. No unpair on logout/offline/restart.

Native STOMP `/ws` handshake needs valid HTTP session and allowed localhost origin. CONNECT requires session CSRF (Spring Security XOR support); only exact `/user/queue/pairing` SUBSCRIBE by actual members permitted; every SEND and other subscription denied. AFTER_COMMIT `pairing.updated` event contains eventId/type/serverTimestamp/state only. HTTP session object/identity revision checked inbound/outbound; logout listener and2s sweep revoke sockets. No private names/dates in signal; authorized REST refresh reconciles state. Source protocols follow [Spring Security WebSocket guidance](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html). This is a single-instance simple broker, not durable inbox. Preview POST bodies/code never logged or put in URL. Pairing request rate default30/IP per15m,16KiB body bound. Date uses confirmed Asia/Yangon; signup dates unchanged. Implemented pairing version field is a numeric internal revision (currently0/1); future wide sequence-string contract remains draft.

## DEV-02 implemented subset (supersedes draft rows below for this increment)

GET `/api/csrf` guest/session → `{headerName,token}`; POST `/api/auth/register` guest only, JSON Signup body from draft →201 `{id,displayName,email}`, no auto-login; POST `/api/auth/login` **application/x-www-form-urlencoded** `email,password` (native Spring Security form filter, not draft JSON) →200 `{next:"/space.html"}`, same-origin session/rotation; POST `/api/auth/logout` CSRF-protected →204/session invalidation; GET `/api/me` authenticated → `{id,displayName,email,relationshipDate,paired,canInvitePartner}`. POST `/api/auth/partner-registration-code` first account only/one account total/CSRF →200 `{code,expiresAt}`. New code revokes prior unused one. Account2 cannot issue; capacity conflict409. This is a registration code, not a join invitation, so no space/member is created. Health and static auth assets remain public; space.html requires authentication; private feature REST/WS deny guests401 and authenticated accounts403. Error DTO code/message/fieldErrors/requestId implemented for identity, filter rejections/CSRF; requestId server-generated in header. Recovery endpoints remain absent. No raw token in URL; no secret persists in browser storage. Full feature inventory below remains draft.

## Shared future transport rules

Same-origin server session; HttpOnly/SameSite=Lax cookie, Secure in HTTPS; rotate session at login; CSRF on unsafe HTTP and STOMP CONNECT, trusted WS origins. No JWT/localStorage identity. Guest auth routes still need CSRF and registration eligibility. Server derives user/space/recipient from principal and membership; a client field cannot set ownership. Unpaired accounts may use pairing only, paired accounts private capabilities. D03 allows either member to edit/delete any shared memory. No app operator content-browsing role.

JSON uses UUID strings, ISO `YYYY-MM-DD` dates, UTC ISO8601 instants with `Z`. To preserve BIGINT precision in JavaScript, sequences/cursors/versions are **decimal strings** (draft engineering choice to review before adapters). Incoming DTOs allowlist fields; no entity binding. Successful writes are sent only after DB commit. Responses never expose password hashes, token hashes, storage paths, partner credentials or raw stack traces.

Error shape (feature implementation pending): `{"code":"VALIDATION_FAILED","message":"Check your input.","fieldErrors":{"relationshipDate":"Required"},"requestId":"<server UUID>"}`. 400 malformed/validation;401 missing/expired auth;403 permission/CSRF;404 consistently concealed/missing resource;409 version/idempotency/state conflict;413 oversized upload;429 rate with Retry-After;5xx generic retryable failure. Tokens never in operational log/request URLs except the reviewed recovery link; requestId is server-generated and safe. Framework foundation errors are not yet the full feature error envelope.

## REST inventory and request/response sketch

Every endpoint ID is draft, unique within this document. `S`=session required; `M`=active own-space member; `U`=unpaired session. All writes require CSRF, including guest authentication writes. Success status is proposed. DB mappings refer to schema draft; only app_user/couple_space/membership exist.

| API ID / source | Method and route | Access / request → response | DB / transaction / errors |
|---|---|---|---|
| API-AUTH-001 AUTH-01/03 | GET /api/csrf | Guest/S; no body →200 `{headerName,token}` | Session CSRF bootstrap; no secret in logs |
| API-AUTH-002 AUTH-01,D01/D02 | POST /api/auth/register | Eligible guest; `{displayName,email,password,confirmPassword,relationshipDate,registrationToken}` →201 `{id,displayName,email,paired:false}` | User+registration eligibility consume atomic; relationshipDate required; slots<=2 even concurrent; invalid token400/403, full409. Field limits/reset policy pending; no auto-login assumption |
| API-AUTH-003 AUTH-02/03 | POST /api/auth/login | Guest; `{email,password}` →200 self summary | Generic invalid credentials401,429; rotate session; unpaired routing flag |
| API-AUTH-004 AUTH-03 | POST /api/auth/logout | S; empty →204 | Invalidate session/owned sockets/call binding; repeat safe |
| API-AUTH-005 AUTH-03 | GET /api/me | S; empty →200 `{id,displayName,email,paired,spaceId?}` | Own account/membership only |
| API-AUTH-006 AUTH-02 | POST /api/auth/password-reset/request | Guest; `{email}` →202 generic | Same response existing/missing; queued mail policy pending |
| API-AUTH-007 AUTH-02 | POST /api/auth/password-reset/confirm | Guest; `{token,newPassword,confirmPassword}` →204 | Consume unexpired token+hash update+revision atomically; revoke sessions/sockets; expired/used400 |
| API-AUTH-008 AUTH-02 | POST /api/me/password | S; `{currentPassword,newPassword,confirmPassword}` →204 | Reauthenticate, change, revoke existing sessions; validation400 |
| API-AUTH-009 PRIV-01–03 | POST /api/me/logout-all | S; credential confirmation payload pending →204 | Own sessions only; revoke sockets/calls |
| API-PAIR-001 PAIR/D02 | POST /api/spaces | U; `{timezone,confirmedRelationshipDate}` →201 space summary | Initialize from signup proposal; singleton constraint; conflict409; no arbitrary member IDs |
| API-PAIR-002 PAIR-01/02 | POST /api/invitations | Waiting-space member; empty/regenerate intent →201 `{code,expiresAt}` | Hash stored, raw code shown only issuance; invalidate prior token atomically |
| API-PAIR-003 PAIR-01/02 | POST /api/invitations/preview | U; `{code}` →200 `{inviterName,relationshipDate,timezone,confirmationToken}` | **Proposed supporting route** for real join confirmation, not in source route list; token binds preview/shared-date revision; avoid code in query logs; invalid/expired400 |
| API-PAIR-004 PAIR-01/02 | POST /api/invitations/join | U; `{code,confirmationToken,confirmedRelationshipDate}` plus Idempotency-Key →200 space | Lock space/invite; member insert+consume atomic; self/full/stale409. Different signup dates explicitly shown/confirmed; canonical policy pending |
| API-PAIR-005 PAIR-03 | GET /api/space | S; no body →200 `{id,relationshipDate,timezone,members,state,version}` or unpaired `{state:"UNPAIRED"}` | Membership-derived, no arbitrary space selector |
| API-HOME-001 HOME | GET /api/home | M →200 `{partner,relationship,recentMemories,upcomingDates}` | Scoped summaries; no fabricated counts on failure |
| API-HOME-002 HOME-01/03 | PATCH /api/me/profile | S; `{displayName,version}` →200 own profile | Own update/version;409 stale |
| API-HOME-003 HOME/MEM | PUT /api/me/avatar | S; multipart `{file,version}` →200 metadata | Same private validation/access rules; limits pending; retain old avatar if failure |
| API-CHAT-001 CHAT-01–03 | GET /api/messages | M; exclusive `afterSeq` or `beforeSeq`, limit(default50 proposed) →200 `{items,nextCursor,hasMore}` | Conversation+message; before returns nearest older page sorted ascending; after ascending catch-up; never skip gap; invalid combo400 |
| API-GIFT-001 GIFT-01 | GET /api/gifts | M →200 `{items:[{code,label,asset,version}]}` | Active catalog only; free/catalog proposal |
| API-CALL-001 CALL-01–03 | GET /api/calls/current | M →200 call summary or `{call:null}` | Active scoped call, reconnect reconciliation |
| API-CALL-002 CALL-03 | GET /api/calls/{id} | Participant M →200 state/timestamps | Foreign/missing404 |
| API-CALL-003 CALL-03 | GET /api/rtc/config | M →200 `{iceServers,expiresAt}` | Short-lived TURN credentials, no permanent secret; no-store |
| API-MEM-001 MEM-01/02 | POST /api/uploads | M; multipart `{file,clientUploadId}` with Idempotency-Key →201 `{uploadId,state,metadata}` | Validate bytes/MIME/codec/quota; private key server-generated;413/400; no memory publish on fail |
| API-MEM-002 MEM-03 | GET /api/uploads/{id} | Owner-space M →200 upload state | No public file path |
| API-MEM-003 MEM-01/02 | POST /api/memories | M; `{title,memoryDate,caption?,uploadIds[]}` with Idempotency-Key →201 memory | Ready own-space uploads only; attach+publish atomic; failed file leaves unpublished |
| API-MEM-004 MEM-03 | GET /api/memories | M; cursor, type=all/photo/video/favorites, date sort →200 `{items,nextCursor,hasMore}` | Keyset(memoryDate,id); limit24 proposed; favorites per principal |
| API-MEM-005 MEM-03 | GET /api/memories/{id} | M →200 memory DTO incl assets/authorship/version | Deleted/foreign404 |
| API-MEM-006 MEM-03/D03 | PATCH /api/memories/{id} | Either M; `{title?,memoryDate?,caption?,assetIds?,version}` →200 memory | Original creator unchanged, updatedBy from principal, stale409 |
| API-MEM-007 MEM-03/D03 | DELETE /api/memories/{id} | Either M; expected version via If-Match →204 | DeletedBy principal; exact hide/purge policy Q03, stale409; no creator-only guard |
| API-MEM-008 MEM-03 | GET /api/assets/{id}/content | M; optional Range →200/206 authenticated stream | Recheck live memory membership each request; invalid range416; no public static link |
| API-MEM-009 MEM-02/03 | PUT /api/memories/{id}/favorite | M; empty →204 | Own favorite; unique pair, idempotent |
| API-MEM-010 MEM-02/03 | DELETE /api/memories/{id}/favorite | M; empty →204 | Own favorite only, idempotent |
| API-DATE-001 DATE-01/02 | GET /api/space/relationship | M →200 `{startDate,timezone,daysTogether,ordinalDay,nextAnniversary,version}` | Q02/Q06 unresolved, no hardcoded policy |
| API-DATE-002 DATE-01–03 | PATCH /api/space/relationship | Either M; `{startDate,timezone,version,confirmed:true}` →200 relationship | Shared date+reminder reschedule consistency;409 stale |
| API-DATE-003 DATE | GET /api/dates | M; cursor if needed →200 `{items}` | Own-space events, next occurrence; limits TBD |
| API-DATE-004 DATE | POST /api/dates | M; `{title,eventDate,repeatYearly,reminderEnabled,reminderLead,reminderLocalTime}` →201 event | LocalDate/reminder validation; occurrence idempotency |
| API-DATE-005 DATE | PATCH /api/dates/{id} | Either M; editable fields+version →200 event | Shared scope/version409 |
| API-DATE-006 DATE-03 | DELETE /api/dates/{id} | Creator M (proposal); If-Match →204 | Permission/delete policy to confirm |
| API-NOTIFY-001 NOTIFY-01–03 | GET /api/notifications | M; cursor →200 `{items,nextCursor,unreadCount}` | Principal recipient only; no sensitive preview default; retention pending |
| API-NOTIFY-002 NOTIFY-01/03 | POST /api/notifications/{id}/read | Recipient M; empty →204 | Own row, idempotent; foreign404 |

Profile/avatar access for unpaired users is to finalize with auth UI; no supporting route becomes approved business scope from this draft. POST invite preview is explicitly a proposed implementation of the source's confirmation screen, not an implemented extra capability. No public multi-couple API, push subscriptions, payments, message mutation or account/unpair deletion contract is added.

## STOMP surface and payloads (future)

Endpoint `/ws`, existing HTTP principal, explicit same-origin allowlist, CONNECT CSRF header from `/api/csrf`. No wildcard origins. Reject anonymous/revoked/disabled users. Recheck membership on SEND/SUBSCRIBE; reject direct client publish to broker `/queue/**`, `/topic/**`, `/user/**`. Only literal own `/user/queue/messages`, `/user/queue/calls`, `/user/queue/notifications`, `/user/queue/errors` subscriptions. UserId queue guessing does not confer access. All tabs stop receiving on session revocation; implement a real connection registry before enabling endpoint.

Client envelope: `{eventId:UUID,type:string,payload:object,clientMessageId?:UUID,callId?:UUID}`. Server: `{eventId:UUID,type:string,serverTimestamp:Instant,resourceId?:UUID,sequence?:decimalString,payload:object}`. Never use senderId/recipientId/spaceId from client as authorization authority. Error queue only originating session, not partner/global broadcast; stable code/requestId/client event correlation.

| Destination | Payload / state / source |
|---|---|
| /app/chat.send | TEXT `{body}` or GIFT `{giftCode,note?}`, clientMessageId mandatory; CHAT/GIFT service commits sequence+message+snapshot; authoritative SENT event after commit |
| /app/chat.delivered | `{messageId,sequence}` recipient ACK only, validate received range; server emit alone is not delivery |
| /app/chat.read | `{lastReadSequence}` validate own conversation, monotonic and bounded; only visible conversation client issues ACK |
| /app/chat.typing | `{typing:boolean}` ephemeral, expiry proposal5s; no history storage |
| /app/presence.heartbeat | `{}` own session only; heartbeat/offline timings proposed; aggregate all tabs |
| /app/call.invite | `{}` + eventId; resolve partner server-side; atomic one active/ringing call/space |
| /app/call.accept /decline | callId + `{}`; callee only, RINGING→CONNECTING/DECLINED; one tab accept wins |
| /app/call.cancel | callId; caller only while RINGING→CANCELLED |
| /app/call.offer /answer | callId + `{sdp}` transient participant-only payload, legal state only |
| /app/call.ice | callId + `{candidate,sdpMid,sdpMLineIndex}` transient, bounded payload; queue until remote description; tolerate duplicate ICE |
| /app/call.connected | **Proposed extra supporting command** callId + `{}` for actual peer connected reporting; both participant reports + server validation needed before ACTIVE/timer (review before DEV-08) |
| /app/call.end | callId + `{reason}` participant-only; terminal transition idempotent; release resources on both clients |

Call states proposed: RINGING→CONNECTING→ACTIVE→ENDED; declined/missed/cancelled/failed terminal alternatives. Accept ≠ media connected. Ring30s/recovery15s remain proposed. Disallow stale-call signaling, never log/store ordinary SDP/ICE/media. WebSocket is signaling only; actual browser media tracks and TURN relay tests required.

Chat retry unique(sender,clientMessageId); equivalent replay returns original message; changed semantic payload409. Server assigns sequence under conversation lock; no ACK before durable save. After-commit live delivery failure leaves history intact. Client reconnect backoff1/2/4…30s+jitter proposed, reauthenticate if expired, resubscribe and catch up after last sequence, dedup by messageId/eventId. No separate REST send path now; future fallback must reuse same service. Durable inbox event unique(sourceEvent,recipient,type); scheduler key(event,occurrence,recipient,reminderType). Transient typing/presence/SDP never offline-retained. General Idempotency-Key persistence window/replay DTO policy remains an engineering contract to finalize per operation; no blind retry of new keys.

## Integration map and risks

INT-CHAT-001: REST/STOMP→shared transaction→DB→after-commit own-user queues; failure reconnect/history. INT-NOTIFY-001: committed feature event→dedup inbox→WS; offline gets inbox. INT-MEDIA-001: authenticated upload→private filesystem+metadata→ready publish; compensate orphans, retention pending. INT-RTC-001: browser→STUN/TURN ICE→browser media; short-lived credential auth, timeout/error cleanup. INT-MAIL-001: proposed reset/security email→SMTP/local test inbox, generic public response, retry/dedup proposed. No external webhook/callback/payment contract exists.

Logging: requestId/event type/opaque resource/outcome only; no message/file contents, cookies, password/token or SDP/ICE. HTTP errors safely generic. Existing consumers: none, so no current client compatibility to preserve; confirm draft sequence strings before production UI. Feature tests must cover source ACs including real browser/device/authorization/retry/DB cases. DEV-01 status does not satisfy those feature ACs. Figma live audit blocked by connector plan limit; exact UI mappings and review remain pending. Related schema and open-rules drafts are mandatory context for the next task.

## DEV-04 current increment (1 November 2026)

See DEV-04_Working_Spec.md and DEV-04_Checkpoint.md for current implementation/evidence. Older unimplemented draft rows above remain historical; this section is effective for DEV-04. Actor/space are always server-derived. Cookie session + CSRF required for writes; ACTIVE membership enforced on all handlers. No WS SEND or new subscription is opened.

| Method / route | Body / response / behavior |
|---|---|
| GET /api/home | Existing Space top-level fields retained; adds self {id,displayName,email,version}, calendar {today,dayNumber,anniversary,daysUntilAnniversary,nextMidnight,timezone}, nextEvent nullable |
| GET /api/dates?page=0 | Same calendar; items up to50, hasMore, page. Upcoming first then historical non-yearly. Each item: id,title,eventDate,repeatYearly,createdBy,version,nextDate,daysUntil,upcoming. Negative daysUntil is history only, never the upcoming Home card. |
| PATCH /api/profile | displayName (1–60 Unicode code points), version; returns own Profile. No target user/email/avatar change. |
| PATCH /api/relationship-date | relationshipDate (today/past in persisted Myanmar zone), version, confirmed=true; returns refreshed Home. Both members affected; signup originals unchanged. |
| POST /api/dates | Required UUID Idempotency-Key header; title (1–100 Unicode code points), eventDate (years1–9999), repeatYearly boolean; 201 SpecialDate. Same actor/key/body returns current event; altered intent409. Receipt survives deletion; replay of deleted event404, no recreation. |
| PATCH /api/dates/{uuid} | Same event fields + version; either member may edit; 200 SpecialDate. Creator preserved, updater recorded. |
| DELETE /api/dates/{uuid}?version=n | Either member (owner's explicit decision). 204; missing404/stale409. Deletes event, keeps minimal creation receipt. |

Dates use ISO LocalDate, never browser timezone conversions. Day number is inclusive (start=1); Feb29 clamps toFeb28 in non-leap years and restores29 in leap years. Yearly dates never occur before their originally selected date. Original dates remain unchanged. Standard error {code,message,fieldErrors,requestId}; 400 validation,401 guest/revoked,403 unpaired/CSRF/unsupported target route,404 absent/foreign resource,409 stale/intent conflict,413 oversized write,429 bounded shared write limiter. Malformed UUID/date/page uses Spring400 handling. Request JSON bounded16KiB including chunked bodies.

AFTER_COMMIT state-only events: profile.updated, relationship.updated, date.updated delivered on existing /user/queue/pairing with eventId,type,serverTimestamp,state=ACTIVE. No names/content in event. Exact-own-queue authorization/session/origin/CSRF preserved. Home subscribes, REST-reconciles, checks every15s while visible, on visibility return and at server-derived nextMidnight. These invalidations are not a durable notification inbox or reminder scheduler.

## DEV-05 effective Chat contract (1 November2026)

Authority: F04 CHAT-01–03 / AC-F04-01–06, D01/D04/D06; owner confirms4000 Unicode code points and Edit/Delete deferred. Existing planned rows above are historical where this section differs. Native/proxy/browser evidence and known limitations will be in DEV-05_Checkpoint.md. Home adds unreadMessages without removing its previous fields. No third party integration/dependency is added.

| Endpoint / destination | Contract |
|---|---|
| GET /chat.html | ACTIVE couple member only; guest→sign-in; unpaired403 |
| GET /api/chat | Optional exclusive before>0 for older page OR after>=0 for forward catch-up; no both. Default latest50. Page {selfId,selfName,partner:{id,displayName,slot},timezone,items,hasMore,latestSequence,unreadCount}; items ascending server sequence, limit50. No client space/user selection. |
| GET /api/chat/state | {latestSequence,unreadCount}; no message previews |
| GET /api/chat/receipts?ids=uuid,... | 1–50 known message IDs in actor's current space; receipt timestamp lookup supports reconnect of older loaded outgoing messages. Any missing/foreign ID403; no body returned. |
| SUBSCRIBE /user/queue/chat | Exact own queue, ACTIVE membership + live HTTP identity/revision; raw/foreign queues denied |
| SEND /app/chat.send | JSON {clientMessageId:UUID,body:string}; body not trimmed, blank forbidden, up to4000 Unicode code points, invalid surrogate/NUL rejected. Session principal determines sender and space; timestamp/order/id assigned server. Exact same sender/key/body replay returns original message; changed intent409. |
| SEND /app/chat.delivered | {messageIds:UUID[1..50]}; only peer-sent messages in own space; no delivery on broker emit alone |
| SEND /app/chat.read | Same IDs; explicit recipient read assertion implies delivered; updates only referenced messages, not unseen older pages. UI sends only when page visible/focused and bubbles intersect conversation viewport. Human attention cannot be proven by server. |

Message DTO {id,sequence,senderId,clientMessageId,body,createdAt,deliveredAt,readAt}; timestamps ISO UTC Instant, rendered Myanmar time. Per-message read/delivery instead of a global read-through cursor prevents latest50 opening from marking all unseen older records read. Unique sender/client ID and space/sequence; allocation row lock and insert one transaction. FK sender must be member of message space. Read implies delivered. No edit/delete/attachment endpoint. Rate defaults:60 new sends/account/minute (equivalent replay does not consume new-send quota),120 ACK commands/account/minute; configurable local protection defaults, not production capacity guarantees. Frame/container32KiB, outbound buffer128KiB; 4000 four-byte emoji verified by native STOMP.

Replies session-only /user/queue/chat: chat.sent {clientMessageId,message}, chat.ack {receipts}, chat.error {clientMessageId nullable,code,detail}; DTO also contains null/empty irrelevant fields. Message errors never contain body/SQL/cookies. Codes include EMPTY_MESSAGE, MESSAGE_TOO_LONG, INVALID_MESSAGE_ID, MESSAGE_CONFLICT409, CHAT_RATE_LIMITED429, INVALID_ACK400, INVALID_RECIPIENT403, MESSAGE_NOT_SAVED and ACK_NOT_SAVED for handled persistence failures. Malformed commands get INVALID_CHAT_COMMAND; unrecognized destinations/unauthorized subscriptions are denied and may close socket. No SENT before transaction commit; commit uncertainty retries same UUID, never a new key.

AFTER_COMMIT events {eventId,type,serverTimestamp,message nullable,receipts}: message.created full immutable record or message.receipt timestamps only, routed server-side to both current members' enabled sessions. Best-effort dispatch, durable history/receipt lookup recovers lost events. State-only chat.updated on existing pairing queue refreshes Home badge; no preview or browser push/inbox promise. Existing pairing queue waiting-member behavior retained; chat queue/commands/outbound frames require ACTIVE. Existing CONNECT origin/session/CSRF and HTTP revocation preserved.

Client subscribe→REST reconcile, gap-safe forward cursor, seq/id dedup, explicit older paging, receipt refresh for loaded outgoing messages;1/2/4…30s+jitter reconnect with GET/me auth recheck and CSRF reload. Visible polling15s, ACK batching3s plus scroll/focus hooks. Failed/pending message retains original UUID/text; browser outbox sessionStorage scoped to account, max50/tab, memory fallback notification, cleared at confirmed signout/expired access. Sent means committed, Delivered means recipient ACK, Read means explicit recipient assertion; no fake Online/Typing. Pending is not guaranteed across session expiry/browser closure. Message search/attachment/call/presence/general notification inbox remain future scope; DEV-06 adds catalogue search and typed gifts below.

## DEV-06 additive character sticker / gift contract

Owner's 1 November 2026 continuation authorizes multiple free character sticker packs; see DEV-06_Working_Spec.md. GET `/api/gifts?pack=&q=&offset=0` requires ACTIVE membership, returns `{items:[{code,name,pack,assetPath}],packs,total,hasMore}`. Page size60, q≤80 code points, offset0..100000; literal case-insensitive name search. Bundled `/stickers/**` assets/notices require the same private membership boundary; no remote runtime images.

Existing `/app/chat.send` also accepts `{clientMessageId,type:"GIFT",giftCode,body:optionalNote}`. Note defaults empty/max500 code points; server resolves recipient, rejects any supplied recipientId, validates active catalogue and snapshots giftCode/name/assetPath. TEXT command remains backward-compatible. Success/history/events add `type,giftCode,giftName,giftAssetPath`; TEXT has null gift fields. Same key with changed type/code/body is MESSAGE_CONFLICT. Exact replay returns the original snapshot even after catalogue deactivation, with no repeated created event. Unknown/inactive gift→INVALID_GIFT; oversized note→GIFT_NOTE_TOO_LONG. Existing membership/session/CSRF, sequence, AFTER_COMMIT, rate, receipt, unread, history and outbox rules apply.

Recipient's new-gift `message.created` event includes `notification:"You received a little gift."`; sender/receipt events have null notification. This is the existing in-app channel, not the deferred DEV-09 inbox. GET `/api/gifts/history?direction=all|sent|received&before=sequence` returns `{items,hasMore}` descending50. Dialog previews/cancel/search/history never create messages. Only Confirm queues the selected intent. Image failure shows saved text label; an image/animation toggle supports text-only display and reduced-motion default. Read ACKs are suppressed while the gift dialog obscures chat, in addition to document/focus/viewport guards.

## DEV-07 planning amendment — not implemented

Owner confirms video<=180s/100MB and document attachments PDF/DOCX/XLSX/PPTX<=20MB per file on1 November2026; DEV-07_Working_Spec.md supersedes conflicting proposed120s limits. Future private memory upload/download contract must represent DOCUMENT assets, validate real content/server-measured duration/size, preserve both-member edit/delete and authorize every metadata/download request. Owner subsequently confirms3-day shared Trash/Restore, automatic permanent deletion at expiry, and immediate permanent deletion of the confirmed Trash batch on Empty Trash. Normal asset access must deny trashed content; Restore must deny expired/purging content. Detailed routes/atomicity/idempotency/error/retry semantics are still to design. No public file URL or external document processing selected. Preview/download UX, codec, photo/count/quota and backup policy remain separate. This note does not activate any upload/Trash route or change Chat APIs.
# DEV-07 implemented contract addendum — 1 November 2026

This addendum supersedes older proposed `/api/uploads` and `/api/assets/{id}/content` memory routes below. Scope: MEM-01–03 / AC-F07-01–08 and owner-confirmed file/Trash decisions in DEV-07_Working_Spec.md. Current code is under verification; graphical acceptance remains pending.

All memory endpoints require a current session and ACTIVE couple membership; mutation requests require the Spring CSRF header. Actor and space always come from the session. No public filesystem path is returned. A missing/foreign/trashed file is concealed with 404. Documents always download as attachments; all file responses use `Cache-Control: no-store, private`.

| Request | Body / response | Invariants |
|---|---|---|
| POST `/api/memory-uploads` | multipart `file`, UUID `Idempotency-Key`; 201 Asset | One file per request; aggregate <=10 attachments when publishing. Early gate admits <=2 upload requests before multipart parsing; 429 with Retry-After when busy. Actual content validated. Unpublished bytes only accessible by uploader for 24h. |
| DELETE `/api/memory-uploads/{id}` | 204 | Only uploader's unattached draft; discard and retryable physical cleanup. |
| POST `/api/memories` | `{title,caption?,memoryDate,assetIds[]}` + UUID Idempotency-Key; 201 Entry | 1–10 ready unique own draft assets; title 1–120, caption <=2,000 code points, today-or-earlier Myanmar date. Atomic attach/publication/receipt; same key/same intent returns original entry, changed intent 409, permanently removed target 404. |
| GET `/api/memories` | `filter=ALL/IMAGE/VIDEO/DOCUMENT/FAVORITES/TRASH`, optional UUID `before`; `{items,nextCursor,today}` | 24 entries, descending (memoryDate,id); favorites per actor, mixed-kind entries appear in each matching type. No draft/expired/purging item. |
| GET `/api/memories/{id}` | optional `trash=true`; Entry | Normal reads ACTIVE; trash flag reads only unexpired TRASHED metadata. |
| PATCH `/api/memories/{id}` | Full `{title,caption,memoryDate,assetIds,version}`; Entry | Either partner; preserve creator; updater from actor; stale version 409. Existing attached assets may be retained/reordered; new assets must belong to editing actor's draft. Removed assets are discarded only with committed edit. |
| PUT `/api/memories/{id}/favorite` | `{favorite:boolean}`; 204 | Only actor's preference, active memory. |
| DELETE `/api/memories/{id}?version=N` | Entry | Either partner; TRASHED with server deletion time and expiry exactly +72 hours. Same actor/version replay is tolerated without a new expiry. |
| POST `/api/memories/{id}/restore` | `{version}`; Entry | Either partner; unexpired TRASHED only; stale 409; expired/PURGING/missing 404. |
| GET `/api/memories/trash/preview` | `{snapshot,count,pending}` | Snapshot binds space and current trashed IDs/versions. `pending` counts irrevocable PURGING work. |
| POST `/api/memories/trash/empty` | `{snapshot,confirmed:true}`; `{deleted,pending}` | 409 if batch changed; explicit confirmation required. Commit PURGING first, revoke access/restore, delete attached binaries/thumbnails and metadata. 200 complete batch, 202 storage retry pending. Active memories excluded. |
| GET `/api/memory-assets/{id}` | optional `thumbnail=true`, `download=true`; 200/206 Resource | Authorize every request; only live memory or own unexpired draft. Authenticated byte ranges handled by Spring MVC. Thumbnail unavailable for documents. |

Asset fields: `id,name,kind,mime,size,sha256,duration,width,height,url,thumbnail`. Entry fields: `id,title,caption,memoryDate,state,createdBy,updatedBy,deletedBy,createdAt,updatedAt,deletedAt,expiresAt,version,favorite,assets`. Limits use decimal MB: photo10,000,000; MP4 video100,000,000 and <=180s; PDF/DOCX/XLSX/PPTX20,000,000 bytes. Working playback baseline H.264 8-bit + optional AAC; files are preserved without transcoding originals. Office packages reject macros, embedded programs, invalid XML/relationships and bounded expansion violations; this is not a malware-scanner certification.

WS: existing exact own `/user/queue/pairing` subscription receives `memory.updated` invalidation for both partners, own `memory.favorite` invalidation, and a partner-only `{eventId:memoryId,type:"memory.published",memoryId,notification:"A new memory was added."}` only after a new committed create. Create replay emits no second publication. Reload via REST reconciles missed events; persistent inbox remains DEV-09. No public topic or client-selected recipient.

Expiry worker: every60s, claim expired Trash as PURGING; request-time checks deny expiry immediately. Physical purge failure retains durable PURGING metadata for retry after restart. Unpublished uploads and unreferenced staging/object files age out after24h. UI never claims an I/O failure was a completed permanent removal. Historical backup-copy deletion is not established by these operations.

Earlier sections retain historical planning context and unrelated future modules; the DEV-07 addendum above supersedes their memory-specific proposals.
# DEV-08 implemented increment — 1 November2026

Supersedes earlier proposed call surfaces for this local build. Parent CALL-01–03 / AC-F06-01–06; owner confirms Voice/Video, ring30s, recovery15s, Busy/one-live-call and first accepting tab. Full working contract and graphical gaps: DEV-08_Working_Spec.md / DEV-08_UI_Coverage.md; exact evidence: DEV-08_Checkpoint.md.

HTTP ACTIVE session only: GET `/api/calls/current` → `{call:View|null}`, GET `/api/calls/recent` → latest20 Views, GET `/api/calls/{id}` → own-space participant View or404; GET `/api/rtc/config` → no-store `{iceServers,relayConfigured,expiresAt}`. Optional coturn uses1-hour HMAC credentials; its master shared secret is server-only. Managed TURN can instead use provider-issued username/credential password, returned only to the same authenticated paired users that can call. In managed mode `expiresAt` is a1-hour config refresh deadline, not a claim that the provider's long-term credential expires then; rotation/lifetime belongs to the provider. Both authentication modes cannot be configured together. Empty relay config explicitly means direct/local only. POST `/api/calls/{id}/leave` + CSRF `{clientId,eventId}` ends that selected page's call; the random page-client token is never exposed in metadata (Views return its SHA256 hash).

STOMP only exact own `/user/queue/calls`. `/app/call.invite/accept/decline/cancel/end/resume/connected/disconnected/heartbeat/offer/answer/ice/media` commands use `{eventId,clientId,callId?,mode?,negotiationId?,sdp?,candidate?,sdpMid?,sdpMLineIndex?,micEnabled?,cameraEnabled?}`. No client recipient/caller authority. Reply `{eventId,type:call.ack|call.error,call,code,detail}` is origin-session scoped. Durable action receipts: invite/accept/decline/cancel/end, actor+event key and intent hash; changed intent409. Signals are bounded transient events, not stored/offline replayed. Local busy/offline replies create no fake successful call.

View: id/callerId/calleeId, **callerClient/calleeClient = SHA256 token hashes**, mode/state, createdAt/ringDeadline/acceptedAt/connectedAt/recoveryDeadline/endedAt/endReason/version/serverTime. Caller/callee selected socket IDs and raw page tokens are not returned. Public metadata `call.updated` after commit goes to both accounts' tabs; offer/answer/ICE/media events `{eventId,type,callId,negotiationId,payload}` use the exact selected peer session destination, including when that session disappears (drop, never fall back to other account tabs). Publisher order remains; client waits for SDP ACK before ICE and queues remote ICE before remote description. Caller alone offers/restarts, callee answers; stale negotiation/role/client/socket rejects. SDP16k/ICE4k limits,240 signals/min/socket and bounded120s transient dedup apply.

States: RINGING→CONNECTING→ACTIVE, optional RECONNECTING, terminal ENDED/DECLINED/MISSED/CANCELLED/FAILED. First accept binds callee tab; accept alone never starts the timer. Both authenticated selected clients must report their actual PC connected state for current negotiation; server stores those reports, not independent RTP proof. Ring30s, initial connection technical timeout30s, recovery deadline15s (not extended on retries); selected heartbeat5s. Server reconciles2s, denies expired state immediately during commands. Logout/revision/HTTP expiry queues irrevocable end; ordinary socket loss permits bounded same-page-client resume after old socket closes. Navigation closes the page's PC/tracks and sends protected keepalive leave. Permanent notification inbox remains DEV-09.

DEV08-UI01: authenticated /user/queue/pairing chat.updated invalidation adds optional incomingMessageId (UUID): present only for a partner's newly committed message; null for sender and receipt-only events. Existing eventId/type/state remain compatible. No message body is added to this invalidation. Home/Memories use it for an optional notification chime; REST/history does not chime.
## DEV-09A — durable notification inbox (local increment)

API uses the authenticated principal and active couple membership; clients cannot choose recipients. GET /api/notifications?before=<positive sequence>&unreadOnly=false returns {items:[{id,sequence,type,createdAt,readAt,version}],nextCursor,unreadCount}, newest25, with a nullable nextCursor. unreadCount covers all own unread rows, independent of filter/page. Types: MESSAGE, GIFT, MEMORY, MISSED_CALL, PARTNER_JOINED. No message body, note, media title or target IDs are included in list previews.

PATCH /api/notifications/{UUID} with CSRF and {read:boolean,version:long} sets state. Same-state retries succeed without version increments; a stale opposing write returns409 NOTIFICATION_CHANGED. Missing/foreign IDs return404 NOTIFICATION_NOT_FOUND. Marking an inbox item read does not send chat read receipts. GET /api/notifications/{UUID}/target returns {available:boolean,url:string|null}, checking current membership, notification ownership and source availability; deleted/trashed memories return unavailable. Open follows the target feature after marking that notification read. Targets are server-generated relative routes; no client URL is stored.

New committed partner message/gift, published memory, missed call and partner-joined events insert inbox metadata in BEFORE_COMMIT of the same business transaction. Unique(source_event_id,recipient_id,type) deduplicates retries. Recipient-only notifications.updated on the existing /user/queue/pairing is dispatched after commit, including read-state changes to own tabs. It is an invalidation, not notification content or a new client publish command. Existing WS deny-by-default rules remain unchanged. REST on mount/reconnect/visibility plus15s visible polling recovers missed invalidations. No old-event backfill, expiry scheduler, reminders or presence in this increment.

DEV-09B owner revision: optional non-persistent Notification API alerts while an authenticated app page remains open/backgrounded; generic content only, defaultOff, browser permission from Settings gesture. No Push API/service worker/server integration. Initial inbox baseline does not alert; later new unread deltas and incoming calls may alert. Sent sound follows confirmed own pending intent, not Send-button click. Notification volume0–100 and browser On/Off are device-local Settings. Prior D06 in-app-only boundary is revised only for this explicit open-page browser/system UI request; closed-site push remains out of scope.

## CHAT-02A owner extension — replies, editable reactions and deletion

Existing /app/chat.send accepts optional replyToId(UUID). Existing 5-field intent remains compatible; reply target participates in stable idempotency. Replies require a visible, nondeleted same-space target when first saved. Message projection adds replyToId, reply:{id,senderId,body,type,giftName,unavailable}, reactions:[{userId,emoji}], deletedAt, version, hidden. Quotes are dynamically resolved for the requesting actor, limited to200 Unicode code points; unavailable originals do not expose content. Shared creation events carry raw reply ID without actor-specific quote content; subsequent protected history/snapshots hydrate it. A hidden send retry returns an empty hidden:true stub; deleted retry returns an empty tombstone. Neither restores content. No additional send event or inbox item is emitted on retry.

GET /api/chat/messages?ids=<1..50 UUIDs> returns {items,removedIds}; validates all IDs in actor's space, returns full actor projections and hidden IDs for removal. /api/chat and /api/chat/state add changeVersion; it advances for new sends/reaction/delete/hide while latestSequence remains message-order only. Client refreshes loaded snapshot batches when changeVersion changes, including reconnect/offline recovery, so old message mutations are not lost behind the send cursor.

POST /api/chat/messages/{id}/hide →204: authenticated actor-only Delete for me, idempotent. POST /api/chat/messages/{id}/delete →Message: sender-only Delete for everyone, idempotent tombstone; clears live body, gift fields, own reply reference and all reactions. Stable intent hash survives; no Memory Trash or automatic recovery introduced. PUT /api/chat/messages/{id}/reaction {emoji:allowedString|null} →Message: one own reaction, replace/remove desired state; unchanged retries do not change version. Allowlist ❤️ 😂 😮 😢 👍 🥰. Invalid400; guessed/unavailable404; foreign snapshot403; other sender's deletion403; deleted reaction/reply409. CSRF/current active membership required. Requests cannot choose sender, recipient, space or another member's reaction.

Mutation AFTER_COMMIT events message.reacted/message.deleted (both partners) and message.hidden (own tabs only) use existing /user/queue/chat; message payload is null and clients reconcile private snapshots. No new STOMP destinations or publish permissions, no reaction notification/sound, no edit-message operation. Unread counts and inbox target resolution exclude deleted/actor-hidden messages; GIF history excludes both. Native client read receipts stay separate from inbox-read state; client suppresses read acknowledgements while call/inbox/message-action dialogs obscure chat.
# CHAT-03 additive private profile/activity contract (2 November 2026)

Owner-authorized mobile UX revision: [CHAT-03 record](CHAT-03_Mobile_Chat_UX.md).
PUT `/api/profile/photo` takes bounded raw JPG/PNG bytes (CSRF/session, own profile); returns `{avatarUrl}`.
GET `/api/profiles/{id}/photo?v=N` is same-active-space only, normalized JPEG, no-store.
GET `/api/presence` returns server-selected partner `{active,lastActiveAt,serverTime}`; POST updates own foreground activity, 204, CSRF required.
Home self/member and Chat partner projections add nullable `avatarUrl`; Chat page also adds nullable `selfAvatarUrl`. Existing fields and STOMP contracts unchanged.
See the record for exact limits, TTL semantics, error statuses and verification boundaries. No live deployment.
# CHAT-07 local extension (2026-11-03)

`PATCH /api/chat/messages/{id}`: authenticated same-space sender + CSRF, body `{body,version}`, returns Message with nullable `editedAt`. No edit time limit; TEXT/FILE 4,000 code points, GIFT note 500, text cannot be blank. Version conflict409 preserves client draft. CALL/deleted messages are immutable. Existing original-send idempotency fingerprint is retained after editing.

Chat Page adds `activity: {messageCount,videoCallSeconds,videoCallCount}` (same calculation as Home). Message adds nullable `callId` / `call: {id,state,connectedAt,endedAt,durationSeconds}`. New `type: CALL` is server-authored, terminal VIDEO metadata only, pre-read/no normal message notification, excluded from message count. New private `message.edited` / `call.logged` invalidations use existing Changed envelope with null message; REST snapshots/paging hydrate current state. Historical pre-feature calls are not backfilled into the timeline. See [CHAT-07 record](CHAT-07_Edit_Activity_And_Call_History.md).
## GAME-01 private Connect 4 extension (local)

GET `/api/games/connect4` and CSRF POST `/api/games/connect4/{invite|accept|decline|cancel|move|resign}` require ACTIVE pairing. Commands `{actionId,gameId,version,column}` use session actor, stable retry key and version. Column0–6 only for move. Server Page `{selfId,members,game}` includes42-cell board, turn, result, winning cells and version. Private after-commit `game.updated` invalidation uses existing own pairing queue with no board payload; REST/poll recovers. Full acceptance/status/error/retry definitions: [GAME-01](GAME-01_Connect_Four.md). Existing calls/chat contracts unchanged.
