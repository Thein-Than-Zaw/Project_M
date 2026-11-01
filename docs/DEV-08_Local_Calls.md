# Local Voice/Video calls and relay setup

Current architecture: existing session/CSRF + STOMP authenticates call metadata/signals; RTCPeerConnection carries the real audio/video. The server stores state/outcomes, not SDP/ICE/media recordings. Open the app with the two existing accounts in separate browsers. Home Voice/Video buttons, Chat phone/camera icons, and the floating Calls panel start or receive calls. Allow devices only when calling/answering. Voice requests microphone only. Video requests microphone/camera. End turns off local tracks immediately even when signaling is reconnecting; server outcome reconciles separately.

Same-PC direct calls use http://127.0.0.1:8080/ as a secure localhost context. A plain LAN HTTP URL is not sufficient for browser device access; phone/two-network testing needs trusted HTTPS plus deliberately configured listen/origin/network settings. No public hosting/firewall change was made for this increment.

When TURN_URLS is empty, `/api/rtc/config` returns no relay and the UI states direct/local limitations. This is not a promise of cross-network connectivity. With an approved relay, export comma-separated TURN_URLS, optional STUN_URLS, and server-only TURN_SHARED_SECRET (32–256 characters); authenticated config issues1-hour username/HMAC-SHA1 credentials. Browser code never receives the permanent shared secret. `relayConfigured` reflects configuration presence, not a successful health or relay test.

## Optional same-machine coturn container

This machine has no Docker engine/CLI and WSL reports not installed. The following source setup is prepared but **NOT RUN**; native credential tests do not prove a working container or media relay. It does not install Windows features or buy a provider.

With a working Docker engine:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-turn-local.ps1
```

The script creates ignored `.local/turn.env` with a random secret and starts `ops/turn/compose.yml` using coturn/coturn4.18.0-r0. Image version follows the [upstream image changelog](https://github.com/coturn/coturn/blob/master/docker/coturn/CHANGELOG.md); pull/digest/runtime are not verified here. It binds3478TCP/UDP and49160–49200UDP on127.0.0.1 only and deliberately permits loopback peers for this isolated relay exercise. The advertised relay address is127.0.0.1; this config is exclusively for both browsers on the same computer, not a LAN/production template. `scripts/local-env.ps1` loads only the three named TURN/STUN settings from that ignored file. Restart the app after successful relay startup.

Open the same page with `?relay=1` in both browsers for a forced-relay exercise. Without configured TURN, starting a relay-only call gives a clear error. A real test must inspect getStats selected candidate pair and verify relay candidates plus increasing inbound/outbound RTP bytes and audible/visible peer media. Candidate gathering alone is not success. No forced-relay PASS is claimed until observed.

## User journey checks

- Caller A starts Voice/Video; only after permission and a committed invite does partner B ring. Decline/Cancel/30s missed/offline/busy explain actual outcome.
- B answers in one tab; other tabs stop ringing and receive no SDP/ICE. Connected/timer starts only after both selected clients report their actual RTCPeerConnection connected state. The server cannot independently observe RTP.
- Mute microphone and camera must alter actual track.enabled; confirm remote silence/black camera output as well as indicators. End/logout/navigation must stop local/remote tracks, close PC and timers. Minimize keeps that same page's call alive.
- Interrupt network/signaling; allow up to15s recovery, then failed outcome/cleanup. A different tab cannot steal an active selected socket. A same-page socket reconnect can rebind after the old socket closes.
- Test autoplay-blocked audio recovery, denied/missing/busy devices, browser reload, backend restart, desktop and mobile overflow/focus. Call history uses Myanmar time and server-connected timestamps.

Graphical browser/media checks are currently unavailable because browser use is disabled in Work settings. They remain NOT_TESTED; no real two-device/two-network, forced-relay, physical-device toggle or camera-release result is inferred from native signaling tests.

Official references: [WebRTC peer connections](https://webrtc.org/getting-started/peer-connections), [MDN signaling and video calling](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Signaling_and_video_calling), [ICE restart](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/restartIce), [coturn configuration](https://github.com/coturn/coturn/blob/master/examples/etc/turnserver.conf).
