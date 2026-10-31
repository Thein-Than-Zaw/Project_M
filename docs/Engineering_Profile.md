# Engineering profile — DEV-01

Date: 1 November 2026, Asia/Rangoon. Mode: NEW application in existing `C:\Projects\Project_M`. Initial inventory: only requirements `.md` and `.docx`; no existing implementation, dependency manifest, Git repository, migration history, AGENTS.md, Maven, PostgreSQL or Docker discovered. No source/history was replaced. Git CLI exists; no Git initialization/commit/push was requested or performed.

Product authority: latest owner instruction → requirements v1.1 confirmed clauses/D01–D06 → reviewed design contracts → Figma reference. Full detailed feature/UI approval remains unrecorded. Owner: Thein Than Zaw per source. Human technical reviewer, release owner, CI provider, Git remote/branch and authorized Drive destination: UNKNOWN. User explicitly authorizes engineering setup and DEV-01 foundation now; no separate ordinary setup approval is inferred as necessary. No production operations authorized.

## Selected engineering baseline

| Item | Selection / observed evidence |
|---|---|
| Language | Java, `release=21`; actual Oracle JDK 27.0.1.0.3 / javac same |
| Boot / build | Spring Boot 4.1.2; Maven 3.9.12; official Maven Wrapper 3.3.4 only-script |
| Web | Spring MVC, embedded Tomcat; static HTML/CSS/JS; same origin |
| Security | Spring Security 7.1.2 (Boot-managed); deny-by-default foundation; CSRF/cache headers/CSP enabled; no generated user |
| Persistence | PostgreSQL 17.7 Windows amd64 portable engine; UTF8, C locale, UTC timestamps; port 54329 loopback |
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
