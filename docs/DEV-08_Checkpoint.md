# DEV-08 — Voice/Video call implementation increment

> Later scoped fix: [DEV08-FIX01_Checkpoint.md](DEV08-FIX01_Checkpoint.md) improves visible/cancellable device setup after the owner's video Answer report. This checkpoint retains its original build/test identity; current runtime uses the fix build. Owner reports same-PC Voice success; real video cause/acceptance remains pending.

1 November2026, Asia/Yangon. Owner starts DEV-08 while personally reviewing DEV-07, and explicitly confirms ring30s→Missed, recovery15s, Busy/one live call and first accepting tab. Root `C:\Projects\Project_M`, branch main, base658251c9c12dcf10092d9f0cabfe6ca4efa34a69; existing dirty DEV-06/07/UI work preserved. No Git reset/new push, real-data fixture writes, public hosting, provider purchase, recording, screen share or closed-site ring introduced.

## Outcome and scope

CALL-01–03 / AC-F06-01–06: call state/signaling and shared browser media code implemented. Home Voice/Video buttons, Chat phone/camera icons, Memories Calls launcher open a responsive call widget with incoming Answer/Decline, outgoing Cancel, Minimize, local/remote media, mic/camera toggles, End, permission/autoplay recovery and recent20 outcomes. Voice requests audio only; Video audio+camera, only after user Call/Answer. User names/status/history use textContent. Timer uses server connectedAt, not acceptance. Actual RTCPeerConnection callbacks/track.enabled/track.stop/PC.close implement media/control cleanup; those are source observations, **not yet real browser/device media evidence**.

CallService/CallController/CallEvents/CallMaintenance/RtcConfiguration, exact private call socket destinations, SocketRegistry revoke events and frontend call-widget.js/call-socket.js/calls.css implement the contract in DEV-08_Working_Spec.md. Initial connection technical timeout30s, selected heartbeat5s, state reconciliation2s. One live call per space; competing invite/accept serialize. Same-page socket resume requires the selected client token and old socket closure. Fresh per-page random client UUID stays private; Views expose SHA256 token hashes. STOMP additionally checks exact selected socket; protected HTTP leave requires that unexposed selected-page capability.

Metadata after commit goes to both accounts' tabs; SDP/ICE/media-state relays go only to the selected opposite **session-addressed destination**. A missing selected socket never falls back to other account tabs. Caller is designated offerer/restart initiator; callee answers. Current negotiation, role, selected client/socket, bounded SDP16k/ICE4k, rate240/min/socket and120s bounded transient dedup apply. SDP/ICE/media never go into DB/history/logs; server only stores authenticated client connection reports, not independently observed RTP. End stops local devices immediately even while signaling is disconnected; server confirmation/reconciliation is separate.

V20 call_session stores same-space participant/state/mode/selected client/socket metadata, authoritative deadlines/audit/version and a partial unique live-call index. V21 call_command_receipt stores actor/event/intent/call FK for invite/accept/decline/cancel/end replay. Both participant membership FKs and native lifecycle constraints apply. Existing migrations were not rewritten; original data is preserved.

## Verification and engineering review

Final native command: load scripts/local-env.ps1, append `?currentSchema=dev07_fresh_20261101` to TEST_DB_URL, run `mvnw.cmd -B -ntp test`. **65 tests PASS,0 failures/errors/skips; BUILD SUCCESS**, ended15:33:52+06:30, elapsed3m02s. Existing53 + CallIntegration10 + RtcConfiguration2. Final evidence `DEV-08_reviewed-tests.txt`; earlier63/64 checkpoints retained separately.

Native calls use actual PostgreSQL18.6, cookies/CSRF and real STOMP sockets with synthetic SDP (no real audio/video). Covers voice lifecycle, both reports before ACTIVE, current/history/relogin, offline/no row, simultaneous invites, exact retry/intent conflict, partner identity spoof ignored, selected-tab-only SDP, other-tab/wrong-client/role/call denial, stale/oversized signals, ring/recovery cutoff and non-extension, native DB-failure rollback/no event then same-key retry, logout and leave, simultaneous accept single winner, old-socket closure then selected resume, and disconnected selected target with another real subscribed account tab receiving no SDP. Subscription helpers wait for their own unique subscription ID, not another tab's registration. Rtc tests cover expiring coturn-compatible credentials, no permanent secret in response, invalid URL/secret rejection and honest unconfigured/direct-only state.

Review repairs/evidence:

- Initial10 passing tests still logged mutable-header warnings after a global ordered-receive flag; removed that unnecessary global override. Existing publisher ordering retained; client gates ICE on SDP ACK. `DEV-08_ordering-warning.txt` retained, later call tests have no Expected-mutable error. No assertion/guard relaxed.
- Found Spring user+missing-session routing fallback during source/security self-review; changed private relay to session-addressed user destination and added disconnected-target regression. Reference: [Spring DefaultUserDestinationResolver](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/simp/user/DefaultUserDestinationResolver.java). Final65 includes that regression. Earlier64 result is historical, not relabeled final.
- Page token exposure/cloned-tab reuse, stale connected reports, offline End being disabled, late media/permission callbacks and inactive launcher obscuring Chat were corrected in source. Node syntax checks call-widget/call-socket PASS; no fake media/screenshot substituted for browser tests. Same-model AI source/usability/security self-review only, no independent technical/human approval.

Final package: `mvnw.cmd -B -ntp -DskipTests package`, **BUILD SUCCESS**15:40:43+06:30 (native65 ran separately); JAR SHA256 **fae2aac3c53943c04b765d76fd12eb767a965efab66581326d1207d808843eea**. Source476 files,14 added +12 modified versus DEV-07; identity **90b3155eda2f58c453d8b0a5cef5f1e6c3534d920ab13fa403ffe5a2732836d5** (`DEV-08_source-manifest.json`). Final extra frontend guard ignores late call views/show requests after pagehide; Node syntax passed, graphical behavior remains unverified. Checked packaged call widget/socket/CSS, all3 page HTML and V20/V21 resource hashes match source. Preserve failed/intermediate evidence rather than overwrite it as PASS.

