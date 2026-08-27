# 🔐 **Login Rate Limiting - Feature Documentation**

## 🎯 **Overview**

HASETApp now protects the **login endpoint** against brute-force credential attacks by tracking consecutive failed login attempts and temporarily **locking the user out** after a threshold is reached. This is a **client-side** mitigation that works alongside Firebase Authentication's built-in server-side throttling.

---

## 🛡️ **Why Rate Limiting Login?**

```
🚨 Threat: Brute-Force Password Guessing
├── 🎯 Attacker repeatedly tries common passwords
├── ⚠️ Risk: Account compromise via guessing
├── 🛡️ Mitigation: Lock out after repeated failures
└-> ✅ Protects real users from account takeover

⚙️ Defense Layers (Combined):
├── 📱 Client-side attempt lockout (this feature)
├── 🔥 Firebase Auth automatic IP throttling
├── 🔑 Strong password policy (client validation)
└-> ✅ Multi-layered protection
```

---

## ⚙️ **How It Works**

### **Client-Side Lockout (New)**

```
🔢 Failed Attempt Tracking:
└-> 🎯 MAX_LOGIN_ATTEMPTS = 3 consecutive failures
        ↓
⏱️ 🔒 LOCKOUT_DURATION_MS = 5 minutes
        ↓
🔓 After lockout expires, user can retry
```

### **Configuration Values**

| Parameter | Value | Location |
|-----------|-------|----------|
| `MAX_LOGIN_ATTEMPTS` | `3` | `LoginActivity.java` |
| `LOCKOUT_DURATION_MS` | `300,000 ms` (5 min) | `LoginActivity.java` |

---

## 🖥️ **Implementation Details**

### **1. Credential Failure Detection (ViewModel/Repository)**

The `AuthRepository` exposes a constant that uniquely identifies a **bad-credential** error (wrong email/password), as opposed to network or service errors:

```java
// AuthRepository.java
public static final String CREDENTIAL_ERROR_MESSAGE = "Incorrect email or password.";
```

`AuthViewModel` sets a `credentialFailure` flag on the `AuthState` when this error is returned:

```java
// AuthViewModel.java
public void login(String email, String password) {
    authState.setValue(AuthState.loading("Logging in..."));
    repository.signInWithEmail(email, password, new FirebaseHelper.OnCompleteListener<FirebaseUser>() {
        @Override
        public void onSuccess(FirebaseUser result) {
            pendingFirebaseUser = result;
            checkEmailVerifiedThenContinue(result);
        }

        @Override
        public void onError(String error) {
            boolean credentialFailure = com.haset.hasetapp.repositories.AuthRepository
                    .CREDENTIAL_ERROR_MESSAGE.equals(error);
            authState.setValue(AuthState.error(error, credentialFailure));
        }
    });
}
```

The `AuthState` class was extended with a `credentialFailure` boolean field:

```java
// AuthViewModel.java (AuthState inner class)
public final boolean credentialFailure;

public static AuthState error(String message)                 { /* credentialFailure = false */ }
public static AuthState error(String message, boolean flag)   { /* credentialFailure = flag   */ }
```

> **Why the flag?** Only actual bad-credential failures should count toward lockout. Network outages, MFA-service unavailability, and expired sessions must **not** trigger (or increment) the lockout.

---

### **2. Lockout State (LoginActivity)**

Instance fields track the attempt counter and the lockout timer:

```java
// LoginActivity.java
private static final int MAX_LOGIN_ATTEMPTS = 3;
private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5 minutes
private int loginAttemptCount;
private long lockoutUntil;
```

#### **Guarding Login**

`loginUser()` short-circuits when the user is still locked out:

```java
private void loginUser() {
    if (isLoginLocked()) {
        showLockoutMessage();
        return;
    }
    // ... existing email/password validation and authViewModel.login(email, password)
}
```

#### **Attempt Counting**

In the `AuthState.ERROR` observer branch, the counter is incremented **only when `state.credentialFailure` is true**. On reaching the threshold, the lockout window is armed and the counter is reset:

```java
case ERROR:
    CustomDialog.hideLoading();
    String loginDetail = com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(
        LoginActivity.this, state.message);
    com.haset.hasetapp.utils.ErrorLogger.log(loginDetail, state.message);
    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content), loginDetail);
    if (state.credentialFailure) {
        loginAttemptCount++;
        if (loginAttemptCount >= MAX_LOGIN_ATTEMPTS) {
            lockoutUntil = SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS;
            loginAttemptCount = 0;
            showLockoutMessage();
        }
    }
    resetLoginButton();
    break;
```

