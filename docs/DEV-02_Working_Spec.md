# DEV-02 Login / Sign Up — working specification

Source: requirements v1.1 F01 AUTH-01–03, F10 PRIV-01–03, D01/D02/D04/D05; owner asks to continue DEV-02 after the proposed setup-token/partner-invite approach. This authorizes the scoped implementation, not a fabricated blanket approval of all proposed F01/F02 rules. Original requirement remains unchanged. Status: IN_PROGRESS / human visual review pending.

Scope: real restricted registration, login, logout, CSRF, idle-session expiry, database-backed identity, generic credential errors, bounded account/IP rate limiting, authenticated unpaired landing, mobile/desktop screens. Password recovery/email, password-change/logout-all, full pairing and authenticated STOMP are later work, explicitly not completed by this task. Existing private feature routes remain closed even after login.

Engineering defaults used for this local increment: first account needs an operator-issued 256-bit setup code; first account can issue/reissue a one-use partner registration code. Codes expire after24h, stored SHA-256 only, serialize issuance/consumption and account-slot allocation on one DB lock. A registration code creates an account, not couple membership; DEV-03 owns space/invite/join/date-conflict confirmation. Keep original signup dates separately. No default account, raw token log, public token issuance, self-service account replacement or third slot. Operator issues setup code through a local script, never a guest API. Reissuing revokes the old unused code.

Validation defaults: trimmed nonblank name1–60 Unicode codepoints; normalized trimmed lowercase email with syntax/max254 validation; password12–128 codepoints, no trim, confirmation exact; adaptive PBKDF2; valid required relationship LocalDate. The proposed today-or-past check depends on a confirmed couple timezone, so that check stays deferred to DEV-03 rather than assuming UTC as the couple's calendar. No guessed date-count/leap policy. Session idle30m configurable; production secure cookie required. Login10 attempts/account and30/IP per15m, registration10/IP per15m; configurable; request.remoteAddr only, no trust of X-Forwarded-For. Retry-After on429, bounded in-memory state for one local instance. App restart clears rate counters/sessions; tokens/accounts persist.

UI reference: docs/UIs.pdf pages1/24 Home,3/30 Pairing,9/31 forms. Warm ivory background, dusty rose primary, dark plum type, serif display headings, rounded cards, restrained landscape illustration, generous desktop spacing, stacked mobile. Auth screens are new adaptations: PDF has no login/signup/reset screens. English labels + Myanmar Unicode input; no external CDN/fonts/analytics.

| Coverage row / source | UI/entry/fields/actions/states | Verification responsibility |
|---|---|---|
| AC-F01-01 | UI-AUTH-01 login tab ↔ UI-AUTH-02 signup; name/email/password/confirm/date/code; loading/validation/success; password visibility | Real DB signup/name/email persistence, adaptive hash; graphical form/reload |
| AC-F01-02 | UI-AUTH-01 generic wrong-login error; retry and password visibility | Wrong/missing account same401, no authenticated identity |
| AC-F01-03 logout/expiry portion | UI-AUTH-03 authenticated unpaired landing; logout; expired-session banner/login | Old cookie denied after logout/expiry; sockets remain disabled, reset portion deferred |
| AC-F01-04 | OUTSIDE declared Login/Sign Up scope | Reset/expired reset tokens deferred, no fake recovery link |
| AC-F01-05 | All unsafe requests require session CSRF token; visible retry if expired | No token/wrong token/no-cookie denial; login rotates session/CSRF |
| AC-F01-06 | OUTSIDE declared scope | Email recovery deferred |
| AC-F01-07 | UI-AUTH-02 required relationshipDate, field error, safe inputs retained | Missing/invalid date rejects without row/token consume; persisted date; timezone-dependent rule deferred |
| AC-F01-08 | UI-AUTH-02 registration code; capacity/full/invalid/expired states; no public signup | Third account/concurrent token claim rejected atomically; native constraints retained |
| AUTH-01 limits/errors | Inline required/name/email/password/confirmation/date/code errors, busy buttons,429 explanation | Server validation authoritative; Unicode lengths/password spaces preserved |
| AUTH-02 session/security | No credential localStorage, login session rotation, HttpOnly/SameSite, CSRF refresh | Cookie and old-CSRF tests; session identity fresh DB check per request |
| AUTH-03 / PRIV-03 | Guests only login/register; unpaired user sees own account and partner-registration action | Authentication required, principal-derived issuer; member features remain denied |
| F02 process-map dependency | UI-AUTH-03 explains account is ready, partner registration code, full pairing next | No fabricated ACTIVE space/home/private sample content |

Minimum resume context: this spec, current code/migrations V1–V3 + new forward migrations, requirement v1.1, DEV-01 manifest/evidence, UI PDF and current test results. Task boundaries: backend+UI+tests together; no unrelated feature production. Base source is local/no Git; record final source/build identity at checkpoint. Code record local; Drive destination remains unspecified/pending; source package NOT_DUE for routine work. Review is AI self-review, no human acceptance/UAT/release claim.
