# Play Console & Google Launch Guide
## HASET Digital Health Application

This document outlines all the non-technical assets and information you need to prepare for the Google Play Console release.

---

### 0. Google Play Console Registration Procedure
Before you can submit your app, you must create and verify your developer account:

1.  **Account Creation**: Go to the [Google Play Console](https://play.google.com/console) and sign in with your Google account.
2.  **Account Type**: Choose **"Organization"** (as HASET Hospital) or **"Personal"**. *Recommended: Organization (requires DUNS number).*
3.  **Registration Fee**: Pay the one-time **$25 USD** fee.
4.  **Identity Verification**: You must provide:
    *   A valid government-issued ID (Passport, National ID, or Driving License).
    *   Phone number and email verification.
5.  **D-U-N-S Number (For Organizations)**: If registering as a company, ensure you have your DUNS number ready for verification.
6.  **Account Approval**: Google takes **24-48 hours** to verify your identity. You cannot publish until this is complete.

---

### 1. Store Listing Assets (Visuals)
Google is very selective about high-quality graphics. You will need:
- [ ] **App Icon**: 512 x 512 px (32-bit PNG with alpha).
- [ ] **Feature Graphic**: 1024 x 512 px (JPEG or 24-bit PNG, no alpha). This is the most important marketing image.
- [ ] **Screenshots**: At least 4-8 screenshots.
    - Phone: 1080 x 1920 or similar (16:9).
    - 7-inch Tablet & 10-inch Tablet screenshots (Google requires these for "Designed for Tablets" badge).
- [ ] **Promotional Video** (Optional): A YouTube URL (30-60 seconds) showing the chat and appointment booking.

---

### 2. Store Listing Text
- [ ] **App Name**: HASET (Maximum 30 characters).
- [ ] **Short Description**: "Digital health app for doctor appointments, pharmacy search, and health tips." (Max 80 characters).
- [ ] **Full Description**: (Max 4000 characters). 
    *Highlight: Patient-Doctor Chat, Appointment Booking, Drug Search, Revenue sharing for Doctors, and Health Articles.*
- [ ] **Category**: Medical.
- [ ] **Tags**: Health, Medical, Doctor, Pharmacy.

---

### 3. App Access (How to provide Test Credentials)
This is where most medical apps fail the review. Google reviewers need a way to see the "Admin" and "Doctor" views.

1.  In Play Console, go to **App content** > **App access**.
2.  Click **Manage** > **+ Add new instruction**.
3.  Add the following three entries:

#### Account A: Admin (For Hospital Management)
- **User Name**: `admin_test@haset.com`
- **Password**: `Test1234!`
- **Note**: "Use this account to review the Admin Dashboard, Doctor Wallet management, and Payout approvals."

#### Account B: Doctor (For Consultations)
- **User Name**: `doctor_test@haset.com`
- **Password**: `Test1234!`
- **Note**: "Use this account to view patient chats, prescribing tools, and earnings wallet."

#### Account C: Patient (Standard User)
- **User Name**: `patient_test@haset.com`
- **Password**: `Test1234!`
- **Note**: "Standard patient account for booking and health articles."

---

### 4. Policy & Compliance (The "Medical" Trap)
Because HASET is a medical app, Google will ask specific questions:
- [ ] **Privacy Policy URL**: Must be a live link: `https://hasethospital.or.tz/legal/privacy-policy` (Verified LIVE).
- [ ] **COVID-19 Status**: You must declare if your app provides COVID-19 testing or info. (Standard answer: No).
- [ ] **Data Safety Section**: You must disclose that you collect:
    - Personal Info (Name, Email, Phone).
    - Health Info (Consultation history).
    - Financial Info (Payment history).
    - Photos (Profile pictures).

---

### 5. Financial & Developer Info
- [ ] **Developer Account**: One-time $25 fee.
- [ ] **DUNS Number**: If you are registering as a "Company" (HASET Hospital), you need a DUNS number for verification.
- [ ] **Support Email**: `support@hasethospital.or.tz`.
- [ ] **Website**: `https://hasethospital.or.tz`.

---

### 6. Release Management
- [ ] **Production APK/AAB**: Build the app in "Release" mode.
- [ ] **App Signing key**: Google Play App Signing is recommended.
- [ ] **Internal Testing**: We recommend releasing to "Internal Testing" first for 14 days with 20 testers (Google requirements for new accounts).

---

### 📝 Final Launch Checklist
- [x] Set `IS_DEBUG_MODE = false` in `Constants.java`.
- [x] Added **Medical Disclaimer** in `activity_about_us.xml`.
- [ ] Verify **Privacy Policy** link works.
- [ ] Ensure **Production Server** (Laravel) is live and SSL is active.
