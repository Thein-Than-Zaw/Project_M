# CHAT-05/06 release — 2 November 2026

Owner explicitly approved the local revision and requested deployment, superseding the prior HOLD. Target: existing `couple-private-space` Render service, My Workspace → Project_M; existing main branch/repository, private Neon database and storage/TURN configuration. Auto deploy remains OFF. Prior owner choice to deploy without a production backup remains applicable; no backup/export/restore or credential change is performed.

Scope: private image/document/video/voice Chat attachments; immediate pending bubbles and compact media with ⋯ options; consistent five-minute Sending/Failed/Retry/Cancel; mobile long-press reactions/actions; fixed laptop partner card; actual Chat read → matching inbox read. See CHAT-05_Local_Media_And_Mobile_UX.md and CHAT-06_Sending_State_And_Compact_Media.md for clause coverage and limitations. Owner's latest approval follows the local review; physical slow-network and two-device production acceptance are not inferred from automated checks.

Database: additive Flyway V27–V30, previous migrations unchanged. V27 backfills only existing read-message notification metadata and adds transactional read sync; V28 intents; V29 private attachments; V30 FILE payload/ownership/fingerprint support. Local V26→V30 startup and native predecessor migration rehearsal succeeded. Preserve existing TEXT/GIFT retry fingerprints and private storage references. No destructive migration or user reset.

Pre-release evidence: final local build SUCCESS; JavaScript64 PASS; affected native31 PASS; local health/assets/guest ACL checks PASS. Final full native regression: **105 tests PASS**, zero failures/errors/skips, Maven BUILD SUCCESS in 2:51 at 08:06:37 Myanmar time. Evidence: ignored `.local/chat06-release-tests.log`; application code unchanged from the tested local version. Final source identity and deployment outcomes follow below.

Recovery: keep the new schema and private bytes intact. Previous release is `e61e419e26ca286f41a12d777fb9cb75fa01f008`; restoring that older runtime after FILE messages exist needs compatibility review, so prefer a forward fix. Do not automatically drop V27–V30 or restore a database. No rollback is performed by this task.

Delivery: a complete secret-free Git-source package will be prepared locally with manifest and setup references. Existing authorized Drive destination remains unresolved: DRIVE_SYNC_PENDING. This does not change the owner's explicit deployment authority; source delivery status remains separate from runtime deployment and human acceptance.
