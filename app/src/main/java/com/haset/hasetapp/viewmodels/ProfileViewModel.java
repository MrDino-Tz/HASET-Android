package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.repositories.ProfileRepository;

public class ProfileViewModel extends AndroidViewModel {
    private final ProfileRepository repository;
    private LiveData<UserEntity> userLiveData;
    private LiveData<Doctor> doctorLiveData;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> passwordChangeSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteAccountSuccess = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ProfileRepository();
    }

    public LiveData<UserEntity> getUserInfo(String userId) {
        if (userLiveData == null) {
            userLiveData = repository.getUserInfo(userId);
        }
        return userLiveData;
    }

    public LiveData<Doctor> getDoctorInfo(String doctorId) {
        if (doctorLiveData == null) {
            doctorLiveData = repository.getDoctorProfessionalInfo(doctorId);
        }
        return doctorLiveData;
    }

    public void deleteAccount(String userId) {
        loading.setValue(true);
        repository.deleteAccount(userId, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                loading.postValue(false);
                deleteAccountSuccess.postValue(true);
            }

            @Override
            public void onError(String err) {
                loading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void changePassword(String oldPassword, String newPassword) {
        loading.setValue(true);
        FirebaseHelper.reauthenticateUser(oldPassword, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                FirebaseHelper.updatePassword(newPassword, new FirebaseHelper.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loading.postValue(false);
                        passwordChangeSuccess.postValue(true);
                    }

                    @Override
                    public void onError(String err) {
                        loading.postValue(false);
                        error.postValue(err);
                    }
                });
            }

            @Override
            public void onError(String err) {
                loading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void updateUserInfo(UserEntity user) {
        loading.setValue(true);
        repository.updateUserInfo(user, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                loading.postValue(false);
                updateSuccess.postValue(true);
            }

            @Override
            public void onError(String err) {
                loading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccess; }
    public LiveData<Boolean> getPasswordChangeSuccess() { return passwordChangeSuccess; }
    public LiveData<Boolean> getDeleteAccountSuccess() { return deleteAccountSuccess; }
}
