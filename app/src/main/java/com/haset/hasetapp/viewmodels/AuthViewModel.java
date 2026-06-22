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

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();

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
                fetchUserData(result.getUid());
            }

            @Override
            public void onError(String error) {
                authState.setValue(AuthState.error(error));
            }
        });
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
        public enum Status { IDLE, LOADING, SUCCESS, ERROR, AUTHENTICATED, UNREGISTERED }
        
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
    }
}
