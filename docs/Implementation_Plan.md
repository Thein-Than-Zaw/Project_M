> Current status (1 November 2026): DEV-08 native call increment and V21 local upgrade are recorded in DEV-08_Checkpoint.md. Latest DEV-08/DEV-07 addenda supersede historical foundation/planning descriptions. Actual browser/media/relay verification remains pending.

# Implementation plan / bounded work register

Source: requirements v1.1 §§5.11, 6 and owner instructions. Draft v0.3; D01–D06 confirmed. DEV-01 and scoped DEV-02 Login/Sign Up verified; current increment DEV-05 private Chat (DEV-05_Checkpoint.md); DEV-03 pairing and DEV-04 core verified with their documented limits. Owner confirmed Asia/Yangon and persistent one-time pairing (login/offline/logout never reset membership). See DEV-03_Working_Spec.md. Other business defaults remain separate; full-product readiness is not asserted.

## Architecture and dependency order

Browser static HTML/CSS/JS → same-origin Spring MVC REST → transactional services → repositories → PostgreSQL. Authenticated STOMP adapters call the same services, delivering after commit to server-derived user queues. Private files are authorized through controller streaming. WebRTC uses STOMP for signaling only and STUN/TURN for media connectivity. In-App notification channels only. Start with one application instance; review sessions/broker/presence/scheduler before scaling.

| Task | Source / outcome | Dependencies / decision gates | Verification before completion | Status |
|---|---|---|---|---|
| DEV-01 | §5.11.7.1; Maven/Spring, DB/Flyway, secure boundary, local scripts/docs | Engineering delegated; no feature business decision required | Actual build/boot, fresh/repeat migrations, PostgreSQL negative/concurrent constraints, HTTP access | See checkpoint |
| DEV-02 | F01/F10 AUTH-01–03, D01/D02; real signup/login/session/CSRF and responsive auth screens | Setup/partner-registration codes per continuation; local defaults recorded, recovery/email and password-change/logout-all deferred from this Login/Sign Up increment | Missing date, two-account/concurrent cap, wrong login, CSRF, session rotation/expiry/revision revocation; full reset tests later | VERIFIED scoped Login/Sign Up; remaining F01 work explicit in checkpoint |
| DEV-03 | F02 PAIR-01–03; one shared space, invite and real join | DEV-02; confirmed Myanmar timezone/one-time pairing; explicit creator date + partner confirmation; no unpair/replacement lifecycle selected | Atomic consume/member/receipt, expired/self/replay/foreign denial, race, live waiting→active, logout socket close, persistent paired login | VERIFIED local scope; AI self-review, human UI/UAT pending; DEV-03 checkpoint |
| DEV-04 | F03/F08 HOME/DATE; profile/home/dates | DEV-03; Q02 confirmed Day 1 / Feb28 in non-leap years (Date_Counting_Decision.md); remaining Q06 and date/reminder defaults | Unicode profile, shared date consistency, midnight/leap/year transitions, conflicts, mobile/laptop | Core IMPLEMENTED/native22 PASS; graphical journeys verified except actual midnight; full task PARTIAL (avatar/media summary/reminder scope); DEV-04_Checkpoint.md |
| DEV-05 | F04/F09 CHAT/NOTIFY; durable real chat and base recovery | DEV-03; reviewed message/reconnect limits and authenticated WS contract | Persist-before-ACK, dedup/conflict, ordered catch-up, receipt authenticity, revoked socket/foreign subscription | Core IMPLEMENTED/native30 PASS; browser core verified; hidden-tab graphical qualification pending DEV05-UI01; full verification PARTIAL; DEV-05_Checkpoint.md |
| DEV-06 | F05 GIFT; actual catalog/selection snapshot and emoji; owner continuation adds bundled licensed character stickers | DEV-05; 1 Nov owner requests multiple cute packs / available licensed stickers | Chosen sticker/note preserved, Unicode, cancel/no send, duplicate replay, invalid code, private assets | Core IMPLEMENTED: 363 Blobfox/BunHD stickers (20 animated), native35 PASS, described final GUI flows verified; DEV05 hidden-tab qualification still pending. See DEV-06_Checkpoint.md |
| DEV-07 | F07 MEM, D03; private photo/video/document uploads, shared edits,3-day Trash/Restore and confirmed Empty Trash | DEV-03; owner confirms video3min/100MB, PDF/DOCX/XLSX/PPTX20MB, Trash3d then permanent purge/Empty Trash immediate; remaining photo/count/quota/backup and file/security/UI contract | Real uploads/downloads/ranges, duration/size/type validation, both-member actions, denied access, expiry/restore/purge and actual file cleanup/retry/concurrency | PLANNED; confirmed decisions/impact in DEV-07_Working_Spec.md |
| DEV-08 | F06 CALL; real two-device WebRTC | DEV-03/05; call defaults, local secure device setup and TURN plan | Two-network audio/video, forced relay, actual track toggles/cleanup, competing calls, stale/foreign signals | PLANNED |
| DEV-09 | F09/F10; notification inbox/settings/session lifecycle | DEV-04–08; retention/presence/session policies | Recipient-only inbox, duplicate reminder/event, multi-tab presence, logout-all/socket disconnect | PLANNED |
| DEV-10 | Integrated delivery | Completed dependent tasks + applicable human decisions | Signup→pair→chat→gift→memory→date→call→logout; unauthorized and real device cases, recovery/runbook | PLANNED |

