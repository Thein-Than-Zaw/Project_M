# Unresolved business rules — draft decision register

Source: v1.1 §§1, 5, 6.2. D01–D06 are settled and are not repeated as questions. Foundation engineering continues independently. No proposed default below is approved by implementation.

| Source | Decision needed / current proposal | Affected task / implementation consequence |
|---|---|---|
| Q02 | Display elapsed days (start=0) vs ordinal day (start=1), or both; Feb29 anniversary Feb28 vs Mar1 | DEV-04: counter labels/calculations/yearly reminders not implemented |
| Q03 | Disabled/replaced/deleted account handling, recovery and unpair/re-pair; ownership/data disposition | DEV-02/03/09: current schema caps all stored accounts, no automatic slot reuse or lifecycle API |
| Q03 | Deleted memory visibility, physical purge and backups; proposal immediate hide / purge after30d, backup30d | DEV-07: no delete/purge jobs now; D03 already permits both members to edit/delete |
| Q03 | Notification retention90d, backup schedule/RPO24h/RTO4h proposal | DEV-09/10: no retention job or backup guarantees |
| Q05 | Avatar2MiB; image10MiB; video100MiB/120s; max10 assets/memory; total quota/storage budget | DEV-07: limits/codec/transcoding strategy and quota errors need agreed contract; no upload endpoint now |
| Q06 | English UI vs Myanmar/bilingual; confirmed couple timezone; target browsers/devices and up to4 sessions/account | DEV-02/04/10: source limits/detailed device tests are proposed; Asia/Rangoon documentation time is not a hardcoded couple timezone |
| F01/F02 [P] | First account bootstrap token + second partner invite, issuance/storage/distribution and recovery | DEV-02/03: registration stays closed until selected; no public signup inferred |
| F02 [P] | Invite24h, regenerate/revoke/one-use behavior; signup-vs-join order | DEV-03: hashes/expiry/atomic consumption plan is draft |
| F02 [P] | Different signup dates: creator date canonical + both shown + explicit joining-partner confirmation | DEV-03: no silent overwrite; DB can store original dates separately |
| F01 [P] | Password12–128, normalized email policy, allowed account email changes, idle30m / absolute expiry / session limits | DEV-02: security validation/session rules need review; foundation30m is configurable engineering default, no real sessions/login yet |
| F01 [P] | SMTP password recovery/security emails, reset30m, retries3, login/reset rate limits and recovery when mail unavailable | DEV-02: email is separate from D06 in-app channel; no SMTP integration enabled |
| F04/F05 [P] | Chat4000 code points, note500, history50, rate/payload limits, free gift catalog, no message edits/attachments | DEV-05/06: types/limits/semantics remain draft; no paid gifts introduced |
| F06 [P] | Ring30s, reconnect15s, busy/multi-tab accept rules and call outcomes | DEV-08: signaling state machine and TURN credentials not implemented |
| F07/F08 [P] | Memory title120/caption2000, future date rejection, per-user favorite, version conflicts; event creator-only delete, reminders0/1/7 at09:00 | DEV-04/07: keep proposed defaults separate from D02/D03 |
| F09 [P] | Persistent inbox/catch-up, preview privacy, typing5s, heartbeat20s/offline60s, event eligibility | DEV-09: D06 only settles notification channel, not all delivery/retention rules |

Hosting/provider/budget and production storage/TURN/mail configuration are deliberately deferred by D05, not DEV-01 blockers. Do not reinterpret missing detail as approval, and do not ask the owner to reconfirm D01–D06. Take the relevant decisions in the next feature task, with concrete UI/API behavior to review.
