# HASETApp Security Audit Report

**Project:** HASETApp (com.haset.hasetapp) — Firebase project `hasetapp-4eeba`
**Date:** 2026-08-14
**Method:** Static analysis of Android source, iOS source, Firebase rules, Cloud Functions, and git history. Research only.
**Overall rating:** ~6.5/10 — good network/payment hygiene, but significant gaps in data-at-rest encryption and the deployed Firebase rules.

---

## Executive Summary

The app has strong transport security (HTTPS-only, system-only trust anchors, `allowBackup=false`) and a compliant hosted-checkout payment flow. The serious issues are:

1. **Critical — Cloudinary API secret committed to git history** (rotated account access; removed from code but still recoverable).
2. **Critical — Firebase appointments rule allows any authenticated user to overwrite any appointment** (IDOR / authorization bypass) in the previously deployed rules. *(Patched in `database.rules.prod.json` — see Findings.)*
3. **High — No encryption-at-rest.** Room DB (prescriptions, payout/bank account numbers, PII) and SharedPreferences are plaintext.
4. **High — Prescription PDFs written to world-readable external storage on Android ≤ 9.**
5. **High — Live GPS coordinates logged to logcat.**
6. **High — Payment webhook verification cannot be confirmed** (backend is external; API docs show no signature check).

---

## CRITICAL

### 1. Cloudinary API Secret in Git History
- **Location:** historic `app/src/main/res/values/strings.xml`, `CloudinaryUploadHelper.java`, `HASETApplication.java` — commits `d6da887d`, `e298eb6b`, `567c8a8a`, `775e1e27`.
- **Value (truncated):** cloud_name `divky8yna`, api_key `672749…`, api_secret `Cx3sdw…`
- **Risk:** Full-account credential (upload/delete/manage) recoverable by anyone with repo access via `git log -p`. Removed from current working tree but **must be rotated externally**.
- **Remediation:** Revoke/rotate the credential in the Cloudinary dashboard immediately. Consider `git filter-repo` to purge history.

