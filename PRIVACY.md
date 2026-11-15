# F11 Privacy Policy

_Last updated: 15 November 2026_

F11 ("the app") helps you limit scrolling in short-form video feeds. It is designed to work entirely on your device.

## What the app does not do

- It does **not** collect, transmit, sell or share any personal data.
- It has **no internet permission**, so it cannot send data anywhere.
- It does **not** read on-screen text, messages, passwords, photos or videos, and it does **not** record your screen or audio.
- It contains no ads, analytics or third-party SDKs.

## Accessibility Service

F11 uses Android's Accessibility API (AccessibilityService) for its core function. It uses it only to:

1. Detect which app is in the foreground (package name).
2. Detect when you swipe in short-form video feeds (Instagram Reels, YouTube Shorts, TikTok, Facebook Reels, Snapchat Spotlight), using the technical resource IDs of the scrolling views.
3. Draw a full-screen shield over those apps when a limit or focus time applies.

This information is processed in memory on your device and is never stored or shared, except for the counters below.

## Data stored on your device

Kept in the app's private storage, and removed when you uninstall:

- Daily swipe and shield counts (last 90 days).
- Your settings: limits, cooldown, guarded apps, focus schedules, language.

## Microphone (optional)

If you turn on **Say a line out loud**, F11 asks for microphone permission and uses Android's **on-device** speech recognizer to check that you spoke the line. Audio is not stored or sent anywhere, and the step is off by default.

## Children

F11 is not directed at children under 13 and collects no data from anyone.

## Changes and contact

Any changes to this policy will be published in this file. Questions: open an issue in this project's repository.
