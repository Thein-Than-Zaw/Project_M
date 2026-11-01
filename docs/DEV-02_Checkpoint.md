# DEV-02 Login / Sign Up — CUR-DEV-02-001

Date:1 November2026,01:15 Asia/Rangoon (UTC+06:30). Implementer:Codex. Record REVIEW / AI self-review, human approval not recorded. Source: requirements v1.1 + D01–D06 + owner's explicit continuation of DEV-02 Login/Sign Up. Original requirement unchanged. Current working scope/decisions: DEV-02_Working_Spec.md; actual UI coverage: DEV-02_UI_Review.md. This is a routine implementation increment, not a release/developer transfer.

## Myanmar review pack

ယခင် foundation status page အစား Login/Sign Up ကို PostgreSQL နဲ့ တကယ်ချိတ်ထားပါပြီ။ Sign Up မှာ Relationship Date မဖြစ်မနေပါပြီး account နှစ်ခုထက်ပို၍ ဖွင့်မရပါ။ ပထမ account ကို local setup code နဲ့ဖွင့်၊ login ဝင်ပြီး partner registration code ထုတ်ပေး၊ ဒုတိယ account က အဲဒီ code နဲ့ဖွင့်နိုင်ပါတယ်။ Password ကို hash ဖြင့်သာသိမ်းပြီး session/CSRF နဲ့ logout/idle expiry ကို စမ်းထားပါတယ်။ UI PDF ပုံစံကို လိုက်ဖက်အောင် auth screens အသစ်ဆွဲထားပါတယ်။ Human visual approval မပေးရသေးပါ။

အသုံးပြုရန် `http://localhost:8080/` → Create account → မိမိ name/email/relationship date/password+confirmation နဲ့ `.local/setup-token.txt` ထဲက private code ကိုထည့်ပါ။ Signup ပြီးရင် Sign in လုပ်ပါ။ ပထမ account မှ Create partner invitation ကိုနှိပ်ပြီး code ကို partner ထံ မိမိကိုယ်တိုင် သီးသန့်မျှဝေနိုင်ပါတယ်။ ဤ code သည် account registration အတွက်သာဖြစ်ပြီး Couple Space pairing မဟုတ်သေးပါ။ Setup code24h ပြည့်/ပြန်ထုတ်လိုပါက account မဖွင့်ရသေးချိန် `scripts/issue-setup-token.ps1` ကို run ပါ။ Source/docs ထဲမှာ real code/password မရေးထားပါ။

Development database ထဲမှာ account/demo data မဖန်တီးထားပါ။ Browser review အတွက် `_test` database သီးခြားမှာ synthetic accounts နှစ်ခုသာဖန်တီးပြီး စမ်းသပ်အပြီး named fixtures ကိုဖယ်ရှားထားပါတယ်။ Password Reset Email၊ change-password/logout-all၊ Couple Space pairing၊ Chat/Memory/Call က နောက်အဆင့်တွေပဲဖြစ်ပါတယ်။ Q02/Q03/Q05/Q06 နဲ့ couple-timezone dependent date validation ကို မသိမသာဆုံးဖြတ်မထားပါ။

## Source / migration / build identity

Root `C:\Projects\Project_M`; Git repository/branch/commit/PR absent (no invented SHA). Base: DEV-01 manifest + requirement/PDF supplied after foundation. Final41 build/source/config/script files hashed in [DEV-02 source manifest](evidence/DEV-02_source-manifest.json), including requirement and PDF digests. Prior DEV-01 manifest remains historical. Exact tested JAR SHA-256: `9a3eeed015f214fd0438c5c4f466882e2b77d69f2ffe38cc33ff48af5cff661f`.

Build `com.couple:private-space:0.1.0-SNAPSHOT`; Java27.0.1.0.3, release21, Maven3.9.12, Spring Boot4.1.2, native PostgreSQL17.7. No new external dependency/service. Actual final source hash comparison passed41/41. Documentation additions after testing do not change runtime identity. UI graphical journeys used unchanged final UI files, before final server-only date-range/paired-summary refinements; final backend tests cover those refinements. Final login page was reopened/captured against final app on8080.

