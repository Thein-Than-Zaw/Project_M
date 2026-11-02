> Current status (1 November 2026): DEV-08 native call increment and V21 local upgrade are recorded in DEV-08_Checkpoint.md. Latest DEV-08/DEV-07 addenda supersede historical foundation/planning descriptions. Actual browser/media/relay verification remains pending.

# Traceability / current coverage v0.1

Latest engineering increment DB18: owner-approved local runtime17.7/54329→installed18.6/5432 supports §5.11.1/6/7 and D05, with no product/API/UI rule changes. ProvisionLocalDatabase.java adds operator connection/privilege-owner guards; VerifyLocalDatabase.java checks app-only access; local-env/init/stop scripts select ignored configuration and avoid implicit fallback/admin use. Dev V1–V9 restored with exact data/history; fresh installed test DB constructed9, auth5/foundation3/pairing5 PASS. Setup token preserved; old portable stopped with data/backup retained. Current evidence/source: PostgreSQL_18_Checkpoint.md / DB18_source-manifest.json. Earlier17.7/UI observations are historical, not relabeled as new graphical18.6 journeys.

Latest DEV-03 mapping: F02 PAIR-01–03 and AC-F02-01–06 ↔ pairing/PairingService (create/invite/preview/join/current/active/member), PairingController; realtime/PairingSocketConfig/SocketRegistry/PairingNotifications; V7–V9; static/space.html/space.js/pairing.css/pairing-socket.js/home.html/home.js plus auth.js/security login next-route. PairingIntegrationTest5 cases cover native HTTP/STOMP, same-space persistence/confirmation, gate/idempotency/concurrency, expiry during lock wait, foreign/CSRF/origin/destination/SEND denial and logout/disable socket closure. Entire final suite13 PASS includes auth5/foundation3. Browser two-host cookie sessions verify live waiting→home, distinct signup/shared dates, later login and actual server restart persistence. Latest user decision: pairing is permanent setup, presence/login never resets membership; timezone Asia/Yangon. Graphical evidence/coverage: DEV-03_UI_Review.md; actual tested source: DEV-03_source-manifest.json. Unrelated features below remain plan-only; full F03 dashboard/date counter/chat/etc not implemented. No human acceptance/UAT/release claim.

