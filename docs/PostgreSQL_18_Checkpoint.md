# Installed PostgreSQL18 cutover / CUR-DB18-001

Recorded1 November2026,02:30 Asia/Rangoon. Owner explicitly authorized proceeding after successful installed PostgreSQL login check. Scope: switch local database engine/connection, preserve existing state, create separate non-admin app role/dev/test databases and verify. No product/API/UI/business rule change. Source: requirements v1.1 §5.11.1/6/7, D05 local development. Review REVIEW / AI self-review only; no human UAT/release signoff invented.

## Myanmar result / review steps

Project ကို installed PostgreSQL18.6 / port5432 နဲ့ ပြောင်းချိတ်ပြီးပါပြီ။ App က postgres admin account မသုံးဘဲ သီးသန့် couple_app user နဲ့ အလုပ်လုပ်ပါတယ်။ လက်ရှိ state၊ setup code နဲ့ Flyway history မပျောက်စေဖို့ backup/restore လုပ်ပြီး source/destination table contents တူညီတာ စစ်ထားပါတယ်။ Portable17.7 server ကို ပိတ်ထားပြီး data directory နဲ့ private backup ကို မဖျက်ထားပါ။

App http://localhost:8080/ မှာ run နေပြီး healthUP ဖြစ်ပါတယ်။ Start/test command အရင်အတိုင်းသုံးနိုင်ပါတယ်။ Test database အသစ်ကို18.6 ပေါ်မှာ V1–V9 အစအဆုံး ဖန်တီးပြီး tests13/13 PASS ဖြစ်ပါတယ်။ မူလ setup code ကိုလည်း ပြန်မထုတ်ဘဲ ဆက်သုံးနိုင်ပါတယ်။ အသေးစိတ် run/provision/recovery: [Installed_PostgreSQL_Setup.md](Installed_PostgreSQL_Setup.md).

## Actual sequence / data preservation

1. Inspected source17.7/54329:0 accounts,0 spaces/memberships,1 registration token,9 successful Flyway rows. Destination18.6/5432 had only default postgres DB and no couple_app role. No existing destination project data/role overwritten.
2. Provisioned couple_app LOGIN with existing locally generated random app credential, NOSUPERUSER/NOCREATEDB/NOCREATEROLE. Created dev/test DBs owned by app role, UTF8/C/C/libc; project DB defaultsUTC. Admin credential supplied process-scoped only, removed afterward, never written into source/local config or used by app. No global server setting/firewall/service configuration changed.
3. Stopped the old app before snapshot. Native pg_dump18.6 dumped source17.7 dev DB in custom format/no-owner/no-ACL; archive listing verified. Target dev DB verified empty before pg_restore18.6 using app role/error-on-failure. No Flyway baseline/checksum history fabricated: actual restored DB history retained naturally.
4. Compared every public table: row counts and ordered row JSON-content fingerprints matched on both servers, including all original IDs/date/timestamps/token hash/history. Setup file's digest still matches one live token on18.6. Source metadata/data preserved, not silently reseeded.
5. Wrote ignored selection .local/database.json installed5432, reusing private app credential file. Startup verifies both selected DBs; no admin credential/implicit portable fallback. Started app on8080, Flyway validated9/up-to-date on restored dev DB.
6. Fresh isolated18.6 test DB started empty: Flyway constructed V1–V9, then full native test suite passed. No fixture/account/space seeded into dev DB.
7. After successful tests, stopped only portable17.7 cluster with explicit workspace data path; installed5432 remained accepting connections and app readiness stayedUP. Native readiness on54329 now no response. Old data/backup retained; no deletion or DROP/reset/clean.

Source content comparison: [DB18_transfer-comparison.txt](evidence/DB18_transfer-comparison.txt). App/current DB evidence: [DB18_active-catalog.txt](evidence/DB18_active-catalog.txt), [connection verification](evidence/DB18_connection-verification.txt), [setup-code preservation](evidence/DB18_setup-code-check.txt), [readiness after old server stopped](evidence/DB18_readiness-after-portable-stop.json). Database defaultsUTC; JDBC startup can select JVM client-session Asia/Yangon as reported by probe. TIMESTAMPTZ/Instant event semantics and explicit couple Asia/Yangon calendar unchanged; no date/instant conversion was performed.

## Changed source / identity / effects

