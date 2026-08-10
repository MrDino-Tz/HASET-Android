package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.repositories.DoctorHomeRepository;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.NotificationBadgeHelper;

import java.util.List;

public class DoctorHomeViewModel extends AndroidViewModel {
    private final DoctorHomeRepository repository;
    private NotificationBadgeHelper badgeHelper;
    private LiveData<List<Appointment>> appointments;
    private LiveData<DoctorWalletEntity> wallet;
    private LiveData<Integer> ratingCount;
    private MutableLiveData<Integer> notificationCount;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> withdrawSuccess = new MutableLiveData<>();

    public DoctorHomeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DoctorHomeRepository();
        this.badgeHelper = new NotificationBadgeHelper(application);
        this.notificationCount = new MutableLiveData<>(0);
        updateNotificationCount();
    }

    public LiveData<List<Appointment>> getAppointments(String doctorId) {
        if (appointments == null) {
            appointments = repository.getAppointments(doctorId);
        }
        return appointments;
    }

    public LiveData<DoctorWalletEntity> getWalletBalance(String doctorId) {
        if (wallet == null) {
            wallet = repository.getWalletBalance(doctorId);
        }
        return wallet;
    }

    public void updateAppointmentStatus(Appointment appointment, String status, FirebaseHelper.OnCompleteListener<Void> callback) {
        repository.updateAppointmentStatus(appointment, status, callback);
    }

    public LiveData<Integer> getNotificationCount(String userId, String role) {
        return notificationCount;
    }
    
    public void updateNotificationCount() {
        if (badgeHelper != null) {
            int newCount = badgeHelper.getNewNotificationsSinceLastOpen();
            notificationCount.postValue(newCount);
        }
    }
    
    public void incrementNotificationCount() {
        if (badgeHelper != null) {
            badgeHelper.incrementGeneralNotifications();
            badgeHelper.incrementNewNotifications();
            int newCount = badgeHelper.getNewNotificationsSinceLastOpen();
            notificationCount.postValue(newCount);
        }
    }
    
    public void clearNotificationCount() {
        if (badgeHelper != null) {
            badgeHelper.markGeneralNotificationsAsRead();
            notificationCount.postValue(0);
        }
    }

    public LiveData<Integer> getRatingCount(String doctorId) {
        if (ratingCount == null) {
            ratingCount = repository.getRatingCount(getApplication(), doctorId);
        }
        return ratingCount;
    }

    public void requestWithdrawalSecure(double amount, String reason, String mfaCode) {
        loading.setValue(true);
        error.setValue(null);
        repository.requestWithdrawalSecure(amount, reason, mfaCode, new FirebaseHelper.OnCompleteListener<Boolean>() {
            public void onSuccess(Boolean result) { loading.postValue(false); withdrawSuccess.postValue(Boolean.TRUE.equals(result)); }
            public void onError(String message) { loading.postValue(false); error.postValue(message); }
        });
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getWithdrawSuccess() { return withdrawSuccess; }

    public LiveData<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> getWithdrawalRequests(String doctorId) {
        return repository.getWithdrawalRequests(doctorId);
    }
}
