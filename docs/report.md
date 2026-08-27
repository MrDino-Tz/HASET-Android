# Doctor Registration Payment Blocked by Email-Verification Gate

**Date:** 2026-08-27
**App:** AfyaHASET — `com.haset.hasetapp`
**Module in scope:** Doctor signup / registration fee payment
**Status:** Root cause confirmed — blocker is on the deployed production backend

---

## 1. Executive Summary

A new doctor attempting to sign up through the app reaches the **payment screen**, enters a mobile
number, and is blocked with the error:

> **"Please verify your email address before continuing."**

The error dialog is surfaced at the payment step (`PaymentActivity`), and the user is returned to
the registration screen with the message "The payment request was rejected or did not succeed." The
doctor account is **never created**, so no verification email is ever sent and the signup cannot
complete.

The Android-side fixes (password policy, phone normalization, doctor-flow logging, soft-fail
handling) are **working as intended** — the logs prove the flow now reaches the payment gate and
surfaces the real blocker instead of failing silently. The remaining blocker is **external to the
Android repo**: the deployed production payment backend refuses the transaction because the
authenticated payment session has **no verified email**.

---

## 2. Symptom (from Logcat)

The newly introduced `HASETDoctorFlow` tag captures the failure:

```
2026-08-27 17:44:37  HASETDoctorFlow  PaymentActivity created (doctor_registration=true)
2026-08-27 17:44:57.982 HASETDoctorFlow  PaymentActivity error (doctor_registration=true):
                                           Please verify your email address before continuing.
2026-08-27 17:44:58  HASETDoctorFlow  error dialog shown (doctor_registration=true)
```

- The payment screen was reached (fee loaded `2000.0`, anonymous sign-in succeeded).
- The user entered their mobile number and tapped **Pay**.
- The payment API returned the error immediately (no USSD push, no polling).
- The `PaymentActivity` error observer logged it and `RegisterActivity.setupPaymentLauncher`
  treated the non-OK result as a soft failure, cleaning up the anonymous session.

---

## 3. Root Cause

### 3.1 The doctor signup flow (app side)

Doctor registration requires an up-front fee. The app implements this as:

1. `RegisterActivity.registerUser()` — validates the form, then (for doctors) calls
   `fetchDoctorRegistrationFeeAndShowPaymentDialog(...)` — `RegisterActivity.java:414-415`.
2. `fetchDoctorRegistrationFeeAndShowPaymentDialog` reads the fee from `appConfig` and, if non-zero,
   calls `ensurePaymentAuthThenShowDoctorRegistrationPaymentDialog` — `RegisterActivity.java:421-447`.
3. `ensurePaymentAuthThenShowDoctorRegistrationPaymentDialog` signs the user in **anonymously**
   because no real account exists yet — `signInAnonymously()` — `RegisterActivity.java:460`.
4. `showDoctorRegistrationPaymentDialog` launches `PaymentActivity` with the form data, including
   `buyer_email = newUser.getEmail()` (the email typed on the form) — `RegisterActivity.java:476-492`.
5. `PaymentActivity.processPayment` sends the request to the **production payment backend** with:
   - `Authorization: Bearer <anonymous Firebase ID token>`, and
   - `buyer_email = kilindosaid773@gmail.com` (the form email) — `PaymentActivity.java:921-960`.
6. Only **after a successful payment** does the app actually create the real doctor account:
   `setupPaymentLauncher` → `authViewModel.register(doctorEmail, doctorPassword, doctorUser)`
   — `RegisterActivity.java:498-507`.

### 3.2 The chicken-and-egg conflict

The **deployed** payment backend (`https://payments.hasethospital.or.tz/public/api/`) requires the
**authenticated user** making the payment request to have a **verified email**.

- The authenticated user in the doctor flow is the **anonymous** Firebase session
  (from step 3) — it has **no email at all**, let alone a verified one.
- The real doctor account (`kilindosaid773@gmail.com`) does not exist yet and is only created
  **after** payment succeeds (step 6).
- Therefore the payment can **never** pass: the backend demands a verified email on a session whose
  verified email cannot exist until the payment succeeds.

This is the definitive blocker, confirmed by the absence of the error string in the entire Android
codebase (grep found **no** local source of "Please verify your email address before continuing.")
and by the request flow in `PaymentRepository.java:113-166` / `withFirebaseAuthHeader`
(`PaymentRepository.java:420-440`).

