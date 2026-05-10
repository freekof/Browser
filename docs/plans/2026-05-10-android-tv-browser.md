# Android TV Browser Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an Android 9 TV WebView browser with fullscreen browsing, hidden controls, SOCKS5 settings, temporary QR phone URL input, video sniffing, and KODI handoff.

**Architecture:** The app uses one native Android activity with a fullscreen WebView and an overlay control panel. Pure Kotlin helpers handle URL normalization and video URL tracking so behavior can be unit tested without Android devices.

**Tech Stack:** Kotlin, Android Gradle Plugin, AndroidX AppCompat, WebView, OkHttp, Ktor, ZXing, JUnit, GitHub Actions.

---

### Task 1: Project Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/layout/activity_main.xml`

**Steps:**
1. Create the Gradle Android project.
2. Configure `minSdk = 28`, `targetSdk = 35`, Kotlin JVM 17.
3. Add WebView layout with hidden overlay controls.
4. Run `gradle assembleDebug` in CI.

### Task 2: Core Helpers With Tests

**Files:**
- Create: `app/src/test/java/com/freekof/tvbrowser/UrlNormalizerTest.kt`
- Create: `app/src/test/java/com/freekof/tvbrowser/VideoSnifferTest.kt`
- Create: `app/src/main/java/com/freekof/tvbrowser/UrlNormalizer.kt`
- Create: `app/src/main/java/com/freekof/tvbrowser/VideoSniffer.kt`

**Steps:**
1. Test bare host normalization to `https://`.
2. Test search text normalization to Google search.
3. Test video extension detection and de-duplication.
4. Implement minimal helper code.
5. Run `gradle testDebugUnitTest`.

### Task 3: First Browser Activity

**Files:**
- Create: `app/src/main/java/com/freekof/tvbrowser/MainActivity.kt`

**Steps:**
1. Enable WebView JavaScript and DOM storage.
2. Set default Chrome Android user agent.
3. Show controls on mouse right-click or Android back key.
4. Navigate from address bar.
5. Record video URLs from WebView requests.
6. Open detected videos through KODI using `Intent.ACTION_VIEW`.

### Task 4: GitHub Actions APK Build

**Files:**
- Create: `.github/workflows/android-apk.yml`

**Steps:**
1. Checkout code.
2. Install JDK 17.
3. Install Gradle 8.9.
4. Run unit tests.
5. Build debug APK.
6. Upload APK artifact.

### Next Milestones

1. Add temporary QR phone input service and QR dialog.
2. Add settings screen for SOCKS5 and User-Agent.
3. Add tabs, bookmarks, downloads, and privacy mode.
4. Improve video sniffing with injected JavaScript.
5. Add release signing workflow when keystore is available.
