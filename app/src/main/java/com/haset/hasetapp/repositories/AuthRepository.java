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
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.Constants;
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
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(mAuth.getCurrentUser());
                } else {
                    callback.onError(mapFirebaseAuthError(task.getException(), "Login failed"));
                }
            });
    }

    public void registerWithEmail(String email, String password, UserEntity userData, FirebaseHelper.OnCompleteListener<FirebaseUser> callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        userData.setUserId(firebaseUser.getUid());
                        saveUserData(userData, new FirebaseHelper.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                callback.onSuccess(firebaseUser);
                            }

                            @Override
                            public void onError(String error) {
                                // Cleanup auth user if database save fails
                                firebaseUser.delete();
                                callback.onError(error);
                            }
                        });
                    }
                } else {
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
                    doctorUpdates.put("approved", false);
                    doctorUpdates.put("verified", false);
                    FirebaseHelper.getDoctorsNodeRef().child(user.getUserId()).updateChildren(doctorUpdates)
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                } else {
                    callback.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordResetEmail(String email, FirebaseHelper.OnCompleteListener<Void> callback) {
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
                                       FirebaseHelper.OnCompleteListener<Void> callback) {
        sendPasswordResetEmailViaFirebase(email, settings, callback);
    }

    private void sendPasswordResetEmailViaBackend(
            String email,
            FirebaseHelper.OnCompleteListener<Void> callback
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
            FirebaseHelper.OnCompleteListener<Void> callback
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
                callback.onError("Unable to send password reset email.");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                boolean successful = response.isSuccessful();
                response.close();
                if (successful) {
                    callback.onSuccess(null);
                } else {
                    Log.w(TAG, "Password reset email backend returned HTTP " + response.code());
                    callback.onError("Unable to send password reset email.");
                }
            }
        });
    }

    private void sendPasswordResetEmailViaFirebase(String email, FirebaseHelper.OnCompleteListener<Void> callback) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(mapFirebaseAuthError(task.getException(), "Failed to send reset email"));
                }
            });
    }

    private void sendPasswordResetEmailViaFirebase(String email, com.google.firebase.auth.ActionCodeSettings settings,
                                                   FirebaseHelper.OnCompleteListener<Void> callback) {
        mAuth.sendPasswordResetEmail(email, settings)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(mapFirebaseAuthError(task.getException(), "Failed to send reset email"));
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
        if (user == null) return;
        user.getIdToken(true)
            .addOnSuccessListener(token -> {
                Request request = new Request.Builder()
                    .url(Constants.EMAIL_VERIFICATION_API_URL)
                    .post(RequestBody.create("{}", JSON))
                    .addHeader("Authorization", "Bearer " + token.getToken())
                    .build();
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.w(TAG, "Email verification request failed", e);
                        sendEmailVerificationViaFirebase(user, callback);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                        boolean successful = response.isSuccessful();
                        response.close();
                        if (successful) {
                            if (callback != null) callback.onSuccess(null);
                        } else {
                            sendEmailVerificationViaFirebase(user, callback);
                        }
                    }
                });
            })
            .addOnFailureListener(error -> {
                Log.w(TAG, "Unable to refresh token for email verification", error);
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
