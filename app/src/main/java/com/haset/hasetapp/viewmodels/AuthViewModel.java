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
import com.haset.hasetapp.api.MobileMfaApiService;
import com.haset.hasetapp.api.RetrofitClient;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();
    private FirebaseUser pendingFirebaseUser;

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
                checkMfaThenFetch(result);
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
    }

    private void checkMfaThenFetch(FirebaseUser user) {
        user.getIdToken(true).addOnSuccessListener(token -> {
            MobileMfaApiService api = RetrofitClient.getInstance().getMobileMfaApiService();
            api.status("Bearer " + token.getToken()).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null) { authState.postValue(AuthState.error("Unable to verify MFA status.")); return; }
                    boolean enabled = response.body().has("two_factor_enabled") && response.body().get("two_factor_enabled").getAsBoolean();
                    if (enabled) authState.postValue(AuthState.mfaRequired()); else fetchUserData(user.getUid());
                }
                public void onFailure(Call<JsonObject> call, Throwable t) { authState.postValue(AuthState.error("MFA service unavailable.")); }
            });
        }).addOnFailureListener(e -> authState.postValue(AuthState.error("Unable to refresh authentication.")));
    }

    public void verifyMfa(String code) {
        if (pendingFirebaseUser == null) { authState.setValue(AuthState.error("Login session expired.")); return; }
        authState.setValue(AuthState.loading("Verifying code..."));
        pendingFirebaseUser.getIdToken(true).addOnSuccessListener(token -> {
            JsonObject body = new JsonObject(); body.addProperty("code", code);
            RetrofitClient.getInstance().getMobileMfaApiService().verify("Bearer " + token.getToken(), body).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) { if (response.isSuccessful()) fetchUserData(pendingFirebaseUser.getUid()); else authState.postValue(AuthState.mfaError("Invalid or expired MFA code.")); }
                public void onFailure(Call<JsonObject> call, Throwable t) { authState.postValue(AuthState.mfaError("MFA verification failed. Try again.")); }
            });
        }).addOnFailureListener(e -> authState.setValue(AuthState.mfaError("Login session expired.")));
    }

    public void resumeAfterMfaSetup() {
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) checkMfaThenFetch(user); else authState.setValue(AuthState.error("Authentication expired."));
    }

    public void register(String email, String password, UserEntity userData) {
        authState.setValue(AuthState.loading("Creating account..."));
        repository.registerWithEmail(email, password, userData, new FirebaseHelper.OnCompleteListener<FirebaseUser>() {
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
        authState.setValue(AuthState.loading("Sending reset email..."));
        repository.sendPasswordResetEmail(email, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                authState.setValue(AuthState.success("Reset email sent. Check your inbox."));
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
    }

    /**
     * In-app password reset: the emailed link opens this app (deep link)
     * with the oobCode instead of a web page.
     */
    public void resetPassword(String email, com.google.firebase.auth.ActionCodeSettings settings) {
        authState.setValue(AuthState.loading("Sending reset email..."));
        repository.sendPasswordResetEmail(email, settings, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                authState.setValue(AuthState.success("Reset code sent. Check your inbox."));
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
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

        private AuthState(Status status, String message, Object data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static AuthState idle() { return new AuthState(Status.IDLE, null, null); }
        public static AuthState loading(String message) { return new AuthState(Status.LOADING, message, null); }
        public static AuthState success(String message) { return new AuthState(Status.SUCCESS, message, null); }
        public static AuthState error(String message) { return new AuthState(Status.ERROR, message, null); }
        public static AuthState authenticated(UserEntity user) { return new AuthState(Status.AUTHENTICATED, null, user); }
        public static AuthState unregistered(String uid) { return new AuthState(Status.UNREGISTERED, null, uid); }
        public static AuthState mfaRequired() { return new AuthState(Status.MFA_REQUIRED, "MFA verification required", null); }
        public static AuthState mfaSetupRequired() { return new AuthState(Status.MFA_SETUP_REQUIRED, "MFA enrollment required", null); }
        public static AuthState mfaError(String message) { return new AuthState(Status.MFA_ERROR, message, null); }
    }
}
