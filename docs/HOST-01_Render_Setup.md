# HOST-01 — Render hosting preparation

Owner authorization: continue Docker/hosting configuration and persistent media
preparation after NEON-01. Existing Java/Spring Security/JDBC/Flyway/STOMP/WebRTC
architecture and all owner-confirmed file/Trash rules are retained.

## Prepared source

- `Dockerfile`: Maven 3.9.12/Temurin 21 build, Temurin 21 JRE, Linux FFmpeg/ffprobe,
  non-root user and bounded JVM heap/direct memory. Node is not a runtime dependency.
- `.dockerignore`: allow only `pom.xml` and application source; excludes `.env*`,
  `.local`, backups, Git history, local tools, target and test fixtures from context.
- `render.yaml`: one Free Docker web service, no paid disks/databases, readiness
  health path, automatic deploy disabled. Region Virginia is close to existing
  Neon Ohio; no Neon region/database was changed.
- `application-render.yml` + `RenderEnvironment`: bind `0.0.0.0:$PORT`, trusted
  proxy forwarded headers, Secure/HttpOnly/SameSite cookies (base settings retained),
  exact HTTPS WebSocket origin from `RENDER_EXTERNAL_URL` or `APP_PUBLIC_ORIGIN`.
  No wildcard origin/CSRF relaxation. Only the render profile maps the direct Neon
  URI to separate JDBC URL/user/password. TLS and supplied channel-binding policy
  are required/preserved. Hikari uses the dedicated app schema and at most 5 direct
  connections; Flyway keeps existing schema/history and validates migrations.
- `RemoteMemoryStore`/`S3MemoryStore`: AWS Java SDK 2.48.0 with path-style addressing,
  HTTPS, explicit storage credentials, finite network timeouts and startup bucket
  reachability check. Hosted mode requires S3; missing credentials never silently
  fall back to ephemeral files. Local mode remains filesystem storage.
- Memory upload validation/FFmpeg staging stays local and bounded. After validation,
  originals/thumbnails are uploaded under UUID-only keys before the database commit.
  Rollback compensates; 24-hour reconciliation handles abandoned remote objects.
  PURGING rows stay durable if object deletion fails and retry through the existing
  worker. Lists inspect at most 100 objects per pass, with continuation.
- Downloads still authorize against membership/draft/Trash/expiry before obtaining
  the storage resource. Browser endpoints, attachment MIME behavior and private
  `no-store` headers are unchanged. Bytes stream through the app; no public bucket
  or presigned URL is exposed. Spring range skipping translates to an upstream S3
  byte offset, and early close aborts remaining upstream bytes.
- No Flyway migration or new business rule. The bucket is declared private in
  `neon.ts`; this declaration is not evidence it has actually been provisioned.

## Neon storage completed; Render activation remaining

Update 2026-11-01 18:19 Asia/Yangon: CLI login succeeded, the private bucket was
provisioned, real upload/Unicode readback/anonymous HTTP403 denial/deletion passed.
Both JDBC inventories found **0 referenced Memory assets**, so no existing asset
transfer was needed at that observation. Recheck before eventual activation if
the owner uploads more media in the meantime.

1. Neon CLI authentication is now complete. The earlier API timeout was reproduced with Node
   bypassing the user's enabled Windows proxy. With that same configured proxy,
   the anonymous API check returned HTTP401 (reachable, authentication required).
   No Windows settings or TLS checks were changed. `scripts/neon-cli.ps1` applies
   proxy settings to the CLI child and restores this process's environment afterward.
   The first CLI login browser request expired after 60 seconds without a callback;
   a fresh request completed successfully. The command below is for recovery/new
   checkout, not a request for the currently authenticated owner to sign in again.

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/neon-cli.ps1 login
   ```

   Complete Login/Authorize in the browser while that terminal waits. Then:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/neon-cli.ps1 deploy --no-env-pull
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/neon-cli.ps1 env pull --file .local/neon-storage.env
   ```

   This provisioned private `couple-memories` and pulled `AWS_ENDPOINT_URL_S3`,
   `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` into a separate ignored
   file. Only missing storage keys and MEMORY_S3_BUCKET were appended to .env.local;
   equality checks confirmed all pre-existing env values were preserved. Never paste
   or commit credentials. Existing project is Ohio, a supported
   Neon Object Storage region. Do not create a replacement project/database.

