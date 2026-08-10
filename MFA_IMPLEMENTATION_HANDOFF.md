# HASET MFA Implementation Handoff

## Verified current state

The Laravel backend is located at `/Users/user/Documents/HASET-Backend`.
Its `.env` contains the payment, database, Firebase, and TOTP configuration. Secret values must not be copied into this document or committed.

The backend already implements:

- Admin TOTP setup, confirmation, status, and disable routes.
- TOTP verification during admin login.
- Fresh TOTP verification for withdrawal creation, approval, rejection, and payout-destination changes.
- Firebase-authenticated mobile payment routes.
- Hosted card checkout, so PAN and CVV are collected by the payment gateway and are not stored by HASET.

## Current integration gaps

These are not yet complete:

1. Android and iOS authenticate users through Firebase, but the backend has no Firebase-user MFA enrollment or verification API.
2. The backend has no doctor-authenticated payout request route. Withdrawal creation and approval are currently admin workflows.
3. The admin web frontend still uses Firebase login instead of the Laravel admin login/TOTP flow.
4. Android/iOS do not yet have a complete server-backed six-digit MFA challenge screen for login and payout confirmation.

Do not implement MFA by generating or checking codes locally. That would be bypassable. MFA must be verified by the backend.

## Existing backend admin routes

Base URL: use the configured backend URL; do not hard-code credentials.

### Authentication and TOTP

- `POST /api/admin/login`
  - Body: `email`, `password`, optional `two_factor_code`
  - Returns `setup_required` plus a short-lived setup token when enrollment is required.
- `GET /api/admin/2fa/status`
- `POST /api/admin/2fa/setup`
  - Body: `password`
  - Returns an `otpauth_uri` and recovery codes.
- `POST /api/admin/2fa/confirm`
  - Body: six-digit `code`
- `POST /api/admin/2fa/disable`
  - Body: `password`, six-digit `code`

### Financial actions requiring fresh TOTP

- `POST /api/admin/withdrawals` — finance maker
- `POST /api/admin/withdrawals/{id}/approve` — finance checker or super admin
- `POST /api/admin/withdrawals/{id}/reject` — finance checker or super admin
- `PUT /api/admin/wallets/{doctor_id}/destination` — super admin

Send the code as `two_factor_code` in the JSON body. Never log it, cache it, or put it in a URL.

## Required backend work before app MFA

Add a Firebase-user MFA API, or explicitly migrate app authentication to the Laravel API. Recommended Firebase-compatible design:

- `POST /api/mobile/mfa/enroll/start`
- `POST /api/mobile/mfa/enroll/confirm`
- `GET /api/mobile/mfa/status`
- `POST /api/mobile/mfa/challenge`
- `POST /api/mobile/mfa/verify`
- `POST /api/mobile/mfa/disable`

The backend must identify the Firebase user from the verified Firebase ID token. Store only an encrypted TOTP secret, recovery-code hashes, enrollment state, used-step/replay state, and audit events. Apply rate limits and lockouts.

Add doctor payout routes, for example:

- `GET /api/mobile/doctor/wallet`
- `PUT /api/mobile/doctor/payout-destination`
- `POST /api/mobile/doctor/withdrawals`
- `GET /api/mobile/doctor/withdrawals/{id}`

Every payout destination change and withdrawal request must require a fresh six-digit MFA code and must authorize the Firebase UID against the doctor record. Keep admin approval as a separate, independently authenticated action.

## Android and iOS implementation order

1. Add the backend MFA API contract to the Android and iOS network clients.
2. Add a reusable six-box digit input component:
   - exactly six numeric boxes;
   - automatic focus movement and paste handling;
   - red error state after an invalid response;
   - clear/reset after failure;
   - redirect or continue only after a successful server response.
3. Require MFA after login when the backend reports enrollment/challenge required.
4. Add MFA enrollment using the backend-provided `otpauth_uri` (QR scanner or copyable setup key) and recovery-code display.
5. Require fresh MFA before doctor payout destination changes and payout requests.
6. Never store PAN, CVV, or raw recovery codes in app storage or logs.
7. Keep card payments on hosted checkout and handle only the returned payment URL/status.
8. Add tests for success, invalid code, expired challenge, replayed code, rate limiting, logout, and cancellation.

## Admin frontend implementation

Replace Firebase-only admin login with the Laravel admin API, or build a deliberate bridge that obtains and refreshes the Laravel admin token. Then implement:

- six-box TOTP input on login;
- first-login setup flow;
- TOTP prompt before withdrawal create/approve/reject;
- role checks and different-admin approval enforcement;
- logout and token expiry handling;
- no TOTP values in Redux/localStorage/logs.

## Security acceptance checklist

- [ ] No PAN/CVV storage or logging.
- [ ] No local MFA verification.
- [ ] Firebase ID tokens verified server-side.
- [ ] TOTP secrets encrypted at rest.
- [ ] Recovery codes hashed and single-use.
- [ ] Six-digit codes rate-limited and replay-protected.
- [ ] Fresh-code requirement enforced server-side for payouts.
- [ ] Doctor identity comes from authenticated token, not a client-supplied doctor ID.
- [ ] Admin maker/checker separation remains enforced.
- [ ] Payment idempotency keys are generated per operation and never reused incorrectly.
- [ ] Production TLS and hosted checkout redirects are allowlisted.

## Verification commands

From the Android repository:

```sh
./gradlew testDebugUnitTest
node --check functions/index.js
git diff --check
```

For iOS source parsing:

```sh
swiftc -parse ios/HASET/Models.swift ios/HASET/Services.swift ios/HASET/DashboardViews.swift ios/HASET/Localization.swift
```

For the Laravel backend, ensure `storage/logs` and `bootstrap/cache` are writable before running:

```sh
php artisan route:list --path=api
php artisan test
```

## Important existing security note

An old Cloudinary secret existed in Git history. Rotate that credential externally even though the current app code no longer embeds it.

