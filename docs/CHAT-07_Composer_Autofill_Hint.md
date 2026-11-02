# CHAT-07 — Chat composer autofill hint

2 November 2026, Asia/Yangon. Owner reports the mobile keyboard's Key/Card/Address accessory bar reduces the chat viewport and asks to try suppressing it. Base main `184b977` (application release2d3294c); final working source is an uncommitted HTML-only change.

C1/UI_REQUIRED: `/chat.html` composer form and message textarea now both specify standard `autocomplete="off"`. Preserve the textarea, accessible label, Myanmar input/IME, keyboard suggestions/spelling defaults, send/Enter behavior, viewport handling, Login password-manager behavior and media controls. No hidden fake inputs, security-setting changes or replacement contenteditable editor.

Evidence: HTMLParser verified both autocomplete hints and retained textarea/accessibility link; existing Chat responsive/sending/media Node tests **28 PASS**, zero failures/skips; diff check PASS; local health UP at port18083. Existing local preview serves static files directly from the source directory, so refreshing uses this HTML change without replacing the running JAR.

Limit: this is a browser hint, not control over the native keyboard accessory UI. Actual affected phone/browser result is **NOT_TESTED**, pending owner review. Browser may ignore this hint or keep its toolbar; no success hiding it is claimed. Backend/database changes and migrations: NONE. No new tests mirroring the simple markup were added. Routine local update, source package NOT_DUE; Code Update Record Drive sync DRIVE_SYNC_PENDING (destination unresolved).

Release: LOCAL ONLY; no commit/push/deploy for this change. The previous deployment request applied to the already deployed CHAT-05/06 version. Online remains2d3294c until a further deployment is requested. Recovery: remove the two autocomplete attributes and refresh the local page.

Owner now explicitly requests server update, superseding the local-only checkpoint above. Release scope remains the two HTML autocomplete hints; no backend/database/configuration changes. Existing28 passing focused checks apply to unchanged source. A complete source package is now due for deployment and will be prepared locally; Drive sync remains pending separately. Cloud build/live outcomes follow after execution. Actual mobile toolbar behavior remains pending owner device review.

## Live update

App commit `3eed567bd01b61d0ac3104fb213eca3647339ac4` pushed to main; Render deployment `dep-dbk8s58ae00c738656o0` **LIVE** at `2026-11-02T13:24:02.715293Z` (**19:54:02 Myanmar time**). Cloud Maven BUILD SUCCESS; runtime startup succeeded. Existing schemaV30 validated, no migration necessary; service configuration/auto deploy OFF retained. Deployment authorization is fulfilled; actual affected-phone toolbar result is still NOT_TESTED.

Final public checks **4 PASS**: readiness200/UP, root200 with HTTPS HSTS/CSP checks, protected Chat page302, guest Chat API401. Initial Node/proxy requests timed out and the native Windows proxy request failed. Direct native request then returned200/UP; process-only removal of proxy variables allowed the final Node checks to pass. No Windows proxy or TLS/security setting changed. Evidence: ignored `.local/chat07-live-check-final.log`. Browser verification unavailable because local browser use is disabled in Work settings; no bypass attempted. No actual account content accessed or changed.

Complete source package PREPARED_LOCAL: ignored `.local/source-packages/Project_M_Source_CHAT07_3eed567bd01b.zip`, 12,552,450 bytes, 804 files, SHA256 `fdfe013ff5d0279508c8a829ed971550e16f3c0a8531e0fb49d728395802918d`. CRC, full Git inventory/blob identity, required source/setup files and known-credential exclusion PASS; manifest beside archive. DRIVE_SYNC_PENDING remains because the existing authorized destination is unresolved. A documentation-only main follow-up records these results; no second app deploy is required. Full native105/JS64 release evidence belongs to unchanged prior backend/JS; this HTML increment uses its own28 focused checks, not a new full-suite claim.
