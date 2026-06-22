# HASETApp - Complete Setup Guide

## Quick Start (5 Minutes)

### Step 1: Firebase Setup

1. **Create Firebase Project**
   - Visit https://console.firebase.google.com/
   - Click "Add project"
   - Name it "HASETApp" (or any name)
   - Disable Google Analytics (optional)
   - Click "Create project"

2. **Add Android App**
   - Click Android icon
   - Package name: `com.haset.hasetapp`
   - App nickname: HASETApp
   - Click "Register app"

3. **Download Configuration**
   - Download `google-services.json`
   - Place it in `app/` folder (replace existing placeholder)

4. **Enable Authentication**
   - In Firebase Console, go to "Authentication"
   - Click "Get started"
   - Select "Email/Password"
   - Enable it and save

5. **Enable Realtime Database**
   - Go to "Realtime Database"
   - Click "Create Database"
   - Choose location (closest to you)
   - Start in "Test mode" (for development)
   - Click "Enable"

### Step 2: Build Configuration

1. **Open in Android Studio**
   ```bash
   # Open Android Studio
   # File > Open > Select project folder
   ```

2. **Sync Gradle**
   - Wait for Gradle sync to complete
   - If prompted, update Gradle plugin
   - Click "Sync Now" if needed

3. **Apply Firebase Plugin**
   - Open `app/build.gradle`
   - Uncomment this line at the top:
   ```gradle
   apply plugin: 'com.google.gms.google-services'
   ```

### Step 3: Run the App

1. **Connect Device or Start Emulator**
   - Physical device: Enable USB debugging
   - Emulator: Create AVD with API 24+

2. **Run**
   - Click green "Run" button
   - Or press `Shift + F10`

3. **Test**
   - Register as Patient
   - Register as Doctor (use different email)
   - Test booking appointments

## Detailed Configuration

### Firebase Security Rules (Production)

For production, update Realtime Database rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    },
    "doctors": {
      ".read": "auth != null",
      "$doctorId": {
        ".write": "auth.uid === $doctorId"
      }
    },
    "appointments": {
      ".read": "auth != null",
      ".write": "auth != null",
      ".indexOn": ["patientId", "doctorId", "status"]
    },
    "messages": {
      "$chatRoomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "prescriptions": {
      ".read": "auth != null",
      "$prescriptionId": {
        ".write": "auth != null"
      }
    }
  }
}
```

### Adding Sample Doctors (Optional)

To test the app with sample doctors, add this data to Firebase Realtime Database:

1. Go to Firebase Console > Realtime Database
2. Click on the database root
3. Click "+" to add child
4. Add this structure:

```json
{
  "doctors": {
    "doctor1": {
      "doctorId": "doctor1",
      "userId": "doctor1",
      "fullName": "John Smith",
      "email": "john.smith@hospital.com",
      "phone": "1234567890",
      "specialty": "Cardiologist",
      "about": "Experienced cardiologist with 15 years of practice",
      "experience": 15,
      "rating": 4.8,
      "isAvailable": true,
      "availableDays": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"],
      "availableTimes": ["09:00 AM", "10:00 AM", "11:00 AM", "02:00 PM", "03:00 PM"]
    },
    "doctor2": {
      "doctorId": "doctor2",
      "userId": "doctor2",
      "fullName": "Sarah Johnson",
      "email": "sarah.j@hospital.com",
      "phone": "0987654321",
      "specialty": "Pediatrician",
      "about": "Specialized in child healthcare",
      "experience": 10,
      "rating": 4.9,
      "isAvailable": true,
      "availableDays": ["Monday", "Wednesday", "Friday"],
      "availableTimes": ["09:00 AM", "10:30 AM", "02:00 PM", "03:30 PM"]
    }
  }
}
```

## Testing Workflow

### 1. Register as Patient
- Open app
- Click "Sign Up"
- Select "I'm a Patient"
- Fill in details
- Email: patient@test.com
- Password: test123456
- Register

### 2. Register as Doctor
- Logout
- Click "Sign Up"
- Select "I'm a Doctor"
- Fill in details
- Email: doctor@test.com
- Password: test123456
- Register

### 3. Book Appointment (Patient)
- Login as patient
- Browse doctors
- Click "Book" on any doctor
- Select date and time
- Confirm booking

### 4. Manage Appointments (Doctor)
- Login as doctor
- View pending appointments
- Approve or decline

### 5. Test Chat
- From appointment, click on doctor/patient name
- Send messages
- Test real-time updates

## Common Issues & Solutions

### Issue: "google-services.json not found"
**Solution:** 
- Download from Firebase Console
- Place in `app/` folder (not `app/src/`)
- Sync Gradle

### Issue: "Firebase Auth failed"
**Solution:**
- Check internet connection
- Verify Email/Password is enabled in Firebase
- Check SHA-1 certificate (for production)

### Issue: "Database permission denied"
**Solution:**
- Set database to "Test mode" in Firebase
- Or update security rules

### Issue: "App crashes on startup"
**Solution:**
- Check Logcat for errors
- Verify all dependencies are synced
- Clean and rebuild project

### Issue: "Doctors not showing"
**Solution:**
- Add sample doctors to Firebase
- Check database path: `/doctors`
- Verify internet connection

## Production Checklist

Before releasing to production:

- [ ] Replace placeholder `google-services.json` with production config
- [ ] Update Firebase security rules
- [ ] Enable ProGuard in `build.gradle`
- [ ] Add SHA-1 certificate to Firebase
- [ ] Test on multiple devices
- [ ] Add error analytics (Firebase Crashlytics)
- [ ] Implement proper error handling
- [ ] Add loading states
- [ ] Test offline scenarios
- [ ] Add data validation
- [ ] Implement rate limiting
- [ ] Add user feedback mechanisms
- [ ] Test payment integration (if applicable)
- [ ] Add privacy policy and terms
- [ ] Implement data backup

## API Integration (Optional)

If you want to use REST API instead of Firebase:

1. **Update Constants**
   ```java
   public static final String API_BASE_URL = "https://your-api.com/api/";
   ```

2. **Implement Endpoints**
   - Update `ApiService.java` with real endpoints
   - Handle authentication tokens
   - Implement error handling

3. **Switch from Firebase**
   - Replace Firebase calls with Retrofit calls
   - Update authentication flow
   - Modify data models if needed

## Performance Optimization

### Image Loading
```java
// Use Glide for efficient image loading
Glide.with(context)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .into(imageView);
```

### Database Queries
```java
// Use indexed queries for better performance
databaseRef.orderByChild("status")
    .equalTo("pending")
    .limitToFirst(20);
```

### Memory Management
- Use ViewBinding instead of findViewById
- Implement pagination for large lists
- Clear listeners in onDestroy()

## Next Steps

1. **Enhance UI/UX**
   - Add animations
   - Improve loading states
   - Add empty states
   - Implement pull-to-refresh

2. **Add Features**
   - Video consultation
   - Payment integration
   - Push notifications
   - Prescription management
   - Medical records

3. **Improve Security**
   - Implement proper authentication
   - Add data encryption
   - Secure API calls
   - Validate user inputs

4. **Testing**
   - Write unit tests
   - Add integration tests
   - Perform security testing
   - Test edge cases

## Support & Resources

- **Firebase Documentation:** https://firebase.google.com/docs
- **Android Documentation:** https://developer.android.com
- **Material Design:** https://material.io/design

## Contact

For questions or issues, refer to the code comments and documentation.
