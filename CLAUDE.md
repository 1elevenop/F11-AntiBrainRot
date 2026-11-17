# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

F11 is an offline Android app that blocks distracting apps (Instagram, YouTube, TikTok, Facebook, Snapchat) on a schedule, shipped as **two APKs** from one Gradle build:

- `:app` → **F11** (`com.f11.app`): Max Focus, focus schedules, friction dialogs, history. Uses Usage access + Display over other apps. Owns all data.
- `:shield` → **F11 Shield** (`com.f11.shield`), an optional plugin: an Accessibility service that counts Reels/Shorts swipes and enforces the swipe budget. Stores nothing.
- `:core` → Android library shared by both (settings model, scroll tracker, shield overlay, theme). All unit tests live here.

The split exists because Play Protect blocks sideloaded apps using Accessibility and UPI payment apps refuse to run while one is on. Don't move Accessibility code into `:app`.

## Commands

Requires Android SDK platform 35 and JDK 17+. On Windows use `gradlew.bat` / `.\gradlew`.

```bash
./gradlew :core:testDebugUnitTest :app:assembleDebug :shield:assembleDebug   # full check
./gradlew :core:testDebugUnitTest --tests com.f11.app.ScrollTrackerTest       # one test class
./gradlew assembleRelease                                                     # signed release (see below)
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r shield/build/outputs/apk/debug/shield-debug.apk
```

There is no lint/format task configured beyond Gradle defaults.

## Architecture

**Single source of truth is F11.** `PreferencesManager` (`app/.../data/`) is the only writer of settings and counts. Every write goes through it and calls `changed()`, which refreshes its `StateFlow` and calls `notifyChange(ShieldContract.SETTINGS_URI)`. It deliberately does not use `OnSharedPreferenceChangeListener` (weakly held; R8 can drop it in release).

**Cross-app IPC** is defined in `core/.../data/ShieldContract.kt`:
- F11 exposes `ShieldProvider` (a `ContentProvider` using only `call()`), guarded by the signature permission `com.f11.permission.SHIELD`, declared in *both* manifests.
- The plugin's `ShieldClient` calls `getSettings` / `recordSwipe` / `recordShield` / `clearBlock`, and observes `SETTINGS_URI` to re-fetch.
- Settings travel as a raw `Bundle` (`PreferencesManager.rawBundle()`) and are decoded in the plugin with `BundleSettingsSource`; both sides build `F11Settings` through the `SettingsSource` interface and `SettingsKeys` in `core/.../data/F11Settings.kt`. Adding a setting means updating `SettingsKeys`, `F11Settings`, `PreferencesManager` (writer + `rawBundle`), and possibly the plugin.
- Both APKs **must be signed with the same key** or the provider rejects the plugin. Shared signing is in `buildSrc/src/main/kotlin/F11Signing.kt`, reading `keystore.properties` (git-ignored) or `F11_STORE_FILE` / `F11_STORE_PASSWORD` / `F11_KEY_ALIAS` / `F11_KEY_PASSWORD`; without them release builds are unsigned. `scripts/create-signing-key.ps1` creates a key.

**Two enforcement paths share `core`'s `OverlayController`** (the full-screen shield):
- `:app` `FocusWatchService`: foreground service polling the foreground app via UsageStats for Max Focus / active schedules; restarted by `BootReceiver`. `FocusWatchService.sync()` starts/stops it based on settings and permissions.
- `:shield` `F11AccessibilityService`: feeds view-id events into `core`'s `ScrollTracker` (swipe counting, debounce, budget), shows `CounterOverlay`, and reports back through `ShieldClient`.

Feed detection (which screens count as Reels/Shorts/Spotlight) lives in `TargetAppsConfig`; it matches package names and view resource IDs only.

Note: `:core` source uses the `com.f11.app.*` package even though its namespace is `com.f11.core`.

## Constraints

- **No `INTERNET` permission** in either manifest; this is a core product promise (see manifest comments, README, PRIVACY.md). Don't add networking or analytics dependencies.
- The Accessibility service must read only package names and view resource IDs, never on-screen text.
- Loosening a rule (turning off Max Focus, editing a running schedule, raising limits) must go through the friction flow: wrap the change in `relax(description) { ... }` in `F11App.kt`, which shows `FrictionCooldownDialog` (30 s pause + typing a line from `FrictionLines`) before applying it. Tightening rules applies immediately. A running schedule can't be ended, edited or deleted.
- When bumping versions, update `versionCode`/`versionName` in `app/build.gradle.kts` and/or `shield/build.gradle.kts` and add a `CHANGELOG.md` entry.
