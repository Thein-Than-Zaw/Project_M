# CHAT-04 — Notification entry, unread list and laptop layout

2 November 2026 (+06:30). Owner requested all three UI corrections and immediate server deployment. Base main: `f6a9d3449b06c6897368cb5aceb1ae5185c21dc6`. Prior deployment authorization remains applicable; no production backup or data export is requested or performed.

| Clause | Before → after | Implementation |
|---|---|---|
| C1 | Two bell-like Chat icons → retain only sound mute toggle | `notificationEntryRoots` returns no inbox entries on Chat; Home/Memories header/mobile entry points remain |
| C2 | Read/all notifications in list → unread notifications only | Always request `unreadOnly=true`; remove All/Unread toggles; render guard excludes non-null readAt; Mark read/Open retains existing confirmed-write/refresh behavior |
| C3 | Laptop partner card narrow → slightly wider card and narrower conversation | Desktop card 250→300px; medium laptop 210→250px; existing ≤1040px hidden-card and mobile layout unchanged |

All three owner clauses are UI_REQUIRED and represented. No database/API/security/notification retention change: historical read rows remain stored, and notification read state remains distinct from Chat read receipts. No user data or message content is deleted by this fix.

Verification: JavaScript syntax PASS; all 50 Node tests PASS, including two new regressions using the actual mount/filter helpers. NotificationIntegrationTest: 8 PASS, no failures/errors/skips; Maven BUILD SUCCESS in 58.692s at 02:21:33 Myanmar time. Existing backend test covers read→excluded-from-unread-query, read/unread persistence and ACL/CSRF. Previous full native 98-test evidence remains applicable to unchanged backend; it was not rerun for this frontend-only fix.

Graphical verification: NOT_TESTED for these three revised clauses. Browser tool explicitly reported local browser use disabled in Work settings; no native browser bypass was attempted. Isolated synthetic fixtures were prepared but no actual browser journey ran. Tests are not a claim of human visual acceptance.

Release LIVE: app commit `e61e419e26ca286f41a12d777fb9cb75fa01f008` pushed to main; Render deploy `dep-dbjphdnf3r2c73cpk5ng` completed at `2026-11-01T19:56:20.876839Z` (2 November 2026 02:26:20 Myanmar time). Cloud Maven BUILD SUCCESS; runtime startup succeeded. No migrations are added; live schema remains V26. Runtime/DB/storage/TURN configuration and auto-deploy OFF stay unchanged.

Post-live checks: 10 HTTPS/security checks PASS, 12 public JS/CSS files HTTP200 + normalized source equality against e61e419 PASS, and 3 private endpoints guest HTTP401 PASS. These verify deployment/source/security, not a graphical inbox journey. Isolated synthetic fixtures were cleaned and the isolated preview server stopped; actual online user content was not read/exported or modified by these checks.

Recovery: previous Git commit contains the prior frontend resources; rollback would restore the unwanted bell/read-list/narrow-card behavior but needs no database rollback. No live restore/data copy is involved.

Source package for the deployed commit is prepared locally: ignored `.local/source-packages/Project_M_Source_CHAT04_e61e419e26ca.zip`, 12,515,948 bytes, 791 files, SHA256 `045577342bb376ff0d421efc49029e65cbdc52a57b609bfb8b9679bca8fd9538`. CRC, exact Git inventory/blob identity and known-credential exclusion PASS. A final documentation-only main commit may follow without another app deployment; application source remains unchanged. Existing Drive destination is still unavailable: DRIVE_SYNC_PENDING. Actual phone/browser visual acceptance remains pending separately from explicit deployment authority.
