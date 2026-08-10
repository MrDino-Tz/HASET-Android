# iOS Port Audit

Date: 2026-06-22
Repo: `/Users/user/Documents/HASET-Android`

## Current repository state

- This repository is a native Android app.
- There is no Flutter project structure: no `pubspec.yaml`, no `lib/`, no `ios/`.
- There is no existing iOS target: no `*.xcodeproj`, `*.xcworkspace`, `Info.plist`, or `Podfile`.
- Because of that, the requested Flutter validation flow cannot be executed against this repo as-is.

## What exists on Android today

### Core entry and auth flow

- `SplashActivity`
- `OnboardingActivity`
- `LoginActivity`
- `RoleSelectionActivity`
- `RegisterActivity`
- `ForgotPasswordActivity`

Observed flow from code:

1. `SplashActivity` checks onboarding completion.
2. If onboarding is incomplete, it opens onboarding.
3. If onboarding is complete, it checks app config from Firebase.
4. If logged in, it routes by role:
   - admin -> `AdminDashboardActivity`
   - others -> `DashboardActivity`
5. If not logged in, it routes to `LoginActivity`.

### Main app surfaces

Activities:

- `DashboardActivity`
- `AdminDashboardActivity`
- `DoctorsActivity`
- `HospitalsActivity`
- `BookAppointmentActivity`
- `PaymentActivity`
- `ChatActivity`
- `NotificationActivity`
- `ArticleActivity`
- `ArticleDetailActivity`
- `ArticleCenterActivity`
- `CreatePostWizardActivity`
- `PharmacyActivity`
- `PrescriptionActivity`
- `SearchActivity`
- `SettingsActivity`
- `EditProfileActivity`
- `DoctorEditActivity`
- `DoctorWalletActivity`
- `AboutUsActivity`
- `ServiceAgreementActivity`
- multiple admin management screens

Dashboard fragment structure:

- patient home -> `PatientHomeFragment`
- doctor home -> `DoctorHomeFragment`
- appointments -> `AppointmentsFragment`
- chat -> `ChatListFragment`
- profile -> `ProfileFragment`

Bottom navigation items from `bottom_nav_menu.xml`:

- Home
- Appointments
- Chat
- Profile

### Additional feature surfaces

Fragments and bottom sheets indicate the Android app also includes:

- appointment status tabs: upcoming, past, cancelled
- doctor reviews
- doctor detail bottom sheet
- profile and user detail bottom sheets
- article feed, article tabs, post comments
- pharmacy home and cart
- prescription detail views
- file attachment chooser
- chat options and management options
- no-internet bottom sheet
- admin home, patients, doctors, appointments, users, profile

## Shared business logic and backend behavior to preserve

### Authentication

Primary auth path in code:

- Firebase Authentication email/password
- Firebase Realtime Database user profile lookup and persistence

Files:

- `app/src/main/java/com/haset/hasetapp/repositories/AuthRepository.java`
- `app/src/main/java/com/haset/hasetapp/utils/FirebaseHelper.java`

Behavior seen:

- login via Firebase Auth email/password
- registration via Firebase Auth email/password
- user profile saved in Realtime Database under `users`
- doctor registration also seeds doctor data under `doctors`
- password reset email via Firebase Auth
- logout via Firebase Auth sign-out

### Session/local storage

File:

- `app/src/main/java/com/haset/hasetapp/utils/PreferenceManager.java`

Persisted values include:

- logged-in flag
- user id
- user role
- user name
- user email
- user phone
- profile photo path
- onboarding seen flag
- theme
- language
- notification preferences
- location preference
- FCM token
- demo doctor flag

### REST API behavior

Files:

- `app/src/main/java/com/haset/hasetapp/api/RetrofitClient.java`
- `app/src/main/java/com/haset/hasetapp/api/ApiService.java`
- `app/src/main/java/com/haset/hasetapp/api/PaymentApiService.java`
- `app/src/main/java/com/haset/hasetapp/utils/Constants.java`

