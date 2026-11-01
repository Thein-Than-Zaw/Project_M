# DEV-08 — Voice/Video Call working contract

1 November2026, Asia/Yangon. User explicitly starts DEV-08 while personally reviewing DEV-07. Preserve DEV-07 and all dirty local work. Parent: F06 CALL-01–03 / AC-F06-01–06, D01/D04/D05/D06. Voice-only call is explicitly included by this request. User confirms ring30s→MISSED, recovery15s, one live call→Busy and first accepted tab wins. No production hosting/provider purchase, recording, screen share, group calls or closed-site ringing.

Bounded implementation: A) durable authenticated call state/signaling and native regression; B) shared responsive in-app call widget and real WebRTC tracks; C) local upgrade/run, media/TURN verification and evidence. UI/media tests cannot be replaced by signaling tests. API and UI statuses remain separate; browser-disabled/TURN-runtime limitations gate only their verification.

## Schema / invariants

V20 call_session: UUID+space+caller/callee IDs, VOICE/VIDEO, RINGING/CONNECTING/ACTIVE/RECONNECTING or terminal ENDED/DECLINED/MISSED/CANCELLED/FAILED; created/accepted/connected/ended UTC timestamps, ring/connect/recovery deadlines, selected caller/callee client UUID and server WS session binding, connected reports, negotiation UUID and version. Partial unique index one nonterminal row per space. Both participants must be different active members; actor comes from authenticated principal. SDP/ICE/media are never persisted or logged.

V21 call_command_receipt: actor+event UUID, command/hash+call reference for durable invite/accept/decline/cancel/end retry. Changed intent conflicts; same invite key never creates another call, including after termination. Space lock serializes state transitions; server events only after commit. Signals remain bounded transient data with negotiation scoping and in-memory retry deduplication.

## API / WS

Authenticated ACTIVE members: GET `/api/calls/current` → `{call:View|null}`; GET `/api/calls/recent` → last20 summaries; GET `/api/calls/{id}` → participant summary; GET `/api/rtc/config` → `{iceServers,relayConfigured,expiresAt}` with no-store; POST `/api/calls/{id}/leave` CSRF + `{clientId,eventId}` for selected page's keepalive cleanup. No recipient/user identity is trusted from the body.

Own `/user/queue/calls` only. STOMP `/app/call.invite`, accept, decline, cancel, end, resume, connected, disconnected, heartbeat, offer, answer, ice, media. Base command `{eventId,callId?,clientId,mode?,reason?,negotiationId?,sdp?,candidate?,sdpMid?,sdpMLineIndex?,micEnabled?,cameraEnabled?}`. Invite mode VOICE/VIDEO; each page has a client UUID. Durable action receipts bind actor/event/intent. Replies: `call.ack` or `call.error` with eventId/code/detail/call; authoritative metadata `call.updated` goes to both users' active tabs. SDP/ICE/media relays target only the selected opposite WS session, never every account tab.

First accept binds callee client/socket; caller stays on initiating socket. Losing tabs dismiss ringing and never acquire peer signals. A reconnect may rebind only the same selected client when old socket is gone; another live tab cannot steal it. Each page load has a fresh random client UUID, retained only in that page's memory; metadata exposes only its SHA256 hash so another tab cannot copy the leave capability from REST/WS views. STOMP still checks the exact selected server socket. Navigation ends a call rather than silently transferring it to a new page. Stale/wrong role/client/call/negotiation signals reject. Single designated offerer (caller) prevents glare; callee answers; queue ICE until remote description. SDP<=16,000 and ICE<=4,000 characters; per-session rate and bounded transient dedup cache. Normal broker topics/foreign user queues stay denied. Publish order remains preserved; clients wait for the offer/answer ACK before sending that negotiation's ICE, so correctness does not depend on executor receive order.

Both selected clients report actual RTCPeerConnection connected state before ACTIVE/timer. The server records authenticated client reports, not an independently observed RTP connection. Initial connection technical timeout30s; recovery keeps original15s deadline rather than extending on repeated reports. Heartbeat5s from selected live page; timeout/invalid socket triggers bounded recovery; logout/revoked session ends call and closes local resources. Startup reconciles stale persisted calls; metadata survives while SDP is renegotiated transiently. Failed/cancelled/missed outcomes remain call summaries; no durable notification inbox is added before DEV-09.

## UI/media / consent

Widget on Home/Chat/Memories; Voice and Video entry buttons, incoming Answer/Decline, outgoing Cancel, Connecting/Reconnecting/Active/Ended/permission/offline/busy/failure states, self-preview/peer playback, duration, mic/camera indicators, mute/camera toggles and End. Access microphone/camera only after Call/Answer click; voice requests audio only. Track.enabled toggles actual outgoing media; end/logout/pagehide stops all tracks, closes peer/audio, clears timers/queued candidates. Autoplay failure offers explicit play-audio action. Navigation ends that page's call with a CSRF-protected keepalive leave; no silent transfer to another page/tab.

No configured TURN defaults to honest local/direct-only configuration (empty ICE list), not a production connectivity claim. Export TURN_URLS and server-only TURN_SHARED_SECRET for authenticated short-lived HMAC credentials; expiry is disclosed and secret never sent to JS. Local relay config is isolated from development source secrets. This Windows machine currently has no Docker/Go CLI; WSL availability is checked before claiming a runnable coturn instance. No unauthenticated/public free TURN dependency is selected.

## Source/verification mapping

| Source | Implemented journey / native responsibility | Required evidence |
|---|---|---|
| AC-F06-01 | Two accounts Voice/Video invite→accept→actual tracks | Native signaling + real two-device/two-network bidirectional audio/video, separately NOT_TESTED until observed |
| AC-F06-02 | Authenticated expiring TURN config and forced relay option | Credential algorithm/authorization tests; actual selected relay pair test remains separate |
| AC-F06-03 | Mic/camera toggles on real tracks plus peer indicator | Browser outgoing/remote media assertions, not button text alone |
| AC-F06-04 | Ring30s, Decline, Cancel, Offline, Busy and tab selection | Native states/races/deadlines, graphical states |
| AC-F06-05 | End/logout/session expiry/navigation/connection recovery cleanup | Native authorization/lifecycle + actual browser tracks/PC stopped |
| AC-F06-06 | Participant/selected-socket/current-negotiation only | Native guest/foreign/stale/wrong-role/wrong-tab/subscribe/rate tests |

Original v1.1 remains historical; this working addendum does not invent human UAT or production approval. Runtime/build/source identity and pending checks belong to DEV-08_Checkpoint.md. Drive destination remains unknown; local record sync pending.
