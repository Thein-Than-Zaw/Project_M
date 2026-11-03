# HOME-05 — Shared activity in “somewhere with you”

2 November 2026, Asia/Yangon. Owner asks for Home landscape-card activity totals. Explicit follow-up decisions: replace Chatting Time with message count; count only messages not deleted; use connected video-call count for the third “Conversation” metric. Existing Spring Boot/PostgreSQL/read-only Home flow retained.

| Clause | Interpretation / implementation | Evidence |
|---|---|---|
| C1 Messages instead of Chatting Time | Count committed TEXT/GIFT/FILE rows in the current couple with deleted_at NULL. Text/image/video/voice/document/sticker each count one saved message. Retries and unsent uploads do not count. | Native text/gift/file/retry/upload tests |
| C2 Exclude deleted messages | Delete for everyone removes a message from the total. Existing Delete for me only hides a personal view; shared total stays the same for both members. This follows existing deletion semantics, clarified in commentary. | Native hide vs delete and both-member equality tests |
| C3 Video call time | Sum elapsed seconds from first connected_at until ended_at, or server CURRENT_TIMESTAMP for an ongoing call. VIDEO only, connected_at required, negative durations clamped0. Count a session once for the couple. | Native completed3661s + connected-failed59s =3720s, voice/unanswered excluded, current-call contribution tested |
| C4 “Conversation” means video-call count | Number of VIDEO call_session rows that actually reached connected_at, including a currently connected call and calls interrupted after connection. Ringing/missed/unanswered do not count. | Native zero/voice/missed/ended/failed/active cases |
| C5 Home landscape integration | Preserve sun/hills and title; accessible section/definition list with Messages, Time on video, Video calls; responsive values, real zero vs unavailable em dash. | Markup check and Node duration/zero/missing-data tests |

All five clauses UI_REQUIRED, with server-side aggregation supporting C1–C4. No typing-time estimate, presence-based reconstruction, daily sessions or voice-call totals introduced. Available existing records supply historical totals automatically; no migration, new tracking writes or user-data import. Date range is all available couple records, not a reset on relationship-date edits. Deleted-for-everyone rows are intentionally excluded even though they were historically sent.

## Contract and calculation

Existing authenticated GET `/api/home` adds `activity: {messageCount, videoCallSeconds, videoCallCount}`. Account membership supplies space_id; every query scopes to it. Home's existing repeatable-read transaction keeps the response internally consistent. Existing15s polling, focus refresh and private event refresh update counters; no per-second browser estimate or new broadcast channel.

Duration uses the same elapsed connected-to-end semantics as the existing call timer. Reconnection grace is included; this is not packet-level media/attendance measurement. Historical call rows do not retain per-outage segments, so no unrecorded active-media interval is invented. Minutes are floored for display: zero `0 min`, positive sub-minute `<1 min`, then `N min` or `Hh Mm`; hours can exceed24. Actual seconds remain in the API. Total duration is derived only from retained metadata; no stronger immutable archival-history guarantee is introduced.

Files/symbols: HomeService.Home adds Activity; private activity(space) aggregate queries; home.html landscape/card semantic markup; home.js renderActivity/formatVideoTime; home.css card sizing/contrast/wrapping. Existing Home fields/calendar/profile/unread counts and call behavior unchanged. No database/schema/security/credential change.

## Verification and status

JavaScript **73 PASS**, zero failures/skips, syntax and diff checks PASS. New formatter tests cover seconds/minutes/hour/day-length totals, invalid/missing values and real zero. Native **12 PASS**, zero failures/errors/skips (existing HomeIntegration10 + HomeActivity2), Maven BUILD SUCCESS in1:33 at23:40:47 Myanmar time. New native tests use only synthetic accounts/messages/private photo fixture/call rows in the guarded `_test` database and clean their own rows/objects. Actual `/api/home` response and unauthenticated401 tested. Evidence ignored `.local/home05-js.log`, `.local/home05-native.log`, `.local/home05-markup.py`.

Final local build/run identity follows after execution. Physical mobile/laptop graphical review remains NOT_TESTED because browser use is disabled in this session; no bypass attempted. All code review is AI self-review, not human acceptance. Code IMPLEMENTED LOCALLY, scoped tests PASS, owner visual acceptance PENDING. No Git push/deploy yet; source package NOT_DUE for routine local work; Code Update Record DRIVE_SYNC_PENDING (existing authorized destination unresolved). Recovery is a source revert/rebuild with no database rollback or restore.

## Final local checkpoint

Base main `fb935d13bea837bc379d6870968be1836a7088a3` plus uncommitted HOME-05 source/tests. `npm.cmd run build` BUILD SUCCESS,13.638s at23:42:33 Myanmar time (`.local/home05-build.log`). JAR SHA256 `2bf094349ad963e7b5a2b8a8ee39d2f0f25accf2f7487b10fb7395b1cd95e54b`. Source/test/scripts manifest `.local/home05-source-manifest.json` SHA256 `7a32fd55d6a0c08158c1130e78a561a1f07f067b7ed881f53451a9c6a6b411ef`. JS73/native12 evidence applies to this source; no full-project native-suite claim.

Local app runs at http://127.0.0.1:18083/ using existing local PostgreSQL/accounts, Java PID29112 (launcher23548). **6 local HTTP checks PASS**: healthUP/root200, Home JS/CSS200 with normalized source equality, protected Home HTML302 and guest Home API401 (`.local/home05-local-http.log`, `.local/home05-app.log`). Existing data retained; no migration needed. Restarting the JVM may require signing in again. Refresh locally and open Home to review the landscape card. Production remains unchanged at applicationcd4376f.


## CHAT-07 continuation

Current local run/build/test identity superseded by [CHAT-07 checkpoint](CHAT-07_Edit_Activity_And_Call_History.md). Totals now share SharedActivityService with desktop Chat and explicitly exclude CALL system rows; existing Home behavior preserved. Local V31–V33 and PID6340 apply to combined work; production unchanged.
