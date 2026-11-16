# F11 Privacy Policy

_Last updated: 15 November 2026_

F11 ("the app") helps you limit scrolling in short-form video feeds. It is designed to work entirely on your device.

## What the app does not do

- It does **not** collect, transmit, sell or share any personal data.
- It has **no internet permission**, so it cannot send data anywhere.
- It does **not** read on-screen text, messages, passwords, photos or videos, and it does **not** record your screen or audio.
- It contains no ads, analytics or third-party SDKs.

## Usage access and Display over other apps (F11)

F11 uses Usage access only to learn which app is in the foreground (its package name), and "Display over other apps" only to cover an app you chose to guard during Max Focus or a focus schedule. While either is active, a notification shows that F11 is watching.

## Accessibility Service (optional F11 Shield plugin)

The optional F11 Shield plugin uses Android's Accessibility API (AccessibilityService) only to:

1. Detect which app is in the foreground (package name).
2. Detect when you swipe in short-form video feeds (Instagram Reels, YouTube Shorts, TikTok, Facebook Reels, Snapchat Spotlight), using the technical resource IDs of the scrolling views.
3. Draw a full-screen shield over those apps when a limit or focus time applies.

The plugin keeps no data of its own. It reads your settings from F11 and reports swipe and shield counts to F11, on your device, through a channel only apps signed by F11's developer can use. Everything else is processed in memory and never stored or shared.

## Data stored on your device

Kept in F11's private storage, and removed when you uninstall:

- Daily swipe and shield counts (last 90 days).
- Your settings: limits, cooldown, guarded apps, focus schedules, language.

## Children

F11 is not directed at children under 13 and collects no data from anyone.

## Changes and contact

Any changes to this policy will be published in this file. Questions: open an issue in this project's repository.
