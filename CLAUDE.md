# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android native marketplace app (Vinted-style) — Kotlin + Jetpack Compose + Material3.
Single-module Gradle build (`:app`), namespace `com.example.vinted`.

Naming quirk: the repo is `Vinder` but `settings.gradle.kts` sets `rootProject.name = "Vinted"` and the Android `applicationId` is `com.example.vinted`. Don't "fix" this — other branches depend on it.

Visual source of truth: `docs/DESIGN.md`. Palette is already translated into `app/src/main/java/com/example/vinted/ui/theme/Color.kt` — pull from there, do not hardcode colors.

## Build / Run

```
./gradlew assembleDebug          # build APK
./gradlew installDebug           # install to attached device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs device)
./gradlew lint                   # Android lint
./gradlew test --tests "com.example.vinted.ExampleUnitTest.someMethod"   # single test
```

Toolchain pins (in `gradle/libs.versions.toml`): AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01. Java 11 source/target. `compileSdk` 36, `minSdk` 24.

## Architecture

**Single-Activity Compose app.** `MainActivity.onCreate` calls `setContent { VintedTheme { <one root composable> } }`. There is no navigation graph and no `androidx.navigation` dependency. When a feature needs multiple screens, follow the in-`AddProductScreen` pattern:

```kotlin
var step by remember { mutableIntStateOf(1) }
when (step) {
    1 -> ScreenA(onContinue = { step = 2 })
    2 -> ScreenB(onBack = { step = 1 })
}
```

Adding `navigation-compose` is fine if a feature truly needs it, but discuss with the team first — current screens assume hand-rolled state.

**Feature layout.** Each feature is its own package under `app/src/main/java/com/example/vinted/ui/<feature>/`. Public entry point is a single `@Composable fun FeatureScreen(...)`; step composables and private helpers (`PrimaryButton`, `TopBar`, etc.) live in the same file. See `ui/addproduct/AddProductScreen.kt` as the reference implementation — it defines reusable `PrimaryButton`, `StepIndicator`, and top-bar patterns inline.

**Theme.** `VintedTheme` (in `ui/theme/Theme.kt`) wraps `MaterialTheme` and applies `LightColorScheme` built from the brand tokens in `Color.kt`. Dark scheme exists but uses legacy purple — light is the supported path for now. Dynamic color is enabled on Android 12+, which can override brand palette; if a screen must always look on-brand, pass `dynamicColor = false` at the call site.

**Media.** Image loading uses `coil3` (`io.coil-kt.coil3:coil-compose`). Camera capture goes through `FileProvider` at `${applicationId}.fileprovider`; paths are configured in `app/src/main/res/xml/file_paths.xml`. Permissions in `AndroidManifest.xml`: CAMERA, plus READ_EXTERNAL_STORAGE (≤ API 32) / READ_MEDIA_IMAGES (≥ API 33).

## Branching

Repo is at `github.com/Pharrellstu/Vinder.git` (not owned by everyone on the team — no force-pushes to shared branches). Active branches:

- `main` — protected, docs only
- `develop` — integration target for feature PRs
- `feat/addproductpage` — currently holds the entire Android scaffold; merge into `develop` is pending
- Feature branches: name them `feat/<what>` (e.g. `feat/messaging-ui`)

The full project scaffold currently only lives on `feat/addproductpage`. Branch new work off `feat/addproductpage` (not `main`) until it lands in `develop`. Keep edits to shared files (`MainActivity.kt`, `ui/theme/*`) minimal to avoid rebase conflicts.

## Jira

Tickets are tracked in Jira project **ADV** ("APP-Development-Vinder"). Hierarchy is **Epic → Task** (not Epic → Story → Task). Tasks carry a `parent` link to an Epic. When picking work, filter for `assignee is EMPTY AND sprint in openSprints()` — the active sprint usually contains a backlog of unassigned Tasks.
