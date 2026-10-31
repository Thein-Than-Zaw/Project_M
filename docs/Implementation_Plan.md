# Implementation plan / bounded work register

Source: requirements v1.1 §§5.11, 6 and current owner instruction. Draft v0.1; D01–D06 confirmed. Other business defaults remain proposed. Engineering scope for this turn: DEV-01 only. Full feature readiness is not asserted.

## Architecture and dependency order

Browser static HTML/CSS/JS → same-origin Spring MVC REST → transactional services → repositories → PostgreSQL. Authenticated STOMP adapters call the same services, delivering after commit to server-derived user queues. Private files are authorized through controller streaming. WebRTC uses STOMP for signaling only and STUN/TURN for media connectivity. In-App notification channels only. Start with one application instance; review sessions/broker/presence/scheduler before scaling.

| Task | Source / outcome | Dependencies / decision gates | Verification before completion | Status |
|---|---|---|---|---|
| DEV-01 | §5.11.7.1; Maven/Spring, DB/Flyway, secure boundary, local scripts/docs | Engineering delegated; no feature business decision required | Actual build/boot, fresh/repeat migrations, PostgreSQL negative/concurrent constraints, HTTP access | See checkpoint |
| DEV-02 | F01/F10 AUTH-01–03, D01/D02; real signup/login/session/CSRF and responsive auth screens | Bootstrap/invite registration policy, password/reset/rate/session defaults; finalized auth UI/contracts | Missing date, two-account/concurrent cap, wrong login, CSRF, session rotation/expiry/reset/revocation, no secret logs | PLANNED |
| DEV-03 | F02 PAIR-01–03; one shared space, invite and real join | DEV-02; token lifecycle, date conflict confirmation and Q03 | Atomic consume/member insert, self/third/replay/foreign denial, race, waiting→active | PLANNED |
| DEV-04 | F03/F08 HOME/DATE; profile/home/dates | DEV-03; Q02/Q06 and date/reminder defaults | Unicode profile, shared date consistency, midnight/leap/year transitions, conflicts, mobile/laptop | PLANNED |
| DEV-05 | F04/F09 CHAT/NOTIFY; durable real chat and base recovery | DEV-03; reviewed message/reconnect limits and authenticated WS contract | Persist-before-ACK, dedup/conflict, ordered catch-up, receipt authenticity, revoked socket/foreign subscription | PLANNED |
| DEV-06 | F05 GIFT; actual catalog/selection snapshot and emoji | DEV-05; free gift/catalog defaults | Chosen gift preserved, Unicode, cancel/no send, duplicate replay, invalid code | PLANNED |
| DEV-07 | F07 MEM, D03; private upload/streaming and shared edits | DEV-03; Q03/Q05, file/security/UI contract | Real upload/persistence/ranges, both members edit/delete, foreign denial, failed file cleanup, stale version | PLANNED |
| DEV-08 | F06 CALL; real two-device WebRTC | DEV-03/05; call defaults, local secure device setup and TURN plan | Two-network audio/video, forced relay, actual track toggles/cleanup, competing calls, stale/foreign signals | PLANNED |
| DEV-09 | F09/F10; notification inbox/settings/session lifecycle | DEV-04–08; retention/presence/session policies | Recipient-only inbox, duplicate reminder/event, multi-tab presence, logout-all/socket disconnect | PLANNED |
| DEV-10 | Integrated delivery | Completed dependent tasks + applicable human decisions | Signup→pair→chat→gift→memory→date→call→logout; unauthorized and real device cases, recovery/runbook | PLANNED |

Each future task stays a coherent user outcome: adapter + service + validation + authorization + persistence + UI states + meaningful tests. Add migrations when its reviewed contract is ready; do not create all speculative tables in DEV-01. Preserve existing source choices when resuming; inspect actual files, Flyway history and working changes before retrying.

## DEV-01 scope / definition of done

Permitted edits: build/wrapper, environment/local setup scripts, application entrypoint/config, public engineering status, three foundation migrations, scoped tests and drafts/traceability/checkpoint. No feature registration, chat, memory, gift, dates or call services. No schema seeds, demo accounts, payment, push, public media or cloud purchase/deploy.

Inputs/minimum context on resume: requirement v1.1 SHA in checkpoint, this plan, engineering profile, schema/API drafts, open rules, current sources and test results. Base identity: no Git repository/code. Final/tested identity: local source manifest in checkpoint. No human approvals recorded beyond explicit current instruction and D01–D06.

Risks: unsupported assumption promotion; private route exposure before membership checks; cap race; environment drift; existing-data misuse; external Figma limit. Mitigations: explicit draft labels, closed private surfaces, native constraints/tests, pinned build/portable tools, no baseline/clean. Engineering setup is independent of unresolved feature choices.

Review states remain separate: implementation, verification, AI self-review, human business/UI/technical acceptance, external record sync and release. CI provider/workflow absent; local tests are not a CI run. Future account lifecycle changes may require forward schema work; no deletion policy is implied by slot constraints.
