# Engineering profile — current DEV-06

Latest layout update CHAT-UI-01 (1 November2026): owner removes top chat heading/tagline/status allocation and decorative right panel for full available conversation space. Only3 static UI resources changed; Java/schema/contracts unchanged. Final JARbc8ad1f6391de621004ff2bb03b0efe92aed9ea7dc5b4e32381f974cfc1a5b5b; package/syntax and actual1363x633/390x844 chat/send/sticker/reload/geometry checks passed. Native35+1 below retain earlier identities and were not rerun for this visual edit. Current source/change/recovery/Drive and human status: CHAT-UI-01_Checkpoint.md. Older entries remain historical.

Current sticker update (1 November2026, Myanmar time): owner's continuation adds multiple bundled licensed character packs to DEV-06.363 original PNG/APNG assets from Volpeon's Blobfox/BunHD (20 animated), Apache-2.0 credit/license and archive/per-file hashes included; runtime needs no external asset request or new dependency. V14 catalogue/V15 typed GIFT payload, same chat/sequence/idempotency/receipt boundary. Full native35 PASS on18.6 in isolated fresh dev06_final_20261101 schema. Final UI fixes (checkbox sizing/Cancel focus) were packaged separately with unchanged native-tested Java/tests; final JAR aa189ad6092aff195968ce3d9dc01412725fe5c7c1f1b88c80bfebcaa5a58789. Real dev13→15 preserved all13 original table projections/fingerprints. GUI journeys/proof and known limits in DEV-06_Checkpoint.md; full DEV05 background-tab qualification still pending, no human UAT or release approval inferred. Base main658251c, current work uncommitted; Git origin/main sync at658251c is historical prior to this update. Drive destination unknown/sync pending. Older entries below retain historical evidence.

Current Chat update (1 November2026): DEV-05 core implemented; owner confirms4000 Unicode code points and Edit/Delete deferred. Native full30 PASS on18.6; freshV1–V13 and real dev11→13 verified with original-table fingerprints unchanged. Actual2 accounts/pairing preserved. No new application dependency; Node24.22.1 used only for syntax/test proxy. Final runtime/JAR identity0284e454… recorded in DEV-05_source-manifest.json;99 runtime/resource files matched. Browser core flows verified, true hidden-tab Read qualification DEV05-UI01 remains unverified; see DEV-05_Checkpoint.md. No UAT/release approval or Drive sync claimed. Older entries below are historical.

Current feature update (1 November2026): DEV-04 core Home/Profile/Dates implemented; owner confirms Day1, non-leap Feb28 and BOTH members may delete Special Dates. V10/V11 applied on installed18.6; native22 tests PASS, separate empty-schema V1–V11 construction and observed9→11 dev upgrade verified. Final UI rerun at390/1440 CSS widths; actual browser midnight rollover not observed. No dependencies changed. See DEV-04_Checkpoint.md and DEV-04_source-manifest.json. Avatar/media summaries and reminder scope remain separate; full task PARTIAL / human UI/UAT undecided / release not ready. Historical entries below retain original evidence.

Current database update (1 November2026): owner authorized installed PostgreSQL18.6 Windows service/5432. Native17.7 backup restored into verified-empty dev DB; every public row count/content fingerprint matched, including actual Flyway history and setup token. Separate fresh18.6 test DB applied V1–V9 and13 integration tests passed. Non-admin couple_app, UTF8/C, defaultUTC and explicit Asia/Yangon business calendar retained. Ignored .local/database.json selects installed5432; startup verifies without admin credentials/implicit fallback. Old portable server stopped, source data/private backup retained. Current evidence/source identity/run guide: PostgreSQL_18_Checkpoint.md, DB18_source-manifest.json and Installed_PostgreSQL_Setup.md. Earlier17.7 checkpoints remain historical.

Historical DEV-03 update (1 November2026): persistent one-time pairing implemented; V7–V9, real user-queue STOMP with spring-security-messaging7.1.2 (same Boot BOM), explicit loopback origins/CONNECT CSRF/session revocation, minimal ACTIVE-only shared-home metadata. Branch main / HEAD31f9789 with dirty changes and pre-existing README merge conflict preserved; no commit/merge created. Original DEV-03 checkpoint used PostgreSQL17.7; current engine verification is recorded separately above. Not full-product/release readiness.

Owner decision update,1 November2026: Couple business timezone confirmed as **Asia/Yangon (Myanmar Standard Time, UTC+06:30)** and configured at app.couple.timezone. Future pairing/calendar/reminder services must consume that setting; UTC persistence for event instants remains unchanged. No DB migration/data change or system-JVM timezone change is needed. Historical statements that couple timezone was unknown refer to the earlier checkpoint.

Historical DEV-02 update: Login/Sign Up is implemented and verified in DEV-02_Checkpoint.md. Static auth UI follows UIs.pdf; identity uses Spring JdbcTemplate and native Spring Security form filter/session, adaptive PBKDF2, registration gate/token migrations V4–V6. Setup/partner registration codes implemented under scoped continuation, not full pairing. No new external service/dependency was added. Original foundation inventory below remains historical evidence. Current source/build identity: DEV-02_source-manifest.json; branch/Git/Drive/human acceptance remain unrecorded. DEV-01 source manifest is historical, not current code identity.

