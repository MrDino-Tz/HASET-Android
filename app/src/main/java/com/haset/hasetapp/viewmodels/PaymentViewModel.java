package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.repositories.PaymentRepository;
import com.haset.hasetapp.models.PaymentStatusResponse;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.SingleLiveEvent;

public class PaymentViewModel extends AndroidViewModel {
    private final PaymentRepository repository;
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> initiated = new MutableLiveData<>(false);
    private final SingleLiveEvent<Boolean> success = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> error = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> canRetry = new MutableLiveData<>(false);
    private final MutableLiveData<String> paymentUrl = new MutableLiveData<>();

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
        processPayment(userId, doctorId, consultationId, amount, "mobile_money", provider, paymentAccount,
                buyerEmail, buyerName, buyerPhone);
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String paymentMethod, String provider, String paymentAccount,
                               String buyerEmail, String buyerName, String buyerPhone) {
        if (Boolean.TRUE.equals(processing.getValue())) {
            return;
        }

        processing.setValue(true);
        initiated.setValue(false);
        success.setValue(false);
        error.setValue(null);
        canRetry.setValue(false);

        repository.processPayment(userId, doctorId, consultationId, amount, paymentMethod, provider, paymentAccount,
            buyerEmail, buyerName, buyerPhone,
            new FirebaseHelper.OnCompleteListener<com.haset.hasetapp.models.PaymentResponse>() {
                @Override
                public void onSuccess(com.haset.hasetapp.models.PaymentResponse result) {
                    if (result != null && result.isSuccess()) {
                        paymentUrl.postValue(result.getPaymentUrl());
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

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }

    public void cancelPayment() {
        processing.postValue(false);
        repository.cleanup();
    }

    public int getCurrentTransactionId() {
        return repository.getCurrentTransactionId();
    }

    public void requestCancelPayment(FirebaseHelper.OnCompleteListener<PaymentStatusResponse> callback) {
        int transactionId = repository.getCurrentTransactionId();
        if (transactionId <= 0) {
            cancelPayment();
            if (callback != null) callback.onSuccess(null);
            return;
        }
        repository.cancelPayment(transactionId, new FirebaseHelper.OnCompleteListener<PaymentStatusResponse>() {
            @Override public void onSuccess(PaymentStatusResponse result) {
                cancelPayment();
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public LiveData<Boolean> getProcessing() { return processing; }
    public LiveData<Boolean> getInitiated() { return initiated; }
    public LiveData<Boolean> getSuccess() { return success; }
    public LiveData<String> getError() { return error; }
    public void clearError() { error.setValue(null); }
    public LiveData<String> getPaymentUrl() { return paymentUrl; }
    public LiveData<Boolean> getCanRetry() { return canRetry; }
}
