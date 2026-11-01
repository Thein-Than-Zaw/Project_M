# HOST-02 — Render deployment continuation

2026-11-01, Asia/Yangon. Owner requested continuing HOST-01, authorized Render CLI
in the browser and explicitly chose **My Workspace → Project_M**.

## Verified target and access

- Workspace `tea-d18r9jjuibrs73dvc0i0`; project
  `prj-dbjgk3vlk1mc7396jf40`; existing Production environment
  `evm-dbjgk3vlk1mc7396jf4g`. No replacement workspace/project was created.
- Existing `SocialMedia` service is unrelated and is not modified.
- Render plugin is installed/enabled but operations are still absent from this
  chat's tool inventory; browser control remains disabled. Continued through the
  official Render CLI skill/access workflow, not browser session/cookie extraction.
- Portable Render CLI **2.34.0**, official windows_amd64 ZIP SHA256
  `3492c4ff56fb42b04303cce589c469ad28c62d7b6f244678219f8861615486ed`,
  installed under ignored `.tools/render-2.34.0` and verified.
- `scripts/render-cli.ps1` uses the owner's existing Windows proxy for its child
  process and restores prior environment. CLI credentials/config stay under ignored
  `.local/render-cli`, separate from the existing plugin/global configuration.
- `whoami` authenticated; selected workspace and existing project/environment were
  read before mutation. `render blueprints validate render.yaml --output json`
  returned **valid:true**, one Free web-service action.

## Incremental source changes

- Existing render profile can import `/etc/secrets/private-space.properties`.
  `RenderEnvironment` resolves its input from Spring's Environment so Render env
  vars take precedence and imported secret-file values work as fallback.
- `prepare-render-secrets.mjs` prepares only the six required DB/storage settings
  in ignored `.local/render-private-space.properties`. The CLI receives its file
  path, never secret values as command arguments. The file is not in the Docker
  build context or Git. Existing .env.local values are unchanged.
- Real hosted-profile startup caught a Spring ambiguity between the production
  S3 constructor and its package-visible test constructor. Marked the production
  constructor `@Autowired`; SDK contract tests alone had not caught this wiring issue.
- No business rules, migrations, plan upgrades, other services or source framework
  changes. Deployment source is isolated on `codex/render-hosting`; main is unchanged.

## Verification before publication

- Final focused tests: **8 passed**, no failures/errors/skips,18:43:29 +06:30:
  hosted env/import precedence4 + S3 SDK contract/range/retry4. After the constructor
  correction these same8 passed again at18:43:29 +06:30.
- `npm run build`: **BUILD SUCCESS**,18:43:45 +06:30,11.259s; test execution is
  separate. JAR SHA256:
  `75156f6d02f1302bf96295672f06c5ab49d9ac4bf9b118d62a853bb01b2381eb`.
- Source identity: `a9c50e7b9511370e767334b2a2ee36dcfa0ecb5611bba5b9b3ef70855aff594c`;
  manifest `docs/evidence/HOST-02_source-manifest.json`.
- Real hosted profile with imported secret file started successfully at18:44:57
  on loopback10001, connected Neon JDBC and S3. HTTP smoke8 passed; session cookie
  attributes Secure=true, HttpOnly=true, SameSite=Lax=true (attribute-only check,
  no cookie value stored). See `HOST-02_hosted-profile-http.txt`.
- Prior HOST-01 full72/local HTTP8/live private storage4 evidence remains historical;
  no claim that those entire suites were rerun for this source increment.
- Current hosted-profile startup, Git publication and actual Render deployment
  outcomes are recorded below as observed. Local rendering uses loopback port10001,
  real Neon JDBC + private S3, and the actual imported secret properties file.

Evidence: `docs/evidence/HOST-02_unit-tests.txt`, `HOST-02_build.txt`,
`HOST-02_blueprint-validation.json`, plus final source/deployment records.

## Current status

Publication/deployment in progress. Public URL and Linux runtime success are not
asserted until observed. No new user account or real-content fixture has been
created. Local Neon instance8082 was stopped before packaging; a successful hosted
activation should use one app instance because STOMP routing is in-memory.

Human UAT/release acceptance and Drive synchronization remain separate;
DRIVE_SYNC_PENDING (authorized destination unknown).
