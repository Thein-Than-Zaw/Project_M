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

Release: owner-authorized push/deploy in progress. Render will build/package the changed static resources under Java21; post-live source equality and HTTPS health checks will be recorded here. No migrations are added; live schema remains V26. Runtime/DB/storage/TURN configuration stays unchanged.

Recovery: previous Git commit contains the prior frontend resources; rollback would restore the unwanted bell/read-list/narrow-card behavior but needs no database rollback. No live restore/data copy is involved.

Source package will be prepared locally from the final commit, with archive/Git inventory and credential exclusion verification. Existing Drive destination is still unavailable: DRIVE_SYNC_PENDING. Actual phone/browser visual acceptance remains pending separately from explicit deployment authority.
