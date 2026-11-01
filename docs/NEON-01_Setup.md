# NEON-01 — PostgreSQL connection checkpoint

2026-11-01, Asia/Yangon. Scope: connect the existing Spring Boot app to the
owner-selected Neon project `weathered-lake-93829440`, branch `production`,
database `neondb`. This is database integration, not a public application deployment.

## Configuration and data boundary

- User-provided credentials live only in ignored `.env.local`; never commit or
  paste that file, setup codes, or credential-bearing connection strings.
- `neon init -y --org-id org-plain-mountain-08088088 --project-id weathered-lake-93829440 --branch production --services postgres`
  completed offline with CLI 8.16.0, installing Neon tooling/config dependencies.
  Its project link was explicitly **not verified** through the API.
- `neon deploy` and `neon login` failed: `Could not reach the Neon API`.
  Anonymous Node HTTPS probes also timed out outside the sandbox. Direct database
  connections subsequently succeeded using URLs saved by the owner from Console.
  Do not report successful CLI deployment or automatic env pull.
- `neon.ts` leaves other services unspecified. No Neon Auth, Functions, Storage,
  Data API, AI Gateway or Realtime service has been enabled by this work.
- Installed Neon `neon` and `neon-postgres` agent skills were consulted. Existing
  JDBC, Hikari, Flyway, Spring Security and STOMP architecture is retained.
- Preflight found `public.playing_with_neon`. It was preserved. Existing V1–V21
  migrations were applied to a new, separate `couple_private_space` schema.
  No Flyway baseline, table reset, local user/content transfer or new migration
  file was necessary. Restart validated schema version 21 without reapplying it.
- Runtime and migrations use `DATABASE_URL_UNPOOLED`, with the JVM's existing
  maximum-five Hikari connection pool. Both URLs are validated for matching
  endpoint/database and mandatory TLS. Passwords are separated from JDBC URLs.
- Live probes showed URL-only `currentSchema` did not resolve the app schema on
  either endpoint. The final runtime explicitly sets Hikari `schema`, and Java
  operator tools call `Connection.setSchema`. Direct connections retain that
  session setting. The underlying URL-only behavior is not attributed to a
  proven Neon/driver defect. Initial background query errors were corrected.
- An initial timeout env name bound incorrectly and caused premature pool
  initialization/sealed configuration. Corrected to
  `SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT`; the final app started successfully.
- Local media uses separate `.local/neon-uploads`; local development media at
  `.local/uploads` is not shared with the Neon database's cleanup jobs.

## Run and first registration

Use Node 24 and the installed JDK from the project root:

```powershell
npm.cmd run build
npm.cmd run start:neon
```

URL: http://127.0.0.1:8082/ . Stop the foreground process with Ctrl+C.
The runner loads `.env` if present, otherwise `.env.local`. Required variables:
`DATABASE_URL` and `DATABASE_URL_UNPOOLED` (actual PostgreSQL URIs, not masked text).
Optional `SERVER_PORT` and `NEON_MEMORY_STORAGE_ROOT` override the separate defaults.
The Windows build uses the existing local Maven 3.9.12 installation if present,
otherwise the Maven wrapper; Unix uses the wrapper. Packaging skips execution of
database tests; do not interpret `BUILD SUCCESS` as a full integration suite pass.

For this new database, a single-use, 24-hour first-account setup code was issued
privately into `.local/neon-setup-token.txt`. Open it locally and paste into
**Create account → Setup or partner invitation code**. No account was seeded.
Existing local accounts cannot log in to this separate database.
To reissue while no account exists, run `npm.cmd run setup:neon`. Reissue revokes
the previous unused setup code. After registering the first account, use the
existing partner invitation flow for the second account. Local token issuance
still uses its original file and local database configuration.

## Verification and limits

- `npm run build`: **BUILD SUCCESS**, 2026-11-01 17:40:36 +06:30; packaging/tests compiled,
  test execution skipped. Maven emitted a Java 27 reflective-final-field warning.
  JAR SHA256: `f2f0d01e164b89974669ee48df04e382bd0c574660b67601a63ec3a7e360a38d`.
  The unchanged application JAR uses the new external Node launcher/env adapter;
  keep those source files with the JAR when reproducing this setup.
- Flyway: `Successfully applied 21 migrations ... now at version v21`; subsequent
  startup: schema version 21, no migration necessary. Flyway's validation count
  includes its schema-creation history entry; 21 versioned migrations are applied.
- `npm run test:env`: 3 tests passed, 0 failed. Includes URL credential decoding,
  TLS rejection, endpoint/database mismatch rejection, schema and storage isolation.
- `npm run check:neon`: both configured JDBC paths connected with TLS required;
  resolved schema `couple_private_space`; V21 present; Unicode write/read/rollback
  passed using a temporary table, with no persistent test fixture.
- `npm run test:neon:http`: 8 checks passed: database-backed readiness UP, home/CSP,
  CSRF token endpoint, unauthenticated API/actuator boundaries, untrusted POST
  rejection, and DB-backed invalid-login lookup. No account was created.
- Evidence: `docs/evidence/NEON-01_http-smoke.txt`,
  `NEON-01_jdbc-probe.txt`, `NEON-01_env-tests.txt`.
- The earlier full local backend suite was not rerun against this production
  Neon database. Existing test guards correctly require a separate `_test`
  database; they were not bypassed. Full feature/browser/two-device call and
  migration-on-preview-branch testing remain separate work.
- Render/JVM hosting, HTTPS/public origin configuration, persistent/object media
  storage, TURN and full hosted end-to-end verification remain pending. Application
  processes and media still run on this computer. CLI/API access remains blocked.
- No business rules changed. No Git commit/push or Drive synchronization performed
  in this increment; DRIVE_SYNC_PENDING.
