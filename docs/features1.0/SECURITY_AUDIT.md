# HASETApp Security Audit Report

**Date:** March 10, 2026  
**App:** HASETApp (Android)  
**Severity:** Critical - Urgent Action Required

---

## Executive Summary

This security audit identifies multiple vulnerabilities in the HASETApp Android application. The most critical issue is the **absence of Firebase security rules**, which likely exposes your entire database to unauthorized access. Immediate action is required to secure user data and prevent potential attacks.

---

## 🔴 CRITICAL VULNERABILITIES

### 1. Missing Firebase Database Security Rules

**Severity:** CRITICAL  
**Location:** Firebase Console (not in codebase)

**Description:**  
No `database.rules.json` found in the project. Firebase Realtime Database is likely using default "public" rules, meaning **anyone can read/write ALL data** in your database.

**Impact:**
- Complete exposure of user personal data (names, emails, phone numbers)
- Attackers can read/modify/delete all medical appointments
- Doctors can modify their own wallet balances
- Attackers can create fake appointments
- Admin functions can be accessed

**Proof of Concept:**
```javascript
// Anyone with the Firebase URL can do this:
firebase.database().ref('users').once('value', snapshot => {
  console.log(snapshot.val()); // All user data exposed
});

// Attackers can also:
firebase.database().ref('doctor_wallets/attacker_id').set({ balance: 999999999 });
```

**Fix:**
Create `database.rules.json` in your Firebase project:

```json
{
  "rules": {
    "users": {
      "$user_id": {
        ".read": "auth != null && auth.uid === $user_id",
        ".write": "auth != null && auth.uid === $user_id"
      }
    },
    "doctors": {
      "$doctor_id": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid === $doctor_id"
      }
    },
    "doctor_wallets": {
      "$doctor_id": {
        ".read": "auth != null && auth.uid === $doctor_id",
        ".write": "auth != null && auth.uid === $doctor_id"
      }
    },
    "appointments": {
      "$appointment_id": {
        ".read": "auth != null && (data.child('patientId').val() === auth.uid || data.child('doctorId').val() === auth.uid)",
        ".write": "auth != null"
      }
    },
    "withdrawal_requests": {
      "$request_id": {
        ".read": "auth != null",
        ".write": "auth != null && root.child('users/' + auth.uid + '/role').val() === 'admin'"
      }
    },
    "audit_logs": {
      ".read": "auth != null && root.child('users/' + auth.uid + '/role').val() === 'admin'",
      ".write": "auth != null && root.child('users/' + auth.uid + '/role').val() === 'admin'"
    },
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

---

### 2. Client-Side Only Permission Checks

**Severity:** HIGH  
**Locations:**
- `FirebaseHelper.java:328` - Appointment creation
- `FirebaseHelper.java:1112` - Rating submissions
- `FirebaseHelper.java:1200` - Doctor registration

**Description:**  
The app relies entirely on client-side checks. Any user can bypass these by modifying the APK or using Firebase Admin SDK.

**Attack Scenario:**
```java
// Attacker modifies the app to bypass role checks
// Then registers themselves as a doctor:
FirebaseHelper.registerAsDoctor(userId, doctorData); // Should be server-side only
```

**Fix:** Move sensitive operations to Firebase Cloud Functions:
```javascript
// Cloud Function example for doctor registration
exports.registerDoctor = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated');
  
  // Only existing users can become doctors
  const userSnapshot = await admin.database().ref(`users/${context.auth.uid}`).once('value');
  if (!userSnapshot.exists()) throw new Error('User not found');
  
  // Validate specialty and fee
  if (!data.specialty || data.fee < 0) throw new Error('Invalid data');
  
  // Write to doctors node
  return admin.database().ref(`doctors/${context.auth.uid}`).set({...});
});
```

---

### 3. No Input Validation on Financial Transactions

**Severity:** HIGH  
**Locations:**
- `FirebaseHelper.java:620-650` - Wallet additions
- `FirebaseHelper.java:759-790` - Withdrawal request processing

**Description:**  
No server-side validation for wallet operations. Attackers can manipulate their balance.

**Fix:** Use Firebase Transactions WITH server-side validation:

```javascript
// Cloud Function to add to wallet
exports.addToWallet = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new Error('Not authenticated');
  
  const { amount } = data;
  if (typeof amount !== 'number' || amount <= 0 || amount > 1000000) {
    throw new Error('Invalid amount');
  }
  
  const walletRef = admin.database().ref(`doctor_wallets/${context.auth.uid}`);
  
  return walletRef.transaction(wallet => {
    if (wallet === null) return { balance: amount, totalEarnings: amount };
    return { 
      balance: wallet.balance + amount, 
      totalEarnings: wallet.totalEarnings + amount 
    };
  });
});
```

---

## 🟠 HIGH RISK VULNERABILITIES

### 4. SharedPreferences Data Tampering

**Severity:** HIGH  
**Location:** `PreferenceManager.java`

**Description:**  
User role and ID stored in SharedPreferences can be modified on rooted devices.

```java
// Lines 30-36 - Role stored in plaintext
editor.putString(Constants.KEY_USER_ROLE, role);
editor.apply();

