package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.repositories.PaymentRepository;
import com.haset.hasetapp.utils.FirebaseHelper;

public class PaymentViewModel extends AndroidViewModel {
    private final PaymentRepository repository;
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> initiated = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canRetry = new MutableLiveData<>(false);

    public PaymentViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PaymentRepository();
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String provider, String paymentAccount) {
        processPayment(userId, doctorId, consultationId, amount, provider, paymentAccount, null, null, null);
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String provider, String paymentAccount,
                               String buyerEmail, String buyerName, String buyerPhone) {
        if (Boolean.TRUE.equals(processing.getValue())) {
            return;
        }

        processing.setValue(true);
        initiated.setValue(false);
        success.setValue(false);
        error.setValue(null);
        canRetry.setValue(false);

        repository.processPayment(userId, doctorId, consultationId, amount, provider, paymentAccount,
            buyerEmail, buyerName, buyerPhone,
            new FirebaseHelper.OnCompleteListener<com.haset.hasetapp.models.PaymentResponse>() {
                @Override
                public void onSuccess(com.haset.hasetapp.models.PaymentResponse result) {
                    if (result != null && result.isSuccess()) {
                        initiated.postValue(true);
                    } else {
                        processing.postValue(false);
                        error.postValue("Payment initiation failed");
                    }
                }

                @Override
                public void onError(String err) {
                    processing.postValue(false);
                    error.postValue(err);
                }
            },
            new FirebaseHelper.OnCompleteListener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    processing.postValue(false);
                    canRetry.postValue(false);
                    success.postValue(result);
                }

                @Override
                public void onError(String err) {
                    processing.postValue(false);
                    canRetry.postValue(true);
                    error.postValue(err);
                }
            });
    }

    public void retryCheckStatus() {
        if (Boolean.TRUE.equals(processing.getValue())) return;

        processing.setValue(true);
        error.setValue(null);

        repository.retryCheckStatus(new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                processing.postValue(false);
                canRetry.postValue(false);
                success.postValue(result);
            }

            @Override
            public void onError(String err) {
                processing.postValue(false);
                canRetry.postValue(true);
                error.postValue(err);
            }
        });
    }

    public void confirmManualPayment() {
        if (Boolean.TRUE.equals(processing.getValue())) return;

        processing.setValue(true);
        error.setValue(null);

        com.haset.hasetapp.utils.AuditLogger.getInstance(getApplication())
            .logAppointmentUpdated("", "MANUAL_PAYMENT_CONFIRMATION",
                "User manually confirmed payment (transaction may have expired on ZenoPay)");

        repository.confirmManualPayment(new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                processing.postValue(false);
                canRetry.postValue(false);
                success.postValue(result);
            }

            @Override
            public void onError(String err) {
                processing.postValue(false);
                canRetry.postValue(false);
                error.postValue(err);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }

    public void cancelPayment() {
        processing.postValue(false);
        repository.cleanup();
    }

    public LiveData<Boolean> getProcessing() { return processing; }
    public LiveData<Boolean> getInitiated() { return initiated; }
    public LiveData<Boolean> getSuccess() { return success; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getCanRetry() { return canRetry; }
}
