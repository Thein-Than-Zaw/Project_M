# CHAT-08 — Own send scrolls to latest message

Code Update Record · 3 November 2026 · Myanmar time · LOCAL ONLY

Owner reports mobile Chat remains at older reading position after sending and incorrectly shows New messages for their own message. Authorized change: own send immediately reveals the outgoing message at the bottom; incoming/background changes retain the existing older-message reading position.

Base main `58bc3ba3e8bfdd057f46e3b879ea1d3a9026b61f`, alongside uncommitted CALL-UI06 camera switching. Preserve that separate work. Scope: chat.js rendering fast path and submit focus, plus four focused JS regressions. No Java/API/schema/signaling/receipt changes.

## Root cause / change

`transmit()` first renders the new pending message with normal mode. The submit handler subsequently calls `render('bottom')`, but its identity matches the already-rendered outbox. The unchanged-identity early return ignored the requested scroll, retaining the previous reading position and New messages banner.

The unchanged-content fast path now honors explicit bottom mode, sets scrollTop to scrollHeight, hides New messages and still queues visible-message receipts. It does not rebuild the DOM. Existing text, sticker, attachment and reply-recovery send paths already request bottom mode and share the fix. Input focus after sending uses preventScroll so browser focus does not undo deliberate conversation alignment. Ordinary background renders still preserve reading position; no forced scroll is added to incoming message/edit/reaction/receipt updates.

## Acceptance / coverage

- C1: Sending while reading older messages reveals own outgoing bubble immediately, including Sending state; New messages clears.
- C2: Same-content render optimization retains its DOM/media-preservation behavior while executing explicit bottom actions.
- C3: Existing text/offline, sticker and attachment send paths benefit from the same bottom action; send confirmation rules unchanged.
- C4: Incoming/background updates do not pull the reader away; users already at bottom continue following updates. Existing prepend/history behavior retained.

`node --test src/test/javascript/*.test.mjs`: **94 PASS**, `.local/chat08-js.log` (includes CALL-UI06's90). Four new tests execute the actual render/transmit functions: duplicate transmit→bottom ordering, offline/text/gift/file pending state, same-content reading preservation, and changed-background/at-bottom behavior. Native backend tests/build not repeated for this two-line frontend behavior fix; no backend/schema contract changes. Previous camera build remains recorded separately; JAR must be rebuilt before a later deployment to include this Chat change.

Local preview uses source static assets at http://127.0.0.1:18083/. `.local/chat08-http.log`: **7 HTTP checks PASS**, including current chat.js/css source equality, root/health and guest access protection. Browser/mobile keyboard interaction and actual iPhone visual acceptance NOT_TESTED here; controlled JS checks are not device UAT. AI self-review only.

Human review: scroll up in Chat, type and Send; verify own bubble appears at bottom immediately and New messages is hidden. Then scroll up and receive a partner message; verify reading position stays. Repeat with keyboard open, offline Sending state and sticker/file send.

Recovery: revert the affected render/focus lines and reload local static assets; no DB restore/migration. Production/Git unchanged in this request. IMPLEMENTED LOCALLY / scoped tests PASS / physical-device UAT PENDING. Ordinary local update source package NOT_DUE; record DRIVE_SYNC_PENDING because authorized Drive destination remains unresolved.

Tested chat.js SHA256: `ff90ab932bdb3e403aa98a72bab6dd25814ef86c4fbb8a5a24aa107fdf5d532a`. Includes uncommitted CHAT-08 over base58bc3ba; camera changes remain separate in the same working tree.
# Manual release preparation continuation

Owner requested manual deployment. Combined final build includes the scroll fix plus CALL-UI06; agent prepares main push while owner performs Dashboard deploy. See [manual release record](Release_CALL_UI06_CHAT08_Manual.md). Earlier local-only/no-build/no-push statements describe that earlier checkpoint; current release has successful combined build and source-package preparation, server deployment pending owner action.
