# CALL-UI05 — Taller mobile self-view and live call reactions

2 November 2026, Asia/Yangon. Owner requests slightly taller phone self-video and Google Meet-style flying emoji reactions during calls. Existing Spring Boot/STOMP/WebRTC architecture retained; this is a local increment after deployed CALL-UI04.

| Clause | Production entry / behavior | Evidence |
|---|---|---|
| C1 Taller mobile self-preview | Mobile ≤760px uses square self-view, width31%, max150px; short landscape viewport uses bounded4:3. Desktop self-view and remote default Fit retained. | CSS review; actual iPhone visual acceptance pending |
| C2 Choose a call reaction | Smile icon in connected-call dock opens six emoji choices: ❤️ 😂 👍 👏 🥰 🎉. Keyboard access, outside dismissal, expanded state and non-modal Escape handling. | Template ID/accessibility checks and actual-source tests |
| C3 Both selected participants see floating effects | Existing private call socket carries transient emoji to accepted partner session; sender animates only after command acknowledgement. Three small floating glyphs per reaction, automatically removed. | Native real-cookie/STOMP partner-tab routing tests and effect lifecycle tests |
| C4 Keep calls usable and private | Selected-client/session, ACTIVE state, current negotiation, fixed emoji allowlist; six reactions per10 seconds per selected socket, separate from ICE budget. No history/chat persistence or notification sound. | Native invalid/stale/other-tab/dedup/rate/state tests |
| C5 Respect motion preference and lifecycle | Reduced-motion preference gives one still emoji; max18 glyph nodes, timers bounded/cleared on call end, exit and pagehide; stale/hidden/wrong-call events cannot replay. | Node reduced-motion/cap/cleanup/routing regressions |

All five clauses UI_REQUIRED, backed by server-side scope checks for C3/C4. Six emoji choices, short effect duration/cap and rate bound are engineering defaults for this owner-requested feature; no human approval of numeric limits is invented. Reactions work during an active voice or video call on the existing shared call surface. Ringing, connecting and reconnecting do not accept new reaction effects. Default uncropped partner-video and fullscreen icon fix remain intact.

## API / WS contract

Existing authenticated `/app/call.reaction` command adds optional `emoji` to the existing Command DTO:
`{eventId, callId, clientId, negotiationId, emoji}`. Event IDs are UUIDs. Server validates current selected actor/socket/client and negotiation, whitelists emoji, enforces ACTIVE state and six/10s cap. Existing `call.ack` / `call.error` reply format retained; applicable codes include INVALID_CALL_REACTION, CALL_REACTION_LIMIT, CALL_STATE, CALL_ELSEWHERE and STALE_NEGOTIATION. Terminal calls retain existing command semantics: return terminal metadata with no effect relay.

After transaction commit, only the selected partner session receives
`{eventId,type:"call.reaction",callId,negotiationId,payload:{emoji}}` on `/user/queue/calls`. No broad queue, caller-supplied recipient or database reaction row. Existing short-lived relay dedup handles identical retries and conflicts. Sender displays on ACK, partner on scoped relay; neither replays effects through REST/history. No schema migration.

Changed sources: CallService Command/relay validation/rate path; exact PairingSocketConfig destination allowlist; SecurityConfig public static module; call-widget picker/send/receive/lifecycle; new call-reactions.js effect helper; calls.css mobile sizing/effects. Existing authentication, call deadlines, media tracks, capture constraints, TURN/storage and notification settings remain.

## Verification and limits

Final JavaScript suite **71 PASS**, zero failures/skips, syntax and call-template unique-ID/inline-video/accessibility checks PASS. Includes bounded effects, reduced motion, malicious emoji rejection, disposal and current-call/tab/negotiation/no-replay checks. Native CallIntegrationTest **12 PASS**, zero failures/errors/skips, BUILD SUCCESS in1:06 at22:24:18 Myanmar time: all10 prior call tests plus2 reaction tests exercising real HTTP sessions and STOMP. Initial run caught the missing exact STOMP destination allowlist (two new tests failed with no reply); the route was added without broadening other destinations and all12 then passed. Tests were not weakened. Native tests use only the dedicated local `_test` database and synthetic data; no actual online account content accessed.

Final local build/start/HTTP identity follows below. Actual iPhone sizing, touch-picker interaction and two-device animation smoothness remain **NOT_TESTED** pending owner review; browser use is disabled in this session, no bypass attempted. Native signaling and controlled effects tests do not prove hardware/media or human acceptance. AI self-review only.

Status: IMPLEMENTED LOCALLY; scoped automated checks PASS; physical UX acceptance PENDING; no Git push/deploy for this feature yet. Source package NOT_DUE for routine local increment; Code Update Record DRIVE_SYNC_PENDING because authorized existing destination is unresolved. Recovery: revert this increment's frontend/backend sources together; no data migration/rollback required.

## Final local checkpoint

Base `95403a7ee717f46c2998dbd948ed162e6847afd4` plus uncommitted CALL-UI05 sources/tests. Final `npm.cmd run build` BUILD SUCCESS,10.036s at22:25:45 Myanmar time (`.local/call-ui05-build.log`). JAR SHA256 `0ef08ea294a02600d7d3dd1e8cfd70c56e921610ad1d47583cef31f91b67234a`. Source/test/scripts manifest `.local/call-ui05-source-manifest.json` SHA256 `8d302d207741f60f7e1748f1d185136fbe738277670efa1386bd144f2fe707a5`. Node71 and native12 evidence belongs to this source; no full-project native-suite claim is made.

Final app running at http://127.0.0.1:18083/ with local PostgreSQL, Java PID24880, launcher11848. **6 local HTTP checks PASS**: healthUP/root200, call widget/reaction module/CSS200 with normalized source equality, guest current-call API401. `.local/call-ui05-local-http.log` and `.local/call-ui05-app.log`. Existing local accounts preserved; restarting the JVM may require signing in again. Online production is unchanged at applicationc90b401. Owner can refresh locally, connect both call clients, then use the smile button beside the camera/end controls to try reactions. No hardware permission was granted on the owner's behalf.

Owner now explicitly requests server deployment, superseding local-only status above. Deploy the same tested CALL-UI05 sources to the existing Render service/main branch, with no migration/schemaV30 change, credential change or user-data export/restore. Source-manifest equality is checked before publication; existing JS71/native12/local-build evidence applies. Exact private reaction route remains the only STOMP authorization addition. A complete source package is now due; prepare locally with Drive sync separately pending. Deployment/source checks follow after execution; actual phone/two-device graphical acceptance is not inferred from deployment authority.
