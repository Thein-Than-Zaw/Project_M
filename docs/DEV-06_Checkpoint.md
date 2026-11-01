# DEV-06 Code Update Record / local return

1 November2026, Asia/Yangon (UTC+06:30), checkpoint after final migration rehearsal11:47:45. Existing `C:\Projects\Project_M`, Java/Spring Boot project; same-model AI implementation/self-review only. Base main `658251c9c12dcf10092d9f0cabfe6ca4efa34a69`; current edits uncommitted. No new push/merge, production deployment or human acceptance asserted.

## Authority / before and after

Requirements v1.1 F05 GIFT-01–03 / AC-F05-01–05; user's DEV-06 authorization, then explicit preferences for cute character sticker packs and all available stickers. This owner continuation permits bundled licensed stickers where original draft disabled unsupported sticker tabs. It does not approve custom upload/marketplace/payment or silently confirm the original six proposed gifts. First pack collection includes **all363 original PNG/APNG assets from Blobfox328 and BunHD35**,20 animated. Further reviewed packs can be added through forward catalogue migrations; no claim of every internet sticker or automatic third-party imports.

Before: text/emoji-only durable chat, Gifts disabled, V1–V13. After: Home/Chat sticker entry, paged searchable catalogue/pack filter, explicit preview/optional plain-text500-code-point note/Confirm/Cancel, selected gift snapshots, mixed TEXT/GIFT history, Sent/Received filters, recipient in-app notice, retained gift retry/outbox metadata, text fallback and image toggle. Start date/Asia-Yangon/authentication/persistent pairing and both-member date rules preserved. No account reset, private media upload, message Edit/Delete or general notification inbox.

## Changed source and contracts

- ChatService: typed Send/Message, gifts/giftHistory, active catalogue snapshot under shared lock, exact type/code/body dedup under existing conversation lock. Original TEXT clients remain supported. Recipient is always server-derived; supplied recipientId rejects. Replay returns original snapshot even after catalogue changes/deactivation. Existing counter/receipts/unread/catch-up apply.
- ChatController: GET `/api/gifts`, GET `/api/gifts/history`; existing `/app/chat.send` gains GIFT payload. No second write path or new broker destination. ChatNotifications emits partner-only gift notice only after commit; retries create no new created event.
- SecurityConfig: ACTIVE membership for catalogue/history and bundled asset/notices. Existing same-origin session/CSRF/revision/subscription boundary retained.
- V14__create_gift_catalog.sql: code/pack/name/local-path/active catalogue + index and363 reference rows, provenance/assets/licenses bundled. V15__add_gift_message_payload.sql: default TEXT/type and gift snapshot columns, typed payload CHECK, gift history index; original unique/FK/order/receipt rules preserved. V1–V13 unchanged.
- chat.html/css/js and home.html: sticker navigation/dialog, Unicode validation, safe DOM text, snapshot bubble, same-intent pending persistence/Retry, keyboard Cancel focus, mobile checkbox correction. Read ACKs also suppressed while dialog obscures chat. No new application dependency.
- GiftIntegrationTest5 cases; Foundation migration count updated13→15 with all original native constraints retained. GiftMigrationTest1 native predecessor rehearsal restores connection schema and removes only its validated unique test-owned schema.

Full contracts/design/classification: DEV-06_Working_Spec.md, API_WS_Contract_Draft.md, Database_Schema_Draft.md. Asset provenance: DEV-06_asset-manifest.json; build/source identity: DEV-06_source-manifest.json.

## Exact build and executed verification

Final running JAR SHA256 **aa189ad6092aff195968ce3d9dc01412725fe5c7c1f1b88c80bfebcaa5a58789**, packaged11:34:31+06:30. Native35-test build was **b9e94c0e2610b71e352d1917dafacd821280fa7f7c61e640908ac956c56860a9**,11:30:15. Subsequent app changes were CSS checkbox dimensions and JS Cancel focus only; Java/services/migrations/native35 test sources unchanged. Those UI fixes were packaged and graphically retested against final JAR. Later added migration-test source does not alter packaged application. Final source manifest443 files;469 target/classes files compared byte hashes against final running JAR,0 differences (DEV-06_runtime-jar-match.txt). HTTP8080 readiness status UP (DEV-06_readiness.json). Final node --check and scoped diff whitespace checks passed. Do not claim a single36-case full-suite run or that tests were rerun after every documentation edit.

Commands from project root, with ignored local credentials loaded by scripts/local-env.ps1:

```powershell
# Normal local startup / full native suite for an empty isolated _test schema
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1
# Exact completed DEV-06 regression used a precreated isolated empty schema:
. scripts/local-env.ps1
$env:TEST_DB_URL += '?currentSchema=dev06_final_20261101'
.\mvnw.cmd -B -ntp verify
# Additional final predecessor TEXT migration rehearsal, same base test schema:
.\mvnw.cmd -B -ntp '-Dtest=GiftMigrationTest' test
```

Observed PostgreSQL18.6/5432, Oracle Java27.0.1.0.3 targeting release21, Maven3.9.12, Boot4.1.2; no version/dependency upgrades. Auth5 + Chat8 + Date2 + Foundation3 + Gifts5 + Home7 + Pairing5 = **35 PASS,0 failures/errors/skips**. Native transport/permission/rollback/concurrency tests use real cookies/STOMP/PostgreSQL. Final regression: DEV-06_final-tests.txt and DEV-06_gift-tests.txt. Initial35 run had2 test failures (stale migration-count expectation13, nonexistent BunHD love search); corrected expectations to15/actual Heart, retained original failure logs. No application guard weakened.

