package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.repositories.DoctorHomeRepository;
import com.haset.hasetapp.utils.SingleLiveEvent;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.NotificationBadgeHelper;

import java.util.List;

public class DoctorHomeViewModel extends AndroidViewModel {
    private final DoctorHomeRepository repository;
    private NotificationBadgeHelper badgeHelper;
    private LiveData<List<Appointment>> appointments;
    private final MutableLiveData<DoctorWalletEntity> wallet = new MutableLiveData<>();
    private final MutableLiveData<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> withdrawals = new MutableLiveData<>();
    private boolean walletLoaded;
    private boolean withdrawalsLoaded;
    private LiveData<Integer> ratingCount;
    private MutableLiveData<Integer> notificationCount;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> error = new SingleLiveEvent<>();
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
        if (!walletLoaded) refreshWalletBalance(doctorId);
        return wallet;
    }

    public void refreshWalletBalance(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        error.setValue(null);
        repository.fetchWalletBalance(doctorId, new FirebaseHelper.OnCompleteListener<DoctorWalletEntity>() {
            @Override public void onSuccess(DoctorWalletEntity result) {
                walletLoaded = true;
                wallet.postValue(result);
            }
            @Override public void onError(String message) {
                // Passive load error: UI falls back gracefully without intrusive Toast popups
            }
        });
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

    public void requestWithdrawalSecure(double amount, String reason, String payoutMethod, String mfaCode) {
        requestWithdrawalSecure(amount, 0, reason, payoutMethod, mfaCode);
    }

    public void requestWithdrawalSecure(double amount, double feeAmount, String reason, String payoutMethod, String mfaCode) {
        loading.setValue(true);
        error.setValue(null);
        repository.requestWithdrawalSecure(amount, feeAmount, reason, payoutMethod, mfaCode, new FirebaseHelper.OnCompleteListener<Boolean>() {
            public void onSuccess(Boolean result) {
                loading.postValue(false);
                withdrawSuccess.postValue(Boolean.TRUE.equals(result));
            }
            public void onError(String message) { loading.postValue(false); error.postValue(message); }
        });
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public void clearError() { error.setValue(null); }
    public LiveData<Boolean> getWithdrawSuccess() { return withdrawSuccess; }

    public LiveData<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> getWithdrawalRequests(String doctorId) {
        if (!withdrawalsLoaded) refreshWithdrawalRequests(doctorId);
        return withdrawals;
    }

    public void refreshWithdrawalRequests(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        error.setValue(null);
        repository.fetchWithdrawalRequests(doctorId,
                new FirebaseHelper.OnCompleteListener<List<com.haset.hasetapp.database.entities.WithdrawalRequest>>() {
            @Override public void onSuccess(List<com.haset.hasetapp.database.entities.WithdrawalRequest> result) {
                withdrawalsLoaded = true;
                withdrawals.postValue(result);
            }
            @Override public void onError(String message) {
                // Passive load error: UI falls back gracefully without intrusive Toast popups
            }
        });
    }
}
