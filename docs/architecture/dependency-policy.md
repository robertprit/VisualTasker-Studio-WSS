# Dependency Maintenance Policy

## Baseline

| Component | Version | Reason |
| --- | --- | --- |
| Android Gradle Plugin | 8.13.2 | Kotlin 2.3 support without an AGP 9 migration |
| Gradle | 8.13 | supported by the selected AGP line |
| Kotlin | 2.3.21 | current Kotlin 2.3 bug-fix baseline |
| Compose BOM | 2026.02.00 | maps Compose 1.10.3, matching Compose DND 0.5.0 |
| Room | 2.8.5 | Kotlin 2.3 metadata-compatible stable Room line |
| Compose DND | 0.5.0 | current stable DND integration baseline |

## Update rules

1. Update the host and composite plugins in one compatibility branch.
2. Never update Kotlin, Compose, AGP or Room independently without compiling
   every included build.
3. Keep versions pinned; no dynamic ranges or `latest.release` selectors.
4. Run plugin unit tests, host unit tests, `assembleDebug`, install and launch
   before accepting a baseline.
5. Upgrade to AGP 9 / API 37 only as a dedicated migration. Compose releases
   that require that toolchain remain blocked until then.
6. Preserve `THIRD_PARTY_NOTICES.md` and add model cards for every bundled AI
   model before a public release.