Forward migrations only: V4 adds enabled/security_revision; V5 adds singleton registration lock row; V6 adds hashed registration tokens/kind/issuer/expiry/consume/revoke constraints and partial unique live-kind index. V1–V3 checksums unchanged. Both existing v3 databases upgraded to v6; repeat startup validated all6 and reported current v6. No customer data or existing user rows existed. Fresh all-six migration on a new separate DB was not independently rerun; foundation fresh V1–V3 evidence and actual v3→v6 upgrade are distinct. No baseline/clean/reset used.

## Code / security / compatibility changes

- `identity/AccountStore`, AccountPrincipal: JDBC-backed normalized identity; password erased by native authentication credential lifecycle; explicit safe response DTOs only.
- RegistrationService: required/valid LocalDate, four-digit year, name/email/password/confirmation/code validation; adaptive PBKDF2 preserving Unicode/outer spaces; gate lock around account insert+token consume; database cap remains ultimate guard.
- AuthController: real csrf/me/register/partner-code endpoints. Login uses native Spring Security URL-encoded form filter (deliberate documented update from draft JSON login), rotates/persists session, JSON success/error. Logout uses CSRF-protected POST/session invalidation/cookie deletion.
- AuthFilters/RateLimiter/ApiFailure/ErrorHandler: server requestId, generic credential/errors, HttpOnly/SameSite cookie, no-store/CSP, bounded single-instance account/IP rate counters and Retry-After,16KiB registration body limit; no trusted forwarded-IP header. Enabled/security revision checked on each authenticated HTTP request. Real WS remains disabled, not falsely certified revoked.
- Auth HTML/CSS/JS and authenticated account page: PDF-inspired new auth screens; actual loading/error/success/expiry/retry states; no passwords/tokens localStorage; textContent for personal data; no fake feature navigation. Old unused foundation status assets removed.
- `IssueSetupToken.java` + PowerShell: local operator bootstrap only, writes raw code to ignored local file; DB stores digest only; refuses after first account; reissue revokes previous unused setup code. No guest setup-code issuance API.
- Docs: working spec, API/schema implementation notes, plan/profile/open rules/RTM, README, UI review, this update record and evidence. API compatibility risk is limited to previously draft/no existing consumer contracts; future pairing APIs remain draft.

Engineering defaults recorded explicitly: code24h; password12–128 codepoints; email normalized; idle30m; login10/account30/IP and signup10/IP per15m, bounded state; production HTTPS must enable secure cookie. Session/rate state local-memory resets on app restart; accounts/tokens persist. No public multi-couple signup, account replacement/deletion lifecycle, mail provider or membership shortcut introduced. The timezone-dependent proposed today/past check is deferred until couple timezone is confirmed, rather than adopting server timezone as business truth.

## Actual tests / evidence

