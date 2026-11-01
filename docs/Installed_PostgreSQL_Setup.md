# Installed PostgreSQL local setup

Current engine: PostgreSQL18.6 Windows service `postgresql-x64-18`, port5432. This supersedes the earlier portable17.7/54329 setup as the active project database. Application URL stays http://localhost:8080/. No business requirement changed.

## Current connection and commands

- Development DB `couple_private_space`; isolated test DB `couple_private_space_test`.
- Both owned by `couple_app`, a LOGIN role without superuser/createdb/createrole privileges. App never uses the installation's postgres admin password.
- EncodingUTF8, C collation/ctype. Project database defaults UTC; PostgreSQL JDBC may select the JVM client-session timezone, while event instants remain TIMESTAMPTZ/Instant and Hibernate UTC. Couple business calendar remains explicitly Asia/Yangon, unchanged.
- Ignored `.local/database.json` selects `{ "mode": "installed", "port": 5432 }`. Ignored `.local/credentials.json` supplies existing random appPassword; no installed admin password is stored in source or local configuration.
- `scripts/local-env.ps1` loads the selected port. `init-local.ps1` verifies installed connections and fails if unavailable; it does not silently start/fall back to portable. No environment variables were changed globally.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

Start runs the app in foreground; Ctrl+C stops the application. Installed PostgreSQL remains managed by its Automatic Windows service. `stop-local-db.ps1` now explains that installed service is left running; it only stops the portable cluster when portable mode is selected. No administrative password is needed for normal start/test. Never point tests at development DB; test safety requires an isolated `_test` database.

## Provisioning/reproduction

After restoring Maven/JDBC dependencies and creating a private random appPassword in ignored `.local/credentials.json`, a local operator can run `scripts/ProvisionLocalDatabase.java` with JDBC driver on classpath and process-scoped `LOCAL_PG_ADMIN_URL=jdbc:postgresql://127.0.0.1:5432/postgres`, `LOCAL_PG_ADMIN_USERNAME=postgres`, `LOCAL_PG_ADMIN_PASSWORD` supplied privately, and `LOCAL_PG_APP_PASSWORD` from the local app credential. It creates only the fixed project role/databases, checks existing privilege/owner conflicts and refuses to silently alter another role/owner. Databases use UTF8/C/UTC. Remove admin environment variables afterward. This task already completed that provisioning; do not recreate or reset current data.

Set `.local/database.json` to installed5432 only after provisioning and any controlled data transfer. `VerifyLocalDatabase.java`, called by normal init, checks both connections and non-admin role without creating or dropping anything. For a genuinely empty database normal app/test startup applies Flyway V1–V9. Existing managed databases validate their original Flyway history; no baseline/checksum edits/custom migration ledger.

## Transfer performed in this task

Source17.7 dev DB was inspected:0 accounts/spaces/memberships,1 registration token and valid migration history1–9. App was stopped before final snapshot. Native18.6 pg_dump created a complete custom archive of the old project DB, without role passwords/global roles. Restored only into the verified empty, newly created5432 project DB using app role/no-owner/no-ACL/error-on-failure. Existing Flyway rows were restored as actual database history, never fabricated/re-baselined. Every public table's row count and ordered-row content fingerprint matched before selecting new DB. This preserves the existing setup code/hash/expiry too; no new code was issued.

Private backup stays under ignored `.local/backups/portable17-before-installed18.dump`; source17.7 data directory remains intact. Test DB on18.6 was deliberately fresh and Flyway constructed all9 migrations there during native integration tests. No test fixtures were transferred from the old test DB. No user account, pairing record or private media was created/removed by this cutover. No media feature/data exists yet; this is not a future full DB+asset backup policy or production recovery guarantee.

Fallback is retained, not automatically synchronized. To return to portable54329, stop the app, assess any writes made on18.6, preserve/reconcile data, then deliberately select portable mode and start. Simply flipping back after new writes would show stale data; don't do it as an automatic retry. Do not delete `.local/pgdata`, overwrite existing DBs or run Flyway clean. The old portable server can stay stopped; installed5432 is current authority.

Detailed observed results, backup/source/build identities and limits: PostgreSQL_18_Checkpoint.md. At the historical database cutover the README merge conflict was preserved. It is now resolved for the authorized Git sync; README is the current run/feature overview and this guide retains the cutover details. Current migrations extend through V13; the V1–V9 statements above describe the earlier transfer checkpoint.
