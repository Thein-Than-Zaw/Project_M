# DEV-07 — private Memories implementation checkpoint

1 November2026, Asia/Yangon. Owner resumed implementation after the account handoff pause. Existing repository `C:\Projects\Project_M`, branch main, base658251c9c12dcf10092d9f0cabfe6ca4efa34a69. DEV-06/CHAT-UI-01 and partial DEV-07 local work preserved; no reset, seed into real accounts, new push, production deployment or account change performed. This is a local Code Update Record; human review/UAT and release approval are separate.

## Scope and outcome

F07 MEM-01–03 / AC-F07-01–08, D03/D04 and explicit owner file/Trash decisions. Private memories now have real upload/storage/metadata, shared list/detail/create/edit/delete, per-user favorites, type filters and24-item keyset pagination. New `memories.html/css/js` integrates Home newest3 and desktop/mobile navigation, private original/thumbnail URLs and video controls/document downloads. Editor has progress/checking/ready/error/retry/remove/cancel, retained draft on failed save, optimistic conflict reload, and Trash/Restore/permanent Empty Trash confirmation. Files and metadata are server-authorized; no E2EE or public link claim.

Accepted limits: up to10 mixed files per memory; JPG/JPEG/PNG/WebP10,000,000bytes; MP4 video<=180s/100,000,000bytes; PDF/DOCX/XLSX/PPTX20,000,000bytes. Shared Trash restores within72 elapsed hours; then permanent purge. Confirmed Empty Trash removes its current snapshot batch and attached originals/thumbnails, excludes active memories, rejects a changed batch with409. Primary-file deletion does not assert deletion of historical backup copies. Working form defaults (title120/caption2,000, today-or-past Myanmar date, >=1 asset, per-user favorite/version guards) retain their engineering-default status. Total storage quota/backup-copy disposition/avatar/account lifecycle remain separate.

## Source changes and root-cause review

