# Migrating AfyaHASET to a New Firebase Project

**App:** AfyaHASET — `com.haset.hasetapp`
**Repo root:** `/home/mrdino/AndroidStudioProjects/HASETApp`
**Current Firebase project:** `hasetapp-4eeba`
**Date:** 2026-08-28

---

## 1. Overview

This guide walks through shifting AfyaHASET from the current Firebase project to a **brand-new,
empty Firebase project** (new project id + new credentials).

The Android app is fully **configuration-driven**: all Firebase wiring (Authentication, Realtime
Database, Cloud Storage, Cloud Messaging) comes from a single file, `app/google-services.json`.
There are **no hardcoded Firebase URLs in the Android Java code**, which keeps the app-side change
small. The heavier work is on the data, rules, functions, and the separate payment backend.

A companion feature — an **admin-panel "Storage" tab** that downloads the current Firebase data and
re-uploads it into the new project — is planned. This document focuses on the end-to-end manual
walkthrough; the Storage tab will automate the "download" and "restore" steps described in
[Section 6](#6-data-download--restore).

---

## 2. What depends on Firebase (complete inventory)

| Area | Where | File / Source |
|------|-------|---------------|
| Authentication | app | `app/google-services.json` |
| Realtime Database | app, functions | RTDB refs via `FirebaseHelper`; `functions/index.js` |
| Cloud Storage | app | `FileUploadHelper` (bucket from `google-services.json`) |
| Cloud Messaging (FCM) | app, backend | `MyFirebaseMessagingService` + dashboard sync; push used by functions |
| Firebase Cloud Functions | backend | `functions/index.js` (hardcoded DB URL) |
| RTDB security rules | project | `database.rules.json` / `database.rules.prod.json` / `database.rules.merged.json` |
| Firebase CLI project alias | tooling | `.firebaserc` |
| Payment backend token validation | separate repo | `HASET-Backend` (verifies Firebase ID tokens) |
| iOS REST config | iOS (optional) | `ios/HASET/Services.swift` |

> **Not affected by the Firebase shift:** the hosted payment backend
> `payments.hasethospital.or.tz` is on Hostinger (PHP/Laravel), **not** Firebase. Its URL lives in
> `app/src/main/java/com/haset/hasetapp/utils/Constants.java` and stays the same.

---

## 3. Step 1 — Create the brand-new Firebase project (console)

1. Go to the [Firebase console](https://console.firebase.google.com) and **Add project**.
   - Choose a **new project id** (e.g. `hasetapp-v2`). This id is permanent once set.
   - Note the new project id — you will need it throughout this guide.
2. **Register the Android app** with the exact package name `com.haset.hasetapp`.
   - Register the debug **SHA-1 / SHA-256** fingerprints used on test devices.
   - Download the generated `google-services.json` for this project.
3. (If iOS is shipped) **Add an iOS app** and download `GoogleService-Info.plist`, or note the new
   **Web API key** + DB URL for the REST config in `ios/HASET/Services.swift`.
4. **Enable the services** the app depends on:
   - **Authentication** → Sign-in methods → Email/Password + Anonymous (Google if used).
   - **Realtime Database**.
   - **Cloud Storage**.
   - **Cloud Messaging (FCM)**.
5. **Re-apply the password policy** in Authentication → Settings (min **12** chars, lower + upper +
   digit, **no symbol**, "Require enforcement"). The app validator is already aligned to this.
6. Copy across any **Push Notifications / FCM server key** config that the payment backend or any
   server uses to send notifications (if applicable).

---

## 4. Step 2 — App changes (Android)

Because the app is config-driven, this is the smallest step:

1. **Replace** `app/google-services.json` with the new project's file.
   - No Java changes are required (no hardcoded Firebase URLs in Android code).
2. Rebuild the **signed release** bundle and bump `versionCode` before re-uploading to the
   internal-testing track.
3. Re-test: login, dashboard, chat, appointments, doctor registration (fee), profile photo upload
   (Storage), and push notifications.

---

## 5. Step 3 — Realtime Database rules + data

### 5.1 Rules
1. Open the **Realtime Database** rules editor in the new project.
2. Paste the rules from **`database.rules.json`** (or `database.rules.prod.json`, whichever is the
   current production set — verify before choosing).
3. Save and publish.

### 5.2 Storage rules
- The new project gets a **new storage bucket name**. There is **no `storage.rules` file** in the
  repo, so you must:
  - Open the **Cloud Storage** rules editor in the **old** project, copy the rules, and paste them
    into the **new** project's bucket, **or** define them fresh.

### 5.3 Data
- This is the largest task. See [Section 6](#6-data-download--restore) for the download/restore
  options. The planned **admin-panel Storage tab** will automate exactly this copy.

---

## 6. Step 4 — Data download & restore

This is the part the **admin-panel Storage tab** will automate: **download** the current Firebase
data as a JSON export, then **restore** it into the new project.

### 6.1 What to export (download)
| Source | What | Method |
|--------|------|--------|
| Realtime Database | Full JSON of all nodes | RTDB "Back up to JSON" / admin SDK export |
| Cloud Storage | All files (profile photos, chat attachments, article images) | `gsutil -m cp -r` or admin SDK list+download |
| Authentication | User accounts (emails, uid mapping) | Admin SDK `listUsers` (optional; auth is often re-seeded) |

The critical path for a **service/account shift** is the **Realtime Database JSON export**, because
all app state (users, doctors, appointments, messages, wallets, audit logs, `app_config`,
`promotional_banners`) lives there and references users by uid.

### 6.2 Restore order (into the new project)
1. **Before** restoring RTDB data: seed `app_config` (doctor registration fee) if the source is
   empty or if you want to start fresh.
2. Import the RTDB JSON export.
3. Upload the storage files to the new bucket, preserving paths.
4. Reconcile **auth uids** if you re-created accounts (RTDB stores uid as keys, so any new uid set
   must match, or a mapping step is needed).

### 6.3 Export caveats
- Firebase console **Back up to JSON** works for reasonably sized databases; very large DBs may need
  a programmatic export via the Admin SDK.
- Referenced **storage download URLs are firebaseapp URLs** tied to the old project. After switching
  projects these stored URL strings may point to the old bucket. Plan to **rewrite** those URL
  prefixes (or re-resolve) so images still load from the new bucket.
- **New project is currently empty** — so the full export/restore is a one-time "lift and shift".

---

## 7. Step 5 — Cloud Functions

`functions/index.js` hardcodes the old database URL. Update it to the new project:

1. Edit **`functions/index.js:6-8`**:
   ```js
   initializeApp({
     databaseURL: 'https://<NEW-PROJECT-ID>-default-rtdb.firebaseio.com',
   });
   ```
   (Use the new project's Realtime Database URL — note the `default-rtdb` region if the new project
   is created in `europe-west1` or another region; match the actual URL shown in the console.)
2. Edit **`functions/test/database.rules.test.mjs:11`** (`projectId = "..."`) to the new project id
   if you run the rules tests.
3. Point the Firebase CLI at the new project:
   ```bash
   firebase use <NEW-PROJECT-ID>
   ```
   (or update `.firebaserc` manually).
4. Redeploy:
   ```bash
   firebase deploy --only functions
   ```
5. The three functions (`onNewAppointment`, `onNotificationCreated`, `onChatMessageCreated`) are
   **data-triggered** via `getDatabase()` and will follow the new URL automatically after redeploy.

---

## 8. Step 6 — Payment backend (separate repo: HASET-Backend)

The payment backend validates **Firebase ID tokens** for the `/api/payment/*` endpoints (the app
sends `Authorization: Bearer <firebase id token>` from `PaymentRepository`).

1. In `HASET-Backend`, update the **Firebase service-account / project id** used to verify tokens so
   it accepts tokens minted by the **new** project (JWT audience, project id, verification keys).
2. Update any Firebase Admin SDK init or config in that backend.
3. Redeploy the backend.
4. `EMAIL_VERIFICATION_API_URL` / `PASSWORD_RESET_EMAIL_API_URL` (`Constants.java:80-81`) only need
   attention if those mobile endpoints verify against the old Firebase project.

---

## 9. Step 7 — iOS (only if iOS is shipped)

`ios/HASET/Services.swift` hardcodes Firebase REST config (lines 8–9):

```swift
static let firebaseAPIKey = "AIzaSyB6XncMhXdlT0fScdU6Fq7Nw_toPmf-tRU"
static let firebaseDatabaseURL = "https://hasetapp-4eeba-default-rtdb.europe-west1.firebasedatabase.app"
```

1. Replace `firebaseAPIKey` with the **new project's Web API key**.
2. Replace `firebaseDatabaseURL` with the **new project's RTDB URL**.
3. If the iOS app uses the Firebase SDK directly, drop in the new `GoogleService-Info.plist`.

---

## 10. Step 8 — Post-migration verification checklist

- [ ] `google-services.json` is the new project's file
- [ ] Login / dashboard load without crashing
- [ ] Chat & appointments work (proves RTDB + rules)
- [ ] Profile photo upload works (proves Storage bucket + rules + URL rewrite)
- [ ] Push notifications arrive (proves FCM + functions)
- [ ] Doctor registration reaches payment (fee from `app_config`) — note the current known
      email-verification payment blocker still applies
- [ ] Cloud Functions logged as firing in the new project
- [ ] Payment backend accepts new-project ID tokens
- [ ] `versionCode` bumped and a new signed internal-testing bundle uploaded

---

## 11. Future: Admin-panel "Storage" tab (planned)

A web **admin panel** is planned to handle the data migration through a new **Storage** tab that:

- **Downloads** the current project's RTDB JSON and Storage files.
- Validates the export (size, node coverage).
- **Restores** the JSON + files into the new project (new bucket + RTDB).
- (Optionally) rewrites storage URL prefixes so images keep loading.

Until that feature ships, use the manual steps in [Section 6](#6-data-download--restore) to perform
the same download/restore.
