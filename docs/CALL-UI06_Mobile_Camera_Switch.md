# CALL-UI06 — Front / back camera switching

Code Update Record · 3 November 2026 · Myanmar time · local implementation

Owner requested a front/rear camera switch during mobile video calls. Base main `58bc3ba3e8bfdd057f46e3b879ea1d3a9026b61f`; only call-media.js, call-widget.js, calls.css and focused JS tests change. Existing approved stack, call states, TURN, signaling, privacy and duration rules retained.

## Acceptance / implementation coverage

- C1: Selected participant's ACTIVE VIDEO call shows a compact camera-switch SVG beside the camera control after multiple video devices or both facing capabilities are exposed. Voice/ringing/unselected tabs hide it. Known single-camera devices and failed/unknown enumeration hide it. Device changes refresh capability. Multiple front-only/virtual devices may still expose the button; an unsupported rear request fails explicitly instead of silently reusing the same lens.
- C2: Each tap switches `user` ↔ `environment` with exact facing constraint and replaces only the video sender track. It preserves the existing peer/call and microphone, including microphone mute and camera-off state. Preview updates after replacement. Front preview remains mirrored; rear preview is not mirrored. Remote rendering remains uncropped by default.
- C3: Only one switch at a time. Busy icon is disabled/labelled “Switching camera…”, camera toggle temporarily disabled, End call and microphone remain available. Permission/device acquisition is bounded to12s per attempt; late streams are stopped. No extra microphone permission or new call invite/offer/answer is initiated by switching.
- C4: Keep original capture when possible. Only busy-device/abort hardware errors trigger release of the old video source and one retry for exclusive mobile hardware. If the switch fails after release, attempt to reopen the original device ID. Permission refusal/unsupported facing keeps the old capture. If restore also fails, retain audio, show an error and allow Camera on to retry. No downgrade of the call mode or silent switch to voice.
- C5: End/session/peer replacement aborts switching and immediately stops candidate/restoration tracks, even while replaceTrack awaits completion. Stale results cannot attach to the new call/peer. If reconnection interrupts an exclusive-camera switch after old video was released, video may need Camera on retry once ACTIVE again; audio/call recovery keeps existing behavior.
- C6:44px mobile controls with reduced gaps keep the added icon within the existing dock, including320px target width. Desktop multi-camera use is allowed by actual capability rather than user-agent detection. Accessible labels announce front/back action and switching state. Real device visual/interaction coverage remains pending.

`requestCallMedia` adds internal videoConstraints/audio options while retaining existing call setup defaults. `canSwitchCallCamera` checks post-permission capabilities/enumeration. `switchCallCamera` owns acquisition/replacement/fallback/disposal; widget retains call ownership/epoch/peer guards and updates actual media-enabled state. Existing call-media.js route is reused: no new endpoint, security allowlist, dependency, database migration or WS contract.

Engineering references: [W3C RTCRtpSender.replaceTrack](https://www.w3.org/TR/webrtc/#dom-rtcrtpsender-replacetrack), [W3C camera facing constraints](https://www.w3.org/TR/mediacapture-streams/#dom-mediatrackconstraintset-facingmode). Track replacement can reject when renegotiation would be needed; this implementation reports/restores instead of silently renegotiating. Hardware behavior still requires actual-device verification.

## Evidence / review / recovery

`node --test src/test/javascript/*.test.mjs`: **90 PASS** (`.local/call-ui06-js.log`). Includes14 new tests for two-way switching, video-only constraints, mute/off preservation, exclusive camera retry, original-device restoration, permission/facing rejection, replaceTrack rejection, late stream/timeouts, call/peer cancellation, immediate disposal during pending replacement, failed restoration, capability visibility and actual widget single-flight/stale-result guards. Existing76 JS checks remain green. Controlled media/peer tests are not evidence of an actual iPhone camera or cross-network video call.

Backend/native tests not rerun: no Java/security/API/schema/WS changes. Build compiles/packages the current app; results and local identity follow below. Graphical/iPhone Chrome/Android/two-device media acceptance NOT_TESTED in this session. Reviewer is AI self-review; human acceptance pending. No Git push or production deployment in this scope.

Recovery: revert these frontend files, rebuild/reload; schema remainsV33. No account/media storage operations, database backup or runtime secret changes. Ordinary local update: full source package NOT_DUE; Code Update Record DRIVE_SYNC_PENDING because authorized Drive destination remains unresolved. Continue authorized local work without treating Drive status as deployment authority.

Human review: local secure-context video call with another participant; switch front→rear→front and verify both previews/partner video, no audio interruption or new call-history item; switch with mic muted and camera off; rapidly tap switch, end while opening, and reject camera permission. Test iPhone Chrome and Android on HTTPS before claiming device acceptance.

## Final local checkpoint

`npm.cmd run build` **BUILD SUCCESS**,12.474s at16:29:07 Myanmar time on3 November2026 (`.local/call-ui06-build-final.log`). JAR SHA256 `b6669ab9205757bbed15e20259aa1dbc397427de7f0782c8263d3008feb5bad5`. Three packaged runtime assets match current source after line-ending normalization; four-file changed source/test hashes in `.local/call-ui06-source-manifest.json`. Final JS90 PASS; CSS subsequently adjusted only to44px controls/gaps for narrow mobile dock. Source/code identity is base58bc3ba plus uncommitted CALL-UI06 diff.

Local app PID19220 at http://127.0.0.1:18083/ using existing local users/settings. **6 HTTP checks PASS** (`.local/call-ui06-http.log`): root/healthUP, call-media.js/widget.js/calls.css served200 with source equality, guest RTC-config401. V33 unchanged; no migration. `git diff --check` PASS. Existing preview restarted; sign-in may be needed. Production not updated, no Git push/commit in this request. Source delivery NOT_DUE / local record DRIVE_SYNC_PENDING / real-device visual and media UAT pending.
# Manual release preparation continuation

Owner requested manual deployment; combined CALL-UI06/CHAT-08 build and main push prepared. This supersedes the earlier no-push status; no agent-triggered server deployment. See [manual release record](Release_CALL_UI06_CHAT08_Manual.md). Source package PREPARED_LOCAL / DRIVE_SYNC_PENDING; actual-device acceptance remains pending.
