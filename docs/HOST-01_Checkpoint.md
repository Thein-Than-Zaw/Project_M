# HOST-01 Code Update Record / Human Review Pack

2026-11-01, Asia/Yangon. Existing repository `C:/Projects/Project_M`.
Owner authorized Docker/Render configuration and durable private storage after
NEON-01, then explicitly requested an audit using the supplied Neon skill URL.

## Identity and scope

- Base Git commit: `4411534fbb8c6277778ed61e95a2af067467656b`.
- Final source identity: `6138ba3df8e279b50b7ef778b1461313d92f7e75badf4accbb3b011f59a974f6`.
- Executable JAR SHA256: `f5c8b076b3df15b9ca994760b82bbd738ff16c009dc29071c8118349b58bb7fe`.
- File hashes: `docs/evidence/HOST-01_source-manifest.json`. Includes source,
  tests, scripts, dependency locks and secret-free config; excludes credentials,
  customer content, databases, generated dependencies and `.local` runtime files.
- JAR built 18:09:24 +06:30, restored on port8082 with local-media Neon mode.
  Later changes were verification scripts/docs/config quoting, not JVM source.
- Existing migrations V1–V21 and business rules unchanged. Existing Neon env
  credentials preserved. The bucket is new; local user content was not migrated.

## Before / after and affected symbols

Before: local-only private filesystem media; no Docker/Render configuration;
Neon CLI could not reach API and first account connection URLs were manual.

After: Docker/Render source prepared, optional private S3 backend with streaming
range support, explicit hosted security/env settings, proxy-aware Neon CLI;
private bucket actually provisioned and provider tests passed.

| Area | Changes and traceability |
| --- | --- |
| Accepted hosting engineering scope | Dockerfile, .dockerignore, render.yaml, application-render.yml; Java21/Linux FFmpeg/non-root runtime, Free plan only, health/PORT/HTTPS/WSS config. |
| Existing identity/private-access scope | PrivateSpaceApplication initializer + RenderEnvironment.properties; preserve Spring Security, reject insecure/mismatched hosted config, separate URI credentials from JDBC logs. |
| MEM-01/MEM-02 storage boundary | RemoteMemoryStore, S3MemoryStore.put/resource/exists; MemoryFiles.promote/available/resource; preserve validation and authenticated browser endpoints. |
| MEM-03 / confirmed shared Trash rule | MemoryFiles.delete/reconcileRemote, MemoryMaintenance; retry durable PURGING and age-gated orphan deletion without public access or local-only durable fallback. |
| Existing download/range contract | MemoryService.Download + download/validateAssets; MemoryController resource body; Spring range skips map to upstream S3 offset with early abort. |
| Neon tool/env setup | scripts/neon-cli.ps1, neon.ts private bucket declaration, package scripts, StorageProbe/storage-probe, env example and setup/audit docs. |

Dependencies: AWS S3 SDK + URLConnection client 2.48.0. Existing Java/Spring,
PostgreSQL/Flyway, authentication and front end are retained. No new UI screens,
database migration, auth provider, paid service or message to a third party.

## Actual verification

1. Full existing/native+new regression run: **72 tests, 0 failures/errors/skips**,
   18:00:54 +06:30, 2m46s, isolated local `_test` database. This run preceded the
   final lazy upstream-range/startup-bucket/channel-binding refinements.
2. Focused final JVM tests after those refinements: **7 passed**, 18:05:59 +06:30.
   Three hosted env tests; four private storage SDK-double tests including actual
   Spring ResourceRegion conversion/upstream Range request, streaming readback,
   fresh-adapter persistence, delete failure sanitization and orphan handling.
   These are not seven additional cases to sum with the full72 total.
3. `npm run build`: **BUILD SUCCESS**, 18:09:24 +06:30, 12.893s; packaging skips
   execution of tests. Compiled Java source matches the final focused test source.