Date: 1 November 2026, Asia/Rangoon. Mode: NEW application in existing `C:\Projects\Project_M`. Initial inventory: only requirements `.md` and `.docx`; no existing implementation, dependency manifest, Git repository, migration history, AGENTS.md, Maven, PostgreSQL or Docker discovered. No source/history was replaced. Git CLI exists; no Git initialization/commit/push was requested or performed.

Product authority: latest owner instruction → requirements v1.1 confirmed clauses/D01–D06 → reviewed design contracts → Figma reference. Full detailed feature/UI approval remains unrecorded. Owner: Thein Than Zaw per source. Human technical reviewer, release owner, CI provider, Git remote/branch and authorized Drive destination: UNKNOWN. User explicitly authorizes engineering setup and DEV-01 foundation now; no separate ordinary setup approval is inferred as necessary. No production operations authorized.

## Selected engineering baseline

| Item | Selection / observed evidence |
|---|---|
| Language | Java, `release=21`; actual Oracle JDK 27.0.1.0.3 / javac same |
| Boot / build | Spring Boot 4.1.2; Maven 3.9.12; official Maven Wrapper 3.3.4 only-script |
| Web | Spring MVC, embedded Tomcat; static HTML/CSS/JS; same origin |
| Security | Spring Security 7.1.2 (Boot-managed); deny-by-default foundation; CSRF/cache headers/CSP enabled; no generated user |
| Persistence | PostgreSQL18.6 installed Windows service/5432; UTF8/C project databases/defaultUTC; old17.7 data retained/stopped |
| DB tooling | Flyway 12.4.0 + PostgreSQL module; PostgreSQL JDBC 42.7.14; all Boot BOM-managed |
| ORM | Spring Data JPA starter; open-in-view off, ddl-auto validate; no entities/repositories yet |
| Realtime | Spring WebSocket 7.0.10/STOMP dependencies only; endpoint not enabled until identity/authorization |
| Tests | JUnit + Spring Boot Test + Java HTTP client; actual PostgreSQL dedicated test DB, no substitute engine |
| Files | Future private `.local/uploads` or configured storage, outside static root; no upload feature now |
| Migration | Standard `src/main/resources/db/migration/Vn__description.sql`, Flyway history only, clean disabled |
| Access | Session design proposed by v1.1; no authentication implementation yet. Local HTTP secure-cookie=false; future HTTPS must set true |
| Topology | Modular monolith, one instance initially; broker not a durable store; no cloud provider chosen |

The Java 21 choice in v1.1 is proposed. Existing Java 27 is retained for local build/run, targeting 21 for source portability. Spring Boot 4.1.2 compatibility through Java 27 was checked in [official requirements](https://docs.spring.io/spring-boot/system-requirements.html). Maven artifact availability and actual compilation were checked; versions are pinned, not assumed from a sample. Maven [3.9.12 release notes](https://maven.apache.org/docs/3.9.12/release-notes.html). PostgreSQL [Windows binary guidance](https://www.postgresql.org/download/windows/). The smaller local engine distribution is from [Zonky embedded PostgreSQL binaries](https://github.com/zonkyio/embedded-postgres-binaries); the server is native PostgreSQL, not an in-memory emulation.

Maven downloaded from Maven Central; SHA-512 matches publisher `.sha512`. Wrapper pins distribution SHA-256. PostgreSQL artifact 17.7.0 is checksum-pinned in bootstrap script (locally observed SHA-256; not claimed as publisher signature verification). Third-party version inventory can be regenerated with `./mvnw.cmd dependency:tree`. No JavaScript packages yet. Dependencies use exact Boot parent/BOM versions; Maven has no separate lockfile.

## Decisions and boundaries

- DEV-01 engineering decision: standard Maven modular-monolith layout, native Flyway migration history, portable local tooling, separate dev/test DBs. User delegated engineering setup. No external approval/signature fabricated.
- D01: two unique account slots and singleton space slot make caps race-safe at DB level. No account replacement/unpair/deletion workflow selected. Slot identity is internal, not an application privilege.
- D02: original signup date persisted non-null. Shared canonical date has no default; creator initialization/conflict workflow stays draft.
- D03: future memory mutation checks membership, never creator-only permissions. Retention remains undecided.
- D04/D06: server-readable private content, no E2EE claim, no push infrastructure.
- Local-only health/status access is allowed; private endpoints remain denied until DEV-02/03. Only loopback binds are provided.

Verification is scoped in DEV-01 checkpoint. No independent review, browser/device acceptance, vulnerability audit, CI execution, business UAT, production TLS, backup/restore, existing-data adoption or release acceptance is implied. Stop/restart local services without deleting data. Recovery from future schema changes uses forward migrations; backup restoration has not been rehearsed.

Drive: no project destination/write authorization supplied. Local records are authoritative working outputs for this request; skill-required external record sync is `DRIVE_SYNC_PENDING`, not a request to publish to an arbitrary Drive folder. This routine foundation update is not a release/developer transfer; source-package status `NOT_DUE`.
