package com.haset.hasetapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.repositories.AuthRepository;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.CrashMonitor;
import com.haset.hasetapp.api.MobileMfaApiService;
import com.haset.hasetapp.api.RetrofitClient;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    private static final long PASSWORD_RESET_COOLDOWN_MS = 30_000L;
    
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();
    private FirebaseUser pendingFirebaseUser;
    private long lastPasswordResetRequestAt;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository();
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

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
                CrashMonitor.report("auth", "AuthViewModel.login", "login rejected: " + error, null);
                boolean credentialFailure = com.haset.hasetapp.repositories.AuthRepository.CREDENTIAL_ERROR_MESSAGE.equals(error);
                authState.setValue(AuthState.error(error, credentialFailure));
            }
        });
    }

    private void checkEmailVerifiedThenContinue(FirebaseUser user) {
        if (user == null) {
            authState.setValue(AuthState.error("Authentication expired."));
            return;
        }

        user.reload()
            .addOnSuccessListener(unused -> {
                FirebaseUser refreshed = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (refreshed == null) {
                    authState.postValue(AuthState.error("Authentication expired."));
                    return;
                }

                if (!refreshed.isEmailVerified()) {
                    repository.sendEmailVerificationViaSmtp(refreshed, new FirebaseHelper.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            blockUnverifiedLogin();
                        }

                        @Override
                        public void onError(String error) {
                            blockUnverifiedLogin();
                        }
                    });
                    return;
                }

                pendingFirebaseUser = refreshed;
                checkMfaThenFetch(refreshed);
            })
            .addOnFailureListener(error -> authState.setValue(AuthState.error("Unable to refresh authentication.")));
    }

    private void checkMfaThenFetch(FirebaseUser user) {
        user.getIdToken(true).addOnSuccessListener(token -> {
            MobileMfaApiService api = RetrofitClient.getInstance().getMobileMfaApiService();
            api.status("Bearer " + token.getToken()).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null) { authState.postValue(AuthState.error("MFA service unavailable. Please try again later.")); return; }
                    boolean enabled = response.body().has("two_factor_enabled") && response.body().get("two_factor_enabled").getAsBoolean();
                    if (enabled) authState.postValue(AuthState.mfaRequired()); else fetchUserData(user.getUid());
                }
                public void onFailure(Call<JsonObject> call, Throwable t) { CrashMonitor.report("auth", "AuthViewModel.checkMfa", "MFA status check failed", t); authState.postValue(AuthState.error("MFA service unavailable.")); }
            });
        }).addOnFailureListener(e -> CrashMonitor.report("auth", "AuthViewModel.checkMfa", "token refresh failed during MFA check", e));
    }

    public void verifyMfa(String code) {
        if (pendingFirebaseUser == null) { authState.setValue(AuthState.error("Login session expired.")); return; }
        authState.setValue(AuthState.loading("Verifying code..."));
        pendingFirebaseUser.getIdToken(true).addOnSuccessListener(token -> {
            JsonObject body = new JsonObject(); body.addProperty("code", code);
            RetrofitClient.getInstance().getMobileMfaApiService().verify("Bearer " + token.getToken(), body).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) { if (response.isSuccessful()) fetchUserData(pendingFirebaseUser.getUid()); else { CrashMonitor.breadcrumb("MFA verify rejected"); authState.postValue(AuthState.mfaError("Invalid or expired MFA code.")); } }
                public void onFailure(Call<JsonObject> call, Throwable t) { CrashMonitor.report("auth", "AuthViewModel.verifyMfa", "MFA verify network failure", t); authState.postValue(AuthState.mfaError("MFA verification failed. Try again.")); }
            });
        }).addOnFailureListener(e -> authState.setValue(AuthState.mfaError("Login session expired.")));
    }

    public void resumeAfterMfaSetup() {
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) checkEmailVerifiedThenContinue(user); else authState.setValue(AuthState.error("Authentication expired."));
    }

    public void register(String email, String password, UserEntity userData) {
        authState.setValue(AuthState.loading("Creating account..."));
        repository.registerWithEmail(email, password, userData, new FirebaseHelper.OnCompleteListener<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                repository.sendEmailVerificationViaSmtp(result, new FirebaseHelper.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        completeRegistrationPendingVerification();
                    }

                    @Override
                    public void onError(String error) {
                        repository.logout();
                        authState.postValue(AuthState.error(
                            error != null ? error : "Unable to send verification email."));
                    }
                });
            }

            @Override
            public void onError(String error) {
                CrashMonitor.report("auth", "AuthViewModel.register", "registration rejected: " + error, null);
                authState.setValue(AuthState.error(error));
            }
        });
    }

    private void blockUnverifiedLogin() {
        pendingFirebaseUser = null;
        repository.logout();
        authState.postValue(AuthState.error(getApplication().getString(com.haset.hasetapp.R.string.verify_email_before_login)));
    }

    private void completeRegistrationPendingVerification() {
        pendingFirebaseUser = null;
        authState.postValue(AuthState.success(getApplication().getString(com.haset.hasetapp.R.string.verify_email_after_registration)));
    }

    /*
    public void loginWithGoogle(String idToken) {
        authState.setValue(AuthState.loading("Connecting with Google..."));
        repository.signInWithGoogle(idToken, new FirebaseHelper.OnCompleteListener<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                fetchUserData(result.getUid());
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
    }
    */

    public void resetPassword(String email) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastPasswordResetRequestAt < PASSWORD_RESET_COOLDOWN_MS) {
            authState.setValue(AuthState.success(passwordResetResponse()));
            return;
        }
        lastPasswordResetRequestAt = now;
        authState.setValue(AuthState.loading("Sending reset email..."));
        repository.sendPasswordResetEmail(email, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                authState.postValue(AuthState.success(passwordResetResponse()));
            }

            @Override
            public void onError(String error) {
                authState.postValue(AuthState.success(passwordResetResponse()));
            }
        });
    }

    /**
     * In-app password reset: the emailed link opens this app (deep link)
     * with the oobCode instead of a web page.
     */
    public void resetPassword(String email, com.google.firebase.auth.ActionCodeSettings settings) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastPasswordResetRequestAt < PASSWORD_RESET_COOLDOWN_MS) {
            authState.setValue(AuthState.success(passwordResetResponse()));
            return;
        }
        lastPasswordResetRequestAt = now;
        authState.setValue(AuthState.loading("Sending reset email..."));
        repository.sendPasswordResetEmail(email, settings, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                authState.postValue(AuthState.success(passwordResetResponse()));
            }

            @Override
            public void onError(String error) {
                authState.postValue(AuthState.success(passwordResetResponse()));
            }
        });
    }

    private String passwordResetResponse() {
        return getApplication().getString(com.haset.hasetapp.R.string.reset_email_sent);
    }

    public void fetchUserData(String uid) {
        repository.getUserData(uid, new FirebaseHelper.OnCompleteListener<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                if (result != null) {
                    currentUser.setValue(result);
                    authState.setValue(AuthState.authenticated(result));
                } else {
                    authState.setValue(AuthState.unregistered(uid));
                }
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
    }

    public void saveUserAndLogin(UserEntity user) {
        authState.setValue(AuthState.loading("Finalizing setup..."));
        repository.saveUserData(user, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                currentUser.setValue(user);
                authState.setValue(AuthState.authenticated(user));
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
    }

    public void logout() {
        repository.logout();
        currentUser.setValue(null);
        authState.setValue(AuthState.idle());
    }

    // Helper class for Auth state
    public static class AuthState {
        public enum Status { IDLE, LOADING, SUCCESS, ERROR, AUTHENTICATED, UNREGISTERED, MFA_REQUIRED, MFA_SETUP_REQUIRED, MFA_ERROR }
        
        public final Status status;
        public final String message;
        public final Object data;
        public final boolean credentialFailure;

        private AuthState(Status status, String message, Object data, boolean credentialFailure) {
            this.status = status;
            this.message = message;
            this.data = data;
            this.credentialFailure = credentialFailure;
        }

        public static AuthState idle() { return new AuthState(Status.IDLE, null, null, false); }
        public static AuthState loading(String message) { return new AuthState(Status.LOADING, message, null, false); }
        public static AuthState success(String message) { return new AuthState(Status.SUCCESS, message, null, false); }
        public static AuthState error(String message) { return new AuthState(Status.ERROR, message, null, false); }
        public static AuthState error(String message, boolean credentialFailure) { return new AuthState(Status.ERROR, message, null, credentialFailure); }
        public static AuthState authenticated(UserEntity user) { return new AuthState(Status.AUTHENTICATED, null, user, false); }
        public static AuthState unregistered(String uid) { return new AuthState(Status.UNREGISTERED, null, uid, false); }
        public static AuthState mfaRequired() { return new AuthState(Status.MFA_REQUIRED, "MFA verification required", null, false); }
        public static AuthState mfaSetupRequired() { return new AuthState(Status.MFA_SETUP_REQUIRED, "MFA enrollment required", null, false); }
        public static AuthState mfaError(String message) { return new AuthState(Status.MFA_ERROR, message, null, false); }
    }
}