Current DEV-02 identity increment supersedes the historical foundation-only statuses below for AUTH Login/Sign Up: see DEV-02_Working_Spec.md and DEV-02_Checkpoint.md. V4–V6, identity/*, config/SecurityConfig, static/index.html/auth.css/auth.js/space.html/space.js, scripts/IssueSetupToken.java/issue-setup-token.ps1 map to AUTH-01–03 and Login/Sign Up portions of AC-F01-01/02/03/05/07/08. AuthenticationIntegrationTest covers native cookie signup/login/rotation/CSRF/logout/expiry/token/race/disable/revision/rate cases; FoundationIntegrationTest remains DB/access regression. Actual graphical mobile signup→login→invite→logout→partner signup/login/reload evidence is in DEV-02_UI_Review.md. Reset AC-F01-04/06 and reset/socket portions of03, password-change/logout-all remain pending; no full F01 completion claim. Future F02–F10 feature mappings below remain plan-only. Final source identity: DEV-02_source-manifest.json.

Source v1.1 and latest owner DEV-01 instruction. This is a working map, not human acceptance/full RTM approval. Full feature clauses remain in original requirements; entries below do not collapse them into feature counts.

| Source clause | Actual files/symbols / DB / contract | Evidence / current limit |
|---|---|---|
| §5.11.7 DEV-01; §5.11.1 technology | pom.xml, mvnw/mvnw.cmd, .mvn; PrivateSpaceApplication.main; application.yml; scripts/* | Actual Maven verify/build/boot; exact scope in checkpoint; no F01–F10 completion |
| D01 / AUTH-01 / AC-F01-08 (DB portion) | V1 app_user.account_slot unique CHECK1/2; V2 singleton deployment_slot; V3 member_slot PK/CHECK/user unique | FoundationIntegrationTest.migrationsAndNativeConstraints / concurrentAccountSlotCannotBeClaimedTwice; no registration HTTP/service flow yet |
| D02 / AC-F01-07 (DB portion) | V1 app_user.signup_relationship_date NOT NULL; V2 shared relationship_date; API-AUTH-002 draft | Native missing-date rejection; form/error/service behavior NOT IMPLEMENTED |
| PAIR-01–03 (DB portion) | V3 membership FK/PK/unique; API-PAIR drafts | Native missing-user/member-slot checks; invite/join/membership authorization/races NOT IMPLEMENTED |
| PRIV-03 / D04; §5.11 operational health | SecurityConfig.securityFilterChain/noFoundationAccounts, application.yml; foundation static page | healthAndPrivateAccessBoundary: health ready/no details, headers, guessed private HTTP/asset/WS denied, CSRF403. Real login/member policies and WS frames NOT IMPLEMENTED |
| §5.11 UTF8/date/time direction | application.yml UTC; scripts/init-local.ps1 UTF8; DATE/TIMESTAMPTZ migrations | Unicode roundtrip in test; business couple timezone/counter rules NOT IMPLEMENTED |
| D03 / MEM-03 / AC-F07-05 | Database_Schema_Draft memory authorship/membership policy; API-MEM-006/007 draft | PLAN ONLY: both-member memory edits/deletes; no upload/memory code |
| D05 | portable tools/local-only bind/no cloud; scripts and README | PostgreSQL17.7 and application on loopback verified; hosting deferred |
| D06 / NOTIFY / CALL | API_WS_Contract_Draft integration/event designs | PLAN ONLY: In-App only; no push integration |
| AUTH-02/03/F10 remainder | API-AUTH drafts/Open_Business_Rules | NOT IMPLEMENTED: signup/login/reset/revocation/WS/session/device behavior |
| HOME/DATE/F03/F08 | implementation plan DEV-04 / schema/API drafts | NOT IMPLEMENTED; Q02/Q06 |
| CHAT/GIFT/F04/F05 | plan DEV-05/06 / schema/API drafts | NOT IMPLEMENTED; commit/ACK/idempotency/receipt/browser cases pending |
| CALL/F06 | plan DEV-08 / API signaling draft | NOT IMPLEMENTED; real WebRTC/TURN/media tests pending |
| MEM/F07 remainder | plan DEV-07 / drafts | NOT IMPLEMENTED; Q03/Q05 |
| NOTIFY/F09/F10 remainder | plan DEV-09 / drafts | NOT IMPLEMENTED; retention/presence/settings/session choices pending |

No source requirement or approval status was edited. Same-model code/design review is AI self-review only. Native DB constraints support later ACs without claiming complete business workflows. No live Figma layout audit/browser/device acceptance performed.

## DEV-04 current code/evidence mapping (1 November2026)

HOME-01–03 / F03 scoped name/profile/Home: home/HomeService.home/editProfile, HomeController /api/home and PATCH /api/profile; static home.html#profile + home.js; existing app_user.version. AC-F03-01(name)/02/04(self)/05 map to HomeIntegrationTest ownUnicodeProfilePersistsAndCannotTargetPartner/sharedStartDateRequiresConfirmationAndRefreshesSameCalendar and DEV-04 browser evidence. Avatar/assets and memory create CTA remain DEV-07; full F03 not complete.

DATE-01–03 / CUR-DATE-001 / owner both-member delete decision: DatePolicy.calendar/occurrence, HomeService.dates/editRelationship/create/edit/delete, HomeController /api/dates and /api/relationship-date; V10/V11; home.html#dates. AC-F08-01/03/04/06 map to DatePolicyTest + HomeIntegrationTest shared date, member CRUD, idempotent/replay/concurrency and scope/history cases. AC-F08-02 backend clock tested, client timer/visibility/poll implemented, actual browser midnight not observed. AC-F08-05 reminder job NOT IMPLEMENTED; pending scope answer. PairingNotifications.homeChanged state-only AFTER_COMMIT uses old private queue; HomeIntegrationTest.profileInvalidationIsStateOnlyAfterCommitAndPartnerCatchesUp + existing PairingIntegrationTest subscription/revocation regression.

Current source/build/native/graphical identities, coverage13 split clauses (9 scoped/4 outside), recovery and separate readiness states: DEV-04_Checkpoint.md / DEV-04_Working_Spec.md / evidence/DEV-04_source-manifest.json. Native22 PASS; browser7 scoped UI AC flows observed, midnight not observed; full DEV-04 PARTIAL, human UI/UAT undecided, Drive sync pending, release not ready. Older draft/unimplemented rows above are historical, not this current increment's effective status.

## DEV-05 current mapping (1 November2026)

F04 CHAT-01–03 / AC-F04-01–06 → chat/ChatService, ChatController, ChatLimiter, ChatNotifications; realtime/SocketRegistry.validChat and PairingSocketConfig; SecurityConfig; chat.html/css/js/chat-socket.js and Home links/unread. V12 conversation allocation / V13 immutable messages and per-message receipts. Exact send/ACK/queue/history/receipt contracts in API_WS_Contract_Draft.md. HomeService.Home adds unreadMessages; state-only private invalidations, no general inbox/presence claim.

ChatIntegrationTest8 maps to commit/history/3x retry, concurrency, actual DB failure no Sent, Unicode4000 transport/blank validation, recipient-only per-message receipts, cursor/old receipt recovery, spoof/raw/foreign/revoked scope. Final full30 PASS across authentication/Chat/date/foundation/Home/pairing on actual18.6. Fresh V1–V13 and actual dev11→13 migration verified; old-table fingerprints match, real2 accounts/pairing preserved. Final JAR/runtime/source identity and evidence: DEV-05_Checkpoint.md / evidence/DEV-05_source-manifest.json.

Source denominator6 parent AC /9 split clauses:6 UI-required,3 backend-only. GUI5 PASS/1 UNVERIFIED (actual hidden-tab prerequisite, DEV05-UI01); backend/native authority tests PASS. Full task verification PARTIAL, human UI/UAT undecided; Drive sync pending, release not ready. Existing rows above retain historical scope, not current implementation status. DEV-09 full notification inbox/presence remains separate; D06 in-app only preserved. DEV-06 addition below supersedes its prior planned state.

## DEV-06 — owner's character sticker continuation, 1 November2026

Authority: v1.1 F05 GIFT-01–03 / AC-F05-01–05 plus explicit owner request for cute character packs and available stickers. Original unsupported Sticker tab rule superseded only for bundled reviewed packs; uploads/marketplace/payment remain outside scope. Six original proposed gifts not promoted to an owner-confirmed catalogue. Actual increment provides363 character sticker gifts across2 licensed packs. Working scope/contract/AC classification: DEV-06_Working_Spec.md; physical V14/V15 and API draft additions.

| Source | Implementation / evidence | Current status |
|---|---|---|
| AC-F05-01 | TEXT composer/picker unchanged; actual Myanmar+emoji and mixed GIFT/TEXT reload GUI; ChatIntegrationTest Unicode regression | PASS described native/GUI |
| AC-F05-02 / GIFT-02 | ChatService.send maps selected catalogue snapshot, server principal recipient; two-account Bunny Heart same note GUI, reverse Fox/no note; inactive/renamed snapshot replay native | PASS described native/GUI |
| AC-F05-03 | Conversation lock + shared sender/client key/type/code/note semantics; concurrent/3 replay native, rejection→reload→Retry GUI and1 retry row | PASS described native/GUI |
| AC-F05-04 | Preview/Cancel/read-only GETs; final Cancel restores focus,0 messages before first Send; cancelled rows0 | PASS described native/GUI |
| AC-F05-05 / GIFT-01/03 | Active catalogue, deny recipient input, membership-scoped API/history/assets, existing own WS queue only | PASS native negatives; full35 regression |
| F05 fields/screens | Pack/search/paged60 catalogue, optional500-code-point note, dialog confirmation, Sent/Received50, empty/error/retry/loading, fallback text, image toggle;390/1440 CSS journeys | PASS described GUI; not every asset/browser/device certified |
| F05 notification | Recipient-only AFTER_COMMIT event text; duplicate-key creates no repeated event native; actual partner status GUI | PASS scoped channel; DEV-09 inbox deferred |

Declared five parent AC / nine working coverage rows:6 UI_REQUIRED +3 BACKEND_ONLY; all represented in production source, described core journeys verified. Source-reference rows group supporting UI states; no whole-product percentage or all-environments claim. Same-model self-review only; human acceptance/UAT NOT RUN. DEV05-UI01 true background/window focus remains UNVERIFIED despite modal-obscured chat delivery≠read being verified. Code core implemented, scoped native/GUI checks verified, overall product/release NOT READY; local records saved/Drive sync pending.

CHAT-UI-01 (1 Nov2026 owner visual request): F04 conversation now fills available viewport, removed top decorative copy/right card, Sign out moved into partner header.3 UI resources changed, no contract/business rule change. Final desktop/mobile geometry, text/sticker send and refresh checks PASS on recorded build; prior native test identities retained separately. See CHAT-UI-01_Checkpoint.md and source manifest/screenshots. Human acceptance and Drive sync pending; DEV05-UI01 unchanged.

DEV-07 planning continuation (1 Nov2026): owner confirms video maximum180s/100MB and extends private memories to PDF/DOCX/XLSX/PPTX documents maximum20MB per file. Owner subsequently confirms3-day Trash/Restore then permanent deletion; Empty Trash permanently removes the confirmed Trash batch immediately, including attached files/thumbnail content. F07 MEM-01–03 / AC-F07-01–08 affected; permission/privacy D03/D04 unchanged. DEV-07_Working_Spec.md records authority, cross-layer impacts, planned document/boundary/security and Trash/restore/expiry/concurrency/file-cleanup checks. Proposed120s video and30d memory purge defaults superseded; photo/count/quota/codec/backup rules remain separate. Documentation only; implementation, migrations and upload/Trash/GUI/UAT tests NOT RUN. Existing Chat/Sticker/layout evidence remains historical and unaffected.
# DEV-07 current traceability addendum — 1 November 2026

This addendum supersedes older PLAN ONLY/no-memory-code rows for current DEV-07 source. Original source IDs retained; no claim of human acceptance.

| Source | Implementation | Verification responsibility |
|---|---|---|
| MEM-01 / AC-F07-01/02 | MemoryFiles actual validation/private files, MemoryUploadGate early request cap, MemoryController upload/download; V17 | Real8-format upload/download/range persistence, video duration/100MB, malformed/spoofed/macros/XML, gate concurrency tests; graphical persistence/playback pending. |
| MEM-02 / AC-F07-04 | MemoryService.registerUpload/create + V18 receipts/rollback compensation | Failed-save unpublished/private state, same-key retry/concurrent dedup, partner notification only on committed new create. |
| MEM-03/D03 / AC-F07-03/05/08 | Membership-scoped service reads/writes, V16 audit/version, V17 composite ownership | Partner edit/delete/audit, unpublished draft isolation, revoked-member/anonymous denial, stale edit/delete409; GUI conflict flow pending. |
| MEM-02/03 / AC-F07-06 | V19 + MemoryService.favorite/list; memories.js filters/paging | Per-user favorites, type filters, 24+1 keyset pagination; graphical filters pending. |
| MEM-02/03 / AC-F07-07 + owner Trash rule | MemoryPurger/Maintenance, V16 TRASHED/PURGING; restore/confirmed Empty Trash dialogs | 72h cutoff, hide/download denial, restore byte preservation, batch-change conflict, physical deletion, durable storage-failure retry/orphan cleanup; actual process-restart purge drill pending. |
| F03 HOME-02 recent memories | home.js real newest3, home/chat mobile/desktop Memories links | Home integration API regression; graphical navigation pending. |

Actual command/result/build identities are recorded in DEV-07_Checkpoint.md and evidence; coverage mapping is not test success.
# DEV-08 source traceability

CALL-01–03 / AC-F06-01–06 → call/CallService, CallController, CallEvents, CallMaintenance, RtcConfiguration; V20/V21; realtime SocketRegistry revoked-session events and exact calls SEND/SUBSCRIBE/outbound guards; static call-widget.js/call-socket.js/calls.css and Home/Chat/Memories entries. Native CallIntegrationTest covers actual cookie/STOMP state, offline/Busy, durable retry/rollback, competing invite/accept, selected-session privacy including disconnected-target routing, wrong roles/IDs/negotiation/size, deadlines/recovery, logout/leave and resumed socket. RtcConfigurationTest covers expiring credentials/secret non-disclosure and invalid configuration. Original source AC and working owner additions mapped in DEV-08_UI_Coverage.md; actual media assertions remain NOT_TESTED, not replaced by synthetic SDP. Exact build/native/evidence identities in DEV-08_Checkpoint.md; qualified human/UAT/release approval unrecorded.

## HOST-01 accepted hosting engineering scope

DEV08-FIX02 / CALL-03: RtcConfiguration managed username/password alternative, application.yml and optional secret export -> RtcConfigurationTest4 + CallIntegrationTest10 PASS; real authenticated TCP TURN allocation and forced-relay software data-channel roundtrip PASS; exact source086fa2d live on Render, public HTTPS10 and anonymous RTC401 checked. API refresh deadline vs provider credential lifetime documented. Real cross-network phone audio/video remains pending; see DEV08-FIX02_Managed_TURN.md.
MEM-01/02 private bytes and download/range -> RemoteMemoryStore/S3MemoryStore + MemoryFiles/MemoryService/MemoryController -> S3MemoryStoreTest + live StorageProbe. MEM-03/owner Trash lifecycle -> remote delete/reconciliation + existing durable purger -> native MemoryIntegrationTest and retry/orphan contract tests. Existing private access -> RenderEnvironment/render profile exact origin + secure cookie settings -> RenderEnvironmentTest and local HTTP smoke. Container/public HTTPS/WSS and authenticated remote media remain UNVERIFIED. No new business requirement or migration. See HOST-01_Checkpoint.md and Neon_Agent_Setup_Audit.md.

DEV08-UI01 / owner full-page call and mute-all notification request: call-widget.js/calls.css + notification-sounds.js; ChatNotifications recipient marker + Home/Chat/Memories consumers. Coverage, browser limitations and exact verification in DEV08-UI01_Full_Page_Calls.md. Existing TURN/provider/call ownership unchanged.

DEV-09A / NOTIFY-01–03, AC-F09-01–03 and expiry portion AC-F09-05 → NotificationService/Controller/Events + V22; notification-inbox.js/notifications.css and Home/Chat/Memories refresh hooks. Scope/AC, native integration tests, UI limits and identity in DEV-09A_Working_Spec.md / DEV-09A_Checkpoint.md. Presence AC-F09-04 and remaining reminder/session requirements are not claimed by this inbox increment. DEV08-UI02 call/settings changes retained locally; no deployment authorized.

DEV-09B owner sent-chime/volume/browser-alert request → notification-sounds.js + browser-notifications.js + Home Settings / Chat confirmation / Inbox delta / Call incoming hooks; coverage, exact local identity and controlled vs graphical evidence in DEV-09B_Notification_Controls.md. No schema change; deployment HOLD remains.

CHAT-02A owner Reply/Delete/React request + editable reaction clarification → ChatService/Controller, V23/V24/V25, Home unread and Notification target guards, chat.html/js/css message actions/reply/reaction UI and call dialog integration. Native ChatActionsIntegrationTest10 and adjacent checks are mapped in CHAT-02A_Checkpoint.md; graphical acceptance remains separate. Earlier DEV05 edit/delete deferral is superseded for Delete only by this explicit request.

CHAT-02B owner interaction correction → chat.js bindMessageInteractions/showHoverReactions/openMessageActions + chat.html/css; two owner UI clauses and five controlled gesture tests mapped in CHAT-02B_Hover_Context_Menu.md. Native server evidence remains separate; graphical review pending; deployment HOLD.
# CHAT-03 trace update (2 November 2026)

F03/Home/Profile and F04/Chat → owner-requested C01–C21 clauses → [CHAT-03 source coverage/evidence](CHAT-03_Mobile_Chat_UX.md) → `chat.*`, `home.*`, `profile-ui.js`, `ProfileMediaController`, `PairingService`, V26 → HomeIntegrationTest, Chat/Pairing regressions, JavaScript UX tests and synthetic browser journeys. Actual-device touch/keyboard and human acceptance remain separate, pending gates. No deployment.
# CHAT-04 notification/layout correction

Owner's 2 November UI/deploy request → C1–C3 in [CHAT-04 record](CHAT-04_Notification_Layout_Fix.md) → notification-inbox.js, chat.css → notification-inbox-ui.test.mjs + existing NotificationIntegrationTest. Browser visual checks unavailable; source/API tests and deployment checks are distinct.

## CHAT-05 media/mobile continuation

Owner's latest seven clauses C1–C7 → [CHAT-05 local record](CHAT-05_Local_Media_And_Mobile_UX.md) → chat.html/js/css + chat-media.js + ChatService/AttachmentController/AttachmentService + shared Memory file validators/maintenance + V27–V30 → gesture/responsive/sent-sound/media Node tests and ChatMedia/ChatActions/Notification native tests. Notification list read remains distinct; actual Chat read now reads its matching inbox entries. Actual touch/layout/playback acceptance remains pending. Local preview only; deployment HOLD.

CHAT-06 owner media/sending review → C1–C6 in [CHAT-06 record](CHAT-06_Sending_State_And_Compact_Media.md) → chat-media.js, chat.js/html/css and generalized ChatService/ChatAttachmentService cancellation → chat-media.test.mjs, chat-sent-sound.test.mjs, ChatMediaIntegrationTest plus Chat/ChatActions/Gift native regressions. No new schema or online operation; device UX acceptance pending.

CHAT-05/06 deployed application identity2d3294c → Render dep-dbjuim67bikc738jtds0 LIVE / schemaV30 → full native105, JS64 and final HTTPS/source/guest-ACL checks PASS. Release evidence and exact package identity: [Release_CHAT06](Release_CHAT06.md). Owner deployment authorization supersedes earlier local HOLD; physical device UAT and Drive sync remain separately pending.

CALL-UI03 owner laptop call screenshots → C1–C6 in [laptop call layout record](CALL-UI03_Laptop_Call_Layout.md) → call-widget.js/calls.css presentation and Fit/Fill → Node64 and markup/accessibility checks PASS; actual graphical/device review pending because browser use disabled. Local only, no deployment.

CALL-UI03 deployment authorization received → application a6196b3 → Render dep-dbka9v2jnfac73a2dssg LIVE at 21:31 Myanmar time on 2 November 2026. JS64, markup and six live health/source/access checks PASS; schemaV30 unchanged. See CALL-UI03_Laptop_Call_Layout.md for exact source/package identity and remaining physical visual review / DRIVE_SYNC_PENDING.

CALL-UI04 owner live screenshot feedback → C1 full remote frame/default Fit and C2 persistent fullscreen icon → call-widget.js plus call-presentation.test.mjs → Node66 and markup checks PASS. Root causes and remaining native-device visual review: [CALL-UI04 record](CALL-UI04_Uncropped_Video_And_Fullscreen_Icon.md). Local correction; no deployment yet.

CALL-UI04 explicitly deployed → appc90b401 / Render dep-dbkah9kipgvc73e5jagg LIVE at 21:46:39 Myanmar time on 2 November 2026 → Node66 and six live readiness/source/access checks PASS; schemaV30 unchanged. Source package locally verified, DRIVE_SYNC_PENDING and physical visual review pending; [full record](CALL-UI04_Uncropped_Video_And_Fullscreen_Icon.md).
