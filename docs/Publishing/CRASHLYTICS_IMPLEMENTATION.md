# Crashlytics — Detailed Implementation Guide (Android)

**App:** AfyaHASET — `com.haset.hasetapp`
**Repo root:** `/home/mrdino/AndroidStudioProjects/HASETApp`
**Target Firebase project:** `hasetapp-abd47` (new)
**Date:** 2026-08-31
**Status:** Not yet implemented

---

## 1. Overview

This document details how to add **Firebase Crashlytics** to the AfyaHASET Android app so that
crashes, non-fatal errors, and Application-Not-Responding (ANR) events are recorded and visible in
the Firebase console (Crashlytics dashboard of project `hasetapp-abd47`).

**Current state (before this change):** the app has **no crash reporting at all**. All error logging
goes to Logcat only via `app/src/main/java/com/haset/hasetapp/utils/ErrorLogger.java` (tag
`HASET_ERROR`). There is no global uncaught-exception handler and no Analytics. This is the gap the
implementation fills.

**Why it's cheap to add:** `ErrorLogger` was explicitly designed as a single seam to plug a
crash-reporting backend into. Wiring Crashlytics in is mostly a Gradle/config change plus routing
`ErrorLogger` calls to `FirebaseCrashlytics`.

---

## 2. Requirements checker

Documentation minimums vs. this project's actual versions:

| Requirement (from docs) | This project | OK? |
|---|---|---|
| Gradle 8.0+ | Gradle 9.5.0 (`gradle/wrapper/gradle-wrapper.properties`) | ✅ |
| Android Gradle plugin 8.1.0+ | AGP 9.3.0 (`gradle/libs.versions.toml` → `agp`) | ✅ |
| Google services Gradle plugin 4.4.1+ | 4.5.0 (`build.gradle`) | ✅ |
| Firebase Android BoM | 34.17.0 (`app/build.gradle`) | ⚠️ see note below |

> **BoM note:** The official guidance in this doc set references `firebase-bom:34.18.0`. The app
> currently pins `firebase-bom:34.17.0`. Both work with `firebase-crashlytics` + `firebase-analytics`
> (BoM controls their versions); you may bump to 34.18.0 or stay on 34.17.0. Keep it consistent
> across the app.

---

## 3. Prerequisite — Google Analytics enabled (for breadcrumbs)

To get **breadcrumb logs** (user actions leading up to a crash) you must enable **Google Analytics**
in the Firebase project.

- For the **new** project `hasetapp-abd47`:
  - If Analytics was **not** enabled at project creation, enable it in the console:
    **Firebase console → Project settings → Integrations → Google Analytics → Enable**.
- Analytics on Android requires the `firebase-analytics` dependency (added below).

> Breadcrumbs are **recommended, not required**. Crashlytics reports crashes/ANRs/non-fatals even
> without Analytics; you just lack the "recent logs" breadcrumb trail.

---

## 4. Step-by-step implementation

### 4.1 Add the Crashlytics Gradle plugin (root `build.gradle`)

Firebase's plugin should be declared with `apply false` at the root so the applied version is shared:

