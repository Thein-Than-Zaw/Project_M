# CHAT-07 — Chat composer autofill hint

2 November 2026, Asia/Yangon. Owner reports the mobile keyboard's Key/Card/Address accessory bar reduces the chat viewport and asks to try suppressing it. Base main `184b977` (application release2d3294c); final working source is an uncommitted HTML-only change.

C1/UI_REQUIRED: `/chat.html` composer form and message textarea now both specify standard `autocomplete="off"`. Preserve the textarea, accessible label, Myanmar input/IME, keyboard suggestions/spelling defaults, send/Enter behavior, viewport handling, Login password-manager behavior and media controls. No hidden fake inputs, security-setting changes or replacement contenteditable editor.

Evidence: HTMLParser verified both autocomplete hints and retained textarea/accessibility link; existing Chat responsive/sending/media Node tests **28 PASS**, zero failures/skips; diff check PASS; local health UP at port18083. Existing local preview serves static files directly from the source directory, so refreshing uses this HTML change without replacing the running JAR.

Limit: this is a browser hint, not control over the native keyboard accessory UI. Actual affected phone/browser result is **NOT_TESTED**, pending owner review. Browser may ignore this hint or keep its toolbar; no success hiding it is claimed. Backend/database changes and migrations: NONE. No new tests mirroring the simple markup were added. Routine local update, source package NOT_DUE; Code Update Record Drive sync DRIVE_SYNC_PENDING (destination unresolved).

Release: LOCAL ONLY; no commit/push/deploy for this change. The previous deployment request applied to the already deployed CHAT-05/06 version. Online remains2d3294c until a further deployment is requested. Recovery: remove the two autocomplete attributes and refresh the local page.

Owner now explicitly requests server update, superseding the local-only checkpoint above. Release scope remains the two HTML autocomplete hints; no backend/database/configuration changes. Existing28 passing focused checks apply to unchanged source. A complete source package is now due for deployment and will be prepared locally; Drive sync remains pending separately. Cloud build/live outcomes follow after execution. Actual mobile toolbar behavior remains pending owner device review.