// Lines 21-28 - User ID stored in plaintext
editor.putString(Constants.KEY_USER_ID, userId);
```

**Impact:** Attackers on rooted devices can change their role to "admin" and access admin functions.

**Mitigation:**
1. Use Android Keystore for sensitive data
2. Always verify role from Firebase on app startup
3. Use server-side role enforcement

---

### 5. No Rate Limiting

**Severity:** MEDIUM-HIGH  
**Affected Endpoints:**
- Password reset emails (`FirebaseHelper.java:116-120`)
- Appointment creation
- Message sending

**Impact:**
- Email bombing via password reset
- Database flooding via rapid appointment creation

**Fix:** Implement Cloud Function rate limiting:

```javascript
exports.rateLimit = functions.runWith({ memory: '256MB', timeoutSeconds: 30 })
  .pubsub.schedule('every 1 minutes').onRun(async (context) => {
    // Check for excessive requests and block if needed
  });
```

---

### 6. Missing Storage Security Rules

**Severity:** HIGH  
**Location:** Firebase Storage

**Description:**  
No `storage.rules` found. User-uploaded profile photos and images may be publicly accessible.

**Fix:** Create `storage.rules`:

```json
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile_photos/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /article_images/{userId}/{fileName} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

---

## 🟡 MEDIUM RISK VULNERABILITIES

### 7. No SSL Certificate Pinning

**Severity:** MEDIUM  
**Description:**  
App doesn't implement certificate pinning, making it vulnerable to Man-in-the-Middle (MITM) attacks.

**Fix:** Add OkHttp certificate pinning:

```java
// In your network client setup
CertificatePinner certificatePinner = new CertificatePinner.Builder()
    .add("firebasestorage.googleapis.com", "sha256/XXXXXXXXXXXXX")
    .build();

OkHttpClient client = new OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build();
```

---

### 8. Excessive Android Permissions

**Severity:** MEDIUM  
**Location:** `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

**Recommendations:**
- `READ_PHONE_STATE`: Remove if not essential
- `ACCESS_FINE_LOCATION`: Request only when needed, not at install time

---

### 9. No Code Obfuscation Verification

**Severity:** MEDIUM  
**Action:** Verify `proguard-rules.pro` is properly configured before release.

---

### 10. Hardcoded API Keys Risk

**Severity:** LOW-MEDIUM  
**Action:** Ensure `google-services.json` is:
- Not committed to public repositories
- Added to `.gitignore`

---

## ✅ GOOD SECURITY PRACTICES (Already Implemented)

| Security Feature | Status | Location |
|------------------|--------|----------|
| `allowBackup="false"` | ✅ Implemented | AndroidManifest.xml:30 |
| `usesCleartextTraffic="false"` | ✅ Implemented | AndroidManifest.xml:39 |
| Network Security Config | ✅ Implemented | network_security_config.xml |
| FileProvider Secure Config | ✅ Implemented | AndroidManifest.xml:232-249 |
| Biometric Authentication | ✅ Available | BiometricHelper.java |

---

## Priority Action Items

### Immediate (Today)
1. [ ] Set up Firebase Database security rules
2. [ ] Set up Firebase Storage security rules

### This Week
3. [ ] Move financial operations to Cloud Functions
4. [ ] Move role changes (user → doctor) to Cloud Functions
5. [ ] Implement rate limiting on sensitive endpoints

### Before Release
6. [ ] Add SSL certificate pinning
7. [ ] Review and reduce Android permissions
8. [ ] Verify ProGuard/R8 code obfuscation
9. [ ] Audit Firebase Storage permissions
10. [ ] Security test the release build

---

## Testing Recommendations

1. **Firebase Database Test:**
   ```bash
   # Try accessing data without authentication
   curl "https://your-project.firebaseio.com/users.json"
   ```

2. **Static Analysis:**
   Use MobSF or similar tools to analyze the APK

3. **Penetration Testing:**
   - Test on rooted devices
   - Test with proxy (Burp Suite)
   - Test Firebase rules

---

## References

- [Firebase Security Rules Guide](https://firebase.google.com/docs/database/security)
- [Firebase Storage Rules](https://firebase.google.com/docs/storage/security)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)
- [Android Security Guidelines](https://developer.android.com/topic/security)

---

**Report Generated:** March 10, 2026  
**Prepared by:** Security Audit