Each future task stays a coherent user outcome: adapter + service + validation + authorization + persistence + UI states + meaningful tests. Add migrations when its reviewed contract is ready; do not create all speculative tables in DEV-01. Preserve existing source choices when resuming; inspect actual files, Flyway history and working changes before retrying.

## DEV-01 scope / definition of done

Permitted edits: build/wrapper, environment/local setup scripts, application entrypoint/config, public engineering status, three foundation migrations, scoped tests and drafts/traceability/checkpoint. No feature registration, chat, memory, gift, dates or call services. No schema seeds, demo accounts, payment, push, public media or cloud purchase/deploy.

Inputs/minimum context on resume: requirement v1.1 SHA in checkpoint, this plan, engineering profile, schema/API drafts, open rules, current sources and test results. Base identity: no Git repository/code. Final/tested identity: local source manifest in checkpoint. No human approvals recorded beyond explicit current instruction and D01–D06.

Risks: unsupported assumption promotion; private route exposure before membership checks; cap race; environment drift; existing-data misuse; external Figma limit. Mitigations: explicit draft labels, closed private surfaces, native constraints/tests, pinned build/portable tools, no baseline/clean. Engineering setup is independent of unresolved feature choices.

Review states remain separate: implementation, verification, AI self-review, human business/UI/technical acceptance, external record sync and release. CI provider/workflow absent; local tests are not a CI run. Future account lifecycle changes may require forward schema work; no deletion policy is implied by slot constraints.
# DEV-07 resumed work — 1 November 2026

User resumed DEV-07 after the account handoff pause. Bounded outcomes: (A) correct real-file validation and native lifecycle/access/concurrency tests; (B) reconcile UI entry points, contracts and coverage; (C) build and protected local upgrade/run with actual evidence. Existing stack/data and confirmed owner rules remain unchanged. No calls, avatar, custom stickers or general notification inbox added.

Current state: source IMPLEMENTED, verification IN_PROGRESS. Namespace bug fixed; initial8 native memory tests PASS, subsequent49 regression tests PASS but initial packaging failed on Windows running-JAR lock. Final gate/native/build evidence supersedes these intermediate results when recorded in DEV-07_Checkpoint.md. Graphical browser verification currently unavailable: Computer Use browser inventory reports local browser use disabled in Work settings. Keep UI verification NOT_TESTED rather than inherit previous screenshots.
# DEV-08 current task update

