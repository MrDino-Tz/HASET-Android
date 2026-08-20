# HASETApp - Features List

## Version: 1.0.0.DTC  
Last Updated: Feb 2026

---

## User Roles

### 1. Patients
- Register/Login with phone number (Firebase Auth)
- Search and filter doctors by specialty, rating, experience
- Book appointments (Visit, Online Chat, Video Call)
- View and manage appointments
- Chat with doctors
- Order medicines from pharmacy
- View prescriptions
- Rate doctors (V2)
- Health tips and articles
- Download/export appointment reports

### 2. Doctors
- Professional dashboard with appointment overview
- Accept/reject appointment requests
- Manage availability schedule
- Write digital prescriptions
- Chat with patients
- Earnings wallet tracking
- Profile management (specialty, bio, fees)

### 3. Admin
- User management (patients, doctors)
- Doctor verification/approval
- View audit logs
- Generate reports (CSV, XLS, PDF)
- Manage banners and articles
- System notifications

---

## Core Features

### Authentication
- [x] Firebase Phone Authentication
- [x] Role-based registration (Patient/Doctor)
- [x] Login/Logout
- [x] Password reset
- [x] Session management
- [x] Profile photo upload

### Doctor Discovery
- [x] Doctor list with search
- [x] Filter by specialty
- [x] Filter by rating
- [x] Filter by availability
- [x] Doctor details view
- [x] Doctor profile photos
- [x] Specialty categories
- [x] Time-based "New" label logic (7 days threshold)

### Appointment Booking
- [x] Date picker (min date = today)
- [x] Time picker
- [x] Appointment types: Visit, Online Chat
- [x] Instant booking (current time)
- [x] Scheduled booking (future date)
- [x] Reschedule appointments
- [x] Cancel appointments
- [x] Appointment status tracking (Pending, Approved, Completed, Cancelled)
- [x] Payment integration (Zeno/M-Pesa)

### Chat System
- [x] Real-time messaging
- [x] Text messages
- [x] Image sharing
- [x] Document sharing
- [x] Voice messages
- [x] Online/Offline status
- [x] Typing indicators
- [x] Message notifications
- [x] Chat history

### Pharmacy
- [x] Product catalog
- [x] Category browsing
- [x] Search products
- [x] Add to cart
- [x] Cart management
- [x] Checkout flow
- [x] Order history
- [x] Bestsellers section
- [x] Product details

### Prescriptions
- [x] View prescriptions
- [x] Prescription details
- [x] Digital prescriptions from doctors
- [x] Prescription history

### Notifications
- [x] Push notifications (Firebase)
- [x] Appointment reminders
- [x] Chat notifications
- [x] Order notifications
- [x] Role-based notifications
- [x] Scheduled reminders

---

## Technical Features

### UI/UX
- [x] Material Design 3
- [x] Dark mode support
- [x] Swahili localization (values-sw)
- [x] Shimmer loading effects
- [x] Smooth animations
- [x] Bottom navigation
- [x] Material cards
- [x] QR code generation (patient profiles)

### Data & Storage
- [x] Firebase Realtime Database
- [x] Firebase Authentication
- [x] Firebase Storage (images, files)
- [x] Room Database (local cache)
- [x] Offline support
- [x] Data sync

### Security (Enhanced for Publishing)
- [x] Firebase Auth security rules
- [x] Firebase Storage rules
- [x] Database security rules
- [x] Permission handling
- [x] Network Security Config (blocks HTTP, enforces HTTPS)
- [x] ProGuard/R8 code obfuscation for release builds
- [x] Payment duplicate initiation guards
- [x] Payment amount validation (min/max limits)
- [x] Secure API client with hostname verification
- [x] Disabled cleartext traffic in production
- [x] Disabled app backup (prevents data leakage)
- [x] SSL/TLS enforcement
- [ ] Biometric authentication (V2 - ready infrastructure)

