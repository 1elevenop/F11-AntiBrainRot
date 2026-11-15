# Google Play listing: F11

## App name (≤ 30)
F11 · Anti-BrainRot Guard

## Short description (≤ 80)
Count every swipe. Cap Reels & Shorts. Block apps on a schedule. 100% offline.

## Full description (≤ 4000)
One life. Stop the scroll.

F11 counts every swipe you make in short-form video feeds and helps you stop when you choose to, not when the algorithm lets you.

KNOW YOUR SCROLL
• A live counter at the top of Reels and Shorts shows how many times you've swiped today.
• Swipes are counted even when the guard is off, just to keep you aware.

CAP THE FEED, NOT YOUR LIFE
• Set a swipe budget per session. Go over it and a full-screen shield takes over for a cooldown.
• Only the feeds count. DMs, stories and regular YouTube videos stay open.
• Or block Reels & Shorts completely.

MAX FOCUS
• Block Instagram, YouTube, TikTok, Facebook and Snapchat entirely: feeds, DMs, stories and videos.
• Schedule it for work, study or sleep. A running focus time can't be ended early.

FRICTION, ON PURPOSE
• Loosening any rule takes a 30-second pause and typing a hard-hitting line, like "I am sacrificing discipline for dopamine."
• Add an extra layer: say a line out loud (recognised on-device).
• Lines in English and Hindi.

SEE THE PATTERN
• 7 and 30 day reports, daily average, busiest day, shields raised and your focus streak.

PRIVATE BY DESIGN
• No internet permission. Nothing F11 sees can leave your phone.
• No ads, no analytics, no accounts.
• Reads only app names and the technical IDs of scrolling views, never text, messages or passwords.

Guarded apps: Instagram Reels, YouTube Shorts, TikTok, Facebook Reels, Snapchat Spotlight.

## Category / tags
Productivity · Digital wellbeing

## Graphics
| Asset | File |
|---|---|
| App icon 512×512 | `play-store/icon-512.png` |
| Feature graphic 1024×500 | `play-store/feature-graphic-1024x500.png` |
| Phone screenshots 1080×1920 | `screenshots/01-home.png` … `07-settings.png` |

## Accessibility API declaration (Play Console)
**Core functionality:** F11 is a digital-wellbeing app that limits scrolling in short-form video feeds. It uses the AccessibilityService API to detect which app is in the foreground and when the user swipes in Instagram Reels, YouTube Shorts, TikTok, Facebook Reels or Snapchat Spotlight (using view resource IDs, not text), and to draw a full-screen blocking overlay when the user's own limit or focus schedule applies. No data is collected or transmitted; the app has no internet permission.

**Prominent disclosure:** shown in-app before the user is sent to Accessibility settings (`ProminentDisclosureDialog.kt`), requiring an explicit "Agree and continue".

**Video:** record enabling the service, swiping Reels until the shield appears, and Max Focus blocking Instagram.

## Data safety form
- Data collected: **None**
- Data shared: **None**
- Encrypted in transit: not applicable (no network)
- Users can request deletion: uninstalling removes all data

## Privacy policy URL
Host [`PRIVACY.md`](../PRIVACY.md), e.g. via GitHub Pages or the repository's raw link.
