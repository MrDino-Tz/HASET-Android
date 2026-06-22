# HASET App - Version 2.0 Upcoming Features

This document outlines features planned for Version 2.0 of the HASET Hospital App.

---

## 1. Doctor Rating & Review System

### Overview
Allow patients to rate and review doctors after appointments to build trust and help other patients make informed decisions.

### Features
- **Star Rating**: 1-5 star rating system for doctors
- **Written Reviews**: Optional text reviews with comments
- **Review Display**: View average ratings and individual reviews on doctor profiles
- **Review Management**: Patients can view their past ratings and update them
- **Analytics**: Doctors can view their rating statistics and patient feedback

### Affected Files (Currently Disabled in V1)
- `RateDoctorBottomSheet.java` - UI for submitting ratings
- `DoctorReviewsFragment.java` - Fragment displaying all reviews for a doctor
- `ReviewsAdapter.java` - RecyclerView adapter for review list
- `ReviewsViewModel.java` - ViewModel for review data
- `ReviewRepository.java` - Repository for review data operations
- `item_review.xml` - Layout for individual review items
- `bottom_sheet_rate_doctor.xml` - Rating bottom sheet UI
- `fragment_doctor_reviews.xml` - Reviews list fragment layout

### Implementation Notes
- Code is currently commented out with `DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0` markers
- Uncomment and complete the implementations to enable in V2
- Firebase integration already partially implemented in `FirebaseHelper.java`

---

## 2. Video Consultation

### Overview
Enable video calling between doctors and patients for remote consultations.

### Features
- **Real-time Video Calls**: WebRTC-based video conferencing
- **Appointment Integration**: Video calls linked to scheduled appointments
- **Call Controls**: Mute, camera toggle, screen sharing
- **Call Recording**: Optional recording for medical records (with consent)
- **Chat During Call**: Text chat alongside video

### UI Elements Ready
- Video call button exists in `activity_chat.xml` (marked as "Coming Soon")

---

## 3. Biometric Payment Authentication

### Overview
Add fingerprint/face authentication as an additional security layer for payments.

### Features
- **Fingerprint Authentication**: Verify identity before completing payments
- **Face Recognition**: Alternative biometric option for supported devices
- **Secure Payments**: Additional layer of security for high-value transactions
- **Fallback Options**: PIN/password fallback for devices without biometrics

### Implementation Status
- **Infrastructure Ready**: `BiometricHelper.java` created with full implementation
- **Dependency Added**: `androidx.biometric:biometric:1.1.0` added to build.gradle
- **Constant Defined**: `REQUIRE_BIOMETRIC_FOR_PAYMENT = false` in Constants.java
- **PaymentActivity Integration**: Ready to uncomment and enable

### To Enable in V2:
1. Set `REQUIRE_BIOMETRIC_FOR_PAYMENT = true` in Constants.java
2. Uncomment biometric check in PaymentActivity.processPayment()
3. Test thoroughly with various biometric configurations

---

## 4. Push Notifications Enhancement

### Overview
Enhanced notification system with more granular controls and rich content.

### Features
- **Rich Notifications**: Images, actions, and expanded content
- **Notification Categories**: Appointment reminders, messages, system updates, promotions
- **Smart Scheduling**: AI-powered optimal notification timing
- **Notification History**: In-app notification center with archive

---

## 5. Medical Records Integration

### Overview
Comprehensive medical records management for patients.

### Features
- **Document Upload**: Upload and store medical documents, lab results, prescriptions
- **Document Categories**: Organize by type (lab results, prescriptions, imaging, etc.)
- **Sharing with Doctors**: Securely share records with consulting doctors
- **Health Timeline**: Visual timeline of medical history
- **Export/Import**: PDF export and secure import from other systems

---

## 6. AI-Powered Health Assistant

### Overview
Chatbot assistant to help patients with basic queries and triage.

