# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.
- This helps avoid redundant entries and keeps the memory file tidy.

## Entries

[Project Knowledge Summary]
- Date: 2026-11-12
- Context: Discovered by Agent while migrating Isposed Manager to the Miuix UI engine (v1.0.2)
- Category: Build Methods
- Instructions:
  - Debug build: `cd /workspace && export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) && export ANDROID_HOME=/opt/android-sdk && ./gradlew :app:assembleDebug --no-daemon`
  - Build stack: Kotlin 2.3.20 + org.jetbrains.kotlin.plugin.compose 2.3.20 + AGP 8.12.0 + Gradle 8.14 + compileSdk 37 (versioned platform)
  - compileSdk is set to 37; requires the `platforms;android-37.0` SDK platform installed via sdkmanager (AGP does NOT auto-download it)
  - Miuix 0.9.0 depends on `top.yukonga.miuix.kmp:miuix-ui:0.9.0`; package root is `top.yukonga.miuix.kmp` (theme/basic subpackages)
  - Miuix 0.9.0 requires compileSdk >= 37; do not downgrade below it
  - `kotlinOptions { jvmTarget = ... }` is removed; use `kotlin { compilerOptions { jvmTarget.set(...) } }` (Kotlin 2.3.20)
  - Miuix theme API: `MiuixTheme(controller = ThemeController(ColorSchemeMode.MonetSystem, keyColor = ..., paletteStyle = ThemePaletteStyle.TonalSpot))`; colors via `MiuixTheme.colorScheme` (type `top.yukonga.miuix.kmp.theme.Colors`), text via `MiuixTheme.textStyles` (body1/body2/footnote1/title1-3/headline1-2/button)
  - Miuix components live in `top.yukonga.miuix.kmp.basic`: Card, Button, Text, SmallTitle, HorizontalDivider, Scaffold, TopAppBar, Switch, TextField, TabRow
  - The app bridges MaterialTheme to Miuix colors/typography inside IsposedTheme so legacy `MaterialTheme.colorScheme.*`/`MaterialTheme.typography.*` usages track Miuix tokens

[Project Knowledge Summary]
- Date: 2026-11-13
- Context: Discovered by Agent while releasing v1.0.8 (LAN discovery, per-app authorization, i18n)
- Category: Operations & Deployment
- Instructions:
  - GitHub token for this repo is retrieved with `echo -e "protocol=https\nhost=github.com\n" | git credential fill` (the `password` field, `ghs_` prefix)
  - `api.github.com` accepts `Authorization: Bearer $TOKEN` or `token $TOKEN`; `uploads.github.com` REQUIRES `Bearer $TOKEN` (using `token` returns 401 Bad credentials)
  - Release upload is a two-step POST: create release on `api.github.com/repos/1953187487/isposed-manager/releases`, then upload the APK with `Content-Type: application/vnd.android.package-archive` to `uploads.github.com/repos/.../releases/{id}/assets?name=...`
  - A 54 MB APK upload needs bash `timeout >= 900000`; after upload, re-download via `browser_download_url` and compare SHA-256 to the local APK
  - Release APK signature SHA-256 is `5ee60bdca8cf1c29f15324f2145ee93f4e0d8b2274002e6c0a2472182621db7c` (release.keystore, storePassword/keyPassword `isposed123`, keyAlias `isposed`, DN `CN=Isposed Manager`) so later versions can upgrade over earlier ones
  - Release builds peak around 3 GiB; run with `background_terminal_create(memory_percent=50, cpu_percent=200, timeout=1200000)` in the managed terminal
