# DEV08-UI02 — Local UI revision, deployment on hold

1 November 2026, Asia/Yangon. Owner feedback on the live UI01 screenshot: notification controls belong only in Notification Settings; video height is insufficient. Explicit instruction: **do not deploy yet**. This revision is local-only and uncommitted for review. No push, Render deployment/configuration change, database migration or provider change was performed.

Scope: approved existing CALL-01–03 / AC-F06-01–06 and UI01 sounds, revised by these two owner UI instructions. Preserve all call behavior and the confirmed notification rule: mute message/call alerts, retain visual alerts and actual call audio.

| Owner clause | Before | Local revision / entry | Verification |
|---|---|---|---|
| Notification controls only in settings | Floating Sounds toggle on Home/Chat/Memories plus sound link under call | Removed both; My profile → Notification settings (#notifications) holds mute switch, enable playback and always-on visual alert explanation; device-local existing preference preserved | Markup/syntax/package checks PASS; interactive settings/browser cross-tab NOT_TESTED |
| More video height | Separate promotional heading/status, two help lines and footer links reduce stage height | Compact partner-name/status header; video flexes to available viewport; one toolbar; secondary history/connection/help/fullscreen options moved under ⋯ | CSS/source self-review and served asset match PASS; measured desktop/mobile/video height NOT_TESTED |

Two UI_REQUIRED owner clauses, two REPRESENTED in inspected local source, zero graphical PASS / two NOT_TESTED. No new official requirement IDs. Source self-review is not human visual approval or browser evidence. Current graphic/media review remains pending; existing browser tool reports Local browser use is disabled in Work settings. Screenshot supplied by owner shows the previous deployed build and is the before reference, not evidence for this revision.

Usability/source self-review: Notification Settings remains a child of My profile (desktop/mobile selected navigation), with back link, labelled keyboard-operable mute switch, 44px actions, persisted state/error feedback, no notification controls leaking into the call or chat. Call has compact header, existing mic/camera/end accessible icons, Back to chat retains current page, and ⋯ native details keeps secondary controls reachable. Mobile menu is viewport-positioned to avoid clipping; stage uses contain so portrait video keeps its full image. Camera-off, Voice, incoming Answer/Decline, device errors, reconnect/play-audio fallback and recent history remain reachable. Short windows may scroll rather than hide controls. No actual viewport/keyboard/device observation claimed.

Local verification: npm.cmd run build BUILD SUCCESS (16.562 s); existing sound4 + media7 controlled JavaScript tests PASS (11 total); home.js/call-widget.js syntax PASS; markup balance and unique IDs + settings confinement checks PASS; packaged five revised files match source. Local app starts with the existing local PostgreSQL database and no new migrations; eight HTTP health/security/DB-lookup checks PASS, four public JS/CSS routes HTTP200 match revised source. No authenticated UI flow was automated. No live user data was used for tests.

Local preview: http://127.0.0.1:8080/ . After local login, open My profile → Notification settings (http://127.0.0.1:8080/home.html#notifications). Review a video call in two browsers/devices: taller video, compact header, mic/camera/end, Back/reopen, ⋯ options. The local database/accounts are separate from the online database. Source base HEAD is 2e74847; current five-file worktree changes are not deployed. Local JAR SHA256 E16FE16446C019C775A18E3E7FBF0D54A213B4EF742E658EC4781BB40DF4A36A. Raw command logs are retained under ignored .local/dev08-ui02-*.

Next gate: owner visual review of local revision; apply further changes as requested. Deployment remains on hold until explicitly authorized. Human visual approval/UAT not recorded. Existing Drive sync remains pending the authorized destination.

DEV-09A continuation: this local UI revision is now included in the newer inbox build. Its current source/JAR identity and test scope are recorded in DEV-09A_Checkpoint.md; the earlier JAR hash above is historical. Deployment remains HOLD.
