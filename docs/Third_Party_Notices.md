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
