# Project_M — Couple Private Space

A private responsive web application for one couple and two accounts. Built with Java/Spring Boot, Spring Security, PostgreSQL/Flyway, WebSocket/STOMP and same-origin HTML/CSS/JavaScript. Voice/Video WebRTC implementation is available for local review; real media/TURN qualification remains pending.

## Current implementation

- **DEV-01:** local foundation, private access boundary and Flyway migrations.
- **DEV-02:** restricted signup, login/logout, CSRF and session/revision validation. Relationship date is required at signup.
- **DEV-03:** persistent one-time pairing, separate registration/pairing invitations and explicit shared-date confirmation.
- **DEV-04 core:** Home, own display-name editing, shared relationship date, anniversary/countdown and Special Dates. Start date is Day1; Feb29 anniversaries fall on Feb28 in non-leap years. Calendar timezone is Asia/Yangon.
- **DEV-05 core:** private text/emoji Chat (up to4000 Unicode code points), durable ordered history, unread badge, Sent/Delivered/Read receipts, duplicate-safe retry and reconnect recovery.
- **DEV-06 core:**363 free Blobfox/BunHD character sticker gifts (20 animated), pack/search picker, preview with optional500-code-point note, private chat persistence and Sent/Received filters. Artwork attribution/license is bundled; no external asset service is needed at runtime. See [DEV-06 checkpoint](docs/DEV-06_Checkpoint.md).
- **DEV-07 core:** private photo/video/document Memories, up to10 mixed files per memory, shared edits, own favorites, 24-item pagination, 3-day Trash/Restore and confirmed permanent Empty Trash. Native tests pass; browser verification remains pending. See [DEV-07 checkpoint](docs/DEV-07_Checkpoint.md).
- **DEV-08 increment:** Voice/Video call widget on Home/Chat/Memories, private selected-tab signaling, one-live-call/races/retry,30s ring/15s recovery, real media track/PC code, local cleanup, outcomes and optional expiring TURN config. Native/backend evidence is separate from still-unverified browser/device media. See [call setup](docs/DEV-08_Local_Calls.md) and [checkpoint](docs/DEV-08_Checkpoint.md).

Both members can edit/delete Special Dates and Memories. Avatar uploads, typing/presence, general notification inbox and authentication recovery/settings remain separate work. Calls still require real browser/two-device/relay verification. Custom sticker uploads/marketplace and paid gifts are outside the current increment. Chat message Edit/Delete is deferred. In-app notifications only; no browser push. Content is private through server-side authorization; no app-wide E2EE claim is made.

This is a local development build, not a production release. Actual background/unfocused-tab Read qualification in real Chrome/Edge remains pending (DEV05-UI01); see the [DEV-05 checkpoint](docs/DEV-05_Checkpoint.md). Human UI/UAT approval is not asserted.

## Run on the configured Windows development machine

Render deployment continuation: [HOST-02 record](docs/HOST-02_Deployment.md).
Docker/private storage preparation: [HOST-01 setup](docs/HOST-01_Render_Setup.md).
Live private bucket upload/readback/privacy/deletion passed; Linux container and public deployment verification are pending;
the Render profile requires working object storage and never uses local files as durable media.

Neon database setup and separate localhost port8082: see
[NEON-01 setup checkpoint](docs/NEON-01_Setup.md). This connects the existing JVM
app to Neon PostgreSQL; it does not host the JVM or media on Neon.

Observed baseline: Oracle JDK27.0.1.0.3 (compiler release21), Spring Boot4.1.2, Maven3.9.12 and installed PostgreSQL18.6 on5432. Detailed versions: [Engineering profile](docs/Engineering_Profile.md). A Java21 runtime has not been independently tested here.

From the project root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

