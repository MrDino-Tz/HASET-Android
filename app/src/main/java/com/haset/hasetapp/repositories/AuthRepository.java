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

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private final FirebaseAuth mAuth;
    private final DatabaseReference usersRef;

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
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(mapFirebaseAuthError(task.getException(), "Failed to send reset email"));
                }
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
