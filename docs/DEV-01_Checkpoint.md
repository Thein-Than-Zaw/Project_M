# DEV-01 checkpoint / Code Update Record CUR-DEV-01-001

Recorded: 1 November 2026, 00:42 Asia/Rangoon (UTC+06:30). Implementer: Codex. Review status: REVIEW (AI self-review only). User scope: requested first engineering/foundation step, not all features or deployment. Source: Couple_Private_Space_Requirements_v1.1.md, D01–D06 and current instruction. Requirement document unchanged.

## Myanmar human review pack

ယခင်အခြေအနေမှာ requirements Markdown/Word ဖိုင်သာရှိပြီး application code၊ Maven နဲ့ PostgreSQL မရှိသေးပါ။ ယခု Spring Boot foundation၊ local database၊ Flyway migrations၊ run/test scripts နဲ့ design drafts ရှိပြီး local မှာ run/test စမ်းပြီးပါပြီ။ Account/Chat/Memory/Video Call feature တွေကို ယခုအဆင့်မှာ မရေးထားသေးပါ။ Public status page မှာ database ready ဖြစ်မဖြစ်သာကြည့်နိုင်ပြီး private routes အားလုံး ပိတ်ထားပါတယ်။

စမ်းကြည့်ရန်: `http://127.0.0.1:8080/` → service ready စာသား; `/actuator/health/readiness` → `{"status":"UP"}`; `/api/me` →401. Real signup/login မရှိသေးသဖြင့် test login/password ပေးထားခြင်းမရှိပါ။ နောက်အဆင့် DEV-02 အတွက် registration access/password recovery/session rules ကို Open_Business_Rules.md နဲ့ API draft တွင် review လုပ်ရန်လိုသည်။ D01–D06 ကို ထပ်မံအတည်ပြုရန် မလိုပါ။

## Source and build identity

- Actual root: `C:\Projects\Project_M`; no Git repository/branch/base/final/tested commit exists. No commit/PR/push claimed.
- Base: requirements-only directory. Final/tested: local source files enumerated with SHA-256 in [source manifest](evidence/DEV-01_source-manifest.json). Manifest covers source/tests/scripts/build/wrapper/env example; excludes secrets, downloaded tools, DB data and generated dependencies. Documentation-only additions after tests do not change runtime source.
- Requirement SHA-256: `87cc044baac1b80dda1fee877ced605fa0059da4abbc0d953e6cf048be196ff6`.
- Verified packaged JAR SHA-256: `36e53d7f947211ac869dde7dc9eb0ec60fd8a94a52a8fd966424fc1d837801a9`.
- Build ID: `private-space:0.1.0-SNAPSHOT / DEV-01`; Java27.0.1.0.3 targeting21, Maven3.9.12, Spring Boot4.1.2, PostgreSQL17.7.
- Current application started by documented start-local script on loopback8080; PostgreSQL loopback54329. Test HTTP port18080 stopped when test process exited. No feature accounts/content seeded.

## Changes / impact

Build+configuration: pom.xml/BOM starters, official checksum-pinned wrapper, application.yml, environment example/ignore rules. Runtime: PrivateSpaceApplication.main; SecurityConfig.securityFilterChain and noFoundationAccounts. Static files: public engineering availability page only; not accepted Figma/product UI. Tools: bootstrap/init/start/test/stop scripts, ProvisionLocalDatabase.java. Database: V1 app_user, V2 couple_space, V3 couple_membership; indexes/cap/non-null/FKs. Design: implementation plan, engineering profile, full schema/API/WS drafts, open rules, traceability and dependency notices.

Before→after: no runnable application → real PostgreSQL-connected foundation; no schema →3 tracked migrations; no access policy → CSRF-enabled deny-by-default private routes; no local instructions → reproducible pinned tools/wrapper/local scripts. This is infrastructure supporting §5.11.7 DEV-01 and DB portions of D01/D02/PAIR, not complete feature ACs.

Impact matrix: existing code/data none; no customer migration/adoption. Permissions: private HTTP/WS stay closed, public allowlist only availability assets/health. API compatibility: no existing consumers; feature contracts draft only. UI: engineering page, no auth/reference fidelity claim. Security: no generated users, secrets stay ignored/local, application DB role is not superuser/role creator/database creator. Reporting/payments/push/cloud: no scope. Required future checks and decisions mapped in [traceability](Traceability.md).

## Actual verification

