# GAME-02 — Win animation and fitted in-call board

Code Update Record · 3 November 2026 · Myanmar time · LOCAL ONLY

Owner reports that playing Connect 4 during video calls still needs scrolling, and requests animated You Win / Partner Win feedback. Base main `f1783db`. Approved GAME-01 rules, private board persistence/turns, invitation/rematch, camera/call signaling and existing data contracts retained.

## Root cause and before/after

GAME-01 used fixed absolute top/bottom offsets plus a board width heuristic with a minimum width. Video, headings, status, players and actions could occupy more than the remaining visual viewport, so game panel overflowed and required scroll even with a short board. Winning feedback was only status text/highlighted cells.

The in-call view now uses bounded grid rows (header/video/game/dock on phone portrait; video/game columns on laptop and phone landscape). Min-height0 flex/grid children allow the board area to shrink instead of overflow. ResizeObserver measures the actual card area after visible siblings, their margins, padding and gaps. All42 square discs fit both width and remaining height. requestAnimationFrame coalesces updates; visualViewport height/offset follows browser bars/orientation. No artificial minimum board width forces overflow. Existing call mic/camera/switch/reaction/end dock remains accessible, and call media is not recreated by layout changes. Extreme short views necessarily make discs smaller; real-device acceptance is still pending.

## Acceptance / implemented coverage

- C1: A winning board shows **You Win!** for its winner or **Partner Wins!** for the other participant. DRAW has a static result label; RESIGNED uses the server winner. Popup window/navigation is not introduced: card is an overlay on the board with View board dismissal, and rematch action remains outside it.
- C2: Active→terminal transition on an open/visible board animates card/star/confetti, with18 bounded CSS particles and2.4s cleanup. Repeated polling/snapshots do not replay it. Historical/restored results show a static card. Closing/hidden state, new game, session denial and pagehide dispose transient effects; dismissal persists for that round in this document. Winning cells retain a finite pulse/highlight. No sound or gameplay change.
- C3: Prefers-reduced-motion disables celebratory motion/particles while retaining clear outcome text. Result region is labelled without duplicating the existing live status announcement. Status text is only rewritten on change, avoiding repeat polling announcements.
- C4: Responsive in-call board fits measured width and height, preserving a live partner/self video view and call controls. Portrait and landscape use different grid arrangements. Layout observers handle invitation/turn/result/resign/action/error changes. Standalone board and normal call layout restore their existing dimensions when Games closes or call ends.
- C5: No server/API/schema/dependency changes. Same-document call embedding, receipt rules, active pairing access and optimistic version checks retained. All player/result content uses textContent; generated CSS lengths are numeric measurements only.

Affected symbols: games.js draw/open/embed/confirmation/cleanup + fitConnectBoard/scheduleGameLayout/fitGameView + gameResultPresentation/createGameResultEffects; games.css in-call grid/board/result/finite animations/reduced-motion; games.test fixture context and new game-results-layout regressions.

## Verification / limits

`.local/game02-js.log`: **115 PASS** (existing108 +7 new tests). New checks cover width/height fit across36 phone/landscape/laptop available spaces,42-square-cell geometry, actual control/padding/gap subtraction, visualViewport dimensions/cleanup, one layout per frame, self/partner/draw/resigned labels, one live celebration/dedup/dismissal, historical/reduced motion/static result and effect disposal. Existing Games retry/embedding/turn tests and camera/call sounds/chat scroll tests remain green.

An initial existing embedding fixture lacked the newly called layout scheduler; it was updated with that dependency, then all checks passed. This was a test-context issue, not a server/data failure. Backend native suite not rerun because no Java/API/schema/WS changes; GAME-01 native29 evidence remains separate. Build/packaged-source and local readiness checks follow below.

No actual mobile/browser screenshots or two-device video/game journey were performed here; physical iPhone/Android/laptop viewport, dynamic browser chrome and touch usability UAT remain NOT_TESTED/PENDING. Geometry/controller checks are engineering evidence, not human visual acceptance. AI self-review only.

Human review: open Games during video call, play to victory from both accounts, verify You Win vs Partner Wins animation once; View board keeps winning discs; poll/refresh never repeats historical confetti; rematch clears result. Test phone portrait/landscape with browser bars open/closed, short laptop height, long player names, resign confirmation and errors; entire board plus call dock should remain visible without panel scroll. Reduced-motion device should show static results.

Recovery: revert these frontend files/rebuild/reload; retain schemaV35 and all stored game state. No database export/reset/backup, production setting, Git push or Render deployment in this request. Ordinary local update source package NOT_DUE; Code Update Record DRIVE_SYNC_PENDING because authorized Drive destination unresolved. Local run/build identity follows below.

## Final local checkpoint

`npm.cmd run build` BUILD SUCCESS,50.812s at22:35:00 Myanmar on3 November2026 (`.local/game02-build.log`). JAR SHA256 `9304087b888d1b0ac4d7883e574d31eba4255e2455834fc69bfd1e863e2cb118`. Packaged Games/call/page assets match current source after normalized line endings. Final547-file source/test/scripts manifest `.local/game02-source-manifest.json`, SHA256 `455bac38f429cb367e9fcb4b8187105f08598752e098d24f5deff2eb8c7d9a59`; basef1783db + uncommitted GAME-02. JS115 PASS against final source.

Local preview PID26832 at http://127.0.0.1:18083/ using retained local accounts and schemaV35. Eight HTTP checks PASS (`.local/game02-http.log`): root200, healthUP200, games.js/games.css/call-widget.js200 with source equality, guest Games API401, Chat HTML302, guest Chat API401. Composed page/call/game template IDs remain unique; desktop/mobile Games entries present (`.local/game02-identity.log`). `git diff --check` PASS. No production push/deploy; physical-device visual/media UAT and Drive record sync pending.

## Git publication

Owner subsequently explicitly requested Git push. Publish the verified GAME-02 source/tests/records to main. Earlier no-push statements describe the local checkpoint; tested547-file identity remains unchanged, so recorded build/JS115 evidence is reused. Git publication only: no Render deploy triggered. Physical-device UAT and Drive record sync remain pending; source package NOT_DUE for Git-only publication.