Owner explicitly starts Voice/Video while personally reviewing DEV-07 and confirms ring30s/recovery15s/Busy/first-tab rules. Source IMPLEMENTED for the bounded call increment; native state/signaling/auth/concurrency/credential tests and local V19→V21 upgrade/run recorded in DEV-08_Checkpoint.md. Graphical actual media, physical two-device/two-network, forced TURN relay and hardware-release qualifications remain NOT_TESTED; Docker absent/WSL not installed and browser automation disabled. Full F06/DEV-08 is not COMPLETE. Continue those checks when runtime/browser access is available; do not silently promote native signaling into real-media acceptance or begin unrelated DEV-09 scope.

## DEV-09A active local increment

Owner continues with durable Notification Inbox while DEV08-UI02 local visual revision is preserved; explicit deployment hold remains in force. DEV-07/08 older table PLANNED labels above are historical; source/checkpoints and latest owner media reports supersede them without claiming full graphical acceptance. DEV-09A scope is own inbox/badge, all/unread paging, read/unread, safe targets, atomic source-event dedup, after-commit private invalidation and offline REST recovery. See DEV-09A_Working_Spec.md and DEV-09A_Checkpoint.md. Remaining DEV-09 presence/typing/reminder/retention/security/session decisions and DEV-10 integrated device/UAT/recovery review remain separate. No Git push/Render deployment during this local increment.

DEV-09B local continuation: owner adds confirmed-Sent sound, notification volume/Test sound and opt-in open-page background browser alerts. Four owner clauses mapped in DEV-09B_Notification_Controls.md. No Web Push/provider/session/presence expansion; no deployment.

CHAT-02A owner continuation: Reply, Delete for me/Delete for everyone and one editable/removable reaction per member from six confirmed emoji. Implemented as bounded chat extension; UI02/DEV09A/B preserved and deployment HOLD remains. Current scope/results/identity in CHAT-02A_Working_Spec.md / CHAT-02A_Checkpoint.md. Message Edit remains deferred.

CHAT-02B local appearance/interaction correction (2 Nov2026): owner requests hover→reaction and right-click→Reply/Delete instead of ellipsis. Source/controlled tests and current identity in CHAT-02B_Hover_Context_Menu.md. No backend/schema change or deployment.
# Latest local update — CHAT-03 (2 November 2026)

[Mobile Chat UX/private profiles record](CHAT-03_Mobile_Chat_UX.md): owner-requested compact responsive Chat, actual right-click timer fix, bubble-corner reactions, scroll pagination, Settings hierarchy, private profile photos and observed activity. Local verification recorded separately from physical-device UAT. Deployment HOLD and DRIVE_SYNC_PENDING continue.

## CHAT-05 local continuation — 2 November 2026

Owner adds mobile long-press actions, fixed laptop partner card, read-to-notification sync, unified emoji/sticker composer, private inline image/document/video/voice and cancellable sending/recording. Source implemented locally in V27–V30 and Chat media modules. See CHAT-05_Local_Media_And_Mobile_UX.md for all seven clause mappings, exact source/build identity, tests and remaining device checks. Local preview runs on 18083. Latest explicit owner deployment HOLD overrides prior release requests: no push/deploy. Human mobile/playback acceptance pending; DRIVE_SYNC_PENDING.

## CHAT-06 sending/media polish — 2 November 2026

Latest owner review → six UI clauses in CHAT-06_Sending_State_And_Compact_Media.md: immediate media bubble, small Sending spinner, shared five-minute budget/Failed/Retry/Cancel, free composer, hidden media filename/Download menu, compact padding. Local source and affected tests updated; schema stays V30. Previous CHAT-05 progress-panel/180s/15s behavior is superseded by this owner instruction. Deployment HOLD and human graphical/mobile review remain.

CHAT-05/06 release status: owner explicitly lifted HOLD and approved deployment. App commit2d3294c is LIVE on Render (2 November 2026 08:10:31 Myanmar time), schemaV30. Full native105 and JS64 PASS; post-live readiness/source/security checks PASS. See Release_CHAT06.md. Actual production signed-in/device acceptance remains separate; source package prepared locally with DRIVE_SYNC_PENDING. Prior HOLD notes remain historical.

## CALL-UI05 local increment