**File:** `build.gradle` (root)

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    id 'com.google.gms.google-services' version '4.5.0' apply false

    // Add the Crashlytics Gradle plugin
    id 'com.google.firebase.crashlytics' version '3.0.8' apply false
}
```

> If `3.0.8` is unavailable on your toolchain, `2.9.9` is the fallback used by the docs for lower
> Gradle/AGP versions; with Gradle 9.5 / AGP 9.3 prefer `3.0.8`.

### 4.2 Apply the plugin + add SDK dependencies (app-level `app/build.gradle`)

**File:** `app/build.gradle`

Add the plugin to the `plugins` block:

```groovy
plugins {
    alias(libs.plugins.android.application)
    id 'com.google.gms.google-services'
    // Add the Crashlytics Gradle plugin
    id 'com.google.firebase.crashlytics'
}
```

Add the Crashlytics and Analytics libraries to `dependencies`. With the BoM you do **not** specify
versions for Firebase libraries:

```groovy
dependencies {
    // existing Firebase (BoM)
    implementation platform('com.google.firebase:firebase-bom:34.17.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-messaging'
    implementation 'com.google.firebase:firebase-storage'

    // NEW — Crashlytics + Analytics
    implementation 'com.google.firebase:firebase-crashlytics'
    implementation 'com.google.firebase:firebase-analytics'

    // ...rest of existing deps unchanged
}
```

### 4.3 NDK / native symbols upload (optional but recommended)

The app builds native ABIs:

```groovy
ndk {
    abiFilters 'arm64-v8a', 'armeabi-v7a'
}
```

To see native stack traces in Crashlytics, upload debug symbols for release builds. Add this inside
`android { }` in `app/build.gradle`:

```groovy
buildTypes {
    release {
        // ...existing (signing, minifyEnabled, shrinkResources)
        ndk {
            debugSymbolLevel 'FULL'   // upload full native debug symbols
        }
    }
}
```

> This uploads native symbol files for release builds to Crashlytics so NDK crashes are symbolic
> rather than raw addresses. Java/ART crashes do not require this step (R8 mapping upload is handled
> by the Crashlytics Gradle plugin automatically).

### 4.4 ProGuard / R8 (release build)

Release uses `minifyEnabled true`, `shrinkResources true`, and `proguard-rules.pro`
(`app/build.gradle`). The Crashlytics Gradle plugin automatically uploads R8 **mapping files** for
de-obfuscated stack traces, and the official Google `proguard` keep rules are bundled with the SDK
(no manual keep rules needed for Crashlytics itself).

> **Important for this app:** we previously added a custom keep rule for
> `com.haset.hasetapp.adapters.PatientBannerAdapter$BannerItem` to fix a release-only dashboard
> crash. Keep that rule intact. With Crashlytics enabled you will now actually *see* such
> release-only crashes instead of guessing.

---

## 5. Wire Crashlytics into the code

### 5.1 Initialize / no-op baseline

The Firebase App initializes automatically via the Google-services plugin. Crashlytics starts with
the SDK — there is generally **no manual `FirebaseCrashlytics.getInstance()` init required** for
basic crash reporting. It captures uncaught exceptions and ANRs automatically.

### 5.2 Route `ErrorLogger` to Crashlytics (single seam)

Update `app/src/main/java/com/haset/hasetapp/utils/ErrorLogger.java` so every existing call site
also forwards to Crashlytics:

```java
package com.haset.hasetapp.utils;

import android.util.Log;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public final class ErrorLogger {

    private static final String TAG = "HASET_ERROR";

    private ErrorLogger() {
    }

    public static void log(String userMessage, String raw) {
        Log.w(TAG, "error: " + (raw != null ? raw : userMessage));
        FirebaseCrashlytics.getInstance().log(raw != null ? raw : userMessage);
        // Non-fatal only if you want failures tracked as events; see notes below.
    }

    public static void log(Throwable throwable) {
        if (throwable == null) return;
        Log.w(TAG, "exception: " + throwable.getMessage(), throwable);
        FirebaseCrashlytics.getInstance().recordException(throwable);
    }
}
```

**Design decision — non-fatals:** Choose one:

- **A (recommended):** `log(Throwable)` calls `recordException(...)` so caught exceptions become
  **non-fatal** issues in the dashboard.
- **B (minimal):** only report uncaught crashes. Keep `recordException` out and let the SDK catch
  fatal crashes only (fewer dashboard events, no "noise" from handled exceptions).

Review the ~20 call sites using `ErrorLogger` (chat, uploads, notifications, registration, wallet,
etc.) and decide whether every handled exception should surface as a non-fatal. If you want only
genuine failures, gate `recordException` by severity.

If you prefer **not** to touch `ErrorLogger` at all, Crashlytics still captures uncaught crashes and
ANRs automatically — the `ErrorLogger` wiring only affects non-fatal coverage.

### 5.3 Global uncaught exception handler (optional)

For defense-in-depth you may add a default handler in `HASETApplication.onCreate()` that routes any
missed uncaught exception to Crashlytics before the SDK's own handler:

```java
Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    FirebaseCrashlytics.getInstance().recordException(throwable);
});
```

> This is optional; the Crashlytics SDK already registers a handler. Adding your own can capture
> cases a framework swallows, but be careful not to break the SDK's stock reporting. Only add if you
> have a specific gap.

### 5.4 Custom keys & logs (recommended enhancements)

Once basic reporting works, enrich reports from key screens:

```java
// In relevant activities/repositories
FirebaseCrashlytics.getInstance().setUserId(userUid);
FirebaseCrashlytics.getInstance().setCustomKey("screen", "PatientHome");
FirebaseCrashlytics.getInstance().log("booking appointment for " + doctorId);
```

Suggested keys: `userId`, `role` (patient/doctor), `screen`, `language` (en/sw), and the
`HASETDoctorFlow` tag context used in the doctor sign-up path.

---

## 6. Force a test crash (finish setup)

To verify Crashlytics is fully connected and see the first report in the console:

```java
Button crashButton = new Button(this);
crashButton.setText("Test Crash");
crashButton.setOnClickListener(v -> {
    throw new RuntimeException("Test Crash"); // force a crash
});
addContentView(crashButton, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT));
```

1. Add the above temporarily (e.g. in a debug-only entry point or `MainActivity`).
2. **Build and run** the app on a device/emulator.
3. **Press the "Test Crash" button** to force the crash.
4. **Restart the app** after it crashes (the pending report is flushed on next launch).
5. In the **Firebase console → DevOps & Engagement → Crashlytics** (project `hasetapp-abd47`), check
   for the test crash. If it does not appear within ~5 minutes, enable debug logging:

```java
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
// or via adb / debug to confirm delivery
```

---

## 7. Verification checklist

- [ ] `firebase-crashlytics` + `firebase-analytics` added to `app/build.gradle` deps
- [ ] Crashlytics Gradle plugin added to root `build.gradle` (apply false) and app `build.gradle`
- [ ] Google Analytics enabled in project `hasetapp-abd47` (console → Settings → Integrations)
- [ ] Release `ndk { debugSymbolLevel 'FULL' }` added (native symbols)
- [ ] `ErrorLogger` routes to `FirebaseCrashlytics` (non-fatal decision made)
- [ ] `google-services.json` is the `hasetapp-abd47` file (new project)
- [ ] Clean rebuild with no Gradle plugin/dependency conflicts
- [ ] **Test crash** appears in Crashlytics dashboard
- [ ] Existing `BannerItem` ProGuard keep rule still present (protects prior fix)
- [ ] Version bump + new signed internal-testing bundle uploaded with crash reporting verified

---

## 8. Rollback

- Remove the two `implementation` lines (crashlytics/analytics) and the plugin lines.
- Restore `ErrorLogger` to its Logcat-only original (or keep it — it's additive).
- Remove `debugSymbolLevel` if you do not want native symbol uploads.
- No data migration needed; Crashlytics data lives in the new project only.

---

## 9. References

- Official: https://firebase.google.com/docs/crashlytics/android/get-started
- Crashlytics Gradle plugin release notes (Firebase).
- App Quality Insights (AQI) in Android Studio to view crashes beside code.
