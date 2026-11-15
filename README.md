<p align="center">
  <img src="media/play-store/feature-graphic-1024x500.png" alt="F11 — Stop the scroll" width="100%">
</p>

# F11 · Anti-BrainRot Guard

**One life. Stop the scroll.** F11 is an Android app that counts every swipe you make in short-form video feeds, caps Reels and Shorts with a swipe budget, and blocks whole apps on a schedule. It runs **100% offline**: the app has no internet permission, so nothing it sees can ever leave your phone.

<p align="center">
  <img src="media/screenshots-overview.png" alt="F11 screenshots" width="100%">
</p>

## Features

| | |
|---|---|
| **Scroll awareness** | A live counter pill at the top of Reels and Shorts. Swipes are counted even with the guard off. |
| **Swipe budget** | Allow *N* swipes per session; the next one raises a full-screen shield for a cooldown. |
| **Feeds only** | Only Reels / Shorts / TikTok / Spotlight count. DMs, stories and regular YouTube videos stay open. |
| **Block completely** | Optionally block Reels & Shorts on the first swipe. |
| **Max Focus** | Block guarded apps entirely: feeds, DMs, stories and videos. |
| **Focus schedules** | Max Focus turns on by itself (Work, Study, Sleep…). A running schedule can't be ended, edited or deleted. |
| **Intentional friction** | Loosening any rule takes a 30 s pause and typing a hard-hitting line, plus an optional spoken line (on-device speech). |
| **English & हिन्दी** | Strong lines in English and Hindi (with masculine/feminine verb forms). |
| **Report** | 7 / 30 day history, daily average, busiest day, shields raised, focus streak. |

Guarded apps: Instagram (Reels), YouTube (Shorts), TikTok, Facebook (Reels), Snapchat (Spotlight).

## Install

Download **F11-v1.0.apk** from the [latest release](https://github.com/1elevenop/F11-AntiBrainRot/releases/latest), open it, then in F11 tap **Enable** and turn on **F11 Shield** in Accessibility settings. If Android shows **"Restricted setting"**, open **Settings → Apps → F11 → ⋮ → Allow restricted settings** and try again. Requires Android 8.0 or later.

### "App blocked to protect your device" (Google Play Protect)

In some countries, including **India**, Google Play Protect's *enhanced fraud protection* blocks any app installed from a browser, messaging app or file manager if it uses an **accessibility service**. F11 needs one to see Reels and draw the shield, so it's blocked before installation. This is a blanket rule for that permission, not a finding about F11, and the dialog has no "install anyway" option.

Options, best first:

1. **Google Play:** not available there yet. Apps installed from Google Play aren't affected by this block.
2. **USB install (adb):** `adb install F11-v1.0.apk` from a computer isn't affected.
3. **Temporarily turn off scanning (advanced, at your own risk):** Play Store → profile icon → **Play Protect → ⚙ Settings** → turn off **Scan apps with Play Protect**, install F11, then **turn scanning back on**. This lowers protection for anything installed while it's off.

You can check that your APK is genuine with the SHA-256 checksum and signing certificate fingerprint on the release page.

## Privacy

- **No `INTERNET` permission.** Verifiable in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml).
- The Accessibility service reads only **package names and view resource IDs**, never on-screen text, messages or passwords. It doesn't record the screen.
- The shield is drawn with `TYPE_ACCESSIBILITY_OVERLAY`; `SYSTEM_ALERT_WINDOW` is not requested.
- The optional voice step uses Android's **on-device** recognizer only; audio is never stored or sent.
- Only daily counts and your settings are stored, in private app storage. See [PRIVACY.md](PRIVACY.md).

## Build

Requirements: Android SDK (platform 35), JDK 17+ (Android Studio's bundled JBR works).

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it, open F11, tap **Enable**, read the disclosure and turn on **F11 Shield** in Accessibility settings.

| | |
|---|---|
| Package | `com.f11.app` |
| SDK | min 26 · target 35 · compile 35 |
| Stack | Kotlin 2.2, Jetpack Compose (Material 3), AGP 9.1, Gradle 9.9 |
| Tests | JUnit: scroll limiter, history & streaks, friction lines, focus schedules, feed detection |

## Project layout

```text
app/src/main/java/com/f11/app/
├── MainActivity.kt
├── data/       PreferencesManager, History, FocusSchedule, FrictionLines, TargetAppsConfig
├── service/    F11AccessibilityService, ScrollTracker, OverlayController, CounterOverlay
└── ui/         F11App, Home/History/Settings screens, friction & schedule dialogs, theme
media/          Play Store icon, feature graphic, store screenshots, listing copy
art/            Logo artwork
```

## Media

Everything for a store listing lives in [`media/`](media): the 512 px icon, the 1024×500 feature graphic, seven 1080×1920 framed screenshots, the raw captures, [listing copy](media/store-listing.md) and the 1280×640 [GitHub social preview](media/github-social-preview.png).

## License

[MIT](LICENSE) © 2026 Jaydeep G. (1elevenop)