| Check | Command / observation | Result |
|---|---|---|
| Runtime inventory | java/javac version, Get-Command, files/services/ports | Existing Java27; no Maven/PostgreSQL/Docker before setup; Git CLI but no repo |
| Download integrity | Maven archive SHA-512 vs publisher; wrapper distribution SHA-256; PG artifact pinned SHA-256 | PASS; PG digest locally pinned, no publisher-signature claim |
| Portable setup | bootstrap-tools.ps1; init-local.ps1 | PASS; real PostgreSQL17.7 UTF8 native server; random credentials; isolated dev/test DBs |
| Wrapper | `$env:MAVEN_USER_HOME` project cache; `./mvnw.cmd -v` | PASS Maven3.9.12 / actual Java27 |
| Build / final integration | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1` → `mvn verify` | PASS, exit0;3 tests,0 failures/errors/skipped; final build00:39:41+06:30 |
| HTTP boundary | FoundationIntegrationTest.healthAndPrivateAccessBoundary | PASS: readiness200/UP without DB details; root200/CSP/no-store; guessed API/assets/WS/actuator-env401; no-CSRF write403 |
| Schema / data | migrationsAndNativeConstraints | PASS:3 successful migrations, slot/check/unique/null/FK rejection; Unicode name roundtrip; transaction rollback |
| Concurrent cap | concurrentAccountSlotCannotBeClaimedTwice | PASS: independent transaction observed waiting for PostgreSQL unique-slot lock; one commit, competing claim23505; synthetic rows cleaned |
| Test safety | requireIsolatedTestDatabase | PASS; writes allowed only database name ending `_test`; no dev fixtures |
| Fresh development boot | start-local.ps1 | PASS: dev schema initially empty; Flyway applied V1–V3; application port8080 ready |
| Repeat migration execution | final test startup on already migrated test DB | PASS: validated3, schema v3 up-to-date; no reapplication |
| Actual dev catalog | read-only JDBC inspection | PASS:3 Flyway rows/constraints observed; app role superuser=false/createdb=false/createrole=false;0 accounts/spaces/memberships |
| DB restart persistence | pg_ctl fast restart, repeated read-only catalog, live readiness request | PASS: history/constraints/empty content counts preserved; application reconnects and returns UP |

Evidence: [final test summary](evidence/DEV-01_final-tests.txt), [full verify log](evidence/DEV-01_verify.log), [native catalog](evidence/DEV-01_database-catalog.txt), [catalog after restart](evidence/DEV-01_database-after-restart.txt), [health headers](evidence/DEV-01_health-headers.txt), [readiness](evidence/DEV-01_health.json), [after restart](evidence/DEV-01_health-after-restart.json). Logs were reviewed for secret-bearing output; JDBC URLs name only local DBs, never passwords. No private content exists.

### Failures found and corrected

- Initial normal-network download was sandbox-blocked; authorized download required. Curl certificate revocation server was offline; best-effort offline revocation fallback retained TLS certificate validation (no insecure flag).
- PowerShell split unquoted dotted Maven `-D` arguments; replaced repository setting with `.mvn/maven.config`, fixed wrapper version properties and verified exact wrapper version. No weakened build check.
- Sandbox PostgreSQL restricted-token startup failed; retried as authorized normal Windows user process, retaining the newly initialized cluster. No data reset.
- Initial tests:2 passed, HTTP test expected403 but received401. CSRF error dispatch was itself denied. Permit ERROR dispatch so framework can render sanitized403; original assertion retained. [Initial failure evidence](evidence/DEV-01_initial-test-failure.txt). Subsequent final test source adds isolated DB guard and an observed concurrent lock wait, and passes.
- Read-only ad-hoc inspection initially hit the host PowerShell execution policy; reran as a process-scoped `-ExecutionPolicy Bypass` script. No machine policy changed.

Maven/Mockito emit Java27 reflective/dynamic-agent compatibility warnings; build/tests succeed. These warnings are not suppressed or treated as proof of Java21 runtime verification.

## Limits / readiness / resume

| Dimension | Status |
|---|---|
| DEV-01 code | IMPLEMENTED |
| Runnable local foundation | VERIFIED |
| DB fresh construction | VERIFIED on actual PostgreSQL17.7 |
| Repeated migration history / local DB restart | VERIFIED |
| Existing-data upgrade | NOT APPLICABLE (no predecessor data); future upgrade/restore not tested |
| DEV-01 test scope | PASS (3 tests plus live boot/catalog/restart checks) |
| Full F01–F10 business/feature tests | NOT IMPLEMENTED / NOT TESTED |
| Browser/mobile/device UI or actual WebRTC/TURN | NOT TESTED |
| Figma audit | BLOCKED by connector Starter-plan call limit; no design fidelity claim |
| Vulnerability audit / CI / backup-restore | NOT RUN; local build is not a vulnerability/CI/recovery audit |
| Human technical/UI/business approval and UAT | NOT RECORDED / NOT RUN |
| Release | NOT READY; local development only |
| Code Update Record Drive sync | DRIVE_SYNC_PENDING: no authorized project destination supplied; preserved locally; no arbitrary Drive publication |
| Source package | NOT_DUE: routine foundation update, no release/developer transfer/designated package milestone |
| Return | LOCAL FOUNDATION VERIFIED; external record sync/human acceptance separate |

Recovery: Ctrl+C foreground application, stop DB with documented script if desired. Keep `.local` private and persistent; do not delete/reinitialize the cluster. Applied shared migrations append-only; fix with forward V4+ rather than changing V1–V3. No backup/restore guarantee; no destructive reset scripts. Source package/Drive sync is not a production deployment.

Next executable task: DEV-02; re-read current files/source manifest and Flyway state, finalize affected signup access/password/reset/session defaults and auth UI/API draft, then implement a bounded real authentication journey. Pairing/WS/memory/calls remain later tasks. No automatic approval or app-level admin browsing role is introduced.