- scripts/ProvisionLocalDatabase.java: configurable process-scoped operator URL/user, existing role privilege/DB owner guards, UTF8/C/UTC project DB provisioning. Portable defaults preserved.
- scripts/VerifyLocalDatabase.java: read-only dev/test connectivity/version/port and non-admin role checks; dedicated _test guard.
- scripts/local-env.ps1: explicit validated ignored mode/port selection, default portable only when no selection exists; app password from ignored credentials.
- scripts/init-local.ps1: installed mode verifies service/database connection and refuses implicit fallback; does not create/start a second portable cluster.
- scripts/stop-local-db.ps1: installed mode leaves Windows service running and explains helper scope; portable path preserved. Installed branch separately executed, service/readiness stillUP.
- .env.example:5432 URLs/placeholders only. Current profile/schema/traceability/run guide updated; old17.7 test/UI checkpoints remain historical.

Migration files/application feature code unchanged. No new business dependency or table/API/UI. No account lifecycle/date-count/upload/call rule silently changed. Encoding/collation retained to avoid sort/normalization drift. Existing README merge conflict/dirty work preserved; no staging/commit/merge performed.

Branch main/base HEAD31f978961dd13f6ee1aea75c5ccc40555e638903; final source dirty/uncommitted. [Source manifest](evidence/DB18_source-manifest.json) hashes55 source/build/config/script files, current build and private backup identity (not contents). Application JAR remains `adf95136f61839d67fd605f68bb95e0d5e3155c5c4ef7c5d3c42e58bcd51c8b9` because feature code did not change; connection and operational scripts are separately hashed/tested. Private archive `.local/backups/portable17-before-installed18.dump`:22151 bytes, SHA256 `6f8d0b58f10f5e464dcd961a1bdc1755322d04416e83b509b60653a79b096bdd`. Source data directory `.local/pgdata` retained intact. Backup/credentials/selection paths are Git-ignored; no password/raw setup code appears in evidence.

## Actual verification and limits

`scripts/test-local.ps1`→Maven verify on installed18.6/5432, exit0, finished02:26:21+06:30: **13 tests,0 failures/errors/skipped**. Auth5 includes real Unicode/password/session/CSRF/logout/idle/revision/rate/signup-cap tests; foundation3 includes native constraints/locks/history9 and HTTP boundary; pairing5 includes two-user durable join/confirmation/replay/concurrency/expiry-during-lock and real STOMP origin/CSRF/subscription/SEND/logout/disable rejection. [Full log](evidence/DB18_verify.log), [auth](evidence/DB18_auth-tests.txt), [foundation](evidence/DB18_foundation-tests.txt), [pairing](evidence/DB18_pairing-tests.txt).

Full log proves empty test schema→all9 applied. Restored dev DB separately proves9 validated/current and all original table content retained. Fresh installed start verification uses app role only; installed stop-helper path tested; old portable off plus live appUP demonstrates current authority is5432. Expected negative STOMP protocol-rejection log and existing Java27 agent/reflection warnings are not test failures. No dependencies downloaded, no admin password persisted.

Code/setup CONNECTED_AND_VERIFIED. Data transfer/table equivalence VERIFIED for actual observed dataset (mostly empty domain tables,1 setup token); no claim of large/real-customer migration coverage. Native fresh construction and existing managed dev-state restore VERIFIED on18.6. No new graphical browser journey run after this engine-only change; prior unchanged UI evidence remains from DEV-03, while real native cookie/STOMP journeys were rerun on18.6. No production deployment, full-product completion, load/soak/security audit/CI run/human UAT or future DB+media backup policy claim. Full installed-machine-from-scratch provisioning beyond this actual setup not independently reproduced on another machine.

## Recovery / remaining statuses

Old17.7 data/private native archive remain available. No automatic synchronization back to portable: stop app and reconcile any new18.6 writes before deliberate fallback; blindly selecting old54329 would expose stale state. Do not erase clusters or edit applied V1–V9. Later restoration/reconciliation of new real data needs a scoped safe plan; this successful tiny local transfer is not a production RPO/RTO guarantee. Windows PostgreSQL18 service stays Running/Automatic; Ctrl+C stops app only. Normal restart/test commands remain unchanged.

Code/tests done for this authorized local cutover; human business/UI/UAT not requested or fabricated, release remains NOT READY due remaining features/review and existing Git conflict. Code Update Record saved locally; Drive sync DRIVE_SYNC_PENDING because no authorized project destination supplied. Source package NOT_DUE for routine engineering update. Next work can continue features using18.6; no further DB credential/setup decision needed for current local run.