Application: [http://127.0.0.1:8080/](http://127.0.0.1:8080/). Readiness: `/actuator/health/readiness`; liveness: `/actuator/health/liveness`. Health exposes no database details. Stop the foreground app with Ctrl+C; installed PostgreSQL stays managed by its Windows service.

Current databases are `couple_private_space` and the isolated `couple_private_space_test`, owned by the non-superuser `couple_app` role. Ignored `.local/database.json` selects installed5432; ignored `.local/credentials.json` holds the private application credential. Normal start/test does not use the installation's administrator password. See [installed PostgreSQL setup](docs/Installed_PostgreSQL_Setup.md) for provisioning and the historical portable17.7→installed18.6 transfer.

`.env.example` is documentation only: Spring does not load it automatically. A manually configured installation can export `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and a separate `TEST_DB_URL`, then run `mvnw.cmd spring-boot:run`. Never use the development database for fixtures/tests.

## New checkout / portable bootstrap

DEV-07 media setup on Windows (requires installed7-Zip):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-media-tools.ps1
```

Alternatively export `FFPROBE_PATH` and `FFMPEG_PATH` to installed compatible binaries. Keep `MEMORY_STORAGE_ROOT` private and outside static resources; preserve this directory together with the database for future backups. Accepted limits: JPG/JPEG/PNG/WebP10MB; MP4 H.2648-bit / optional AAC,180s/100MB; PDF/DOCX/XLSX/PPTX20MB;10 mixed files per memory. Documents download as attachments. Trash restore lasts72 hours; Empty Trash requires permanent-deletion confirmation. Primary-file purge does not prove old backup copies are erased. Total storage quota and backup-copy policy remain pending.

Current DEV-07 regression command loads `scripts/local-env.ps1`, appends `?currentSchema=dev07_fresh_20261101` to `TEST_DB_URL`, then runs `mvnw.cmd -B -ntp test`. That isolated schema already exists on this machine; do not recreate it or point tests at the development database. Final results and graphical limitations: [DEV-07 checkpoint](docs/DEV-07_Checkpoint.md).

Local credentials and installed-database selection are intentionally absent from Git. Do not copy another user's private `.local` folder into a checkout or reset an existing database to make setup work.

The repository also retains a portable Windows setup path. Without an installed-mode selection, provisioning defaults to portable PostgreSQL17.7 on loopback54329:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/bootstrap-tools.ps1
$env:MAVEN_USER_HOME = "$PWD/.tools/maven-user"
.\mvnw.cmd -B -ntp -DskipTests package
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/init-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

Initial tool/dependency downloads require internet. Installed mode verifies its selected connections and does not silently fall back to portable mode. These are separate database instances; changing the selected port does not transfer or synchronize data. Preserve/reconcile existing writes before any deliberate switch.

## First registration and one-time pairing

For a genuinely new installation, start the app once to apply migrations, then issue the first-account setup token:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/issue-setup-token.ps1
```

Open `.local/setup-token.txt` privately and use it in **Create account**. The setup token is single-use and the script refuses issuance after an account exists. No default/demo accounts are seeded.

1. First account: register with the setup token, then sign in.
2. On the account/space screen, choose **Create account registration code**. The partner uses this different code to register their own second account.
3. Create the shared space and confirm the relationship date; choose **Create pairing invitation**.
4. The signed-in partner enters that separate pairing code, reviews the shared date and confirms joining.

Registration codes and pairing invitations serve different purposes. Later visits use **Sign in** with existing credentials; do not register or pair again. Login/offline/logout/restart never reset the persisted membership. Application restart can require a fresh login because sessions are held in memory.

## Build and test

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

This runs Maven verify with real PostgreSQL and an HTTP/STOMP test server on18080. Fixtures require an empty isolated `_test` schema and refuse unrelated existing accounts. There is no H2 fallback. Reports: `target/surefire-reports/`; packaged app: `target/private-space-0.1.0-SNAPSHOT.jar`.

Current DEV-06 verification: **35-test regression suite passed**, plus **1 separate pre-existing-text migration rehearsal passed**, on PostgreSQL18.6. Fresh V1–V15 and actual dev13→15 upgrade verified; original13 table projections unchanged. Browser sticker journeys and exact build/test limitations: [DEV-06 checkpoint](docs/DEV-06_Checkpoint.md). The additional migration test was run separately, not as an asserted single36-test suite.

Historical DEV-05 verification:30 native tests passed on its recorded build. Browser core flows and remaining background-tab qualification are recorded in the [DEV-05 checkpoint](docs/DEV-05_Checkpoint.md). Prior evidence keeps its original identity.

Flyway applies forward migrations and validates existing history; clean and automatic baseline are disabled. Never edit applied migrations, drop database tables or remove `.local/pgdata` to resolve startup errors. Hibernate does not create/update the schema; feature persistence uses Spring JdbcTemplate.

## Project layout and references

- `src/main/java/com/couple/privatespace`: identity, pairing, home, chat, realtime and configuration.
- `src/main/java/com/couple/privatespace/memory`: private memory uploads, lifecycle and physical cleanup.
- `src/main/resources/static`: same-origin responsive UI.
- `src/main/resources/db/migration`: V1–V21 append-only schema history.
- `src/test/java`: native authentication, pairing, Home/date, Chat, sticker and predecessor migration verification.
- `scripts`: local bootstrap/provision/start/test helpers.
- `docs`: requirements, contracts, task/checkpoint records, UI reference and evidence.

Start with the [requirements](docs/Couple_Private_Space_Requirements_v1.1.md), [implementation plan](docs/Implementation_Plan.md), [API/WS contract](docs/API_WS_Contract_Draft.md), [schema](docs/Database_Schema_Draft.md), [open rules](docs/Open_Business_Rules.md) and [traceability](docs/Traceability.md). UI reference: `docs/UIs.pdf`. See [third-party notices](docs/Third_Party_Notices.md).

Keep `.local`, `.tools`, `.env`, generated `target`, credentials, setup codes and database backups out of Git. Source manifests contain file/build hashes, not private credentials. Detailed checkpoints distinguish code/test results, pending human qualification, Drive sync and release status.
