# DEV08-UI01 — Full-page calls and notification sounds

Authority: owner's 1 November 2026 request for Meet-like full-page calls, microphone/camera icons, message chime and call melody; confirmed **mute all notification sounds, keep visual alerts**. Existing CALL-01–03 / AC-F06-01–06, CHAT-01–03 and docs/DEV-08_UI_Coverage.md remain the baseline. No database migration, transport/provider change, recording or screen sharing.

Before: bounded modal dialog with text controls and opt-in single beep. After: same-document full-viewport call section, dark partner stage, mirrored self-preview, labelled SVG microphone/camera/end controls, Back to chat retaining the connection, optional browser full screen. Camera-off/voice fallback, setup/error/reconnect, answer/decline/cancel, other-tab ownership and recent history remain represented.

Sound: original synthesized incoming-call melody and short incoming-message chime. Enable sounds once per browser; saved opt-in still needs a gesture after a new page load due to browser autoplay rules. The persistent Sounds button toggles notification mute; preference is device/browser-local and propagates between same-origin tabs. Muting stops scheduled ringtone notes immediately and preserves visual alerts and call audio. History, own messages and receipt updates do not chime; message IDs suppress duplicates (cross-tab shared storage is best effort, not a browser-wide audio lock). Open browser/page and browser sound permissions are required; this is not background push or an uploaded custom song.

## Scoped coverage and usability review

Six owner clauses, all UI_REQUIRED / both partners, represented in source; graphical runtime **0 PASS / 6 NOT_TESTED**. Source self-review is not human visual approval. Existing backend/native signaling checks are separate evidence.

| Source clause | Entry / selectors / expected behavior | Verification |
|---|---|---|
| Full-page video, no popup | Home/Chat/Memories call entry → #call-panel section, #call-stage, #call-remote, #call-local; fills viewport, self-preview does not crop partner video | Source inspected; actual desktop/mobile layout NOT_TESTED |
| Microphone and camera icons | #call-mic / #call-camera SVG + visible desktop labels and accessible mobile labels; pressed state follows local track.enabled; camera hidden for voice | Existing state contract preserved; device output NOT_TESTED |
| End and return | #call-end releases media; #call-close or Escape returns to chat without navigation/media teardown; Calls reopens; focus restored and background inert while open | Source self-review; browser focus/continuity NOT_TESTED |
| Message sound | chat.js live partner messages and Home/Memories pairing invalidation incomingMessageId; receipts/self/history excluded | Sound engine + recipient marker automated tests; audible/browser WS NOT_TESTED |
| Incoming melody | call-widget ring(call.id), terminal/accepted-elsewhere cleanup; no copyrighted audio asset | Controlled sound engine tests; audible/autoplay NOT_TESTED |
| Mute all, visual alerts remain | #notification-mute and #call-sound share persistent preference; storage event silences notes; no changes to remote video audio or microphone | Controlled immediate-stop/preferences tests; actual browser multi-tab NOT_TESTED |

Responsive source review covers 390px narrow layout, desktop and reduced motion: 44px controls, mobile icon labels for assistive tech, wrapping footer, scroll for short screens, safe-area padding, focus outline, dynamic participant names via textContent. This is AI source self-review, not a measured visual/accessibility pass. Browser inventory on this task again reports **Local browser use is disabled in Work settings**; native app/headless access was not used to bypass it. Actual graphical/device checks remain pending; no full UI readiness or UAT claim.

Test and deployment results will be appended below. No user-data reset/migration. Drive synchronization remains pending existing authorized destination.

## Local verification

Final npm.cmd run build: BUILD SUCCESS (14.233 s). Local PostgreSQL-backed CallIntegrationTest10 + ChatIntegrationTest8 and RtcConfigurationTest4: 22 passed, no failures. Recipient marker ChatNotificationsTest1: passed. Controlled sound engine4 + media acquisition7: 11 passed. Total 34 scoped checks; these do not prove rendered layout or audible physical media. Evidence: docs/evidence/DEV08-UI01_Verification.txt. Node warns about module type inference in this existing mixed Node/browser project; tests still pass. No test fixtures touched Neon or live accounts.

## Deployed verification

Source a1cd35c337cc5f466a9e05a5ed529b1ee133b8d2 is LIVE on https://couple-private-space.onrender.com (Render dep-dbjldu3bc2fs73d5si1g, 2026-11-01T15:16:28Z). Six served UI modules/styles matched normalized source; ten public health/security/database-lookup checks passed after live. Initial asset comparison during rolling startup saw the previous version; final retest passed. Evidence: docs/evidence/DEV08-UI01_Deployed.txt. Existing free service, database, private storage and TURN configuration preserved.

Owner reported cross-network Voice/Video working before this change; that is user-reported historical evidence, not a graphical pass for this new build. Review now: hard-refresh, enable sounds, send a partner message, answer Video, toggle mic/camera, Back to chat then reopen Calls, mute sounds and verify incoming visual alert without melody on both desktop and phone. Ordinary browser Full screen is optional; full-page layout works without Fullscreen API. Call audio should remain audible while notification sounds are muted.
