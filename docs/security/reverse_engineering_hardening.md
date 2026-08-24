# Reverse Engineering Hardening — AfyaHASET

**Date:** 22 August 2026
**Scope:** What protects the app from being unpacked, analyzed, and tampered with — and what remains for the backend/ops side.

---

## Current Protections (verified in code)

### 1. ✅ R8 Code Obfuscation & Shrinking (release)
- `app/build.gradle`: `minifyEnabled true`, `shrinkResources true`
- Release APK classes are renamed/optimized; string resources pruned
- Effect: decompiled Java (via jadx/apktool) is hard to follow; no API keys or secrets to find (verified separately)

### 2. ✅ No Secrets in the APK
- Firebase config values (API keys, project IDs) are public identifiers by design — security is enforced server-side by Firebase rules, not by hiding them
- No payment credentials, no signing secrets embedded

### 3. ✅ Network Layer
- HTTPS-only (`cleartextTrafficPermitted=false`), system trust anchors only
- Payment WebView restricted to `https://*.snippe.me`; cleartext navigation blocked

### 4. ✅ Backup / Extraction Off
- `allowBackup="false"`, `fullBackupContent="false"` + data extraction rules — attackers can't pull Room DB / prefs via adb backup

### 5. ✅ Minimal Exported Surface
- Only `SplashActivity` (launcher) and `ForgotPasswordActivity` (reset deep link) exported

### 6. ✅ Root/Tamper Detection (NEW — this pass)
- `utils/RootIntegrityHelper.java`: passive detection of su binaries, test-keys ROMs, root-manager packages, debug builds
- Wired into `HostedCheckoutActivity`: shows a **non-blocking warning dialog** before payment on suspicious environments
- Deliberately does NOT block rooted users (health app accessibility); it informs and deters casual tampering

---

## Recommended Next Layers (backend/console side)

### A. 🔴 Firebase App Check — PENDING (highest priority, not yet wired)

**Status: NOT IMPLEMENTED.** Logcat confirms: *"No AppCheckProvider installed"* —
REST/Firestore endpoints currently accept requests from ANY client (scripts,
modified APKs). Do this when ready; steps below are copy-paste ready.

1. **Console:** Firebase Console → App Check → Apps → register Android app
   (`com.haset.hasetapp`) with the **Play Integrity** provider.
2. **Dependency** (`app/build.gradle`):
   ```gradle
   implementation 'com.google.firebase:firebase-appcheck-playintegrity'
   ```
3. **Initialize** in `HASETApplication.onCreate()`, after Firebase init:
   ```java
   com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
       .installAppCheckProviderFactory(
           com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance());
   ```
4. **Rollout:** leave App Check in **monitor mode** (non-enforcing) for 1–2 weeks;
   watch the metrics dashboard for legitimate traffic; only then flip on
   **Enforce** for Firestore / Realtime Database / Cloud Functions.
5. **Debug builds:** register a debug provider or add the debug token to the
   console allowlist, otherwise debug APK requests fail attestation once enforced.

> Free tier friendly (no Blaze required). This is the single biggest anti-tamper
> upgrade: modified APKs and curl scripts fail attestation server-side.

### B. Play Integrity API verdicts (server-side)
For high-value operations (payout approvals), verify integrity verdicts server-side before processing.

### C. Certificate Pinning (use with care)
Possible via `<pin-set>` in network_security_config.xml, but pinning without controlling cert rotation can brick all networking on certificate renewal. If pursued: pin the intermediate CA with an expiration + backup pins, and set a calendar reminder. Given Let's Encrypt 90-day rotations on hasethospital.or.tz, **App Check is the safer equivalent**.

---

## Honest Limits (all Android apps)

A determined attacker with the APK can always:
- Hook Frida at runtime to bypass client checks
- Patch out the root-warning dialog
- Read any string in the binary

**Therefore:** treat the client as untrusted. Real protection lives in:
- Firestore/RTDB rules (done — validated earlier)
- Backend authorization on every sensitive endpoint
- App Check enforcement (recommended above)

Client-side measures raise effort/cost for attackers; they don't replace server-side authority.