### 2. Appointments IDOR — Authorization Bypass
- **Location:** deployed rules `appointments/$appointmentId` `.write = "auth != null"` (was present in the rules previously deployed to production; the repo's `database.rules.json` always had the secure version).
- **Risk:** Any authenticated user can overwrite **any** appointment — including setting `status: "approved"` without paying — which unlocks gated chat (`ChatActivity.checkAppointmentStatus` gates on `STATUS_APPROVED`).
- **Status:** **FIXED** — `database.rules.prod.json` and `docs/firebase-rules-corrected.md` now carry the secure, owner-scoped `appointments` rule from `database.rules.json`.
- **Action required:** Confirm the patched file is what is deployed to production, and verify the live rules in the console.

---

## HIGH

### 3. No Encryption-at-Rest
- **Location:** `AppDatabase.java:2746-2747` (plain `Room.databaseBuilder`, no `.openHelperFactory()`/SQLCipher); `PreferenceManager.java:17` (`MODE_PRIVATE` plaintext prefs).
- **Data at risk:** prescriptions (`PrescriptionEntity.java:23-24` medicines/instructions), payout bank & mobile-money **account numbers** (`WithdrawalRequest.java:28-30`), wallet balances, user PII (email/phone/reg no/age/gender), FCM token (`PreferenceManager.java:210`).
- **Also:** plaintext DB copies at `DatabaseBackupHelper.java:37-50`.
- **Remediation:** Add SQLCipher for Room + `EncryptedSharedPreferences`; remove plaintext backup helper or encrypt backups.

### 4. Prescription PDFs on World-Readable Storage (Android ≤ 9)
- **Location:** `fragments/PrescriptionDetailFragment.java:362-375`, `utils/PrescriptionDetailBottomSheet.java:348-362` — `Environment.getExternalStorageDirectory()` → `/sdcard/HASET/Prescriptions/`.
- **Risk:** Any app with storage permission can read medical PDFs on pre-Android-10 devices. `.nomedia` only hides them from the gallery.
- **Remediation:** Write to app-private storage (`getExternalFilesDir(...)`/`getFilesDir()`) and share via `FileProvider` when exporting.

### 5. Live GPS Coordinates Logged
- **Location:** `LocationService.java:66,93` — `Log.d` writes `location.getLatitude() + "," + location.getLongitude()`.
- **Risk:** Precise live location harvestable from logcat (READ_LOGS apps on older Android, bug reports).
- **Remediation:** Remove or strip coordinates from logs.

### 6. Payment Webhook Verification Not Verifiable
- **Location:** `from ZENOPAY/BACKEND_REQUIREMENTS.md:133-236` requires `x-api-key` verification; `from ZENOPAY/PAYMENT_API_DOCUMENTATION.md:260-281` documents `/api/payment/callback` with **no** signature check. The deployed backend is external and not in this repo.
- **Risk:** If the live `/api/payment/callback` lacks a shared-secret/signature check, an attacker can forge `payment_status: COMPLETED` and settle unpaid transactions.
- **Remediation:** Confirm (or implement) server-side verification of a webhook secret/signature on the external backend.

### 7. Stale Firebase Dependencies
- **Location:** `app/build.gradle:86` Firebase BOM `32.7.0` (Oct 2023); `app/build.gradle:3` google-services plugin `4.4.0`.
- **Risk:** Past security-relevant fixes for auth/database/messaging components.
- **Remediation:** Upgrade BOM + plugin (and verify compiled deps for CVEs).

---

## MEDIUM

| # | Finding | Location |
|---|---|---|
| 8 | No TLS certificate pinning (platform-default trust only) | `api/RetrofitClient.java` |
| 9 | `service_payment_requests` transaction-ID replay — one successful `payment_transactions/{id}` can mark multiple service requests `paid` | `database.rules.prod.json` `service_payment_requests` `.write` |
| 10 | `minSdk 24` (Android 7.0, 2016) — unsupported OS, outdated TLS/WebView for a health app | `app/build.gradle:12` |
| 11 | Payment/sensitive data logged: transaction IDs, amounts, provider | `PaymentRepository.java:117-118,242,286,299-304`; `PaymentActivity.java:158-161,910` |
| 12 | FCM token + PII in plaintext SharedPreferences | `PreferenceManager.java:22-77,210`; `MyFirebaseMessagingService.java:391-402` |
| 13 | WebView gaps: `http://` subresources allowed in checkout WebView; `allowFileAccess`/`allowContentAccess` not disabled in maps WebView (API<30 default) | `HostedCheckoutActivity.java:100-102`; `HospitalsLocationBottomSheet.java:57-67` |
| 14 | Conflicting rules files in repo (secure `database.rules.json` vs previously insecure `database.rules.prod.json`) — now aligned after patch | repo root |
| 15 | Firebase API key hardcoded in client + iOS and committed (public-by-design, but should be restricted + App Check) | `app/google-services.json:24`; `ios/HASET/Services.swift:8` |
| 16 | User UID, patient names/IDs logged | `FileUploadHelper.java:60`; `PatientNotificationManager.java:134,239,312,558,642,714` |
| 17 | Location permissions (Fine+Coarse) requested together | `AndroidManifest.xml:12-13` |
| 18 | `data_extraction_rules.xml` is an unmodified template with TODO | `app/src/main/res/xml/data_extraction_rules.xml:8` (Low impact: `allowBackup=false` already disables backups) |

---

## LOW

- Missing explicit `android:exported` on 6 activities (`AndroidManifest.xml:31-39,209`) — safe by default at targetSdk 36, but should be explicit.
- `ShimmerTestActivity` shipped in production manifest (`AndroidManifest.xml:168-170`).
- Material version conflict: catalog `1.13.0` vs hard-pinned `1.11.0` (`gradle/libs.versions.toml:7` vs `app/build.gradle:130`).
- `FileProvider` exposes broad `/sdcard/HASET/` path (`res/xml/file_paths.xml:12`).
- Release signing silent-fallback to unsigned APK if `haset-release.jks` missing (`app/build.gradle:33-52`).
- Cloudinary unsigned upload preset (public-by-design; prefer backend-signed uploads).
- Test credentials documented in `docs/features1.0/TEST_CREDENTIALS.md` (test-only accounts).

---

## Reconciliation of the Independent Audit Report

The following claims from the independent report were **verified and corrected**:

1. **Debug URL `http://192.168.1.126:8000` — NOT present** in any Java/Swift/Kotlin/XML source. Stale finding.
2. **"No `printStackTrace`" — false.** 9 occurrences: `AppointmentsFragment.java:154,281`, `PrescriptionDetailFragment.java:357,381`, `PrescriptionDetailBottomSheet.java:343,370,566`, `AppointmentReminderHelper.java:96`, `ChatAdapter.java:773`.
3. **"No sensitive logging" — overstated.** PII is logged via `Log.d` (GPS coords, names, transaction IDs) — see findings #5, #11, #16.
4. **`data_extraction_rules.xml` TODO — real but Low severity** (backups already disabled via `allowBackup=false`).
5. **Cloudinary unsigned preset — Low, not Medium** (public-by-design).
6. **Firebase API key — Medium, not High** (public-by-design; restrict + App Check).

**Missed by the independent report (found here):** appointments IDOR (Critical), no encryption-at-rest incl. bank account numbers (High), prescription PDFs on shared storage (High), GPS logging (High), webhook verification gap (High), stale Firebase BOM (Medium), transaction-ID replay (Medium).

---

## Security Strengths (Verified)

- HTTPS enforced at two layers: `usesCleartextTraffic="false"` (`AndroidManifest.xml:28`) + `network_security_config.xml` (system-only trust anchors, allowlisted HTTPS domains).
- Payment: hosted checkout (PAN/CVV never stored), screenshot blocking (`SensitiveActivityHelper`), success only from server-confirmed polling — never client-trusted.
- `android:allowBackup="false"`, `fullBackupContent="false"` (`AndroidManifest.xml:19-21`).
- FCM service `exported="false"`, `FLAG_IMMUTABLE` PendingIntents, `VISIBILITY_PRIVATE` notifications.
- No SQL injection (Room bind params only), no Firebase path injection, no SSL-bypass/trust-all code.
- Passwords never stored locally (Firebase Auth only); Firebase ID tokens never persisted (on-demand `getIdToken`).
- Release build: R8 minify + resource shrinking; signing via environment variables (no secrets in build files).
- No dangerous SMS/contacts/storage permissions; no deep-link attack surface; no `addJavascriptInterface`.

---

## Recommendations by Priority

### Immediate
1. Rotate/revoke the Cloudinary secret in git history.
2. Confirm the patched `database.rules.prod.json` (secure `appointments` rule) is the file deployed to production.
3. Implement `EncryptedSharedPreferences` + SQLCipher (or remove plaintext DB backups).
4. Stop writing prescription PDFs to shared external storage.
5. Remove GPS coordinates from logcat.

### Short-term
6. Add Firebase App Check + restrict the API key.
7. Move Cloudinary uploads to backend-signed presets.
8. Add certificate pinning for payment/API endpoints.
9. Upgrade Firebase BOM + google-services plugin.
10. Fix WebView `allowFileAccess`/`http` allowances.

### Long-term
11. Encrypt prescription data and payout account numbers at rest.
12. Add security logging, tamper detection, and rate limiting on sensitive operations.
13. Remove stale test credentials from docs/history.
14. Confirm HIPAA/GDPR posture (BAA with Firebase), privacy policy review.

---

## Compliance Notes

- **PCI DSS:** Card payments use hosted checkout — compliant.
- **HIPAA:** Medical data in Firebase — needs a Business Associate Agreement with Firebase.
- **GDPR:** User data handling needs privacy-policy review.
- **Data residency:** Firebase RTDB in `europe-west1`.
