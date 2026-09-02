package com.haset.hasetapp.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

// import com.google.firebase.auth.AuthCredential;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.CrashMonitor;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AuthRepository {
    public static final String CREDENTIAL_ERROR_MESSAGE = "Incorrect email or password.";
    private static final String TAG = "AuthRepository";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final long HTTP_TIMEOUT_SECONDS = 20;
    private final FirebaseAuth mAuth;
    private final DatabaseReference usersRef;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build();

    public AuthRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_PATH);
    }

    public void signInWithEmail(String email, String password, FirebaseHelper.OnCompleteListener<FirebaseUser> callback) {
        CrashMonitor.step("auth", "AuthRepository.signIn", "sign-in started for " + safeEmail(email));
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    CrashMonitor.breadcrumb("sign-in succeeded uid=" + currentUid());
                    callback.onSuccess(mAuth.getCurrentUser());
                } else {
                    CrashMonitor.report("auth", "AuthRepository.signIn", "sign-in failed: " + safeEmail(email),
                            task.getException());
                    callback.onError(mapFirebaseAuthError(task.getException(), "Login failed"));
                }
            });
    }

    public void registerWithEmail(String email, String password, UserEntity userData, FirebaseHelper.OnCompleteListener<FirebaseUser> callback) {
        CrashMonitor.step("auth", "AuthRepository.register", "register started for " + safeEmail(email));
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    CrashMonitor.breadcrumb("register auth created uid=" + currentUid());
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        userData.setUserId(firebaseUser.getUid());
                        saveUserData(userData, new FirebaseHelper.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                CrashMonitor.breadcrumb("register user data saved role=" + userData.getRole());
                                callback.onSuccess(firebaseUser);
                            }

                            @Override
                            public void onError(String error) {
                                CrashMonitor.report("auth", "AuthRepository.register",
                                        "register DB save failed (rolling back auth): " + error, null);
                                // Cleanup auth user if database save fails
                                firebaseUser.delete();
                                callback.onError(error);
                            }
                        });
                    }
                } else {
                    CrashMonitor.report("auth", "AuthRepository.register",
                            "register auth creation failed: " + safeEmail(email), task.getException());
                    callback.onError(mapFirebaseAuthError(task.getException(), "Registration failed"));
                }
            });
    }