Command: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-local.ps1` → Maven verify, final exit0, build completed01:10:02+06:30. **8 tests,0 failures,0 errors,0 skipped**. Real test PostgreSQL + real HTTP cookie clients, not H2 or mocked authentication. Test-profile idle1m and raised per-IP allowances are documented; account limit10 and429 behavior still tested. Production defaults unchanged.

| Test / source | Actual outcomes |
|---|---|
| AuthenticationIntegrationTest.signupValidationLoginRotationLogoutAndPermissions / AC01/02/03/05/07 | Missing/out-of-range date and mismatched password reject without consume/account; no-CSRF403; successful signup persists Myanmar name/date/hash, spaces retained; no auto-login; wrong login401; normalized email login; session ID changed, HttpOnly/SameSite observed; old CSRF403; own DTO safe; logged-in feature REST/WS403; logout204, old cookie401 |
| partnerCodeIsOneUseScopedAndAccountCapSurvivesConcurrentSignup / AC08 | Code replacement invalidates old; duplicate email does not consume; independent simultaneous second-signup results201/409; count2; third409; account2 invite403 and account1 full409;0 memberships; no raw code in token-hash column |
| expiredInvitationWrongCodesAndGenericRateLimitedLogin / AUTH01/03 | Expired/wrong code400; no account;10 generic missing-account401 then429 with positive Retry-After |
| disabledAndRevisedAccountsRevokeExistingSessions / PRIV02 | Changed security revision invalidates old session; disabled account rejects existing session and fresh login |
| idleSessionExpiresOnRealHttpRequest / AC03 portion | Actual session idle62s with test timeout1m, next self request401 |
| FoundationIntegrationTest (3 tests) | Native migration/history6, account/space/member checks/FKs and observed independent lock contention; health/no details, no-store/CSP, guest API/assets/WS denial and CSRF403 |
| JavaScript parse | `node --check` auth.js and space.js both exit0 |
| Graphical browser+DB | Real first signup→wrong/correct login→reload→partner code→logout→guest protected-route denial→partner signup/login with distinct date→logout; mobile/desktop widths checked. See UI review for exact graphical limits |
| Local startup / catalog | Final app started01:11:12 on127.0.0.1:8080; readinessUP;6 successful Flyway rows; non-superuser app role;0 dev accounts/spaces/memberships; private setup file digest checked against live DB token without revealing code |

Evidence: [final verify log](evidence/DEV-02_verify.log), [auth test summary](evidence/DEV-02_auth-tests.txt), [foundation summary](evidence/DEV-02_foundation-tests.txt), [development catalog](evidence/DEV-02_development-catalog.txt), [health](evidence/DEV-02_readiness.json), [headers](evidence/DEV-02_login-headers.txt), [setup check](evidence/DEV-02_setup-check.txt), [graphical review/screenshots](DEV-02_UI_Review.md). Log inspection found no real password/token values; synthetic credentials remain test-only source. No user content or raw live registration code is in evidence.

Initial regression was expected3 migrations vs actual6 after adding forward V4–V6. The native-capacity/permission assertions were retained; migration-count expectation was updated to6 for the new schema. [Original failure](evidence/DEV-02_initial-regression-failure.txt), [upgrade/initial run](evidence/DEV-02_upgrade-and-initial-regression.log), [first8-test pass](evidence/DEV-02_first-auth-pass.log) retained. Final repeat was justified by four-digit date validation/paired-summary refinements and removed obsolete assets; no failures deleted or guards weakened. Java27 Maven/Mockito compatibility warnings remain nonfatal; no Java21 runtime or vulnerability audit claim.

## Readiness / recovery / resume

Code IMPLEMENTED and scoped Login/Sign Up VERIFIED; AI self-review only. Full F01 PARTIAL (reset AC04/06, reset/socket part of03, password-change/logout-all pending); F02–F10 not implemented. Human UI/business/technical approval and UAT NOT RECORDED/NOT RUN. Real phone/iOS/Safari, SMTP, WS frame security/revocation, load/soak, dependency vulnerability audit, CI and backup restore NOT TESTED. Local release NOT READY for production. Source package NOT_DUE for routine update. Drive Code Update Record sync DRIVE_SYNC_PENDING: no authorized project destination supplied, record/evidence preserved locally; no arbitrary external publication. These are separate from passing local code/tests.

Recovery: stop foreground app with Ctrl+C; local DB may remain running or stop using documented script. Preserve `.local` credentials/data; no account cleanup/reset is offered for user data. V4–V6 are additive; keep matched code/schema and use forward corrective migrations after shared use. Reverting to old code alone does not undo issued registration permissions/accounts; no destructive rollback/recovery claim. Only named synthetic UI fixtures were deleted from `_test` after review. Local setup file is secret and excluded from source/evidence.

Next task DEV-03: review actual source/manifest/Flyway state; finalize space creation/invitation/join confirmation, couple timezone and signup-date conflict handling; transactional two-member pairing + route guards + real waiting-state update. Preserve separate original signup dates and D01–D06. Authentication recovery/change/logout-all require their own remaining bounded work; don't label all F01 complete.
