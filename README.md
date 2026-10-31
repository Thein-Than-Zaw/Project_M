# Couple Private Space

DEV-01 local foundation, based on [requirements v1.1](docs/Couple_Private_Space_Requirements_v1.1.md) and the owner's current D01–D06 confirmations. Features F01–F10 are not implemented yet. The original requirements are unchanged.

## Local Windows setup

Java is required. This machine has Oracle JDK **27.0.1.0.3**, used for actual build/test; the compiler targets Java **21** bytecode. Spring Boot **4.1.2**, Maven **3.9.12**, PostgreSQL **17.7** are pinned. A Java 21 runtime has not been tested here. Versions and references: [engineering profile](docs/Engineering_Profile.md).

Run from `C:\Projects\Project_M` in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/bootstrap-tools.ps1
# First dependency restore (requires internet):
$env:MAVEN_USER_HOME = "$PWD/.tools/maven-user"
.\mvnw.cmd -B -ntp -DskipTests package
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/init-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

**Local URL: http://localhost:8080/** (IPv4 binding `127.0.0.1`; use http://127.0.0.1:8080/ if localhost resolves to IPv6). Readiness: http://127.0.0.1:8080/actuator/health/readiness. Liveness: `/actuator/health/liveness`. Health exposes only `UP`/`DOWN`; readiness includes a real database connection.

The status page is an engineering checkpoint, not an approved product UI or Figma reproduction. There is no working signup/login yet. All private HTTP routes, management routes other than health, and `/ws` are closed. No default/demo users are created.

`start-local.ps1` starts the database if needed and runs Spring Boot in the foreground. Stop the application with Ctrl+C. The database intentionally remains persistent/running until:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local-db.ps1
```

No Windows service, system PATH change, Docker, Node.js, SMTP or TURN service is required for DEV-01. PostgreSQL listens only on `127.0.0.1:54329`; the non-superuser application role owns separate development and test databases. Random local credentials are generated in ignored `.local/credentials.json`. Treat this directory as private to your Windows account; do not share it. The local operator password is never used by the running application. Do not delete `.local/pgdata` to fix startup errors.

`.env.example` documents variables; Spring does **not** load it automatically. For an existing PostgreSQL installation, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and run `./mvnw.cmd spring-boot:run` instead of the local provisioning scripts. Use an empty dedicated database; no automatic baseline/adoption of existing schemas is enabled. Set a separate `TEST_DB_URL` for tests.

## Test/build

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

Runs `mvn verify` against the actual PostgreSQL test database, with an HTTP server on `127.0.0.1:18080`. The test script requires the local tools/credentials above. Development data is not a test fixture. Tests intentionally fail if PostgreSQL is unavailable; there is no H2 fallback. Surefire reports: `target/surefire-reports/`. Packaged app: `target/private-space-0.1.0-SNAPSHOT.jar`.

Flyway automatically applies `V1`–`V3` on first startup and validates history/checksums on later startups. Never edit migrations applied in a shared environment. Use new forward migrations. Flyway clean and automatic baseline are disabled. Hibernate schema creation/update is disabled; there are no JPA entities yet, so catalog/constraint tests provide the current schema proof.

## Documents and next work

- [Implementation plan and task register](docs/Implementation_Plan.md)
- [Database schema and migration draft](docs/Database_Schema_Draft.md)
- [REST/WebSocket contract draft](docs/API_WS_Contract_Draft.md)
- [Open business rules](docs/Open_Business_Rules.md)
- [Traceability](docs/Traceability.md)
- [Verification and checkpoint](docs/DEV-01_Checkpoint.md)
- [Third-party dependencies](docs/Third_Party_Notices.md)

Future code uses feature packages under `com.couple.privatespace`: identity, pairing, home, chat, gifts, calls, memories, dates, notifications. Keep HTTP/STOMP adapters separate from transactional services and repositories. Static HTML/CSS/JavaScript remains same-origin under `src/main/resources/static`. Do not expose private file storage as static resources. WebRTC media stays browser-to-browser/TURN, not in the Spring WebSocket broker.
