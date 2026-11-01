# Third-party dependency inventory

DEV-01 uses dependencies, not copied third-party product imagery/fonts/UI kits. Status screen uses system fonts and original HTML/CSS. Figma reference has not been exported or bundled. No JavaScript package or downloaded asset is included.

| Component | Version / source | License reference |
|---|---|---|
| Spring Boot / Spring Framework / Spring Security / Spring Data | Boot BOM4.1.2-managed; Maven Central | Apache2.0; [Spring Boot](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt) |
| Apache Maven / wrapper | 3.9.12 /3.3.4 | Apache2.0; generated wrapper retains license header; archive contains LICENSE/NOTICE |
| PostgreSQL server | 17.7, packaged by Zonky17.7.0 | [PostgreSQL License](https://www.postgresql.org/about/licence/); extracted engine retains its license files |
| PostgreSQL JDBC | 42.7.14 | BSD2-Clause; [pgjdbc license](https://github.com/pgjdbc/pgjdbc/blob/master/LICENSE) |
| Flyway Community | 12.4.0 | Apache2.0; [Flyway](https://github.com/flyway/flyway/blob/main/LICENSE.txt) |
| Hibernate ORM | Boot BOM, observed7.4.13.Final | [Hibernate licensing](https://hibernate.org/community/license/) |
| JUnit / test tooling / embedded server and transitive dependencies | Boot BOM-managed, exact dependency tree regenerable | Licenses/POM notices retained in original Maven artifacts; review complete resolved inventory before redistribution |
| Installed Oracle JDK | 27.0.1.0.3 | Existing machine runtime, not redistributed by project |

`.tools`, `.local`, `target` and downloaded archives are ignored, not application source. Tool bootstrap restores pinned downloads. This inventory is not a legal review or dependency vulnerability scan. Review licenses and current advisories before deployment/distribution.
# DEV-06 artwork addition — 1 November2026

363 original Blobfox/BunHD PNG/APNG assets by Volpeon (formerly Feuerfuchs), licensed Apache-2.0 on the original creator pack pages. Original artwork retained unchanged; project adds catalogue codes and readable labels. Original pages: https://volpeon.ink/emojis/blobfox/ and https://volpeon.ink/emojis/bunhd/. Original archives: https://strapi.volpeon.ink/uploads/blobfox_1703c01b78.zip and https://strapi.volpeon.ink/uploads/bunhd_cf2a62ba99.zip.

Runtime credit/copyright: `src/main/resources/static/stickers/NOTICE.txt`; complete license: `Apache-2.0.txt` in the same directory. Archive/per-file SHA256 and animation inventory: `docs/evidence/DEV-06_asset-manifest.json`. No external runtime image requests, no dependency on Telegram/Discord APIs or user accounts. Other creator packs have their own licenses and are not implicitly covered by this notice.
# DEV-07 document/media tooling

- Apache PDFBox3.0.5 is resolved by Maven for PDF structure validation. Original dependency license/notices remain in the Maven artifacts; [Apache PDFBox](https://pdfbox.apache.org/) is licensed under Apache2.0.
- Local FFmpeg/ffprobe8.1.2 Windows essentials build from [Gyan builds](https://www.gyan.dev/ffmpeg/builds/), linked by [FFmpeg download](https://ffmpeg.org/download.html). `scripts/setup-media-tools.ps1` pins and hashes the original archive. Extracted `.tools` retains the publisher's distribution/license files and is excluded from project source delivery. This build includes GPL components; it is not represented as an Apache-licensed project asset. No tool binaries are added to Git by this increment; redistribution obligations have not been reviewed as a release activity.
# DEV-08 call infrastructure

WebRTC APIs are supplied by the user's browser; no third-party JS call SDK/assets copied. Optional local container config references coturn/coturn4.18.0-r0 from [upstream](https://github.com/coturn/coturn), BSD3-Clause [license](https://github.com/coturn/coturn/blob/master/LICENSE). Image/binaries are not in application source; pull/run/redistribution not performed here. Application code generates standard expiring TURN REST credentials with JDK cryptography; no free public TURN account or provider credentials bundled.
