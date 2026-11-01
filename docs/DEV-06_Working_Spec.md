# DEV-06 — Emoji and character stickers / gifts

1 November 2026, Asia/Yangon. Base main `658251c9c12dcf10092d9f0cabfe6ca4efa34a69`.
User authorized DEV-06, then explicitly requested cute character sticker packs and as many available stickers as possible. This supersedes F05's disabled unsupported Sticker tab for **bundled, licensed** packs only; custom uploads/third-party marketplace remain outside scope. Original v1.1 stays unchanged. Six proposed virtual gifts were not approved; do not silently treat that list as confirmed. First increment uses character stickers as the selectable free gifts with the existing preview/note/confirm workflow.

Scope: all PNG/APNG stickers in original creator Volpeon's Blobfox and BunHD archives, local bundled assets, searchable paged catalogue, pack selection, preview, optional plain-text note up to 500 Unicode code points, explicit Send/Cancel, typed GIFT chat bubbles and sent/received history filters. No purchase, external runtime asset requests, custom uploads, paid packs, message edits/deletes or general notification inbox. New packs require reviewed sources/licenses and forward catalogue migrations. This is an incremental pack collection, not a claim to support every sticker on the internet.

## Contract and data

- GET `/api/gifts?pack=blobfox|bunhd&q=...&offset=0` active-member only, page 60, query at most 80 code points, bounded nonnegative offset. Response items `{code,name,pack,assetPath}`, packs and total/hasMore. No client asset URL or recipient accepted as authority.
- Existing STOMP `/app/chat.send`: TEXT remains `{clientMessageId,body}`; GIFT uses `{clientMessageId,type:"GIFT",giftCode,body:optionalNote}`. Explicit recipientId is rejected. Saved Message adds `type,giftCode,giftName,giftAssetPath` server snapshots. Retry identity covers type/code/note; replay of a discontinued gift still returns its original row. New inactive/unknown gifts reject. All messages share sequence, receipts, unread and recovery.
- GET `/api/gifts/history?direction=all|sent|received&before=...` 50-row keyset history scoped to server-resolved space; snapshot assets/names persist. Returns items/hasMore.
- V14 creates/seeds gift_catalog with immutable local paths and active flags, source/license provenance in asset manifest/notices. V15 adds typed payload and immutable snapshot columns to chat_message, preserves original TEXT rows/keys/receipts. No account or real-message seeds.
- AFTER_COMMIT partner event carries `You received a little gift.` once per new gift creation. A repeated client key emits no new creation notification. History/unread is durable recovery; general persisted notification inbox belongs to DEV-09 and is not claimed here.

## Source coverage before implementation

| Source | Classification / required result | Implementation / verification plan |
|---|---|---|
| AC-F05-01 | UI_REQUIRED: Myanmar + emoji survives reload | Existing composer/picker, native Unicode regression and graphical reload |
| AC-F05-02a | UI_REQUIRED: selected sticker + same note on both sides | Catalogue → preview → confirm → two-account chat and reload |
| AC-F05-02b / GIFT-02 | BACKEND_ONLY: snapshot survives catalogue change | Native mutate catalogue then query original snapshot |
| AC-F05-03a | BACKEND_ONLY: retry/concurrent retry one DB row/event | Native real STOMP/transaction tests |
| AC-F05-03b | UI_REQUIRED: one bubble after retry/reconnect | Client same UUID/type/code/note outbox and merge |
| AC-F05-04 | UI_REQUIRED: cancel no message/unread effect | Native counts plus graphical Cancel journey |
| AC-F05-05a | BACKEND_ONLY: unknown/inactive code, forged recipient denied | Native transport negative tests and unpaired access |
| F05 screen/field clauses | UI_REQUIRED: packs/search/preview/note validation, loading/empty/error/retry, Sent/Received | Chat navigation/dialog/history at mobile + desktop |
| F05 notification clause | UI_REQUIRED: partner receives in-app notice without duplicate retry | Recipient-only event and UI status, native event count |

Five parent AC, nine rows: 6 UI_REQUIRED, 3 BACKEND_ONLY. Design draft represented by this contract; runtime all NOT_TESTED initially. DEV05-UI01 actual background/focus qualification remains UNVERIFIED after native Computer Use URL-policy termination; do not inherit a PASS from source inspection. Normal engineering setup is already authorized. Drive destination unknown: local records, DRIVE_SYNC_PENDING; human acceptance/UAT/release separate.

## Work and recovery

Current: core IMPLEMENTED; native35 PASS; fresh V1–V15 and actual dev13→15 upgrade verified with original data projections unchanged. Final GUI journeys/coverage/limits in DEV-06_Checkpoint.md supersede the initial NOT_TESTED state above. Local record saved, Drive sync pending, human UI/UAT not run, full product release not ready. Do not reset real two-account data. Failed tests remain evidence; final build identity controls claims. Rollback to a pre-GIFT build cannot render new GIFT messages; use forward corrections and preserve snapshots/assets. Backup restore is not an automatic data-safe undo.
