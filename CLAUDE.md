# Browser — Android App

## What This Is

A restricted/kiosk-style Android web browser. Users can only visit pre-approved sites and personal bookmarks. Designed for controlled environments (e.g. educational or enterprise use).

- **Package:** `com.browser.app`
- **Language:** Java (100%, no Kotlin)
- **Min SDK:** 26 (Android 8) / Target SDK: 34 (Android 14)
- **Orientation:** Landscape only

## Architecture

### Activities

| Class | Role |
|-------|------|
| `HomeActivity` | Launcher screen — fetches and displays allowed sites and bookmarks from GitHub |
| `BrowserActivity` | Multi-tab WebView browser with URL allowlisting |

### Key Classes

| Class | Role |
|-------|------|
| `Allowlist` | Fetches allowed hostnames from GitHub at runtime; validates URLs before loading |
| `Bookmarks` | Fetches bookmark URLs from GitHub |
| `FullScreen` | Immersive full-screen utility (API-level aware: pre/post Android 11) |

### Remote Config (GitHub)

- **Allowlist:** `https://raw.githubusercontent.com/allenlkq/browser/main/allowlist`
- **Bookmarks:** `https://raw.githubusercontent.com/allenlkq/browser/main/bookmarks`

Both are fetched at app startup with a 10-second timeout.

## Features

- **Multi-tab browsing** — horizontal tab bar with closeable chips
- **URL allowlisting** — only hostnames in the allowlist can be navigated to; static resource hosts (Google, Instructure/Canvas) are always permitted
- **Google Sign-In** — redirects to Chrome Custom Tabs for auth; returns to `aistudio.google.com` after 2FA challenges
- **Full-screen immersive UI** — hides system bars; back gesture re-shows them on Android 11+
- **Popup windows** — open as new tabs instead of separate windows

## Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

**Dependencies:**
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.11.0`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `androidx.browser:browser:1.7.0` (Chrome Custom Tabs)

## UI / Theme

- Dark background: `#111827`
- No action bar; full-screen theme
- `Theme.Browser` (BrowserActivity) — fully immersive
- `Theme.Browser.Home` (HomeActivity) — non-fullscreen

## Notes

- ProGuard is disabled — not production-hardened
- No tests exist
- Networking uses plain `HttpURLConnection` (no OkHttp/Retrofit)
- WebView has file access and geolocation disabled for security
- Third-party cookies are enabled (needed for auth flows)