### 3.3 Where the check actually lives

The error string is **not** produced by any code in the Android repo, and it is **not** produced by
the local `HASET-Backend` Laravel repo either:

- `HASET-Backend/app/Http/Controllers/Api/PaymentController.php` — does **not** read
  `buyer_email`, does **not** check email verification, and ignores the Firebase token.
- `HASET-Backend/app/Services/SnippePaymentService.php` — sends a **hardcoded**
  `customer@haset.app` email to Snippe (`SnippePaymentService.php:54-58`).

The gate therefore lives in the **deployed production backend** at
`payments.hasethospital.or.tz`, which validates the Bearer Firebase ID token, resolves the
authenticated Firebase user, and enforces `emailVerified` before initiating payment. That deployed
backend is **not present** in either local repo
(`/home/mrdino/AndroidStudioProjects/HASETApp` or
`/home/mrdino/Desktop/DTC/HASET/HASET-Backend`).

---

## 4. Evidence

| Item | Location | Detail |
|------|----------|--------|
| Error surfaced in payment observer | `PaymentActivity.java:216-256` | Logs the `HASETDoctorFlow` error line |
| App payment base URL | `Constants.java:77-79` | `https://payments.hasethospital.or.tz/public/api/` |
| Anonymous auth for payment | `RegisterActivity.java:460` | `signInAnonymously()` |
| buyer_email passed to payment | `RegisterActivity.java:488` / `PaymentActivity.java:932` | Form email `kilindosaid773@gmail.com` |
| Registration only after payment | `RegisterActivity.java:498-507` | `authViewModel.register(...)` |
| Firebase token sent as Bearer | `PaymentRepository.java:420-440` | `getIdToken(true)` → `"Bearer " + token` |
| Error string absent from Android repo | grep | "Please verify your email address before continuing." → **0 matches** |
| Local backend ignores email/verification | `PaymentController.php` / `SnippePaymentService.php` | No email-verification gate, hardcoded email |

---

## 5. Scope of the Issue

- **Affected:** Doctor registration when the configured `doctorRegistrationFee` is non-zero.
  (Free registration, `fee == 0`, bypasses payment entirely — `RegisterActivity.java:432-435`.)
- **Not affected:** Regular patient registration (no fee, direct `authViewModel.register`) and all
  post-registration payments (performed by a real, verified logged-in user).
- **Impact:** New doctors cannot complete signup → cannot be onboarded. Blocking for the product.

---

## 6. Recommended Resolutions (for discussion)

Because the verified-email gate is enforced by the **deployed production backend** (not in either
local repo), this cannot be resolved purely from the Android repo. Options:

1. **Verified first, pay second (most correct).** Change the doctor flow to create the real Firebase
   account and send the verification email **before** payment, then pay as the real (possibly
   verified) user, then finish profile setup. Requires the backend to accept the real user's token
   for the transaction and to keep enforcement on that user's verification status.

2. **Exempt the registration fee from the email gate (backend change).** Allow
   `doctor_id == "doctor_registration"` transactions through the anonymous/verification-gated
   session, or validate the passed `buyer_email` instead of the token holder's verification.

3. **Temporarily bypass only for registration (backend change).** Short-term unblock so onboarding
   is not 100% blocked, with the real verification still enforced at first login.

4. **Keep as-is and document.** No code change now; file as a known blocker pending a backend/design
   decision.

---

## 7. Appendix — Confirmed Working (from this session)

The following Android-side fixes are verified operational via `HASETDoctorFlow` logs and are **not**
the cause of the current failure:

- **Generator/policy alignment:** password validator now enforces 12+ chars, lower + upper + digit,
  no symbol, matching the Firebase console password policy.
- **Phone normalization:** `+2550XXXXXXXX` → `+255XXXXXXXX` etc. in `RegisterActivity.java:365-368`.
- **Doctor-flow logging:** distinct `HASETDoctorFlow` tag across fee fetch, anonymous auth, payment
  success/failure.
- **Soft-fail payment → register:** non-OK result now cleans up the anonymous session and returns
  the user to the form with a clear message instead of leaving a dangling state
  (`RegisterActivity.java:494-524`).
- **Login rate limiting:** 3-attempt / 5-minute client-side lockout.