/*
    public void signInWithGoogle(String idToken, FirebaseHelper.OnCompleteListener<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(mAuth.getCurrentUser());
                } else {
                    callback.onError(task.getException() != null ? task.getException().getMessage() : "Google sign-in failed");
                }
            });
    }
*/

    public void getUserData(String uid, FirebaseHelper.OnCompleteListener<UserEntity> callback) {
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        UserEntity user = parseUser(snapshot);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Failed to parse user data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user data", e);
                        callback.onError("Error parsing user data: " + e.getMessage());
                    }
                } else {
                    callback.onSuccess(null); // User record doesn't exist yet
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void saveUserData(UserEntity user, FirebaseHelper.OnCompleteListener<Void> callback) {
        usersRef.child(user.getUserId()).setValue(user)
            .addOnSuccessListener(aVoid -> {
                if (Constants.ROLE_DOCTOR.equals(user.getRole())) {
                    java.util.Map<String, Object> doctorUpdates = new java.util.HashMap<>();
                    doctorUpdates.put("doctorId", user.getUserId());
                    doctorUpdates.put("regNo", user.getRegNo());
                    doctorUpdates.put("nin", user.getNin());
                    doctorUpdates.put("ninDocumentUrl", user.getNinDocumentUrl());
                    doctorUpdates.put("mctCertificateUrl", user.getMctCertificateUrl());
                    doctorUpdates.put("documentsStatus", "pending");
                    doctorUpdates.put("documentsVerified", false);
                    doctorUpdates.put("approved", false);
                    doctorUpdates.put("verified", false);
                    doctorUpdates.put("registrationPaymentStatus", "pending");
                    FirebaseHelper.getDoctorsNodeRef().child(user.getUserId()).updateChildren(doctorUpdates)
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                } else {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordResetEmail(String email, FirebaseHelper.OnCompleteListener<String> callback) {
        // Keep password-reset mail on the Hostinger SMTP implementation.
        // Falling back to Firebase here would send the outdated Firebase template.
        sendPasswordResetEmailViaBackend(email, callback);
    }

    /**
     * Sends a password reset email whose link is handled inside the app
     * (handleCodeInApp=true). Tapping the email link deep-links back into
     * AfyaHASET carrying the oobCode, so no Firebase web form is shown.
     */
    public void sendPasswordResetEmail(String email, com.google.firebase.auth.ActionCodeSettings settings,
                                       FirebaseHelper.OnCompleteListener<String> callback) {
        sendPasswordResetEmailViaFirebase(email, settings, callback);
    }

    private void sendPasswordResetEmailViaBackend(
            String email,
            FirebaseHelper.OnCompleteListener<String> callback
    ) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            sendPasswordResetEmailViaBackend(email, null, callback);
            return;
        }

        user.getIdToken(true)
            .addOnSuccessListener(token -> sendPasswordResetEmailViaBackend(
                    email,
                    "Bearer " + token.getToken(),
                    callback))
            .addOnFailureListener(error -> {
                Log.w(TAG, "Unable to refresh token for password reset email", error);
                sendPasswordResetEmailViaBackend(email, null, callback);
            });
    }

    private void sendPasswordResetEmailViaBackend(
            String email,
            String bearerToken,
            FirebaseHelper.OnCompleteListener<String> callback
    ) {
        String body = "{\"email\":\"" + jsonEscape(email) + "\"}";
        Request.Builder requestBuilder = new Request.Builder()
            .url(Constants.PASSWORD_RESET_EMAIL_API_URL)
            .post(RequestBody.create(body, JSON));
        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            requestBuilder.addHeader("Authorization", bearerToken);
        }
        Request request = requestBuilder.build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "Password reset email request failed", e);
                callback.onError("Unable to send password reset email. Check your connection and try again.");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                String responseBody = "";
                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Unable to read password reset response", e);
                } finally {
                    response.close();
                }
                handlePasswordResetResponse(response.code(), responseBody, callback);
            }
        });
    }

    private void handlePasswordResetResponse(
            int code,
            String responseBody,
            FirebaseHelper.OnCompleteListener<String> callback
    ) {
        String message = "";
        String status = "";
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("message") && !json.get("message").isJsonNull()) {
                message = json.get("message").getAsString();
            }
            if (json.has("status") && !json.get("status").isJsonNull()) {
                status = json.get("status").getAsString();
            }
        } catch (Exception ignored) {
            // Fall back to HTTP status handling below.
        }

        if (code >= 200 && code < 300 && "success".equals(status)) {
            callback.onSuccess(message.isEmpty()
                ? "A password reset link has been sent to your email. Please check your inbox and spam folder."
                : message);
            return;
        }
        if (code == 404 || "not_found".equals(status)) {
            callback.onError(message.isEmpty()
                ? "No account found with this email address. Please check the email or register."
                : message);
            return;
        }
        if (message.isEmpty()) {
            message = "Unable to send password reset email.";
        }
        Log.w(TAG, "Password reset email backend returned HTTP " + code + ": " + responseBody);
        callback.onError(message);
    }

    private void sendPasswordResetEmailViaFirebase(String email, com.google.firebase.auth.ActionCodeSettings settings,
                                                   FirebaseHelper.OnCompleteListener<String> callback) {
        mAuth.sendPasswordResetEmail(email, settings)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess("A password reset link has been sent to your email. Please check your inbox and spam folder.");
                } else {
                    callback.onError(mapPasswordResetFirebaseError(task.getException()));
                }
            });
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public void sendEmailVerificationViaSmtp(FirebaseUser user) {
        sendEmailVerificationViaSmtp(user, null);
    }

    public void sendEmailVerificationViaSmtp(FirebaseUser user, FirebaseHelper.OnCompleteListener<Void> callback) {
        if (user == null) {
            if (callback != null) callback.onError("Unable to send verification email.");
            return;
        }

        user.getIdToken(true)
            .addOnSuccessListener(token -> {
                Request request = new Request.Builder()
                    .url(Constants.EMAIL_VERIFICATION_API_URL)
                    .post(RequestBody.create(
                        "{\"email\":\"" + jsonEscape(user.getEmail()) + "\"}", JSON))
                    .addHeader("Authorization", "Bearer " + token.getToken())
                    .build();
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.w(TAG, "SMTP email verification request failed; trying Firebase fallback", e);
                        sendEmailVerificationViaFirebase(user, callback);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                        boolean successful = response.isSuccessful();
                        String responseBody = "";
                        try {
                            if (response.body() != null) responseBody = response.body().string();
                        } catch (IOException ignored) {
                        }
                        Log.w(TAG, "SMTP email verification response: HTTP " + response.code()
                                + " body=" + responseBody);
                        response.close();
                        if (successful) {
                            try {
                                JsonObject body = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (body.has("sent") && !body.get("sent").getAsBoolean()) {
                                    successful = false;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        if (successful) {
                            if (callback != null) callback.onSuccess(null);
                        } else {
                            Log.w(TAG, "Backend verification email failed with HTTP "
                                    + response.code() + "; trying Firebase fallback");
                            sendEmailVerificationViaFirebase(user, callback);
                        }
                    }
                });
            })
            .addOnFailureListener(error -> {
                Log.w(TAG, "Unable to refresh token for SMTP email verification; trying Firebase fallback", error);
                sendEmailVerificationViaFirebase(user, callback);
            });
    }

    private void sendEmailVerificationViaFirebase(FirebaseUser user, FirebaseHelper.OnCompleteListener<Void> callback) {
        if (user == null) {
            if (callback != null) callback.onError("Unable to send verification email.");
            return;
        }

        user.sendEmailVerification()
            .addOnSuccessListener(unused -> {
                if (callback != null) callback.onSuccess(null);
            })
            .addOnFailureListener(error -> {
                Log.w(TAG, "Firebase email verification fallback failed", error);
                if (callback != null) callback.onError("Unable to send verification email.");
            });
    }

    public void logout() {
        CrashMonitor.step("auth", "AuthRepository.logout", "sign-out requested");
        mAuth.signOut();
    }

    private UserEntity parseUser(DataSnapshot snapshot) {
        String userId = snapshot.getKey();
        String email = snapshot.child("email").getValue(String.class);
        String fullName = snapshot.child("fullName").getValue(String.class);
        String role = snapshot.child("role").getValue(String.class);
        
        Object phoneValue = snapshot.child("phone").getValue();
        String phone = (phoneValue instanceof Long) ? String.valueOf(phoneValue) : (String) phoneValue;
        
        String profileImage = snapshot.child("profileImage").getValue(String.class);
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        String regNo = snapshot.child("regNo").getValue(String.class);

        if (userId != null && email != null && fullName != null && role != null) {
            UserEntity user = new UserEntity();
            user.setUserId(userId);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRegNo(regNo);
            user.setPhone(phone != null ? phone : "");
            user.setRole(role);
            user.setProfileImage(profileImage != null ? profileImage : "");
            user.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
            return user;
        }
        return null;
    }

    private String mapPasswordResetFirebaseError(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(errorCode) || "user-not-found".equalsIgnoreCase(errorCode)) {
                return "No account found with this email address. Please check the email or register.";
            }
            if ("ERROR_INVALID_EMAIL".equals(errorCode) || "invalid-email".equalsIgnoreCase(errorCode)) {
                return "Please enter a valid email address.";
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(errorCode) || "too-many-requests".equalsIgnoreCase(errorCode)) {
                return "Too many attempts. Please try again later.";
            }
        }
        return mapFirebaseAuthError(exception, "Failed to send reset email.");
    }

    private String safeEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.substring(0, 2) + "****" + email.substring(at);
    }

    private String currentUid() {
        FirebaseUser u = mAuth.getCurrentUser();
        return u != null ? u.getUid() : "none";
    }

    private String mapFirebaseAuthError(Exception exception, String fallbackMessage) {
        if (exception == null) {
            return fallbackMessage;
        }

        String message = exception.getMessage();
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_INVALID_CREDENTIAL".equals(errorCode)
                    || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(errorCode)
                    || "ERROR_WRONG_PASSWORD".equals(errorCode)
                    || "ERROR_USER_NOT_FOUND".equals(errorCode)
                    || "invalid-credential".equalsIgnoreCase(errorCode)
                    || "wrong-password".equalsIgnoreCase(errorCode)
                    || "user-not-found".equalsIgnoreCase(errorCode)) {
                return CREDENTIAL_ERROR_MESSAGE;
            }
            if ("ERROR_INVALID_EMAIL".equals(errorCode) || "invalid-email".equalsIgnoreCase(errorCode)) {
                return "Please enter a valid email address.";
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(errorCode) || "too-many-requests".equalsIgnoreCase(errorCode)) {
                return "Too many attempts. Please try again later.";
            }
            if ("ERROR_USER_DISABLED".equals(errorCode) || "user-disabled".equalsIgnoreCase(errorCode)) {
                return "This account has been disabled.";
            }
            if ("ERROR_WEAK_PASSWORD".equals(errorCode) || "weak-password".equalsIgnoreCase(errorCode)
                    || "password-does-not-meet-requirements".equalsIgnoreCase(errorCode)) {
                return "Password must be at least 12 characters with uppercase, lowercase, and a number.";
            }
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(errorCode) || "operation-not-allowed".equalsIgnoreCase(errorCode)) {
                return "Firebase Authentication is blocking sign-up. Enable Email/Password sign-in in the Firebase console.";
            }
        }

        if (message != null && message.toLowerCase().contains("restricted to administrators only")) {
            return "Firebase Authentication is blocking sign-up. Enable Email/Password sign-in in the Firebase console.";
        }

        return message != null && !message.trim().isEmpty() ? message : fallbackMessage;
    }
}