4. Restarted final app: `Started PrivateSpaceApplication` at18:10:29 +06:30,
   localhost8082; final HTTP smoke **8 passed**, including Neon-backed readiness,
   DB login lookup, CSP and private/CSRF denial boundaries.
5. Real Neon API: CLI login **Auth complete (profile DEFAULT)**. Reviewed plan
   only `+ bucket couple-memories`; deploy applied that change. Bucket list access
   level **private**. No default project/branch replacement.
6. Real provider probe at18:19:29 +06:30: owned random object upload, Unicode
   readback, anonymous GET denied **HTTP403**, delete confirmed: **4 PASS**.
   Probe objects were cleaned up; no bucket-wide delete or user-key mutation.
7. Both JDBC inventory probes: resolved app schema, 21 applied versioned migrations,
   0 referenced Memory assets and temporary Unicode transaction/readback/rollback PASS.
8. Node env tests **3 passed**; script syntax and `git diff --check` passed.

Evidence: `docs/evidence/HOST-01_full-tests.txt`, `HOST-01_final-unit.txt`,
`HOST-01_build.txt`, `HOST-01_neon-http.txt`, `HOST-01_live-storage.txt`,
`HOST-01_neon-inventory.txt`, source manifest. Build/source verification does not
prove a Linux container, public host, browser UX or real two-device media call.

## Failures and recovery observed

- Neon API timeout: Node bypassed existing Windows proxy. Honor that proxy only
  for CLI child requests; anonymous API returned401, subsequent OAuth succeeded.
  One 60-second OAuth listener expired; owner's old callback showed connection
  refused. A fresh login completed. No TLS disabling or Windows network changes.
- Final focused run once failed during JUnit TempDir cleanup under restricted
  Windows permissions (not assertions). Reran with temp access: all7 passed.
- Windows packaging failed while stale old project JVMs held the JAR open.
  Verified specific project processes/session, stopped those, repackaged successfully
  and restored the final executable/app. No unrelated Java process or DB service stopped.
- Full native run contains expected failure-path maintenance/socket warnings; the
  summary reports zero failing tests. Java27/Maven reflective/agent warnings remain.

Recovery: local profile remains available; no DB rollback is required for this
source increment and no user content was moved. Do not remove the bucket/credentials
after real assets exist. Disable hosted activation until verified; retain S3 bytes
and PostgreSQL metadata together when reverting source. Provider backup/history
retention is separate from app deletion, as already documented open business scope.

## Remaining work and separate statuses

- **Implementation:** prepared; build/native/focused/local HTTP + live S3 evidence above.
- **Neon agent audit:** complete; 9 skills up to date, existing enabled plugin/MCP
  verified, no duplicate installs or Codex config rewrite. See Neon_Agent_Setup_Audit.md.
- **Render connection:** owner confirmed connected; plugin directory installed/enabled.
  Render operation tools did not appear in this running chat's tool inventory yet.
  No duplicate connection requested; actual service/deploy remains unperformed.
- **Hosted runtime:** Docker absent locally; Linux container build, Java21/native
  media memory/cold-start behavior, Render service env/HTTPS/WSS, restart persistence,
  full authenticated remote Memory flows and two-device/TURN calls remain pending.
- **Git/publication:** changes uncommitted/unpushed in this checkpoint; no remote
  service consumes these local files yet. No Render deployment URL claimed.
- **Human acceptance / release:** not granted/not declared.
- **Documentation sync:** local record only; authorized Drive register destination
  unknown, **DRIVE_SYNC_PENDING**. This is ordinary implementation preparation,
  not a completed release/transfer milestone or source-package delivery declaration.

မြန်မာ review note: build နဲ့ database/storage စမ်းသပ်မှု အောင်မြင်ထားပေမယ့်
Render ပေါ်မှာ မတင်ရသေးပါ။ Local URL8082 ကိုသာ လက်ရှိသုံးနိုင်ပါသည်။
Neon MCP/skills ထပ်တပ်စရာမလို၊ Render connection ထပ်လုပ်စရာမလိုပါ။
