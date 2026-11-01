# CHAT-03 — Mobile Chat UX and private profiles

Code Update Record · 2 November 2026, Myanmar time (+06:30).
Base: `codex/render-hosting`, `2e74847`; earlier local UI02/DEV-09/CHAT-02 changes preserved.
Authority: owner's latest chat request; extends F03/Home/Profile and F04/Chat. Deployment HOLD remains in effect.
This is an ordinary local code update, not a release or accepted UAT. Drive destination remains unestablished: DRIVE_SYNC_PENDING.

## Bounded outcomes and clause coverage

| Key | Owner clause / implemented journey | Source / behavior |
|---|---|---|
| C01 | Icon-only Send | Chat composer, accessible Send message label |
| C02 | Remove Shift+Enter instruction | Instruction removed; existing newline keyboard behavior retained |
| C03 | Repair right-click | Top-layer modal context menu, viewport clamping; no scroll dismissal after focus |
| C04 | Easier alternative actions | Compact hover/tap toolbar Reply/More; long-press and Shift+F10 retained |
| C05 | Small corner reactions | Reaction badges inside bubble wrapper, positioned over bottom corner; editable/removable |
| C06 | Video icon | Consistent outlined camera SVG on Chat/Home/call entry |
| C07 | Silence changes icon | Chat bell becomes crossed bell immediately; browser-local sound mute, call audio unchanged |
| C08 | Settings hierarchy | Home/Chat/Memories navigation → Settings → Profile / Notification settings; legacy anchors remain |
| C09 | Active / last active | Authenticated partner-only activity API; foreground heartbeat; no fabricated login time |
| C10 | Shorter text box | One-line auto-height composer, capped at 100px or 22% of visual viewport |
| C11 | Desktop width + side UI | Partner/story panel at desktop widths, hidden below 1040px |
| C12 | Editable profile photo | Settings → Profile → Edit profile; private normalized image upload |
| C13 | Photo in profile icons | Home, Chat header/partner card/couple icons, Memories and call person avatar |
| C14 | No horizontal mobile scroll | Bounded flex/grid/textarea/header; x overflow contained |
| C15 | Conversation usable with keyboard | VisualViewport resize/offset drives chat height; fixed compact header/composer |
| C16 | Hide bottom mobile navigation in Chat | Home navigation remains available through Back |
| C17 | Chat Back | Explicit accessible back-to-home chevron |
| C18 | Hide mobile Chat Sign out | Desktop sign out retained; account/settings navigation outside Chat |
| C19 | Smaller mobile icons/header | 18px glyphs, 32–36px controls, compact 54px header |
| C20 | Smaller stickers + automatic pagination | 50px mobile thumbnails; near-bottom load with in-flight/end/error guards |
| C21 | Automatic earlier history | Top-scroll keyset pagination with spinner, prepend position preservation and explicit failure Retry |

All 21 local source clauses are UI_REQUIRED and represented in production source. Graphical, native API and controlled JavaScript evidence are reported separately below; representation alone is not a pass.

## API / schema and engineering choices

V26 is additive: `app_user.avatar_jpeg`, `avatar_version`, `last_active_at`. No modification to applied migrations, account membership, message rules, trash rules, TURN or deployment configuration.

* PUT `/api/profile/photo`: session + CSRF + active membership, raw binary JPG/PNG, maximum 2 MiB encoded and 16 megapixels decoded. Center crop/resize to 256×256 and JPEG re-encode strips active content/metadata. Own account only; no client target ID. Database stored normalized bytes capped at 256 KiB. This small two-account image is independent of Memory/S3 retention.
* GET `/api/profiles/{id}/photo?v=N`: active same-space member only, image/jpeg, no-store. Missing/nonmember 404; unpaired 403; unauthenticated 401.
* Home self/member and Chat partner/self projections expose a versioned private URL or null. Never return binary photo data in metadata.
* POST `/api/presence`: authenticated active member + CSRF, updates server timestamp for caller; 204.
* GET `/api/presence`: server-selected partner only; `{active,lastActiveAt,serverTime}`. No request-selectable account.
* Foreground pages heartbeat every 20 seconds; last heartbeat younger than 45 seconds is Active. Hidden/closed pages stop heartbeats and expire within 45 seconds. Multiple visible tabs keep the account active. This is observed foreground-site activity, not a guarantee that a human is typing. Unknown history displays Offline; network failure Status unavailable. Login/session security semantics unchanged.
* Photo limits/crop and heartbeat window are explicit engineering defaults for this requested feature, not a change to existing Memory limits.
* Photo upload follows successful name save; if upload fails the editor explicitly reports that name saved/photo failed, preserves selection and refreshes profile version for retry.
* Actual browser root cause: `timers.clearTimeout()` invoked a native Window timer with the plain timers object as receiver, throwing `TypeError: Illegal invocation` before right-click and touch handlers could open the menu. Default timer wrappers now call the native functions in global scope. A receiver-sensitive regression test supplements real right-click checks. Native modal top layer also prevents background focus/scroll interference.

## Verification and remaining checks

Final source manifest: `evidence/CHAT03_Source_Manifest.json` (521 source/config/test files).

