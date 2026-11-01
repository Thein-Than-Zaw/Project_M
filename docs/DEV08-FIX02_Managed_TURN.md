# DEV08-FIX02 — Cross-network call relay

2026-11-01, Asia/Yangon. User reported Chat works and same-Wi-Fi Voice/Video works,
but Wi-Fi/mobile-data calls stay Connecting then Interrupted. Inspected deployed
secret configuration had no STUN_URLS/TURN_URLS/TURN credential. Existing WebRTC
therefore gathered direct candidates only. This supports the missing NAT-traversal
configuration diagnosis; it is not a capture of the user's actual ICE packets.

Owner selected **ExpressTURN Free** and saved provider-issued TURN credentials in
ignored `.local/express-turn.env`. Its bare hostname/port was normalized to TURN
URLs with UDP/TCP alternatives. STUN uses Cloudflare's public STUN service.
ExpressTURN advertises Free1000GB/month, TCP/UDP3478; other ports/premium capability
are not assumed. No paid plan or shared public Internet credentials were enabled.

## Source and contract

Base commit: a359d0b77eac2fe25fe57aa983ccd1ba972f8874. Local tested source identity:
`b140cbf8ce0d33988cdfe64b303b89c279970e62cd423d9b17473a4246910a48`;
see docs/evidence/DEV08-FIX02_source-manifest.json. Deployment source is identified
separately by the exact Git commit below (Docker rebuilds the JAR on Java21).

- RtcConfiguration supports provider-issued username/credential password as an
  alternative to existing coturn REST HMAC. Both cannot be enabled together.
  Incomplete/control-character/overlong settings fail closed. Production constructor
  is explicitly autowired; the compatible three-argument constructor is retained.
- application.yml maps TURN_USERNAME/TURN_PASSWORD. prepare-render-secrets includes
  optional relay values from the ignored ExpressTURN file/main env for future setup.
- GET /api/rtc/config stays paired/authenticated and no-store. Coturn master secret
  never leaves server. Managed TURN username/password must reach those two browsers
  for WebRTC authentication; they are not provider account-login/admin credentials.
- Managed Config.expiresAt is the browser's refresh deadline, not a claim of one-hour
  provider credential expiry. Lifetime/revocation/rotation follows ExpressTURN.
  API_WS_Contract_Draft.md documents that distinction.
- No database migration, account/data transfer, call timer change, recordings,
  media logging, UI redesign or STOMP/security relaxation. Ring30s/recovery15s retained.
- Existing Render single secret file is backed up privately and updated by a
  narrow TURN overlay. DB/storage values are checked unchanged. Other files,
  services, user accounts and pairing remain untouched.

## Evidence before deployment

- Native tests: **14 passed**,0 failures/errors/skips: RtcConfiguration4 +
  CallIntegration10, isolated local test DB. Timestamp20:35:11 +06:30.
- `npm run build`: **BUILD SUCCESS**,20:44:44 +06:30,12.374s; packaging skips tests.
  Local JAR SHA256:
  `792c33ff1167e9ccf7d3c40c1472139ca9b9f5edf43759556c465c77e45335a6`.
- Actual supplied settings accepted by final Java RTC configuration without logging values.
- Standalone werift0.26.0 test tooling is isolated under ignored .tools/rtc-probe;
  no Node/WebRTC application dependency added to the Java app.
- Actual TCP TURN allocation passed. Two local software peers, both forced to
  relay-only, completed a synthetic WebRTC data-channel request/reply through
  ExpressTURN. No microphone/camera/browser session was accessed. Candidate IPs,
  credentials and SDP are not in exported evidence.
- Evidence: docs/evidence/DEV08-FIX02_tests.txt, DEV08-FIX02_build.txt,
  DEV08-FIX02_relay-probe.txt. Those probes do not certify real phone audio/video
  on the user's mobile carrier, Free quota exhaustion, or maximum call duration.

## Deployment and user verification

Existing branch codex/render-hosting published commit
`086fa2dd85d9067717c41047faeaaa2249019bcd`. Existing Free service
`srv-dbjisdqjnfac7384lr50` deployed it as `dep-dbjkmh8473hc73a4qv80`: **live**.
URL remains https://couple-private-space.onrender.com . No new service or database.

Actual Render secret-file readback exactly matched the prepared TURN overlay;
all unrelated database/storage values were preserved. The one-file REST update
returned HTTP200 and the subsequent exact-commit deploy passed Docker build,
Java21 startup and health checks. Shared secrets/API tokens/provider credentials
were not put in Git or command arguments. Backup remains in ignored .local.

Post-deploy public HTTPS smoke **10 passed**. Anonymous `/api/rtc/config` remained
HTTP401 with no relay credential in its response. See
docs/evidence/DEV08-FIX02_public-https.txt and DEV08-FIX02_deployment.json.
No real paired user's password/session was requested or impersonated to test this.

The first private API operation used a stale proxy-selection assumption and failed
before any request; honored the current enabled HTTP/HTTPS proxy entries afterward.
No global network/TLS settings changed. This was operator connectivity, not a
device/media test or production data failure.

After activation, refresh/sign in on both devices and test Wi-Fi vs mobile data.
For an explicit relay-only check, open /chat.html?relay=1 on both paired sessions.
Do not infer relay success from a same-network direct call. Carrier/firewall access
to the supplied TURN port and actual voice/video still need that device check.

Recovery: keep credentials/DB/storage backup private. Roll back code and that one
secret file together if reverting to an old build that lacks managed credentials.
Never substitute TURN_PASSWORD for the coturn master shared secret. Rotate a leaked
TURN credential via the provider, update the secret file and redeploy. Auto-deploy
remains off. No user-data migration is resumed by this fix.

Implementation/build/network-probe status is separate from human device/UAT
acceptance and release. Local code update record only; DRIVE_SYNC_PENDING
(authorized register destination unknown).

References: https://webrtc.org/getting-started/turn-server ,
https://www.expressturn.com/ , https://developers.cloudflare.com/realtime/turn/faq/ .