### Features
- **Symptom Checker**: AI-based preliminary symptom analysis
- **FAQ Automation**: Answers to common hospital and health questions
- **Appointment Guidance**: Helps patients choose the right specialty
- **Medication Reminders**: Smart reminders for prescriptions
- **Health Tips**: Personalized health advice based on history

---

## 7. Payment Integration (Enhanced)

### Overview
In-app payment processing for consultation fees and other services.

### Features (Partially Implemented in V1)
- ✅ **Multiple Payment Methods**: M-Pesa, Tigo Pesa, Airtel Money (V1)
- ✅ **Consultation Billing**: Pay for consultations (V1)
- ✅ **Payment History**: Transaction tracking (V1)
- ✅ **Payment Security Guards**: Duplicate prevention, amount validation (V1)

### V2 Enhancements
- **Invoices**: Generate and download payment receipts
- **Insurance Integration**: Link with health insurance providers
- **Refund Processing**: Handle payment refunds
- **Subscription Plans**: Monthly/yearly health plans
- **Wallet System**: In-app wallet for quick payments

---

## 8. Advanced Appointment Features

### Overview
Enhanced appointment management with more flexibility.

### Features
- **Recurring Appointments**: Schedule follow-up appointments automatically
- **Waitlist**: Join waitlist for fully booked doctors
- **Appointment Reminders**: Multiple reminder channels (SMS, email, push)
- **Rescheduling**: Easy rescheduling with doctor availability check
- **Appointment Notes**: Doctors can add private notes to appointments
- **Multi-language Support**: Appointment details in preferred language

---

## 9. Telemedicine Features

### Overview
Comprehensive telemedicine capabilities beyond video calls.

### Features
- **Virtual Waiting Room**: Patients wait in virtual queue
- **E-Prescriptions**: Digital prescription generation and pharmacy integration
- **Follow-up Automation**: Automated follow-up scheduling
- **Remote Monitoring**: Integration with wearable devices for vital signs
- **Digital Health Forms**: Pre-appointment questionnaires

---

## 10. Community Features

### Overview
Build a health-focused community within the app.

### Features
- **Health Forums**: Topic-based discussion forums
- **Support Groups**: Disease-specific support communities
- **Health Challenges**: Gamified health challenges with rewards
- **Expert Q&A**: Ask doctors questions in public forums
- **Health Articles**: Curated health content and blogs

---

## 11. Administrative Enhancements

### Overview
Improved backend and admin capabilities.

### Features
- **Doctor Verification**: Streamlined doctor onboarding and verification
- **Hospital Analytics**: Dashboard for hospital administration
- **Staff Management**: Manage doctors, nurses, and support staff
- **Resource Management**: Room and equipment scheduling
- **Patient Insights**: Demographics and health trend analytics

---

## 12. Financial & Tax Automation

### Overview
Automate tax compliance and transaction fee management for doctor payouts.

### Features
- **Withholding Tax Automation**: Automatically subtract 5% (or configurable %) Withholding Tax from doctor payouts.
- **VAT Management**: Track VAT (18%) on HASET's 40% commission for tax reporting.
- **Dynamic Commission Rates**: Adjust `HASET_REVENUE_SHARE` and `DOCTOR_REVENUE_SHARE` dynamically for different specialties.
- **Transaction Fee Logic**: Option to choose whether HASET or the Doctor absorbs mobile money transfer fees/levies.
- **Automated Invoicing**: Generate professional tax invoices for both doctors (commission) and patients (consultation).

---

## 13. Curated Hospital Finder & Directory

### Overview
A fully integrated, internally managed database of partnered or recommended hospitals and clinics. It allows patients to browse high-quality healthcare facilities without the app needing invasive location permissions or costly third-party map APIs.

