# HASET App: Production Readiness & Safety Checklist

This document outlines the critical features and configurations required to safely transition the HASET application from development to a live production environment.

## 1. Remote "Maintenance Mode" & Force Update
**Objective:** Provide the ability to remotely disable app functionality during server maintenance or in emergency situations.

*   **Mechanism:** On every app launch, `MainActivity` checks the `/app_config/maintenance_mode` flag in Firebase.
*   **UI Influence:** If enabled, users are redirected to a dedicated "Maintenance" screen explaining that the service will be back shortly.
*   **Production Value:** Prevents failed payments and data corruption during backend logic updates.

## 2. Mandatory Version Check (Force Update)
**Objective:** Ensure all active users are running a secure and compatible version of the app.

*   **Mechanism:** Compare local `versionCode` against `/app_config/min_required_version` stored on the server.
*   **UI Influence:** If the user is below the minimum version, a non-dismissible dialog forces them to update via the Play Store.
*   **Production Value:** Essential for pushing security patches or if the API structure changes.

## 3. Firebase Security Rules 🔒
**Objective:** Secure user data and prevent unauthorized database modifications.

*   **Rules Logic:**
    *   **Patients:** Read/Write only their own `appointments` and `user` data. No access to `withdrawal_requests`.
    *   **Doctors:** Access to their own `wallet`, `appointments`, and `withdrawal_history` only.
    *   **Admins:** Write access to `audit_logs`, `app_config`, and `wallet` payout status.
*   **Production Value:** Prevents malicious users from tampering with financial records or viewing private patient health info.

## 4. Error Tracking (Firebase Crashlytics)
**Objective:** Real-time visibility into app stability for live users.

*   **Mechanism:** Integration of the Crashlytics SDK to log crashes, non-fatal errors, and ANRs (App Not Responding).
*   **Production Value:** Allows the development team to fix bugs before they impact a large number of users without needing the user to manually report the crash.

## 5. User Support / Feedback System
**Objective:** Provide a direct bridge between users and the HASET hospital support team.

*   **Mechanism:** A "Help & Support" button in the Settings/Profile menu.
*   **Features:**
    *   **Report a Bug:** Users can submit issues with the app.
    *   **Payment Issue:** Specific flow for financial discrepancies.
    *   **Direct Support:** Links to WhatsApp support or a toll-free number.
*   **Production Value:** Builds trust with users and provides a professional outlet for troubleshooting.

---
*Status: Planning Phase. Awaiting implementation of Points 2 & 5.*