Owner requests taller mobile self-preview and ephemeral flying reactions. Square phone preview + six-emoji connected-call picker + selected-session-only STOMP reaction relay implemented; no schema migration. Local build and scoped tests passed; see CALL-UI05_Mobile_Self_View_And_Reactions.md for contract, exact identity, rate/privacy/lifecycle behavior and pending physical UX review. Production remains unchanged until further deployment instruction.

CALL-UI05 subsequently deployed on owner instruction: appcd4376f LIVE, eight health/source/access checks PASS, no migration/configuration change. Full evidence/source package identity in CALL-UI05_Mobile_Self_View_And_Reactions.md; prior local-only status is historical. Actual two-device UX and Drive sync remain separate pending items.

## HOME-05 shared activity card

Owner confirms Messages (no Chatting Time), undeleted messages only and connected Video Call count. Home landscape card now displays shared saved-message total, elapsed connected video-call hours/minutes and connected video-call count, scoped to the couple from existing records. JS73/native Home12/local HTTP6 PASS; no schema migration. Local source only; actual visual review/Drive sync pending. See HOME-05_Shared_Activity_Card.md for exact contract, deletion/duration semantics and source/build identity.
CHAT-07 local work: shared desktop Chat/Home activity; owner-only unlimited-age message/note/caption edit with Edited marker and optimistic version; durable terminal video call timeline (all outcomes, no message-count inflation). Additive V31–V33. [Acceptance, API, schema, evidence and review](CHAT-07_Edit_Activity_And_Call_History.md). Deployment not included; graphical/UAT review and Drive sync pending.

CHAT-07 + HOME-05 deployed by owner authorization: application3e2865e / Render dep-dbkmeuec6qcc73as6q20 LIVE 3 November2026 at11:21:07 Myanmar, NeonV33, post-live10PASS. Source archive verified locally, DRIVE_SYNC_PENDING; owner device UAT pending. See CHAT-07_Edit_Activity_And_Call_History.md.
CALL-UI06 local front/rear camera switch: bounded video-only capture, same sender replacement, mute/off preservation, exclusive-hardware fallback/restore, lifecycle disposal, rear unmirrored preview and44px responsive control. JS90 PASS; physical mobile/two-device UAT and Drive sync pending. No new API/schema/WS or automatic deploy. [Record](CALL-UI06_Mobile_Camera_Switch.md).
CHAT-08 local own-send scroll correction: unchanged-DOM render now honors explicit bottom action and clears New messages; input focus prevents unintended browser scrolling. Incoming reading position retained. JS94 PASS; mobile device UAT/Drive sync pending. [Record](CHAT-08_Own_Send_Scroll.md).
CALL-UI07 local outgoing ringback/terminal tones and remote microphone-muted stage badge: existing volume/mute preferences, terminal dedup/history gating, selected-negotiation media signals, lost-event final-state recovery. JS103 PASS; physical audio/two-device UAT/Drive sync pending. [Record](CALL-UI07_Call_Sounds_And_Peer_Mute.md). No production deployment.
GAME-01 approved Connect 4: private two-member invitation/acceptance,7×6 server board, random then alternating starter, unlimited turn time, win/draw/resign/rematch, idempotent/versioned moves, same-document video call board. V34–V35, GAME unit/native/HTTP plus existing call/migration regressions and JS108 PASS. Local only; physical device UAT and Drive sync pending. [Record](GAME-01_Connect_Four.md).
GAME-02 local You Win/Partner Wins feedback and responsive in-call board: one-time finite celebration, reduced-motion/static historic results, bounded grid and measured width+height fitting with visualViewport/ResizeObserver. JS115 PASS; physical mobile/laptop/video-game UAT and Drive sync pending. No schema/production change. [Record](GAME-02_Win_Feedback_And_Fitted_Call_Layout.md).
ACCOUNT-01 local Change Password + actor-private partner nickname (owner explicitly chose partner label/own-side only). Atomic credential revision + all own session/socket revocation, partner account preserved; V36 personal label projection/versioned clear; Settings UI, current call label refresh. Native46/JS122 PASS; physical device UAT/Drive sync pending; Forgot Password OTP deferred. [Record](ACCOUNT-01_Change_Password_And_Private_Nickname.md).