#### **Resetting on Success**

A successful authentication clears the counter and any lockout window:

```java
case AUTHENTICATED:
case MFA_REQUIRED:
case MFA_SETUP_REQUIRED:
    loginAttemptCount = 0;
    lockoutUntil = 0;
    // ... continue with normal flow
```

#### **Lockout Helpers**

```java
private boolean isLoginLocked() {
    return SystemClock.elapsedRealtime() < lockoutUntil;
}

private void showLockoutMessage() {
    long remainingMs = lockoutUntil - SystemClock.elapsedRealtime();
    if (remainingMs < 0) remainingMs = 0;
    long remainingMinutes = (remainingMs + 60_000L - 1L) / 60_000L;
    String message = getString(R.string.login_locked, remainingMinutes);
    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content), message);
}
```

---

### **3. Lockout Message (Strings)**

The lockout message includes the remaining **minutes** (rounded up):

| Locale | Key | Value |
|--------|-----|-------|
| English | `login_locked` | `Too many failed login attempts. Please try again in %1$d minute(s).` |
| Swahili | `login_locked` | `Majiribio mengi ya kuingia yameshindwa. Tafadhali jaribu tena baada ya dakika %1$d.` |

---

## 🔥 **Server-Side Protection (Firebase Auth)**

Firebase Authentication provides **automatic** brute-force protection. When the server detects too many requests, it returns the `ERROR_TOO_MANY_REQUESTS` code. This is surfaced to the user with a dedicated message:

```java
// AuthRepository.java (in mapFirebaseAuthError)
if ("ERROR_TOO_MANY_REQUESTS".equals(errorCode) || "too-many-requests".equalsIgnoreCase(errorCode)) {
    return "Too many attempts. Please try again later.";
}
```

```
🛡️ Protected Endpoints:
├── 📱 signInWithEmailAndPassword (Firebase Auth)
├── 🔥 Firebase error code: too-many-requests / ERROR_TOO_MANY_REQUESTS
└-> 📣 User message: "Too many attempts. Please try again later."
```

---

## 🖇️ **Related Fix: Weak-Password Message Consistency**

As part of this work, the stale `ERROR_WEAK_PASSWORD` message in `AuthRepository` was aligned with the app's actual password policy (`ValidationUtils.isStrongPassword`):

| Before | After |
|--------|-------|
| "Password must be at least **13** characters with an uppercase letter and a **special character** (!@#$%^&*)." | "Password must be at least **12** characters with uppercase, lowercase, and a **number**." |

---

## 🧪 **Behavior / Test Scenarios**

```
✅ Wrong password 3 times in a row:
   └-> After the 3rd failure, login is locked for 5 minutes,
       button actions show the lockout Snackbar with remaining time.

✅ Wait for the lockout window to expire:
   └-> Login attempts are allowed again (counter cleared).

✅ Successful login before reaching the threshold:
   └-> Counter resets to 0.

✅ Network outage / MFA-service error:
   └-> Does NOT increment the counter (only credential failures count).
```

---

## 📁 **Files Modified**

| File | Change |
|------|--------|
| `app/src/main/java/com/haset/hasetapp/activities/LoginActivity.java` | Added lockout constants, counter, `isLoginLocked()`, `showLockoutMessage()`, attempt counting in observer |
| `app/src/main/java/com/haset/hasetapp/viewmodels/AuthViewModel.java` | Added `credentialFailure` flag to `AuthState`; set it in `login()` |
| `app/src/main/java/com/haset/hasetapp/repositories/AuthRepository.java` | Added `CREDENTIAL_ERROR_MESSAGE` constant; aligned `ERROR_WEAK_PASSWORD` text |
| `app/src/main/res/values/strings.xml` | Added `login_locked` (English) |
| `app/src/main/res/values-sw/strings.xml` | Added `login_locked` (Swahili) |

---

## 📝 **Notes / Limitations**

```
⚠️ Client-side lockout is a UX + first-line defense:
   └-> Can be bypassed by a determined attacker (e.g. clearing app data)
   └-> Real enforcement comes from Firebase Auth / backend throttling

🌐 The mobile/auth + MFA endpoints (mobile/mfa/*, mobile/password/reset)
   are served by a separate deployed backend NOT present in this repo —
   its rate limiting cannot be verified or changed here.
```
