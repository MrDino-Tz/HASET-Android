# HASETApp - Healthcare Assistance & Seamless E-Therapy

HASETApp is a premium, modern Android application designed to bridge the gap between patients and specialized doctors through seamless digital consultations. It provides instant chat, real-time video therapy, and appointment scheduling in an intuitive, high-performance package.

## 🚀 Technology Stack

### Core Framework & Development
*   **Platform**: Android (Native)
*   **Language**: Java
*   **Minimum SDK**: 24 (Android 7.0 Nougat)
*   **Target SDK**: 36 (Android 15)
*   **Build System**: Gradle with Version Catalogs (libs.versions.toml)

### Backend & Infrastructure
*   **Cloud Infrastructure**: **Firebase (Google)**
    *   **Authentication**: Secure user login/registration and role-based access control.
    *   **Real-time Database**: Ultra-low latency synchronization for messaging and appointment status.
    *   **Cloud Messaging (FCM)**: Push notifications for incoming calls, messages, and reminders.
    *   **Cloud Storage**: Used for hosting clinical documents, prescriptions, and media attachments.
*   **Media Management**: **Cloudinary Android SDK**
    *   Optimized upload and delivery of high-resolution profile photos and clinical media.

### Communication & Telemedicine
*   **Real-time Video/Audio**: **Agora RTC SDK (v4.1.0)**
    *   Provides secure, encrypted, and low-latency 1-on-1 video consultations.
    *   Supports Picture-in-Picture (PiP) mode for multitasking during calls.
*   **Instant Messaging**: Custom Firebase-driven chat engine.
    *   Supports text, attachments (documents/audio/video), and real-time typing indicators.

---

## 🏛️ Architectural Overview

The app follows a modified **Single Activity per Feature** pattern with a strong emphasis on **State Management** and **Asynchronous Processing**.

### Key Architectural Layers:
1.  **UI Layer (Activities & Fragments)**: 
    *   Heavy use of **ConstraintLayout** for complex, responsive designs.
    *   **Material Design 3 (M3)** components for a premium, unified look.
    *   Fragments used for high-level navigation (Home, Appointments, Inbox, Profile).
2.  **Logic Layer (Utils & Helpers)**:
    *   `FirebaseHelper`: Centralized singleton for all database references and queries.
    *   `PreferenceManager`: Encapsulated SharedPreferences for local configuration (Theme, Language, User State).
    *   `ProfilePhotoHelper`: Abstracted logic for loading and caching images using **Glide**.
3.  **Data Layer (Models)**:
    *   Clean POJO structures for `User`, `Doctor`, `ChatMessage`, and `Appointment` to ensure easy serialization for Firebase.
4.  **Local Persistence**: 
    *   **Room Database**: Leveraged for cached data and potential offline-first capabilities.

---

## 🔌 Integrated APIs & Libraries

### Network & Data
*   **Retrofit 2**: Used for communication with external medical APIs (`https://api.hasetapp.com/`).
*   **OkHttp 4**: Underlying HTTP client for optimized networking and logging.
*   **Gson**: JSON serialization/deserialization across the entire app.

### UI & UX Enhancement
*   **Glide**: Advanced image loading and caching library.
*   **Facebook Shimmer**: Elegant loading states for data-heavy views (Doctor lists, Feed).
*   **CircleImageView**: High-quality circular cropping for profile avatars.
*   **Material Components**: Handling Date/Time Pickers, Bottom Sheets, and Floating Action Buttons.

### Utilities
*   **ZXing (Zebra Crossing)**: QR Code generation for medical profiles and verification.
*   **Google Play Services Location**: For finding nearby hospitals or clinics (if enabled).

---

## 🛠️ Folder Structure Reference

*   `activities/`: Screen-level controllers (Login, VideoCall, Chat, Dashboard).
*   `fragments/`: Modular UI components for the main navigation.
*   `adapters/`: Data binders for RecyclerViews (Chat messages, Doctor lists).
*   `models/`: Data object definitions.
*   `utils/`: Core helper classes (Constants, Notifications, Preferences).
*   `firebase/`: Firebase-specific singleton logic.

---

## 🔐 Security & Privacy
*   **End-to-End Encryption**: Video calls are handled on Agora's secure global backbone.
*   **Rule-Based Database Access**: Firebase security rules ensure patients only see their own appointments and doctors only see their assigned cases.
*   **Audit Logging**: Internal system to track critical actions (e.g., appointment cancellations) for clinical accountability.

---

**Developed with ❤️ by Antigravity for HASETApp.**