## Development data and recovery

Before upgrade the project app was stopped, then a private DB+asset snapshot saved to ignored `.local/backups/dev08-before-migrations/`. Database dump SHA256 **7e87f93dba2ebdb6c19ea1e88718dcf247c0edcbc51078074b4faf16dae89503**;5 private files /13,668,314bytes copied. Initial pg_dump file-argument syntax failed before dumping; corrected the argument and completed the snapshot. No private files/credentials/dump are source-package artifacts. Backup restore is not rehearsed and this is not a production retention/RPO/RTO claim.

Actual installed dev V19→V21 startup15:28 applied2 additive migrations. **All18 predecessor table aggregate projections unchanged**, including Flyway history<=19. At snapshot:2 accounts/2 memberships/9 chat messages,3 memories/3 assets/6 upload-create receipts/1 favorite and363 stickers. Thus real owner-created Memories were preserved, not reset or replaced with fixtures. Evidence DEV-08_before-upgrade.txt, after-upgrade.txt, upgrade-comparison.txt, backup-summary.txt, initial-start.txt. Subsequent user writes must not be mistaken for migration effects.

Recovery uses forward fixes, preserving V20/V21, existing user content and private upload files. Do not restore the pre-upgrade dump over later user writes or delete migration history. Existing in-memory sessions require login after restart; pairing persists. Persisted call rows survive app restart, but absent socket/media state must recover or fail within the bounded deadline; an actual process-crash/live-media restart drill remains NOT_TESTED.

## Local run / human review

Application: http://127.0.0.1:8080/ . Existing accounts sign in normally; no registration or pairing again. From root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

Open two browser/account sessions. Home Voice/Video or Chat icons → allow devices → partner Answer. Check actual two-way audio/video, mute/camera effect at the peer, Minimize/Chat interaction, End hardware release, Decline/Cancel/Missed/Busy, two answering tabs and reconnect. UI/layout and detailed test setup: DEV-08_Local_Calls.md / DEV-08_UI_Coverage.md. Owner-confirmed rules are not reopened.

လူဖြင့်စမ်းရန်: မူလ Account နှစ်ခုကို browser နှစ်ခုမှာ Login ဝင်ပါ။ Home သို့မဟုတ် Chat က Voice/Video ကိုနှိပ်ပြီး mic/camera ခွင့်ပြုပါ။ Partner မှာ Answer နှိပ်ကာ တကယ့်အသံ/ရုပ်ကို စစ်ပါ။ Mic/Camera ပိတ်ချိန် တစ်ဖက်မှာပါ ပြောင်းလဲရမယ်။ End နှိပ်ရင် device သုံးနေမှု ရပ်ရမယ်။ စာမျက်နှာထွက်ရင် call ပြီးဆုံးမယ်; Minimize လုပ်ရင် အဲဒီ page မှာ ဆက်ရှိမယ်။

## Remaining gates / status

Browser tool explicitly returns `Local browser use is disabled in Work settings`. Real graphical journeys, bidirectional RTP, actual hardware toggles/release, physical mobile/autoplay, two-device/two-network and forced-relay tests remain **NOT_TESTED**. Native synthetic SDP/credential tests are not media success. PDF reference pages36/46 inspected; no new app screenshot fabricated. Full F06/DEV-08 and UI implementation review readiness are **NOT_READY / PARTIAL**, not COMPLETE. User is separately reviewing DEV-07; its own UI and previous DEV05-UI01 remain historical pending gates.

Docker CLI absent, WSL reports not installed; no Windows feature/reboot/provider purchase performed. Optional `scripts/start-turn-local.ps1` + loopback-only `ops/turn/compose.yml` prepared for coturn/coturn4.18.0-r0; **image pull/container/forced relay NOT RUN**. Local direct config uses empty ICE servers and clearly discloses limitations; configured TURN generates1-hour credentials from server-only secret. See setup document for constraints and actual relay-candidate/RTP evidence required. No production HTTPS/listen/origin/firewall setup asserted.

Local code/update/contracts/traceability and evidence SAVED; Drive destination unknown, **DRIVE_SYNC_PENDING**, Developer Return not COMPLETE. Ordinary incremental update source package NOT_DUE; no new release/handoff or arbitrary external upload invented. Human UI/business UAT, CI/performance/load and release acceptance are unrecorded.

## Final runtime / teardown — 1 November2026

Final JAR runs on8080, Java31208/unified session11419, started15:41:56+06:30; Flyway21 already up to date. Readiness `{"status":"UP"}`. Anonymous current-call and RTC-config endpoints both401. Live call-widget.js SHA2565469aca1f437ebb856c05e633458e9187c605ba0cc44542f9df3ddd9653204c0 matches current source; evidence DEV-08_final-start.txt, final-package.txt, readiness.json and runtime-identity.txt. This is a runnable implementation observation, not media success.

DEV-08_local-state.txt records dev2 users/2 members/9 messages/3 memories/3 assets/1 favorite/363 catalog/0 calls/0 call receipts. Isolated test schema21 has0 users/members/messages/memories/assets/favorites/calls/receipts,363 catalog and0 temporary fault constraints. Private5-file backup hashes match the original upload directory byte-for-byte (`DEV-08_asset-backup-check.txt`). No synthetic call/message/memory was written into the real users' space. Current ordinary Git diff whitespace and call JS syntax checks PASS. No temporary graphical review server was started, and no Drive upload/new Git push occurred.
