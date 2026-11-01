# DEV08-FIX01 — visible, cancellable Video Answer device setup

1 November2026, Asia/Yangon. Parent F06 CALL-01–03 / AC-F06-01/03/05 and permission/device/error UI. Owner reports same-computer Voice works, Video rings but Answer appears inactive and shows no visible message. This is a scoped defect repair; no changed modes, automatic voice fallback, ring/recovery rules, migration, TURN provider or real-data reset.

## Observations versus diagnosis

- Read-only last-call metadata shows VOICE ENDED/accepted=true/connected=true, and VIDEO DECLINED/MISSED/CANCELLED with accepted=false/connected=false. No names, SDP, ICE, device IDs or user media collected. The reported video attempts did not reach backend acceptance. This narrows the investigation to device setup/Answer handling before `accept`; it is not proof of a specific camera driver failure.
- A single camera can be unavailable to a second browser depending on permissions/device contention; official [Microsoft Edge guidance](https://learn.microsoft.com/en-us/troubleshoot/microsoft-edge/development/edge-camera-microphone-not-working) describes those failure causes. [MDN getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia) also documents indefinitely pending permission requests and device failures. No actual browser/device error was accessible to the agent: browser tools remain disabled in Work settings.
- Source defects: Answer waited indefinitely for getUserMedia, pending device acquisition could outlive call cancellation, and failure text above a scrolled media stage could remain outside the visible area. Those are repaired without asserting that the user's hardware issue is proven or real video now passes.

## Changes

New static call-media.js encapsulates cancellable device acquisition, progress hint after6s, outgoing setup cap30s and incoming cap at the existing ring deadline (never extend/change the confirmed30s ring). Permission/hardware messages preserve error reason and explain busy devices/permission settings. Cancel rejects immediately; if the browser later grants a stream, every returned track is stopped. It does not pretend to dismiss the browser's own permission prompt.

call-widget.js shows Opening microphone/camera directly on Answer, provides Cancel device setup without silently declining the call, reveals/focuses Answer failure text, aborts pending media on cleanup and uses setup generations to ignore stale completions. Guards prevent expired/cancelled setup from opening devices after delayed config. Voice still requests no camera; Video still requires the requested video stream and never silently becomes voice. Existing Call/Answer consent and selected-tab controls remain.

SecurityConfig permits GET of the new public code module, containing no private data/secret. CallIntegrationTest checks that it actually loads so a denied static module cannot break calls. No Java call service/schema contract changed.

## Actual verification / identity

- `node --test src/test/javascript/call-media.test.mjs`: **7 PASS**,0 failed. Tests cover voice/video constraints, hung permission timeout, cancellation, late-stream track disposal, pre-cancelled requests, busy-device/no-fallback, permission refusal, progress and late rejection. These use controlled fake device promises/tracks: they test JavaScript control/resource behavior, not real RTP/hardware.
- Load scripts/local-env.ps1, TEST_DB_URL with `?currentSchema=dev07_fresh_20261101`; `mvnw.cmd -B -ntp -Dtest=CallIntegrationTest,RtcConfigurationTest test`: **12 PASS**,0 failures/errors/skips;55.493s, ended15:57:19+06:30. Existing selected-tab/role/race/retry/timeout/credential tests remain intact. Previous65-test full regression belongs to the preceding DEV-08 build; it was not rerun or relabeled as final for this bounded frontend fix.
- Node syntax check call-widget.js PASS. `mvnw.cmd -B -ntp -DskipTests package`: BUILD SUCCESS,11.973s,15:59:07+06:30. Evidence DEV08-FIX01_media-tests.txt, native-tests.txt, package.txt.
- Base repository main658251c9c12dcf10092d9f0cabfe6ca4efa34a69 with earlier uncommitted scope retained; predecessor DEV-08_source-manifest.json. Current478 source files,5 changed (2 added/3 modified); source identity **0dc8ec97b5a8d27a1d10eeaa438ba8361738923ef7c8a8d8128a2bc8642e8dce**. JAR SHA256 **f2f0d01e164b89974669ee48df04e382bd0c574660b67601a63ec3a7e360a38d**. Manifest DEV08-FIX01_source-manifest.json; packaged widget/new media module/resources match source.

## Review, run and remaining limits

Refresh both account/browser pages and sign in again after restart. Retry Video Answer. It should visibly show device opening, a cancellable wait, then either acceptance or a useful visible error. If the second browser reports a busy camera, close other camera apps or use independent cameras/devices. Current loopback app is not automatically accessible to a phone: two-device testing still needs deliberately configured trusted HTTPS/network access. No camera source is silently changed by this fix.

Observed Voice success is owner-reported on one computer. Real Video Answer, device-sharing behavior, two-device/two-network, forced TURN, mobile/focus/scroll/hardware-release results remain NOT_TESTED by the agent. No new screenshot or audio/video PASS fabricated. Root hardware/browser cause remains unconfirmed pending the improved visible error/user retest. AI source self-review only; human UAT/release unrecorded.

No migration/data cleanup/fixture write in the real space, new Git push or external upload. Existing DB/private files retained. Local Code Update Record saved; Drive destination unknown, DRIVE_SYNC_PENDING; ordinary source package NOT_DUE. Recovery: revert this scoped helper/widget/module-allowlist change coherently and rebuild; no DB rollback needed. That reversion would restore the unbounded-device-wait defect.

Final local readiness/source match is saved as DEV08-FIX01_runtime-identity.txt. Full DEV-08 media acceptance is still pending.