Observed API configuration:

- production base URL: `https://payments.hasethospital.or.tz/api/`
- debug base URL: `http://192.168.1.126:8000/api/`
- current code sets `IS_DEBUG_MODE = true`
- client forces HTTP/1.1
- client injects `ngrok-skip-browser-warning`
- client has hostname handling for debug and selected production domains

Important iOS implication:

- The debug API URL is plain HTTP on a LAN IP. iOS will require App Transport Security exceptions for this exact behavior during development.

### Data/storage dependencies

Android dependencies in use:

- Firebase Auth
- Firebase Realtime Database
- Firebase Messaging
- Firebase Storage
- Room local database
- Retrofit + OkHttp
- Cloudinary
- Google Play Services Location
- ZXing
- WorkManager

This means an iOS port would also need:

- Firebase iOS SDK setup
- Realtime Database config
- Messaging/APNs setup if notifications are preserved
- Storage support
- Cloudinary integration
- iOS local persistence equivalent to the current session and any local data needs
- iOS QR implementation if QR features are actually user-facing

## Theme, typography, assets

### Theme/colors

Files:

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`

Observed design tokens:

- primary green: `#008800`
- primary red: `#DD0000`
- background primary: `#F8F9FA`
- card background: `#FFFFFF`
- text primary: `#1F2937`
- text secondary: `#6B7280`
- error: `#D32F2F`

Theme:

- `Theme.Material3.DayNight.NoActionBar`
- global font family: `@font/poppins`
- button corner radius: `8dp`
- card corner radius: `12dp`
- outlined text inputs with `12dp` radii
- bottom sheets with `24dp` top corners

### Fonts

Files present:

- `app/src/main/res/font/poppins_regular.ttf`
- `app/src/main/res/font/poppins_medium.ttf`
- `app/src/main/res/font/poppins_italic.ttf`
- `app/src/main/res/font/poppins_black.ttf`
- `app/src/main/res/font/poppins_semibold_italic.ttf`

### Assets

Android drawable/mipmap assets include:

- app logos and notification icons
- onboarding illustrations
- pharmacy and article imagery
- profile placeholder assets
- many XML vector icons
- splash backgrounds and gradients

These can be reused for an iOS target, but they are not yet packaged into an iOS asset catalog.

## Validation and UX behavior to preserve

Observed in auth flow:

- name validation
- email validation
- phone validation
- password validation
- doctor registration number required for doctor role
- network availability gating
- loading dialogs
- snackbar-based error/success messaging
- onboarding gate
- role-based routing

Relevant files:

- `app/src/main/java/com/haset/hasetapp/utils/ValidationUtils.java`
- `app/src/main/java/com/haset/hasetapp/activities/LoginActivity.java`
- `app/src/main/java/com/haset/hasetapp/activities/RegisterActivity.java`

## Android permissions currently in use

From `AndroidManifest.xml`:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `SCHEDULE_EXACT_ALARM`
- `POST_NOTIFICATIONS`
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `READ_MEDIA_IMAGES`
- `RECORD_AUDIO`
- `CAMERA`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `MODIFY_AUDIO_SETTINGS`
- `ACCESS_WIFI_STATE`
- `BLUETOOTH`
- `BLUETOOTH_CONNECT`
- `WAKE_LOCK`

## iOS permission/config mapping required for parity

If the iOS port is created, these are the likely `Info.plist` additions required:

- `NSCameraUsageDescription`
- `NSPhotoLibraryUsageDescription`
- `NSPhotoLibraryAddUsageDescription` if saving/exporting media
- `NSMicrophoneUsageDescription`
- `NSLocationWhenInUseUsageDescription`
- `NSUserNotificationsUsageDescription`

Potential platform configuration:

- Firebase iOS initialization via `GoogleService-Info.plist`
- Push notification capability and APNs config if FCM parity is required
- Keychain or secure storage decision for session/token material if used beyond current shared preferences behavior
- ATS exception for `http://192.168.1.126:8000` in debug builds only, if preserving current debug API behavior

## Blocking issues preventing full implementation in this repo today

1. There is no iOS codebase to modify.
2. There is no Flutter project to share Dart widgets or run `flutter clean`, `flutter pub get`, `flutter run`, or `flutter build ios`.
3. Reaching acceptance would require building a new iOS app target from scratch for a large multi-screen Android app.
4. The current app is not a small surface area; it includes auth, role-based dashboards, chat, media upload, notifications, articles, pharmacy, admin flows, and appointment management.
5. The installed Flutter SDK cannot complete even `flutter --version` under the current sandbox because it attempts to write to `/Users/user/develop/flutter/bin/cache/engine.stamp`.

## Commands executed during inspection

- `pwd`
- `rg --files`
- `find . -maxdepth 2 -type d | sort`
- `find . -maxdepth 2 \\( -name ios -o -name lib -o -name android \\) -type d | sort`
- `sed -n '1,220p' app/src/main/AndroidManifest.xml`
- `find app/src/main/java/com/haset/hasetapp/activities -maxdepth 1 -name '*.java' -exec basename {} .java \\; | sort`
- `find app/src/main/java/com/haset/hasetapp/fragments -maxdepth 1 -name '*.java' -exec basename {} .java \\; | sort`
- `find app/src/main/java/com/haset/hasetapp/repositories -maxdepth 1 -name '*.java' -exec basename {} .java \\; | sort`
- `sed -n ...` on key files including `SplashActivity.java`, `LoginActivity.java`, `RegisterActivity.java`, `DashboardActivity.java`, `RetrofitClient.java`, `AuthRepository.java`, `PreferenceManager.java`, `ApiService.java`, `Constants.java`, `colors.xml`, `themes.xml`, `strings.xml`, and `bottom_nav_menu.xml`
- `flutter --version`
- `xcodebuild -version`
- `pod --version`

## Toolchain check results

- `xcodebuild -version` -> available (`Xcode 26.4.1`, build `17E202`)
- `pod --version` -> available (`1.16.2`)
- `flutter --version` -> failed under sandbox because Flutter tried to write to its cache outside the writable workspace

## Practical next step

To actually satisfy the original iOS implementation request, one of these must happen first:

1. Provide the actual Flutter project that already contains `ios/`, `lib/`, and `pubspec.yaml`.
2. Authorize creating a brand-new iOS app target in this repository and treat this as a native Android-to-iOS port project, which is materially larger than an iOS platform fix.

Without one of those, any claim that the iOS app has been implemented, built, or validated would be inaccurate.

## Current iOS status update

The repository now includes a native iOS target at `ios/HASET.xcodeproj` with SwiftUI screens, Firebase auth/profile handling, permissions, fonts, resources, and a buildable app target.

### What is wired

- Splash, onboarding, login, register, forgot password, role selection, and dashboard routing
- Role-based home, appointments, chat, profile, settings, about, notifications, edit profile, doctors, hospitals, pharmacy, articles, and admin views
- Firebase email/password auth and Realtime Database profile reads/writes
- iOS notification and location permission handling
- Keychain-backed session persistence
- Bundled Android fonts and copied image resources
- ATS exception for the Android debug API host

### Remaining parity gaps

- Advanced chat media, voice, and attachment flows
- Payment, pharmacy cart, prescription, QR, and upload flows
- Deep admin management screens and moderation actions
- Live push notification/APNs wiring
- Full asset-catalog packaging on this host, because simulator runtime tooling is unavailable here

### Testing readiness

- The app target builds successfully with `xcodebuild` on this machine.
- Live simulator launch and tap-through testing are still blocked by the host’s missing CoreSimulator runtime services.