### Performance
- [x] Image caching (Glide)
- [x] Shimmer loading
- [x] Pagination
- [x] ABI filters (reduce APK size)

---

## Report Generation (Admin)

### Features Added:
- [x] **True PDF Generation** - Uses Android PdfDocument API (not HTML)
- [x] **4 Report Types:**
  - Users List
  - App Statistics
  - Appointments
  - Audit Logs
- [x] **3 Export Formats:** CSV, XLS, PDF
- [x] **App Logo** - Embedded in PDF headers
- [x] **Export Result Bottom Sheet** - Shows:
  - File name
  - File path (phone storage location)
  - Open/View file button
  - Share button
- [x] **Audit Logging** - All exports logged for security

### PDF Features:
- Professional layout with headers
- App branding and logo
- Table formatting with borders
- Multi-page support for large datasets
- Date/time stamps

---

## Additional Features

### Health & Wellness
- [x] Health tips (daily)
- [x] Health articles
- [x] Article categories
- [x] Article comments
- [x] Article sharing

### Social
- [x] Profile sharing (QR code)
- [x] Share app with friends

### Settings & Preferences
- [x] Theme settings (Light/Dark/System)
- [x] Language preference
- [x] Profile editing
- [x] Account deletion

### Admin Features
- [x] User management
- [x] Doctor approval
- [x] Audit logging
- [x] Report generation (CSV, XLS, **PDF**)
- [x] Banner management
- [x] Article management

---

## In-App Rating
- [x] Manual "Rate App" button in Profile
- [x] Auto-prompt after 5 launches + 3 days
- [x] Opens Play Store for rating

---

## Payment Integration

### Security Features:
- [x] Zeno USSD integration
- [x] M-Pesa support
- [x] Tigo Pesa support
- [x] Airtel Money support
- [x] Payment status tracking
- [x] **Duplicate payment prevention** - Guards against multiple payment requests
- [x] **Amount validation** - Minimum and maximum payment limits
- [x] **Payment timeout** - Configurable timeout (5 minutes default)
- [x] **Biometric-ready infrastructure** - Prepared for V2

### Payment Security Constants:
```java
MIN_PAYMENT_AMOUNT = 1,000 TZS
MAX_PAYMENT_AMOUNT = 10,000,000 TZS
PAYMENT_TIMEOUT_MS = 300,000 (5 minutes)
REQUIRE_BIOMETRIC_FOR_PAYMENT = false (V2)
```

---

## API & Backend
- [x] Retrofit for API calls
- [x] OkHttp networking
- [x] Gson JSON parsing
- [x] Laravel backend ready
- [x] Environment-based API URLs (dev/production)
- [x] SSL/TLS hostname verification
- [x] Debug logging toggle

---

## Version History

### V1.0.0.DTC (Current)
- All core features
- True PDF report generation
- Export result bottom sheet with file location + share
- Enhanced security for Play Store publishing
- Network security config
- Payment security enhancements
- Biometric infrastructure ready (V2)

---

## Upcoming / Not in V1
- [ ] Doctor rating system (V2)
- [ ] Video call appointments (coming soon)
- [ ] In-app purchases
- [ ] Doctor wallet withdrawals
- [ ] Pharmacy delivery tracking
- [ ] Biometric payment authentication (V2)

---

## Build Info
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Build System**: Gradle 8.9+
- **Architecture**: MVVM + Repository Pattern
- **Version Code**: 1
- **Version Name**: 1.0.0.DTC

---

## Play Store Readiness Checklist

- [x] HTTPS enforced (Network Security Config)
- [x] Code obfuscation (ProGuard)
- [x] No cleartext traffic
- [x] Backup disabled
- [x] Payment security guards
- [ ] Production API URL configured
- [ ] Privacy Policy URL
- [ ] App Icon (512x512)
- [ ] Screenshots
- [ ] Content Rating
- [ ] Play App Signing
