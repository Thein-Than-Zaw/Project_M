# CALL-UI07 — Outgoing/end sounds and peer microphone badge

Code Update Record · 3 November 2026 · Myanmar time · LOCAL ONLY

Owner requests audible calling feedback, hang-up/unanswered sounds, and a visible icon when the partner mutes their microphone. Base main `2a882363d4ae290cc3efb6a47fef123af090a4a4`. Preserve CALL-UI06 camera switching, CHAT-08 scrolling, existing private WebRTC, signaling and notifications. Scope: notification-sounds.js, call-widget.js, calls.css, focused JS tests.

## Acceptance / design / implementation

- C1: The selected caller hears repeating ringback after the server confirms RINGING. Stop it on acceptance/cancel/end/session cleanup. Repeated polling or socket state does not duplicate timers. Incoming ringtone is retained; ordinary message/preview chimes do not interrupt call ringing.
- C2: Current participating call transitions to ENDED/CANCELLED play a short descending tone; MISSED/DECLINED/FAILED play a distinct three-note unavailable tone. Do not play history, another tab's connected call, unrelated call IDs, repeated terminal events, or post-expiry/navigation outcomes. A ringing recipient can hear missed/cancelled/declined outcomes. Bounded memory + shared last-call-end marker suppress repeated same-call sounds across events/tabs. A newer call ringtone takes priority.
- C3: Existing Mute all sounds and0–100 volume control apply to ringback/outcomes. Muted/blocked outcomes are consumed rather than replayed on later unmute/enable. Explicit Call/Answer gesture unlocks the existing audio engine without changing mute/volume; permission/autoplay restrictions may still silence it. Synthesized oscillator tones only, no external music asset/license/download or new setting required.
- C4: A visible mic-off SVG + “Microphone muted” badge appears over the remote stage while the partner's validated media signal says micEnabled=false. It works with video visible or voice/avatar view, clears on unmute/end and stays hidden for ringing/unselected tabs. This reflects the app microphone toggle, not undetectable OS/hardware mute.
- C5: Existing selected-call/current-negotiation media validation and initial/reconnected media-state sends are retained. No SDP/candidates/media are persisted. If the terminal socket event was lost, existing authenticated GET call metadata recovers the final outcome before applying the terminal transition. Stale IDs/versions retain existing apply guards. No additional WS payload/API/schema contract.

Root cause: audio engine previously supported incoming ringing/message/sent sounds only; outgoing state never started ringback and terminal transitions only cleaned media. Peer mic text existed inside the avatar fallback, which video hides. New tones reuse the existing sound preferences/engine; peer badge sits directly on the stage outside that hidden fallback.

## Verification / review

`node --test src/test/javascript/*.test.mjs`: **103 PASS**, `.local/call-ui07-js.log` (existing94 +9 focused tests). Added tests exercise outgoing repeat/stop, distinct outcome/dedup across tabs, mute/zero/volume/no replay, new-ring precedence, gesture enable, participant/history terminal gating, visible badge lifecycle, actual media signal negotiation/call/dedup validation and lost-event REST recovery. These are controlled JS tests, not actual hardware/audio-output UAT. Native backend suite not repeated because Java/security/schema/API unchanged; build still compiles/packages project.

Physical iPhone/Android audio audibility, browser gesture/background behavior and two-device mic indicator UAT NOT_TESTED. Human review pending; AI self-review only. Verify caller ringback stops when partner answers; end/cancel/miss/decline once each; refresh old history stays silent; mute all/volume0 suppress sounds but preserve badge; toggle partner mic with video visible and verify badge clears on unmute.

Recovery: revert changed frontend assets/rebuild/reload; schemaV33 unchanged, no database restore. No production secrets, storage objects or customer data touched. No commit/push/deployment in this request. Ordinary local update source package NOT_DUE; local Code Update Record DRIVE_SYNC_PENDING because authorized Drive destination unresolved. Local runtime/build identity follows below.

## Final local checkpoint

Combined app `npm.cmd run build`: BUILD SUCCESS,14.174s at20:15:10 Myanmar time on3 November2026 (`.local/call-ui07-build.log`). JAR SHA256 `a2306ecd36648e9c147a4bf81ae0545ea6e198419cf38715d96db9c8ae5ede8b`. Three packaged runtime feedback assets match current source after line-ending normalization. Five-file changed source/test manifest `.local/call-ui07-source-manifest.json`; base2a88236 + uncommitted CALL-UI07 diff. JS103 PASS against current source; no full native-suite/device claim.

Local preview PID6100 at http://127.0.0.1:18083/ with existing accounts/settings; refresh/sign-in may be needed. Six HTTP checks PASS (`.local/call-ui07-http.log`): root200, healthUP200, sounds/widget/CSS200 with source equality, guest RTC-config401. `git diff --check` PASS. DatabaseV33/production unchanged. Owner physical-device/audio UAT PENDING; Drive record sync PENDING.

## Git publication

Owner subsequently explicitly requested Git push. Publish the verified CALL-UI07 source/tests/records to main; earlier no-commit/no-push wording describes the local checkpoint. Tested five-file identity unchanged, JS103/build/source checks reused. This request authorizes Git publication only; no Render deployment triggered. Source package remains NOT_DUE for this Git-only update; Drive record sync and device UAT remain pending.
