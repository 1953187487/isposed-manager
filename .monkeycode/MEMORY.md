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
