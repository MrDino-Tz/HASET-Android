# MediConnect - Doctor-Patient Appointment App

A complete Android application built with Java and XML for managing doctor-patient appointments, real-time chat, and prescriptions.

## Features

### Authentication
- Firebase Authentication
- Role-based registration (Doctor/Patient)
- Email and password validation
- Secure login/logout

### Patient Features
- Browse and search doctors by specialty
- View doctor profiles with ratings and experience
- Book appointments with date and time selection
- View appointment history
- Real-time chat with doctors
- View prescriptions

### Doctor Features
- View pending and approved appointments
- Approve/decline appointment requests
- Manage schedule and availability
- Real-time chat with patients
- Upload prescriptions
- Dashboard with appointment statistics

### Technical Features
- MVVM Architecture
- Firebase Realtime Database
- Firebase Authentication
- Retrofit for API calls (placeholder)
- Material Design 3
- RecyclerView with custom adapters
- Bottom Navigation
- Fragment-based navigation
- Real-time updates

## Project Structure

```
app/src/main/
├── java/com/haset/hasetapp/
│   ├── activities/          # All activity classes
│   │   ├── SplashActivity
│   │   ├── LoginActivity
│   │   ├── RegisterActivity
│   │   ├── RoleSelectionActivity
│   │   ├── DashboardActivity
│   │   ├── BookAppointmentActivity
│   │   └── ChatActivity
│   ├── fragments/           # Fragment classes
│   │   ├── PatientHomeFragment
│   │   ├── DoctorHomeFragment
│   │   ├── AppointmentsFragment
│   │   ├── ChatListFragment
│   │   └── ProfileFragment
│   ├── adapters/            # RecyclerView adapters
│   │   ├── DoctorAdapter
│   │   ├── AppointmentAdapter
│   │   ├── ChatAdapter
│   │   └── TimeSlotAdapter
│   ├── models/              # Data models
│   │   ├── User
│   │   ├── Doctor
│   │   ├── Appointment
│   │   ├── ChatMessage
│   │   └── Prescription
│   ├── utils/               # Utility classes
│   │   ├── Constants
│   │   ├── ValidationUtils
│   │   ├── PreferenceManager
│   │   └── DateTimeUtils
│   ├── firebase/            # Firebase helper
│   │   └── FirebaseHelper
│   └── api/                 # API service (placeholder)
│       ├── ApiService
│       ├── ApiResponse
│       └── RetrofitClient
└── res/
    ├── layout/              # XML layouts
    ├── values/              # Colors, strings, themes
    ├── drawable/            # Drawables and shapes
    └── menu/                # Navigation menus
```

## Setup Instructions

### 1. Prerequisites
- Android Studio (latest version)
- JDK 11 or higher
- Android SDK API 24+
- Firebase account

### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.haset.hasetapp`
4. Download `google-services.json`
5. Replace the placeholder `app/google-services.json` with your downloaded file

6. Enable Firebase Authentication:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication

7. Enable Firebase Realtime Database:
   - Go to Realtime Database
   - Create database in test mode (or set up security rules)
   - Database structure will be created automatically

### 3. Database Structure

The app uses the following Firebase Realtime Database structure:

```
hasetapp/
├── users/
│   └── {userId}/
│       ├── userId
│       ├── email
│       ├── fullName
│       ├── phone
│       ├── role (patient/doctor)
│       └── profileImage
├── doctors/
│   └── {doctorId}/
│       ├── doctorId
│       ├── userId
│       ├── fullName
│       ├── specialty
│       ├── experience
│       ├── rating
│       ├── availableDays[]
│       └── availableTimes[]
├── appointments/
│   └── {appointmentId}/
│       ├── appointmentId
│       ├── patientId
│       ├── patientName
│       ├── doctorId
│       ├── doctorName
│       ├── date
│       ├── time
│       ├── status
│       └── reason
├── messages/
│   └── {chatRoomId}/
│       └── {messageId}/
│           ├── senderId
│           ├── receiverId
│           ├── message
│           └── timestamp
└── prescriptions/
    └── {prescriptionId}/
        ├── appointmentId
        ├── patientId
        ├── doctorId
        ├── medicines[]
        └── instructions
```

### 4. Build and Run

1. Open project in Android Studio
2. Sync Gradle files
3. Connect Android device or start emulator
4. Click Run button or use `Shift + F10`

### 5. Testing the App

#### Create Test Accounts:

**Doctor Account:**
- Email: doctor@test.com
- Password: doctor123
- Role: Doctor

**Patient Account:**
- Email: patient@test.com
- Password: patient123
- Role: Patient

## Color Scheme

### Green Theme (Primary)
- Primary: #008800
- Light: #11AA11
- Dark: #006600

### Red Theme (Secondary)
- Primary: #DD0000
- Light: #FF2222
- Dark: #BB0000

## Dependencies

```gradle
// Firebase
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-database'
implementation 'com.google.firebase:firebase-messaging'
implementation 'com.google.firebase:firebase-storage'

// Retrofit
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Lifecycle & ViewModel
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata:2.6.2'

// Navigation
implementation 'androidx.navigation:navigation-fragment:2.7.6'
implementation 'androidx.navigation:navigation-ui:2.7.6'

// Glide for images
implementation 'com.github.bumptech.glide:glide:4.16.0'

// CircleImageView
implementation 'de.hdodenhof:circleimageview:3.1.0'
```

## API Integration (Optional)

The app includes placeholder API service classes using Retrofit. To integrate with a REST API:

1. Update `Constants.API_BASE_URL` with your API endpoint
2. Implement the API endpoints in `ApiService.java`
3. Use `RetrofitClient.getInstance().getApiService()` to make calls

## Features to Implement

The following features have placeholder implementations:

- [ ] Prescription upload and viewing
- [ ] Push notifications
- [ ] Doctor profile editing
- [ ] Patient medical history
- [ ] Payment integration
- [ ] Video consultation
- [ ] Appointment reminders
- [ ] Rating and reviews
- [ ] Advanced search filters

## Troubleshooting

### Firebase Connection Issues
- Ensure `google-services.json` is in the `app/` directory
- Check package name matches in Firebase console
- Verify internet permissions in AndroidManifest.xml

### Build Errors
- Clean and rebuild project: `Build > Clean Project`
- Invalidate caches: `File > Invalidate Caches / Restart`
- Check Gradle sync completed successfully

### Authentication Errors
- Verify Firebase Authentication is enabled
- Check email/password provider is enabled
- Ensure device has internet connection

## License

This project is created for educational purposes.

## Support

For issues and questions, please check the code comments and Firebase documentation.