* Source SHA256: `d20ab49dee3c6077ab2def55a527d7fb3004ce265a25d5de911054003f73e245`.
* Packaged JAR SHA256: `4e444e974ed5623ec18763719ea603fef1fc8d15396cadb64679620e82728ef2`.
* `npm.cmd run build`: BUILD SUCCESS, 10.929s, 2 November 2026 00:58:48 +06:30. Build skips test execution; test evidence below is separate.
* `node --test src/test/javascript/*.test.mjs`: 48 PASS, 0 fail. Includes 7 new responsive/root-cause regressions plus preserved sound/browser-alert/call/gesture cases. These exercise real extracted handlers with controlled timers/DOM; they are not a substitute for browser checks.
* Native PostgreSQL 18.6 `HomeIntegrationTest`: 10 PASS (including 3 new photo/activity HTTP/CSRF/scope/persistence tests). `ChatIntegrationTest,PairingIntegrationTest`: 13 PASS. Fresh isolated `chat03_fresh_20261102` repeated the 3 new Home methods: 3 PASS. Total 26 executions, 23 unique tests. All affected Java/API/schema code remained unchanged after those runs; subsequent edits were frontend/wording/layout only.
* Flyway: test public V15→V26 upgrade, isolated fresh V1→V26, isolated UI fresh V1→V26, and main local V25→V26 upgrade PASS. Main local accounts/data retained. No Neon/Render migration or deployment.
* Local HTTP readiness/auth/CSRF/database lookup smoke: 8 PASS; no account created.
* Balanced HTML/unique IDs for Home/Chat/Memories and two shared templates PASS. Final packaged/source equality checks cover 14 static assets and V26. JavaScript syntax and `git diff --check` PASS.

Actual browser journeys used two synthetic members and 90 seeded messages in `couple_private_space_test.chat03_ui_20261102`, served at port 18081. No real account messages or photos were used. The initial browser run exposed the native-timer error which controlled timer tests had missed; fixed code was reloaded, and actual right-click showed Reply/Delete. Actual checks included Unicode reply saved/reloaded, reaction add/remove/change, own/partner menu authorization visibility, mute toggle immediately switching its label/icon, 50→91 history messages after top scroll with prepend position retained, 60→120 stickers after bottom scroll, Back navigation, Settings/Notification settings routing, actual photo file upload and reload, and Home/Chat/Memories avatar URLs.

Desktop 1366×768: conversation 832px + 248px partner panel, document width 1366px. Mobile 360×740: document width 360px, header 54px, composer 55px. Short viewport 320×420 with multiline Burmese draft: width 320px, no horizontal overflow, conversation 255px, composer 110px, textarea 92px. These are observed browser layout values, not physical-keyboard measurements.

Final-package browser recheck and screenshot identities are recorded in `evidence/CHAT03_Verification.txt`. Screenshots use synthetic data: `CHAT03_Mobile.jpg`, `CHAT03_Context_Menu.jpg`, `CHAT03_Desktop.jpg`, `CHAT03_Short_Viewport.jpg`.

Clause-level graphical accounting: 21 represented; 18 PASS in the bounded desktop/emulated-mobile journeys, C04/C13/C15 NOT_TESTED for their remaining real-touch/all-active-call-avatar/physical-keyboard portions. There are successful partial observations for those clauses, described above; do not count them as full passes. No current functional failure remains in exercised flows. Actual iOS/Android keyboard, long-press, active-call avatar/media and human UAT remain pending. Graphical implementation acceptance is NOT_READY for those untested portions; no human approval inferred.

AI self-review also removed an overbroad mobile launcher hide so an active call remains reachable after Back to chat. Idle call launcher remains hidden by existing call state logic. Profile editor wording now includes photo editing.

## Recovery and human review

Local pre-V26 pg_dump: ignored `.local/backups/chat03-20261102/local-before-v26.dump`; do not publish private data. No restore drill claimed. V26 adds nullable columns/default counter and retains all existing rows; ordinary recovery keeps additive migration and reverts feature code if required. Do not downgrade shared migration history or restore over current user data without a separate recovery decision.

Human review: Local Chat ဖွင့်ပြီး mobile အရွယ်/တကယ့်ဖုန်းတွင် keyboard ဖွင့်ခြင်း၊ မြန်မာစာ multiline၊ back၊ long-press၊ reaction ပြောင်း/ဖြုတ်ခြင်း၊ history scroll နဲ့ sticker scroll ကို စမ်းပါ။ Settings → Profile မှာ ပုံရွေး/save/reload လုပ်ပြီး partner browser မှာ photo/Active status ပြောင်းလဲမှုကို စမ်းပါ။ Browser-emulated checks နှင့် actual-device UAT ကို သီးခြားမှတ်သားပါ။

Release: HOLD. Human acceptance: NOT_RECORDED. No Git push or deployment authorized for this update.

## Resumption checkpoint

Local final JAR is running at `http://127.0.0.1:8080/`; Chat `/chat.html`, Settings `/home.html#settings`, Profile `/home.html#profile`, Notification settings `/home.html#notifications`. Main local Flyway is V26. Restart (after stopping this task's existing preview to free port/JAR): source `scripts/local-env.ps1`, then `java -jar target/private-space-0.1.0-SNAPSHOT.jar`; normal project development entry remains `scripts/start-local.ps1`.

The isolated port-18081 browser test server has been stopped. Its named synthetic members, conversation and uploaded test photo have been cleaned using the test-database/schema guard; migration-only test schemas remain for evidence/resumption. Browser viewport overrides were reset and agent-created test tabs closed. Main port-8080 readiness is HTTP 200 after cleanup. Current application process and launcher IDs are tracked privately by `.local/chat03-app.log` / `.local/chat03-app.pid`; verify identity before stopping any process. Never stop all Java processes or restore a backup over current data as a routine preview action.

Next review: real phone keyboard/long-press, active-call avatar visibility and user visual review. Continue local fixes from this source identity; do not deploy without a new explicit instruction. Local Code Update Record/evidence saved and readable; authorized Drive destination remains unavailable, so DRIVE_SYNC_PENDING is separate from implementation/test completion.
