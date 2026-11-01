# Relationship date counting decision / CUR-DATE-001

Date: 1 November 2026, Asia/Yangon. Authority: direct owner response confirming Day 1 and February 28. This resolves Q02 for DEV-04; other business rules retain their existing status.

- The relationship start date is Day 1. For valid dates on or after that date, the displayed day number is the number of calendar days between the shared relationship date and today in Asia/Yangon, plus one. The next calendar date is Day 2; elapsed hours are not used.
- A February 29 anniversary is observed on February 28 in non-leap years and February 29 in leap years. Preserve the original stored relationship date; only the anniversary occurrence for the target year changes.
- Acceptance examples: start date 2024-02-29 shows Day 1 on that date and Day 2 on 2024-03-01. Its anniversary falls on 2025-02-28 and 2028-02-29. Day numbering advances at Myanmar local midnight.

This is a decision/documentation update only. No counter, reminder scheduler, UI behavior, migration or data rewrite was implemented. Future-date validation, event permissions, reminder timing and other open rules are not approved implicitly.

Updated references: Open_Business_Rules.md and Implementation_Plan.md. Documentation checked for consistency; runtime tests are not applicable. DEV-04 must verify inclusive counting, Myanmar midnight boundaries and leap/non-leap anniversary occurrences when implemented.

Status: owner decision CONFIRMED; implementation PLANNED; human UI/UAT and release approval not asserted. Local update record saved here; Drive sync DRIVE_SYNC_PENDING (no authorized destination); source package NOT_DUE. Requirements v1.1 and unrelated working changes preserved.