### Features
- **Data Structure (Firebase)**: `hospitals` table/node with `id`, `name`, `location_string`, `image_url` (Cloudinary), `services_offered`, `contact_info`.
- **Admin Management Portal**: Add a "Manage Hospitals" quick-access tile to the Admin Profile. Admins can upload a hospital photo directly from their gallery and input details.
- **Patient Interface**: "Find Hospitals" quick shortcut in the Patient Home leading to `HospitalsListActivity`.
- **Map & Routing Integration**: "Take me there" button that opens native Google Maps app via Geo Intent, eliminating the need for `ACCESS_FINE_LOCATION` permissions.

---

## V1.0.DTC Features (Recently Implemented)

These features were added in the current version (V1.0.DTC):

### Report Generation Enhancements
- ✅ **True PDF Generation**: Uses Android PdfDocument API (not HTML)
- ✅ **App Logo in PDFs**: Embedded logo in PDF headers
- ✅ **4 Report Types**: Users, Statistics, Appointments, Audit Logs
- ✅ **3 Export Formats**: CSV, XLS, PDF
- ✅ **Export Result Bottom Sheet**: Shows file path + share button
- ✅ **File Location Display**: Shows exact file path in phone storage

### Security Enhancements
- ✅ **Network Security Config**: Blocks HTTP, enforces HTTPS
- ✅ **SSL/TLS Enforcement**: All network traffic encrypted
- ✅ **Payment Security Guards**: Duplicate payment prevention
- ✅ **Amount Validation**: Min/max payment limits (100 - 10M TZS)
- ✅ **Secure API Client**: Hostname verification
- ✅ **Disabled Cleartext Traffic**: Prevents man-in-the-middle attacks
- ✅ **Disabled App Backup**: Prevents data leakage
- ✅ **Code Obfuscation**: ProGuard/R8 enabled for release
- ✅ **Biometric Infrastructure**: Ready (disabled for V2)

### API Configuration
- ✅ **Environment-based URLs**: Development/Production switching
- ✅ **Debug Mode Toggle**: Enable/disable logging
- ✅ **Proper API Prefix**: `/api` for Laravel backend

---

## Migration Notes for V2

### To Enable Biometric Payments:
1. Set `REQUIRE_BIOMETRIC_FOR_PAYMENT = true` in Constants.java
2. Uncomment biometric check in PaymentActivity:
```java
if (Constants.REQUIRE_BIOMETRIC_FOR_PAYMENT && BiometricHelper.isBiometricAvailable(this)) {
    BiometricHelper.authenticatePayment(this, new BiometricHelper.BiometricCallback() {
        @Override
        public void onAuthenticationSuccess() {
            processPayment();
        }
        // ... other callbacks
    });
} else {
    processPayment();
}
```
3. Test with various biometric configurations
4. Update Play Store listing to highlight new feature

### To Enable Rating System:
1. Uncomment the code blocks marked with `DISABLED FOR V1` in:
   - All rating-related Java files
   - XML layout files
2. Ensure Firebase Realtime Database rules allow rating submissions
3. Test the rating flow with sample appointments
4. Add rating display to doctor profile screens if not already present

### Database Changes Needed:
- `doctor_ratings` table already defined in Room database
- Firebase nodes structure already implemented
- No additional database migration required for existing V1 users

### Testing Checklist for V2:
- [ ] Submit rating after appointment
- [ ] View all ratings for a doctor
- [ ] Average rating calculation accuracy
- [ ] Update existing rating
- [ ] Star UI properly displays filled/empty stars
- [ ] Review text display formatting
- [ ] Offline rating sync to Firebase
- [ ] Biometric authentication flow
- [ ] Fingerprint not enrolled handling
- [ ] Biometric fallback to PIN/password

---

## Version History

| Version | Release Date | Features |
|---------|-------------|----------|
| **V1.0** | Feb 2026 | Core appointment booking, chat, profile management |
| **V1.0.DTC** | Feb 2026 | PDF reports, export bottom sheet, security enhancements |
| **V2.0** | TBD | Rating system, video calls, biometrics, AI assistant |

---

*Last Updated: February 2026*
*For questions contact: development@hasethospital.or.tz*