Separate final **GiftMigrationTest1 PASS** at11:47:45: standard Flyway creates isolated random schema to target13, inserts real synthetic TEXT with Myanmar/emoji and delivered/read timestamps, migrates14→15, compares exact original message projection, verifies TEXT/null gift defaults and363 catalogue rows, tears down only its test-owned schema. Evidence DEV-06_migration-rehearsal.txt / DEV-06_migration-test.txt. Prior first rehearsal evidence preserved before explicit pooled-connection schema-restore refinement.

Browser final journeys: DEV-06_browser-journeys.txt; desktop1440x960 and mobile390x844 screenshots. Actual two-account local UI/database flow verified: catalogue60→120, pack/search/empty, preview501 validation/Cancel/focus, same selected Bunny+Myanmar/ZWJ/XSS-as-text note, reverse Fox/empty note, mixed history after reload, Sent/Received/empty filters, after-commit partner notice, database-failure→retained failed sticker across reload→Retry1 row, actual missing-image label fallback, image toggle. Final5 UI rows/5 keys/4 gifts/1 text; cancelledPreviewRows0,retryRows1. Native bad-code/recipient spoof/unpaired reads/concurrent replay and original auth/WS revocation regression PASS. No every-image/device/browser/load certification.

Source denominator: five parent AC / nine working rows,6 UI_REQUIRED +3 BACKEND_ONLY. All represented; described core graphical/native journeys PASS. Supporting states are grouped under the explicit field/screen row, not a whole-product completeness percentage. Distinct AI self-review found/fixed checkbox layout and Cancel focus. Human review/UI acceptance/UAT NOT RUN. DEV05-UI01 actual hidden/unfocused Chrome/Edge qualification remains **UNVERIFIED**. Modal-obscured delivery1/read0 before closing/viewing, followed by read1, is separately verified and must not be substituted for that prerequisite.

## Real development data / teardown / recovery

Private pre-upgrade dump `.local/backups/dev06-before-migrations.dump`,34651bytes, SHA2560318ac7deace2d0ffff9eb5a82fb3a7230f5c5ae4e5586c0c89cb819c02d685f. Backup/credentials/setup code remain ignored and are not source artifacts. Actual dev13→15 boot applied2 forward migrations; all13 original table count/content projections unchanged (chat compared original columns, Flyway≤13). Actual real dev messages were0; nonempty predecessor text is covered by the separate synthetic rehearsal, not invented real data. Evidence DEV-06_before-upgrade.txt, after-upgrade.txt, upgrade-comparison.txt and application-start.txt.

Teardown verified: real dev2 accounts/2 memberships/0 messages, catalogue363/migrations15; separate public_test0 accounts/members/messages, catalogue363/migrations15. Temporary fault constraint0; original catalogue asset restored. UI review server stopped, temporary review tabs closed except stale browser-error tab whose lifecycle action was denied (temporary tab auto-cleanup owns it). Development app remains on8080 using final JAR. Native empty fresh schemas retained as test evidence; unique migration rehearsal schema removed. No real-user message was sent by this work.

Recovery: preserve GIFT snapshots and referenced asset files; use forward fixes. Older DEV-05 cannot render new gifts and its old checksum expectations exclude14/15. Dropping tables/columns or restoring pre-upgrade dump would lose new writes; restore/reconciliation/RPO/RTO not rehearsed. Normal restart requires login with existing accounts; no re-registration/re-pairing.

## Status and Myanmar human review pack

Code core IMPLEMENTED; native35 regression +1 separate migration rehearsal PASS; fresh/actual forward upgrade VERIFIED; described final graphical flows PASS. Full product/release NOT READY (remaining modules, DEV05-UI01 and human acceptance). Local Code Update Record/evidence SAVED; Drive destination unknown, **DRIVE_SYNC_PENDING**, Developer Return not COMPLETE. Routine source package NOT_DUE (ordinary incremental local update, not release/handoff). No invented Drive destination/upload or human signature.

လူဖြင့်စစ်ရန်: http://127.0.0.1:8080/ ကို Refresh လုပ်ပြီး မူလ Account နှစ်ခုနဲ့ Login ဝင်ပါ။ Home → Stickers & gifts သို့မဟုတ် Chat ရဲ့ ✧ ကို နှိပ်ပါ။ Pack/Search နဲ့ ရွေး၊ Preview မှာ note ထည့်၊ Send လုပ်ပါ။ Partner မှာ ရွေးထားတဲ့ sticker/note တူညီရမယ်၊ Refresh နောက်လည်း ရှိရမယ်။ Sent/Received ကို စစ်ပါ။ Cancel လုပ်လျှင် message မတိုးရ။ ဒီအဆင့်မှာ pack နှစ်ခုက363ခု ပါပြီး နောက်ထပ် reviewed/licensed packs တိုးနိုင်သည်။ Business/UI လက်ခံမှုကို မိမိစမ်းသပ်ပြီး ဆုံးဖြတ်ရန် ဖြစ်သည်။

Next: owner review of character collection/flow, then close DEV05-UI01 with actual Chrome/Edge background/window focus; remaining DEV-07 Memories and later modules follow the plan. Keep six original proposed virtual gifts unconfirmed unless owner selects them; do not turn this sticker increment into uploads/marketplace or payments.
