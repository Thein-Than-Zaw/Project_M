# CALL-UI03 — Laptop call presentation

2 November 2026, Asia/Yangon. Owner supplies the current laptop-to-laptop call screenshot and a reference with a prominent video surface, small self-view and compact floating icon controls, asking for a more polished screen. Existing main base `11ea028`; this is a local, uncommitted presentation increment.

| Owner/reference clause | Implementation | Verification |
|---|---|---|
| C1 Prominent laptop video | Full-page light neutral canvas; rounded stage uses available height; desktop defaults to Fill without stretching video | Source review; actual laptop graphical review pending |
| C2 Compact call controls | Floating dark dock overlays the lower video; mic/camera/end icon buttons retain accessible labels, titles and pressed states | Existing Node regressions64 PASS; markup/ID checks PASS |
| C3 Small self-view | Bounded16:10 self preview at lower-right; moves above dock on narrower laptops; compact-height adjustment | Source review; physical device review pending |
| C4 Clear identity and connection | Small translucent name/status area; actual ACTIVE state drives green indicator; duration retained | Existing state/render paths preserved; no fabricated connection status |
| C5 Reference-style quick controls | Existing fullscreen action moved to a header icon; Back to chat remains connected-call navigation | Unique control IDs/labels and unchanged handler verified |
| C6 Preserve complete video option | Existing ⋯ menu adds Fill video frame toggle; pressed=Fill, unpressed=Fit | Presentation dataset and CSS modes reviewed; no capture/track constraints changed |

All clauses UI_REQUIRED. Reference-only analytics/experience score, extra participants and unrelated tools are outside this bounded two-person call scope. Fill crops edges to avoid large side areas; Fit preserves the whole frame and may show side areas. Mobile initially uses Fit; existing mobile full-page call flow remains, with compact header icons. No popup, new backend endpoints, database migration, media capture/codec/TURN/signaling or call business-rule change.

Changed symbols: `call-widget.js` static header/menu, `render` presentation data attributes, `renderVideoFit`/fit toggle; `calls.css` bounded desktop canvas/overlays/dock/self preview and short/narrow viewport adaptations. Existing Answer/Decline/Cancel/Resume/Play audio/setup cancellation and End actions retain IDs/handlers. Self video stays muted/playsinline; remote remains playsinline. Screen-reader labels remain when visual text is clipped. Incoming/error/reconnect states are retained.

Evidence: JS syntax PASS; all **64 Node tests PASS**, zero failures/skips; HTMLParser verified unique call IDs, all essential control entry points, accessible Back action and self-preview muting/inline attributes; diff check PASS. Local health UP at http://127.0.0.1:18083/; existing preview reads static source directly, so refresh loads this revision without a JAR rebuild. No native tests rerun because backend/media acquisition code is unchanged. No production operation.

Graphical verification **NOT_TESTED**: attempted local browser preview was explicitly blocked because local browser use is disabled in Work settings; no native-browser bypass attempted. Actual laptop fill/crop quality, short-window fit, menu placement and mobile camera playback require owner review. Automated tests do not establish human visual acceptance; review is AI self-review only.

Code IMPLEMENTED LOCALLY; scoped automated checks PASS; human visual acceptance PENDING. No commit/push/deploy for this revision; prior CHAT-07 deployment request covered the earlier autofill change. Source package NOT_DUE for this routine local update. Code Update Record DRIVE_SYNC_PENDING, existing authorized destination unresolved. Recovery: revert these two presentation files; no database or call-state rollback required.

Source identity: call-widget.js SHA256 `501cbc4e757030459fd902451112c96cfbda2df68f15e052754b3c8a9e49cdea`; calls.css SHA256 `00814976a874582200b7ea2e9f84c1f8a7894e99902cda324bb98c25817c5638`. Test evidence `.local/call-ui03-js.log` and `.local/call-ui03-markup.py`; base11ea028 plus uncommitted presentation diff.

Owner subsequently explicitly requests server deployment, superseding local-only status above. Target the existing Render service/main branch without changing backend, schemaV30, storage/TURN configuration or auto deploy OFF. Pre-release repeat: all64 Node tests and call markup/syntax/diff checks PASS (`.local/call-ui03-release-js.log`). No real-user data export/backup/restore. A complete source package is now due and will be prepared locally; authorized Drive destination remains unresolved. Cloud build/live/source verification results follow after execution. Actual physical call visual acceptance remains separate from deployment authorization.

## Deployed result

Application commit **`a6196b3ead663c298c91a52df6eebbdc3a77edfa`** pushed to main. Render deployment **`dep-dbka9v2jnfac73a2dssg`** is **LIVE**, completed `2026-11-02T15:01:00.025073Z` = **2 November 2026, 21:31:00 Myanmar time**. Cloud Java21 Maven BUILD SUCCESS; startup/readiness succeeded. Existing schemaV30 is up to date, no migration necessary. Auto deploy OFF and service configuration retained.

Post-live **6 HTTP/source checks PASS**: readiness200/UP; root200 with HTTPS HSTS/CSP assertions; public call-widget.js and calls.css200 with exact released-source equality after newline normalization; protected Chat page302; guest Chat API401. Evidence: ignored `.local/call-ui03-live-check.log`. Direct connection used because the prior release exposed a local proxy failure; only child-process proxy variables cleared, no Windows network/security setting changed. No production user content accessed/modified. These checks prove runtime/source/access status, not an actual two-device video call or visual acceptance.

Complete source package **PREPARED_LOCAL**, ignored `.local/source-packages/Project_M_Source_CALL_UI03_a6196b3ead66.zip`, **12,557,689 bytes**, **805 files**, SHA256 **`b6710970a2c25dedf9382a7230c48775ac0d8454df495f5679bfd7cefc20661c`**. CRC, full Git inventory/blob identity, required setup/source/migrations and known-credential/private-key exclusion PASS; manifest beside archive. **DRIVE_SYNC_PENDING** because existing authorized destination remains unresolved. Deployment LIVE, JS64 and post-live checks PASS; physical laptop/mobile visual review still PENDING. A documentation-only main follow-up records results; app source staysa6196b3 with no second deploy.