2. Prove real storage works after credentials exist:

   ```powershell
   npm.cmd run check:storage
   ```

   The probe writes/reads/deletes only a random owned UUID test object. It does not
   delete a bucket or any existing user key. Live bucket privacy must also be
   checked by confirming anonymous GET for an owned test object is denied.
   SDK-double tests do not prove the live provider or credential scopes. The live
   probe separately passed, including anonymous HTTP403 denial.

3. Before switching the existing Neon app from local to remote storage, inventory
   its referenced asset UUIDs and original/thumbnail hashes in `.local/neon-uploads`.
   If any exist, transfer only those files to the private bucket and verify readback
   before switching the backend. Do not copy unrelated local-database content or
   start the remote backend against referenced local-only assets.

4. Docker is not installed on this Windows machine. Linux container build/run,
   actual Java21 runtime and FFmpeg packaging remain unverified here. On a Docker
   host use `docker build -t couple-private-space:host01 .`, then test with a private
   env file and `-p 127.0.0.1:10000:10000`. Hosted env requires `PORT=10000` and an exact
   HTTPS `APP_PUBLIC_ORIGIN` when Render's auto-provided URL is absent.

5. After verification, commit/push the reviewed source to the repository/branch
   Render will use. In Render create a Web Service with runtime Docker, plan Free,
   Dockerfile `./Dockerfile`, health check `/actuator/health/readiness` and no disk.
   Set these secrets from the ignored env file using Render's secret fields:

   ```text
   DATABASE_URL_UNPOOLED
   AWS_ENDPOINT_URL_S3
   AWS_REGION
   AWS_ACCESS_KEY_ID
   AWS_SECRET_ACCESS_KEY
   MEMORY_S3_BUCKET=couple-memories
   SPRING_PROFILES_ACTIVE=render
   ```

   Render injects PORT/RENDER_EXTERNAL_URL. For a custom domain set APP_PUBLIC_ORIGIN
   to that single HTTPS origin. Existing credentials were not published by this work.

6. Test the public service: HTTPS login/pairing, two-browser WSS chat, actual Memory
   upload/playback/seek/download, restart persistence, partner authorization,
   Trash/restore and confirmed purge. Calls need two-device/direct and TURN-relay
   verification; TURN credentials are optional server secrets, not configured here.
   No sleeping-workaround pings or paid service were introduced.
   Stop the old Neon-connected localhost instance when activating the hosted
   instance: the in-memory STOMP broker and maintenance/storage configuration are
   designed for one app instance. Separate local PostgreSQL development is unaffected.

The owner confirmed the Render plugin connection and the plugin directory confirms
it is installed/enabled. The current running chat tool inventory did not expose its
Render operations yet. Do not request a duplicate connection or claim a Render
service/deploy has been created; resume using those operations once available.

## Scope and retention limits

Render Free has ephemeral local storage; durable assets must stay outside it.
Neon supports these S3 operations but documents DeleteObject as branch soft-delete;
service history/backup retention is distinct from permanent removal from the app.
No promise of erasure from provider backups is made. App Trash remains 72 hours and
confirmed Empty Trash removes access immediately with retryable physical/provider
deletion. Overall storage quota/backup retention remain earlier open rules.

Free JVM+native-media capacity and cold-start behavior need actual Render testing;
the existing accepted file sizes/formats were not reduced to fit a hosting plan.

References: [Render Docker](https://render.com/docs/docker),
[Render filesystem](https://render.com/docs/deploys),
[Blueprint specification](https://render.com/docs/blueprint-spec),
[Neon storage quickstart](https://neon.com/docs/storage/get-started),
[S3 compatibility](https://neon.com/docs/storage/s3-compatibility).

See `HOST-01_Checkpoint.md` for actual build/test identities and pending status.
