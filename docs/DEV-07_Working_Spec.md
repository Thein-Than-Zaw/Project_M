# DEV-07 — private memories: current owner decisions

1 November2026, Asia/Yangon. Working addendum to requirements v1.1 F07 MEM-01–03, Q03/Q05 and D03/D04/D06. Owner now authorizes implementation after accepting the photo and batch limits below. DEV-07 IN_PROGRESS; runtime verification remains pending until recorded. The v1.1 source remains historical; explicit decisions below supersede conflicting proposed defaults.

## Confirmed by owner in chat

| Item | Confirmed behavior | Authority |
|---|---|---|
| Photos / batch | JPG/JPEG, PNG, WebP, max10 MB per photo; max10 files total per memory, combining photos/videos/documents | Owner accepts the explained per-memory10-file limit and tells us to implement the proposed photo/batch rules now |
| Short video | Maximum3 minutes (180 seconds), maximum100 MB per file | Owner describes TikTok-like short clips and accepts the explicit3min/100MB proposal |
| Documents | Memory attachments also support PDF, Word `.docx`, Excel `.xlsx`, PowerPoint `.pptx`; maximum20 MB per file | Owner requests documents, then explicitly accepts these4 formats and20MB |
| Trash / permanent deletion | Delete moves a memory and its attachments to shared Trash for3 days; restore is available during that period. After3 days permanently delete. Empty Trash permanently deletes everything currently in Trash immediately; active memories are not included. | Owner replaces the proposed30-day Trash with3 days and explicitly requests permanent removal when Trash is emptied |
| Shared permissions | Both active partners may view/edit/delete shared memories and their attachments | Existing D03/D04, not reopened |
| Privacy | Every metadata/file request must authorize active couple membership; private server storage, no E2EE claim | Existing D04 |

Size accounting uses decimal MB:100MB=100,000,000bytes and20MB=20,000,000bytes. File limits are per asset; request-level multipart limits must support the eventual allowed batch count. Video duration is measured on server from media metadata, never trusted from browser/client input. Video codec/container allowlist remains to be finalized; the older proposed MP4/H.264/AAC baseline is not a new owner decision.

This supersedes proposed120-second video and30-day memory purge defaults. Confirmed photo10MB and mixed batch10 now apply. Avatar, overall storage quota and backup-copy disposition remain separate; no unlimited storage or production backup guarantee is implied. Engineering resource checks and clear disk-full failure preserve existing content. Working source form defaults (title120/caption2000, nonfuture Myanmar date, min1 asset, per-user favorites) are used as recorded implementation defaults, not invented owner confirmations. Local playback baseline: MP4/H.264 with optional AAC audio; reject unsupported encoding clearly without silently transcoding originals.

### Trash lifecycle clarification

Deletion hides the memory from ordinary lists and denies its normal asset/thumbnail/download endpoints, retaining content only for the authorized shared Trash/Restore workflow. The3-day restore period is measured from server deletion time (72 elapsed hours, UTC timestamps displayed in Asia/Yangon). Expired or permanently deleted content cannot be restored through the app. Empty Trash requires an explicit irreversible-action confirmation and covers all items already in the couple's Trash at that operation's server snapshot; concurrent new deletions must not silently join an already confirmed batch. Both-member shared authorization remains; no creator-only permission added.

Permanent deletion must remove memory content metadata and its underlying private photo/video/document/thumbnail files, not merely hide records indefinitely. Server must revoke access/restore immediately and handle storage failures with durable retry/reconciliation, reporting failures honestly; technical lifecycle/atomicity contract is still to be written. Automatic expiry also needs real scheduler/restart-recovery verification. Existing backup-copy removal/expiry policy remains unresolved; app deletion alone is not proof that historical backup copies disappeared. No actual memory/file deletion is performed by this documentation update.

## MEM-DOC-01 impact record (local tracking key, not a new official requirement ID)

Classification: video business-rule clarification/change to a proposed default + document scope addition. Requested/confirmed by project owner in the current chat. Purpose: keep short videos and everyday documents in the couple's private memories. Complexity/risk: medium, because Office package validation/download behavior adds work beyond images/video. No commercial estimate or release date supplied.

| Layer | Affected design / implementation work |
|---|---|
| Requirement / RTM | F07 MEM-01–03 and AC-F07-01–08; extend file-kind scope to documents, keep existing permissions and original AC IDs; add checkable document clauses in DEV-07 contract |
| UI / UX | Add/edit accepts approved documents alongside photos/video; show escaped filename, type/size, upload progress/error/retry/remove; Documents filter and authorized download action; PDF preview vs download-only remains an explicit UI design item |
| Data / migration | Future memory asset metadata needs DOCUMENT kind and verified format/size/private storage reference; duration only for VIDEO; no existing memory tables/data migrated in this documentation update |
| API / WS | Future uploads validate actual bytes/type and server-derived membership; enforce3min/100MB videos and4 document formats/20MB; no client-supplied public storage URL; notification channel stays in-app |
| Security / storage | Treat documents as untrusted data, verify signature and Office package structure as appropriate; no file execution, macros, external document viewer/conversion service or public permanent link introduced; authenticated streaming/download and safe response filenames; review archive/decompression/resource limits before implementation |
| Testing | Real valid/invalid uploads;180s and size boundaries; forged extension/MIME, malformed Office package, foreign/anonymous download denial, reload/download byte identity, both-member edit/delete, failed-save/orphan cleanup and concurrency |
| Reports / external integrations | No report/payment/provider change; no external document integration selected |
| Existing work / release | Chat/stickers/layout unaffected; native/GUI evidence retains prior identities; DEV-07 implementation/UAT NOT RUN, product release readiness unchanged |

## Planned acceptance additions and unresolved design

- Valid approved document <=20MB uploads, persists and downloads with byte identity for either member; unrelated/anonymous requests deny. PDF/DOCX/XLSX/PPTX tests use real representative files, not only renamed text.
- Unsupported or malformed document and oversized file rejects clearly without publishing a broken memory; mixed media/document save must preserve existing content on failed edit.
- Valid video <=180s and <=100MB can publish; above either limit rejects on server. Codec/transcoding/playback validation must be resolved before claiming playback readiness.
- Both partners' edit/delete permissions apply to document assets as part of a memory; no creator-only restriction added. Trash/Restore is available for3 days, then permanent purge; Empty Trash purges its confirmed trash batch immediately. Test attached binaries/thumbnail cleanup, ordinary route denial, restore cutoff, foreign denial, concurrent restore/purge, partial storage failure and restart recovery.
- Images, max asset count, total storage quota, backup policy and remaining memory form defaults must be settled for their affected implementation. Keep independent engineering planning moving; no approval/signature is invented for these items.

Documentation changes only: this file plus open-rule register, implementation plan, RTM and API/schema draft impact notes. No application code/schema/test change and no new runtime verification asserted. Local decision/impact record SAVED; Drive destination unknown, DRIVE_SYNC_PENDING. Human acceptance applies only to explicit video/document/Trash decisions above, not the full memory UI or remaining proposed defaults. Trash amendment affects MEM-02/03 and AC-F07-03/05/07/08; retains original source IDs. Data model needs deletion/expiry/purge state and safe file cleanup tracking; API/UI need shared Trash list, Restore and confirmed Empty Trash; implementation/release/UAT remain pending.
