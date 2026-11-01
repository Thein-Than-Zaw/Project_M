# DEV-02 UI coverage / graphical review

Source: requirements v1.1 AUTH-01–03, scoped Login/Sign Up portion of F01, PRIV-01–03, D01/D02; DEV-02_Working_Spec.md. UI reference: UIs.pdf53 pages (no auth screens). Auth screens adapt pages1/24 branding/home,3/30 pairing and9/31 form rhythm, without inventing prototype business behavior. Owner human UI approval NOT RECORDED. This review is AI self-review, not independent human acceptance or formal accessibility certification.

## Source coverage and UI inventory

Working-spec matrix accounts for12 local source-coverage rows:9 UI_REQUIRED,1 BACKEND_ONLY (password/session internals with server tests),2 OUTSIDE_DECLARED_SCOPE (reset AC04/06). Nine UI-required rows are REPRESENTED in this Login/Sign Up increment; no unknown classification. AC03 is explicitly partial at full-F01 level: logout/idle-expiry HTTP only, reset and real WS revocation remain deferred. Counts describe scoped source coverage, not number of official requirements completed or universal test coverage. Full F01 design/implementation is still partial.

| UI ID / source | Entry / actual selectors / behavior | States/evidence |
|---|---|---|
| UI-AUTH-01 / AC01/02/03/05, AUTH-01–03 | `/`, #login-tab, #login-form, #login-email, #login-password; show/hide; native session login | Empty, busy, generic invalid credentials, network error,429, expired/signed-out notice; source form/handler inspected; actual wrong/valid login browser+HTTP |
| UI-AUTH-02 / AC01/07/08, D01/D02 | `/`→#signup-tab→#signup-form; displayName/email/relationshipDate/password/confirmPassword/registrationToken; #field-error mapping | Required-field focus/errors; server errors retained; successful create returns sign-in; no password/code browser storage; actual two registrations via browser+DB |
| UI-AUTH-03 / AC03, AUTH-03, F02 process dependency | `/space.html`, #account-content, #account-email, #account-date, #invite-card, #create-code/#partner-code/#copy-code, #logout | Guest redirect→expired notice; authenticated own account; creator-only partner registration code; account2 no invite action; busy/failure/expiry/cap error handling inspected, logout actual |

Known scope boundaries visible to users: unpaired account page says pairing is next; no fake chat/memory/call data, no unfinished clickable recovery action. Private Couple Space functionality is not enabled by merely signing in. Original signup dates remain separate until DEV-03 confirmation.

## Actual graphical journey

In-app Chromium browser against isolated real test database, temporary review server `127.0.0.1:18081`, same identity code/static UI as tested build before final four-digit-date/membership-summary hardening (auth UI bytes unchanged in final source). No production/development fixture account was created. Two synthetic accounts named `ui-review-first@example.invalid` and `ui-review-second@example.invalid` only in `_test`; named fixtures and their registration tokens removed after review, temporary server stopped. No raw invitation code is saved in screenshot evidence.

1. Open desktop1440×960 Login; inspect reference color/type/layout and working tab controls.
2. Create account with all fields empty: inline required errors and focus on first invalid input.
3. Mobile390×844: enter Myanmar name `နှစ်ယောက် 🌸`, test email, leap-day2020-02-29, synthetic password+confirmation, disposable setup code. Actual server response returns success/sign-in; no automatic login.
4. Wrong password: generic “Email or password is incorrect.”; email preserved, retry available. Correct password: navigates to authenticated account page, own name/email/date rendered as text.
5. Reload account: DB-backed identity/date still present. Create partner registration code: one-use code+expiry shown only first account, no couple membership created.
6. Sign out: returns signed-out message. Direct `/space.html` after logout redirects to login/expired notice with no account content.
7. Partner uses actual generated code to create second account with date2020-03-01, signs in; sees own separate date/name and no create-invite action. Logout succeeds.
8. Overflow measurements from rendered DOM: documentWidth1440 at viewport1440;753 at768;390 at390;345 at360 (scrollbar accounts for narrower inner document). No horizontal overflow. Mobile card/form stack, vertical scroll keeps all signup fields/actions reachable. Viewport override reset after review.

UI→source self-review: auth inputs/actions map to declared source; setup code/private partner issuance supports restricted two-account signup; signed-in unpaired page accurately states remaining pairing work. No gift/payment/push/E2EE capability was added. Safe text uses textContent rather than HTML injection. Labels, field error descriptions, status/alert roles, native keyboard buttons, focus rings,44px+ actions and password autocomplete attributes inspected. Empty-submit focus was visibly observed; full screen-reader/keyboard/browser/device matrix not run.

## Findings / readiness

No blocker/major task-flow issue observed in the tested journeys. Auth screens extend the PDF visual direction and need real owner visual feedback; no APPROVED label is supplied. Current product note about future pairing is intentional scope transparency. Windows/in-app Chromium only; real Android/iOS/Safari, OS keyboard/autofill behavior, network-offline interaction and every error layout remain NOT TESTED graphically (server negative/rate/expiry cases separately automated). Clipboard copy fallback source inspected; OS clipboard permission success is NOT TESTED.

Status: scoped Login/Sign Up graphical journeys PASS; full F01 NOT READY because recovery/change/logout-all and dependent WS are pending; human design/business UAT NOT RUN; release NOT READY. No percentages or all-cases guarantee. Actual final API/security test identity is in checkpoint/manifest, with source hashes tying unchanged auth UI to browser proof.

Screenshots: [desktop login](evidence/DEV-02_login-desktop.png), [desktop signup](evidence/DEV-02_signup-desktop.png), [mobile login](evidence/DEV-02_login-mobile.png), [mobile signup](evidence/DEV-02_signup-mobile.png), [mobile validation](evidence/DEV-02_signup-mobile-validation.png), [mobile account](evidence/DEV-02_account-mobile.png). These show real UI; synthetic displayed account information is not user personal data. Developer review still differs from owner approval/UAT.