- New `memory/MemoryFiles`, `MemoryService`, `MemoryController`, `MemoryPurger`, `MemoryMaintenance`, `MemoryNotifications`, `MemoryUploadGate`; affected SecurityConfig/AuthErrorHandler, application/test YAML and frontend socket/Home/chat integration.
- V16 memory; V17 memory_asset; V18 memory_receipt; V19 memory_favorite. Same-space FK/audit, version,72h state/expiry checks, asset size/kind/position constraints and indexes. Binary content stays outside static resources/DB BLOBs. Existing applied migrations were not rewritten.
- Fix resumed failure: Office root namespace validation omitted `/2006/` and rejected normal DOCX/XLSX/PPTX. Exact supported standard namespaces replace the faulty suffix check, retaining safe XML/ZIP validation. Reference: [Microsoft Open XML package example](https://learn.microsoft.com/en-us/office/open-xml/general/how-to-create-a-package). Native valid-format and wrong-namespace/XXE regressions cover both acceptance and rejection; no guard weakened to accept malformed bytes.
- Resource/privacy review: cap two upload requests before CSRF multipart parameter lookup/spooling; ordinary Spring CSRF still validates token. Pre-probe image/MP4 signature checks reject indirect playlists before native media tools can follow file references. Native probing/thumbnail generation has bounded time/output, codec/resolution/duration checks and no network protocol allowance. Office ZIP expansion/XML/executable/macro checks and PDF structure parsing apply. This is not full malware scanning.
- Fixed Home mobile Memories entry and five-link width allocation; memory controls get44px minimum targets; late detail responses are ignored after navigation and private labels are cleared on pagehide. Browser behavior remains unverified, rather than inferred from source or earlier chat screenshots.
- PDFs use PDFBox3.0.5; FFmpeg/ffprobe8.1.2 local setup is pinned in `scripts/setup-media-tools.ps1`, archive SHA256e25b682664025d49034c981afb4bae36238a40f29a3cc1c713ad9a8b5b3528f6. Existing Java27/release21, Boot4.1.2, Maven3.9.12, PostgreSQL18.6 retained. `.env.example`/README document private storage and overrides; `.tools` binaries remain ignored.

## Verification and exact evidence

- Initial pause failure:8 tests,4 failures, preserved in pause summary. After namespace correction:8 PASS (`DEV-07_namespace-fix-tests.txt`). Expanded intermediate regression49 PASS, packaging failed on Windows running-JAR rename lock (`DEV-07_initial-verify.txt`); stopped only known project app and rebuilt.
- Subsequent full native/build run53 PASS (`DEV-07_final-native-verify.txt`), including Memory15 + upload gate2 and existing Auth5/Chat8/Date2/Foundation3/Gift5/migration1/Home7/Pairing5.
- After final signature/CSS hardening, one regression run had52 PASS and1 Chat STOMP `chat.sent` timeout (`DEV-07_regression-stomp-timeout.txt`). Memory15 and gate2 still PASS. Unchanged Chat8 passed a focused recheck (`DEV-07_chat-recheck.txt`). No assertion timeout was enlarged or guard disabled; intermittent cause not established. Final reviewed run/build status is recorded in the final execution section below; retain this failed evidence even after a green rerun.
- Memory native coverage includes actual8 supported formats/private byte identity/thumbnail/range206/relogin; owner/partner draft isolation and revoked membership; partner edit/audit/per-user favorites/stale writes; ten mixed files/reorder and eleven rejection;72h Trash/Restore/expiry and physical purge; Empty Trash snapshot conflict/active preservation; durable PURGING on real filesystem deletion failure followed by retry;24h abandoned/orphan cleanup; failed-save rollback/idempotency/concurrent replay;24+1 pagination; actual180/181s and100MB/+1 video boundaries; macro/spoofed MIME/namespace/XXE/indirect media rejection; committed-only deduplicated partner STOMP notice. Gate tests check anonymous/missing-header pre-parse rejection and two slow concurrent requests with busy third/released retry.
- This is not exhaustive format security, load/performance, exact10MB photo/20MB document boundary, physical-device playback or actual process-restart purge testing. Those are remaining verification items, not silently claimed PASS. Node syntax checks for memories/home/chat passed. No CI execution, independent technical review, qualified human UAT or production certification asserted.

## Protected development upgrade and recovery

Before real upgrade: private dump `.local/backups/dev07-before-migrations.dump`,42711bytes, SHA2564cf4983ae333155eb7ae856e8817b6325d27b4943a2bfd93a747a3aaea63f43a. Do not upload this dump/credentials/setup tokens to source or chat. Restore was not rehearsed.

Actual local V15→V19 boot applied4 forward migrations on installed PostgreSQL5432. Before/after all14 predecessor table aggregate projections match, including Flyway history through15. Real data:2 accounts,2 memberships,3 chat messages,363 catalog entries; new memory tables0. No real-user content was created by tests. Evidence: DEV-07_before-upgrade.txt, after-upgrade.txt, upgrade-comparison.txt, application-start.txt and local-state.txt. Isolated test schema `couple_private_space_test.dev07_fresh_20261101` has19 migrations and0 accounts/members/messages/memories/assets/receipts/favorites after the recorded cleanup; no temporary fault constraints. Latest teardown is rechecked in the final execution section.

Rollback/recovery uses forward fixes while preserving V16–V19 and private files. Do not reset migrations or restore the pre-upgrade dump over subsequent user writes. Earlier JARs lack current migrations/feature contracts and are not a validated rollback. Durable PURGING rows deny access/restore immediately; storage failures retry after restart, but an actual crash/restart purge drill remains pending. Overall quota and production backup RPO/RTO are not established by this local run.

## Run and human review

Normal local start from project root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

URL: http://127.0.0.1:8080/ ; Memories: http://127.0.0.1:8080/memories.html . Existing accounts sign in normally; do not register/pair again. Stop the foreground project app before repackaging a running Windows JAR. Sessions may require login again after restart; pairing persists.

Native command: load `scripts/local-env.ps1`; append `?currentSchema=dev07_fresh_20261101` to TEST_DB_URL; run `mvnw.cmd -B -ntp verify`. The test schema already exists; do not blindly recreate it or substitute the real DB. Local FFmpeg setup: `scripts/setup-media-tools.ps1` (installed7-Zip or explicit tool paths required).

User review: Home/Chat → Memories → Add memory; choose supported photo/video/document files, enter title/date/story, Save and reload. Partner should be able to edit and move it to Trash without changing creator. Favorites are personal. Restore within3 days; Empty Trash asks for an irreversible confirmation and preserves active memories. Check mobile navigation, video playback/seek, document download, Cancel/Retry and stale edit/reload.

## Status and remaining work

Core source IMPLEMENTED; described native verification evidenced above; local upgrade/run observed. Graphical journeys NOT_TESTED: CUA inventory explicitly reported local browser use disabled in Work settings. [UI coverage](DEV-07_UI_Coverage.md) maps eight parent AC and four source/decision rows, separating native from graphical evidence. UI implementation review readiness NOT_READY until actual browser journeys pass; human acceptance/UAT and release remain pending. Prior DEV05-UI01 actual hidden/unfocused-browser Read qualification also remains unverified. General persistent notification inbox is DEV-09; no calls/avatar/custom upload scope added.

Local Code Update Record/contracts/schema/plan/RTM/setup notes saved. Existing authorized Drive destination unknown: DRIVE_SYNC_PENDING; Developer Return not COMPLETE. This is an ordinary local implementation update, not a new release/developer transfer; full source-package milestone NOT_DUE. No arbitrary Drive destination, external publish or new Git push was invented.

## Final execution facts — 1 November2026, 14:41 Myanmar time

- Final `mvnw.cmd -B -ntp verify`: **53 tests PASS,0 failures/errors/skips; BUILD SUCCESS**, ended14:38:48+06:30, elapsed2m48s. Evidence `DEV-07_reviewed-verify.txt`. This includes the new signature/indirect-playlist regression. The earlier Chat timeout remains disclosed above; its focused8-test rerun and this subsequent full53 run passed without changing the Chat test assertions.
- Final JAR SHA256: **fec65c92413e9b3db17aaadc3efb3ae8ab5a416c84ebea8b366acc398b61934d**. Source manifest462 files,19 added +14 modified versus CHAT-UI-01; identity **39402635bb67dc657398a327784e00ef0c038899a987d189bc6e821a0e9631a2**. See `DEV-07_source-manifest.json`. Checked packaged Memories HTML/JS/CSS, Home HTML/CSS and V19 hashes match their current source resources.
- Final JAR is running on loopback8080, Java process27156 (unified session95196), boot14:39:41+06:30 with V19 already up to date. Readiness `{"status":"UP"}`; anonymous `/api/memories`401. Live `memories.js` SHA256bc76689217f19a5217efc593853922396c2897b03f234448e54d4076d69dc97c matches source. Evidence `DEV-07_final-start.txt`, readiness.json and runtime-identity.txt. No temporary UI review server was created because browser automation was disabled.
- Final test-schema teardown:19 migrations; accounts/members/chat/memory/assets/receipts/favorites0; catalog363; temporary failure constraints0 (`DEV-07_final-test-state.txt`). Development preservation evidence remains the14 matching predecessor projections captured at upgrade. Native fresh-schema and V13→V19 rehearsal executed against test-only schemas; no development reset.
- Node syntax and normal Git diff whitespace checks PASS. Same-model source/security/usability self-review only. No graphical evidence, exact photo/document byte-boundary drill, process-restart purge drill, CI/performance/load certification or human UAT is substituted by the native53 result.

လူဖြင့်စမ်းရန်: မူလ Account နဲ့ Login ဝင်ပြီး Memories ကိုဖွင့်ပါ။ ဓာတ်ပုံ၊ ဗီဒီယိုတို၊ document ထည့်ပြီး Save လုပ်ကာ Refresh ပြန်စစ်ပါ။ Partner က Edit/Delete လုပ်နိုင်ရမယ်။ Favorites က တစ်ဦးချင်းစီအတွက်ပါ။ Trash ထဲမှာ၃ရက်အတွင်း Restore လုပ်နိုင်ပြီး Empty Trash အတည်ပြုရင် အပြီးဖျက်မယ်။ Browser automation ပိတ်ထားလို့ UI/mobile/video playback စမ်းသပ်မှုကို pending အဖြစ် ရှင်းရှင်းလင်းလင်း ချန်ထားပါတယ်။ Local app ရနိုင်သော်လည်း DEV-07 အပြည့်အစုံလက်ခံပြီးဟု မဆိုလိုပါ။
