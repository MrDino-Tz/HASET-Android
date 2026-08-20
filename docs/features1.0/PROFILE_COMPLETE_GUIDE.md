# 👤 HASET App — Profile System: Complete Guide
> *Combined from: PROFILE_DB_INTEGRATION_GUIDE.md · PROFILE_PHOTO_UPLOAD_GUIDE.md · EDIT_PROFILE_GUIDE.md*

---

## 📑 Table of Contents
1. [System Overview](#1-system-overview)
2. [Data Architecture](#2-data-architecture)
3. [Profile Display Flow](#3-profile-display-flow)
4. [Edit Profile Flow](#4-edit-profile-flow)
5. [Profile Photo System](#5-profile-photo-system)
6. [Role-Based Visibility](#6-role-based-visibility)
7. [UI Components](#7-ui-components)
8. [PreferenceManager Reference](#8-preferencemanager-reference)
9. [Error Handling & Fallbacks](#9-error-handling--fallbacks)
10. [Files Reference](#10-files-reference)

---

## 1. System Overview

The profile system combines **three layers** of functionality: real-time database reading, SharedPreferences caching (for offline access), and role-based UI visibility.

```mermaid
graph TD
    PF[ProfileFragment] --> LSH[LocalStorageHelper\nRoom Database]
    PF --> PM[PreferenceManager\nSharedPreferences]

    LSH -->|Primary source| DB[(SQLite\nusers table)]
    PM -->|Fallback / Offline cache| PREF[SharedPreferences]

    DB --> UE[UserEntity]
    UE --> UI[Profile UI\nname, email, phone, role]

    PF --> VIS{Role?}
    VIS -->|Doctor| PROF[Show Professional Info\nHide My Prescriptions]
    VIS -->|Patient| MED[Show My Prescriptions\nHide Professional Info]
    VIS -->|Admin| ADM[Show Admin Info\nHide both]

    EPA[EditProfileActivity] --> LSH
    EPA --> PM

    PPH[ProfilePhotoHelper] --> STOR[Private App Storage\nprofile_photos/]
    STOR --> CIV[CircleImageView\nAll screens]
```

---

## 2. Data Architecture

### UserEntity (Room Database)
```java
@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    private String userId;
    private String email;
    private String password;   // SHA-256 hashed
    private String fullName;
    private String phone;
    private String role;       // "patient" | "doctor" | "admin"
    private String profileImage;
    private long createdAt;
}
```

### SharedPreferences Keys (PreferenceManager)

| Key | Purpose |
|-----|---------|
| `userId` | Logged-in user ID |
| `userRole` | Role for UI routing |
| `userName` | Cached full name |
| `userEmail` | Cached email address |
| `userPhone` | Cached phone number |
| `profilePhotoPath` | Local path to profile photo |
| `isLoggedIn` | Session status |

---

## 3. Profile Display Flow

```mermaid
flowchart TD
    A([User Opens Profile]) --> B[ProfileFragment.onCreateView]
    B --> C[setupUserInfo]
    C --> D{userId in\nPreferenceManager?}

    D -->|Yes| E[LocalStorageHelper.getUserById]
    D -->|No| K[loadFromPreferences]

    E --> F{Firebase/DB\nSuccess?}
    F -->|Yes - UserEntity returned| G[Update UI\nname, email, phone, role]
    F -->|Error| K

    G --> H[Save to PreferenceManager\nfor offline cache]
    H --> I[Load Profile Photo\nProfilePhotoHelper]
    K --> I

    I --> J{Photo path\nin preferences?}
    J -->|Yes| L[Load image from\nprivate storage]
    J -->|No| M[Show default ic_person]
```

---

## 4. Edit Profile Flow

```mermaid
flowchart TD
    A([User Taps Edit Profile]) --> B[Open EditProfileActivity]
    B --> C[loadUserData via LocalStorageHelper]
    C --> D[populateFields\nfill form with current data]

    D --> E[User edits Name / Email / Phone]
    E --> F[User taps Save]

    F --> G{Input Validation}
    G -->|Full name empty| ERR1[Show error on name field]
    G -->|Email empty or invalid| ERR2[Show error on email field]
    G -->|Phone empty| ERR3[Show error on phone field]
    G -->|All valid| H[showProgress true]

    H --> I[LocalStorageHelper.updateUser]
    I --> J{DB Update Success?}
    J -->|Yes| K[Save to PreferenceManager\nname, email, phone]
    K --> L[Toast: Profile updated!]
    L --> M[finish — return to ProfileFragment]
    M --> N[ProfileFragment auto-refreshes\nvia onResume]

    J -->|Error| O[showProgress false]
    O --> P[Toast: Failed to update profile]
```

### Input Validation Rules

| Field | Rule |
|-------|------|
| Full Name | Required, not empty |
| Email | Required, must match `EMAIL_ADDRESS` pattern |
| Phone | Required, not empty |
| Role | Read-only (system assigned, not editable) |

---

## 5. Profile Photo System

### Photo Upload Flow

```mermaid
flowchart TD
    TAP([User taps profile photo\nin EditProfileActivity]) --> DIALOG[showPhotoSelectionDialog]
    DIALOG --> CHOICE{User selects}

    CHOICE -->|Take Photo| PERM{Camera + Storage\nPermissions granted?}
    CHOICE -->|Choose from Gallery| GALLERY[openGallery\nIntent.ACTION_PICK]
    CHOICE -->|Cancel| DISMISS[Dismiss dialog]

    PERM -->|Granted| CAM[openCamera\nMediaStore.ACTION_IMAGE_CAPTURE]
    PERM -->|Denied| REQ[Request runtime permissions]
    REQ -->|Granted| CAM
    REQ -->|Denied| SILENT[Silent fail — no crash]

    CAM --> RESULT[onActivityResult REQUEST_CAMERA]
    GALLERY --> RESULT2[onActivityResult REQUEST_GALLERY]

    RESULT --> COPY[copyImageToAppStorage\nto profile_photos/]
    RESULT2 --> COPY

    COPY --> SAVEPATH[saveProfilePhotoPath\nin PreferenceManager]
    SAVEPATH --> UIUPDATE[imageView.setImageURI\nshow new photo immediately]
    UIUPDATE --> ALL[All screens now show new photo\nHomeFragment / ProfileFragment]
```

### Storage Details

```
Private App Storage
└── profile_photos/
    └── profile_{timestamp}.jpg   ← each upload creates a new file
```

```java
// Static load utility (used everywhere)
public static void loadProfilePhoto(Context context, ImageView imageView) {
    String photoPath = preferenceManager.getProfilePhotoPath();
    if (photoPath != null && !photoPath.isEmpty()) {
        imageView.setImageURI(Uri.parse(photoPath));
    } else {
        imageView.setImageResource(R.drawable.ic_person);  // fallback
    }
}
```

### FileProvider Configuration (`file_paths.xml`)
```xml
<paths>
    <external-files-path name="images" path="Pictures/" />
    <files-path name="profile_photos" path="profile_photos/" />
    <cache-path name="cache" path="." />
</paths>
```

### Integration Points

| Screen | Component | Method |
|--------|-----------|--------|
| EditProfileActivity | CircleImageView (100dp) | `profilePhotoHelper.showPhotoSelectionDialog()` |
| ProfileFragment | ImageView (80dp) | `ProfilePhotoHelper.loadProfilePhoto()` |
| PatientHomeFragment | CircleImageView (52dp) | `ProfilePhotoHelper.loadProfilePhoto()` |

---

## 6. Role-Based Visibility

```mermaid
flowchart TD
    ROLE{User Role?}

    ROLE -->|Doctor| DR[Show: Professional Info\nShow: Specialty / Fee / Available Times\nHide: My Prescriptions / Medical Records]
    ROLE -->|Admin| AD[Show: Administrator label\nHide: My Prescriptions / Medical Records\nHide: Consultation Fee / Available Times]
    ROLE -->|Patient| PA[Show: My Prescriptions / Medical Records\nHide: Professional Info section]
```

### Visibility Logic (from updateUserUI)

| View | Patient | Doctor | Admin |
|------|---------|--------|-------|
| `cardMedicalRecords` (My Prescriptions) | ✅ Visible | ❌ Gone | ❌ Gone |
| `tvMedicalRecordsTitle` | ✅ Visible | ❌ Gone | ❌ Gone |
| `cardMedicalInfo` (Professional Info) | ❌ Gone | ✅ Visible | ✅ Visible |
| `cardMedicalInfo2` | ❌ Gone | ✅ Visible | ✅ Visible |
| `tvProfessionalInfoTitle` | ❌ Gone | ✅ Visible | ✅ Visible |
| `layoutConsultationFee` | ❌ Gone | ✅ Visible | ❌ Gone |
| `layoutAvailableTimes` | ❌ Gone | ✅ Visible | ❌ Gone |

---

## 7. UI Components

### EditProfileActivity Layout Structure
```
ScrollView
└── LinearLayout
    ├── Header (Back button + "Edit Profile" title)
    ├── ProfileImageSection
    │   └── CircleImageView (100dp, clickable)
    ├── FormSection
    │   ├── TextInputEditText — Full Name
    │   ├── TextInputEditText — Email Address
    │   ├── TextInputEditText — Phone Number
    │   └── TextInputEditText — Role (read-only)
    ├── ActionButtons
    │   ├── MaterialButton — Save
    │   └── MaterialButton — Cancel
    └── ProgressOverlay (shown during save)
```

### Profile Photo XML (CircleImageView)
```xml
<!-- EditProfileActivity -->
<de.hdodenhof.circleimageview.CircleImageView
    android:id="@+id/ivProfileImage"
    android:layout_width="100dp"
    android:layout_height="100dp"
    android:src="@drawable/ic_person"
    android:clickable="true"
    app:civ_border_color="@color/white"
    app:civ_border_width="2dp" />

<!-- HomeFragment -->
<de.hdodenhof.circleimageview.CircleImageView
    android:id="@+id/ivProfile"
    android:layout_width="52dp"
    android:layout_height="52dp"
    app:civ_border_color="@color/white"
    app:civ_border_width="2.5dp" />
```

---

## 8. PreferenceManager Reference

### New Methods Added
```java
// Email
public void saveUserEmail(String email) { ... }
public String getUserEmail() { ... }

// Phone
public void saveUserPhone(String phone) { ... }
public String getUserPhone() { ... }

// Profile photo
public void saveProfilePhotoPath(String path) { ... }
public String getProfilePhotoPath() { ... }
```

---

## 9. Error Handling & Fallbacks

```mermaid
flowchart LR
    DB[Room DB Call] -->|Success| UI[Update UI with real data]
    DB -->|Error / userId null| PREF[Load from PreferenceManager\ncached data]
    PREF -->|Data exists| UI2[Update UI with cached data]
    PREF -->|Nothing cached| EMPTY[Show empty state\nfields remain blank]

    PHOTO[Load Profile Photo] -->|Path exists| IMG[setImageURI]
    PHOTO -->|No path / error| DEFAULT[setImageResource\nic_person fallback]
```

---

## 10. Files Reference

| File | Role |
|------|------|
| `ProfileFragment.java` | Main profile display, role-based visibility |
| `EditProfileActivity.java` | Profile editing, validation, save logic |
| `ProfilePhotoHelper.java` | Camera/gallery selection, storage, loading |
| `PreferenceManager.java` | Caching layer for offline profile access |
| `Constants.java` | All SharedPreferences key constants |
| `fragment_profile.xml` | Profile screen layout |
| `activity_edit_profile.xml` | Edit profile form layout |
| `fragment_patient_home.xml` | Home header with CircleImageView |
| `file_paths.xml` | FileProvider paths for camera/gallery |
| `circle_background_white.xml` | Oval background for profile photo |

---

*Last Updated: 2026-02-22 | HASET App — Profile Module*
