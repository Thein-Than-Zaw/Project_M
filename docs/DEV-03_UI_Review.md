# DEV-03 UI coverage and real-browser review

Source: v1.1 F02 PAIR-01–03, AC-F02-01–06, D01/D02/D04/D06, owner-confirmed Myanmar timezone and persistent one-time pairing. Actual clause/action matrix is DEV-03_Working_Spec.md. Denominator10 local coverage rows:7 UI_REQUIRED represented,3 BACKEND_ONLY assigned to actual tests. No unidentified source row; no claim of full product or all-case testing. This is AI self-review only; human design/business approval and UAT not recorded.

## Actual UI entries / fields / states

| UI ID / source | Entry / selectors / permissions | Evidence |
|---|---|---|
| UI-PAIR-01 / PAIR-01, AC01 | Auth login→space.html; #create-space/#shared-date/#create-confirm; unpaired user only, signup-date default, explicit confirmation, Myanmar timezone | Empty confirmation rejected with focus/error; create persists one waiting member; desktop screenshot |
| UI-PAIR-02 / AC02/04, PAIR-02 | #waiting-card/#create-pair-code/#pair-code/#pair-expiry/#connection-state | Live connection status, real code issuance/replacement, waiting data read from server; live transition to Home without creator manual reload |
| UI-PAIR-03 / AC02/06 | #preview-invite/#join-code→#preview-card; own/foreign date comparison, #join-confirm/#confirm-join/#cancel-preview | Invalid code error/retry; actual date mismatch displayed; unchecked confirmation refuses join; confirmed join redirects |
| UI-PAIR-04 / source signup dependency | #invite-card/#create-code/#partner-code distinct account registration vs pairing code | Permission/visibility source inspection; prior DEV-02 graphical registration verified. With both accounts present, this card correctly hidden |
| UI-PAIR-05 / AC01/04 + owner one-time rule | home.html/home.js, #home-members/#home-date; ACTIVE own membership only | Actual two members/date/timezone; creator and partner both Home; logout/re-login directly Home; server restart then login directly Home |

Source→UI reconciliation: all7 UI-required working rows represented; backend3 rows tested by native HTTP/DB/STOMP rather than fake controls. UI→source: every create/join/confirmation/invitation/logout/retry action traces to F01/F02; no unpair, fake day-count, payment, external notification or arbitrary membership edit. Minimum Home is pairing-success landing, not full F03 dashboard. Partial/future features are stated honestly with no dead clickable chat/memory/call buttons.

## Actual graphical review steps / fixtures

Final JAR on temporary18081, native PostgreSQL `_test`; two synthetic accounts only, Alex signup2020-01-01 and Jamie signup2020-02-01. Fixture helper seeds accounts only (no space/member). Browser host127.0.0.1 for creator and localhost for partner provides separate cookie sessions; allowed origins tested. Review server used separate UIREVIEWSESSION cookie on restart; real app defaults remain JSESSIONID. After evidence capture, temporary server stopped and only the fixed synthetic UUID accounts/related pairing rows deleted from test DB. Dev data never seeded/reset.

1. Creator login opens unpaired setup. Date prefilled2020-01-01, Myanmar timezone shown. Submit without confirmation→clear error and recovery, no space created. Confirm/create→WAITING and live connection success.
2. Issue private pairing code (raw code not saved to screenshots/evidence); creator stays Waiting.
3. Partner login on390px mobile: only join form, since partner already created space. Invalid code→“This invitation is invalid or expired.” without losing input. Correct code→preview shows inviterAlex, own2020-02-01, shared2020-01-01 and explicit mismatch notice.
4. Join with unchecked checkbox→clear confirmation-required error. Check and submit→partner Home; creator's existing waiting tab simultaneously navigates Home without reload. Automated test separately proves actual STOMP AFTER_COMMIT event, so REST fallback is not substituted as WS evidence.
5. Both Home pages show actual same two members/shared date. Creator logout→login again→direct Home, no pairing prompt.
6. Capture DB identity before restart, stop/restart temporary server without changing DB, old session correctly expires. Creator logs in→direct Home again. Persisted spaceUUID/date/timezone/member-count exactly match before/after files.
7. Measured desktop Home viewport1440/document1440; mobile preview viewport390/document375 (scrollbar), narrow Home360/document345, tablet Home768/document768. No horizontal overflow in those measured states. Mobile confirmation requires vertical scroll and keeps button/checkbox reachable. Browser screenshot of inactive tab initially used a smaller default viewport; desktop proof was recaptured from active1440px tab with measured dimensions. Viewport overrides reset afterward.

Visual QA inspected the actual desktop Home and mobile date confirmation PNGs: ivory/rose/plum and serif headings follow UIs.pdf; clear date hierarchy, checkbox/tap targets, mobile stacking and rounded cards. Labels/aria alerts/status/focus and plain-text personal-data rendering inspected. Actual empty-confirmation focus/error observed. No blocker/major journey issue observed in these fixtures. Not a full keyboard/screen-reader/accessibility certification. Real Android/iOS/Safari, OS clipboard permission, all language/name lengths, network outage/reconnect graphical states and every expired/stale error layout NOT TESTED graphically; native negative/concurrency/CSRF/origin/expiry cases independently automated. Copy fallback inspected, actual OS clipboard success not claimed.

Evidence: [setup desktop](evidence/DEV-03_create-desktop.png), [waiting desktop](evidence/DEV-03_waiting-desktop.png), [mobile differing-date confirmation](evidence/DEV-03_date-confirmation-mobile.png), [actual Home desktop](evidence/DEV-03_home-desktop.png), [Home mobile](evidence/DEV-03_home-mobile.png), [Home after restart](evidence/DEV-03_home-after-restart.png). Dates/names shown are synthetic fixture values read from real DB, not production fake data. Native proof: [before restart](evidence/DEV-03_pair-before-restart.txt), [after restart](evidence/DEV-03_pair-after-restart.txt).

Status: scoped implemented Pairing journey VERIFIED, owner visual/business acceptance NOT RECORDED, full F03/F04+ features NOT IMPLEMENTED, release NOT READY. No independent reviewer or human approval inferred. External record sync pending authorized destination; records remain local.
