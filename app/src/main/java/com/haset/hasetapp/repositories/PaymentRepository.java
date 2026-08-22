package com.haset.hasetapp.repositories;

import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haset.hasetapp.api.PaymentApiService;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.models.PaymentRequest;
import com.haset.hasetapp.models.PaymentResponse;
import com.haset.hasetapp.models.PaymentStatusResponse;
import com.haset.hasetapp.models.CancelPaymentRequest;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentRepository {
    private static final int STATUS_CHECK_INTERVAL = 6000;
    private static final int MAX_STATUS_CHECKS = 60;

    private PaymentApiService apiService;
    private Handler statusCheckHandler;
    private java.util.List<Call<?>> activeCalls = new java.util.ArrayList<>();
    private static boolean isProcessingPayment = false;

    private int currentTransactionId = -1;
    private String currentDoctorId;
    private double currentAmount;
    private FirebaseHelper.OnCompleteListener<Boolean> pendingFinalCallback;

    private interface AuthHeaderCallback {
        void onSuccess(String authHeader);
        void onError(String error);
    }

    public PaymentRepository() {
        apiService = RetrofitClient.getInstance().getPaymentApiService();
        statusCheckHandler = new Handler(Looper.getMainLooper());
    }

    private void trackCall(Call<?> call) {
        synchronized (activeCalls) {
            activeCalls.add(call);
        }
    }

    private void untrackCall(Call<?> call) {
        synchronized (activeCalls) {
            activeCalls.remove(call);
        }
    }

    public void retryCheckStatus(FirebaseHelper.OnCompleteListener<Boolean> callback) {
        if (currentTransactionId <= 0) {
            if (callback != null) callback.onError("No previous transaction to check");
            return;
        }
        pendingFinalCallback = callback;
        isProcessingPayment = true;
        startStatusPolling(currentTransactionId, currentDoctorId, currentAmount, callback);
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String provider, String paymentAccount,
                               FirebaseHelper.OnCompleteListener<PaymentResponse> initiationCallback,
                               FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {
        processPayment(userId, doctorId, consultationId, amount, "mobile_money", provider, paymentAccount,
                null, null, null, initiationCallback, finalCallback);
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String provider, String paymentAccount,
                               String buyerEmail, String buyerName, String buyerPhone,
                               FirebaseHelper.OnCompleteListener<PaymentResponse> initiationCallback,
                               FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {
        processPayment(userId, doctorId, consultationId, amount, "mobile_money", provider, paymentAccount,
                buyerEmail, buyerName, buyerPhone, initiationCallback, finalCallback);
    }

    public void processPayment(String userId, String doctorId, String consultationId, double amount,
                               String paymentMethod, String provider, String paymentAccount,
                               String buyerEmail, String buyerName, String buyerPhone,
                               FirebaseHelper.OnCompleteListener<PaymentResponse> initiationCallback,
                               FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {

        if (isProcessingPayment) {
            if (initiationCallback != null) {
                initiationCallback.onError("Payment already in progress");
            }
            return;
        }

        isProcessingPayment = true;
        currentTransactionId = -1;
        currentDoctorId = doctorId;
        currentAmount = amount;
        pendingFinalCallback = finalCallback;

        PaymentRequest request = new PaymentRequest(userId, doctorId, consultationId, amount, paymentMethod,
                provider, paymentAccount, buyerEmail, buyerName, buyerPhone,
                "https://hasethospital.or.tz/payment");
        // Stay comfortably below Snippe's 30-character gateway limit. Some downstream
        // card processors reject boundary-length keys with PAY_001.
        String idempotencyKey = UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        withFirebaseAuthHeader(new AuthHeaderCallback() {
            @Override
            public void onSuccess(String authHeader) {
                Call<PaymentResponse> call = apiService.initiatePayment(authHeader, idempotencyKey, request);
                trackCall(call);

                call.enqueue(new Callback<PaymentResponse>() {
                    @Override
                    public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                        untrackCall(call);
                        if (response.isSuccessful() && response.body() != null) {
                            PaymentResponse paymentResponse = response.body();
                            if (paymentResponse.isSuccess() && paymentResponse.getTransactionId() <= 0) {
                                isProcessingPayment = false;
                                String message = paymentResponse.getMessage() != null
                                        ? paymentResponse.getMessage()
                                        : "Payment response did not include a transaction ID. Please retry.";
                                if (initiationCallback != null) {
                                    initiationCallback.onError(message);
                                }
                                if (finalCallback != null) {
                                    finalCallback.onError(message);
                                }
                                return;
                            }
                            currentTransactionId = paymentResponse.getTransactionId();

                            if (initiationCallback != null) {
                                initiationCallback.onSuccess(paymentResponse);
                            }

                            if (paymentResponse.isSuccess()) {
                                startStatusPolling(paymentResponse.getTransactionId(), doctorId, amount, finalCallback);
                            } else {
                                isProcessingPayment = false;
                                if (finalCallback != null) {
                                    finalCallback.onError("Payment initiation failed: " + paymentResponse.getMessage());
                                }
                            }
                        } else {
                            isProcessingPayment = false;
                            String errorMsg = "Payment initiation failed";
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = paymentErrorMessage(
                                            response.errorBody().string(), paymentMethod
                                    );
                                }
                            } catch (Exception ignored) {
                            }
                            if (initiationCallback != null) {
                                initiationCallback.onError(errorMsg);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PaymentResponse> call, Throwable t) {
                        untrackCall(call);
                        isProcessingPayment = false;
                        if (call.isCanceled()) return;
                        if (initiationCallback != null) {
                            initiationCallback.onError("Network error: " + t.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                isProcessingPayment = false;
                if (initiationCallback != null) {
                    initiationCallback.onError(error);
                }
            }
        });
    }

    private static String paymentErrorMessage(String rawBody, String paymentMethod) {
        Integer transactionId = null;
        try {
            JsonObject error = JsonParser.parseString(rawBody).getAsJsonObject();
            if (error.has("transaction_id") && !error.get("transaction_id").isJsonNull()) {
                transactionId = error.get("transaction_id").getAsInt();
            }
        } catch (Exception ignored) {
        }

        String reference = transactionId == null ? "" : " Reference: " + transactionId + ".";
        if ("card".equals(paymentMethod)) {
            return "Card payment is temporarily unavailable. Please use mobile money or try again later."
                    + reference;
        }
        return "Payment could not be started. Please try again later." + reference;
    }

    private void startStatusPolling(int transactionId, String doctorId, double amount,
                                    FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {
        currentTransactionId = transactionId;
        currentDoctorId = doctorId;
        currentAmount = amount;
        pollPaymentStatus(transactionId, doctorId, amount, 0, finalCallback);
    }

    private void pollPaymentStatus(int transactionId, String doctorId, double amount,
                                   int attemptCount, FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {
        if (attemptCount >= MAX_STATUS_CHECKS) {
            isProcessingPayment = false;
            if (finalCallback != null) {
                finalCallback.onError("Payment is taking longer than expected. Transaction #" +
                    transactionId + " is still being processed. Tap 'Check Status' to verify.");
            }
            return;
        }

        statusCheckHandler.postDelayed(() -> {
            checkPaymentStatus(transactionId, new FirebaseHelper.OnCompleteListener<PaymentStatusResponse>() {
                @Override
                public void onSuccess(PaymentStatusResponse result) {
                    if (result != null && result.getTransaction() != null) {
                        PaymentStatusResponse.Transaction transaction = result.getTransaction();
                        if (transaction.isSuccess()) {
                            handlePaymentSuccess(transactionId, doctorId, amount, finalCallback);
                        } else if (transaction.isFailed()) {
                            isProcessingPayment = false;
                            if (finalCallback != null) {
                                finalCallback.onError("Payment was unsuccessful or cancelled.");
                            }
                        } else if (transaction.isProcessing()) {
                            pollPaymentStatus(transactionId, doctorId, amount, attemptCount + 1, finalCallback);
                        } else {
                            pollPaymentStatus(transactionId, doctorId, amount, attemptCount + 1, finalCallback);
                        }
                    } else {
                        pollPaymentStatus(transactionId, doctorId, amount, attemptCount + 1, finalCallback);
                    }
                }

                @Override
                public void onError(String error) {
                    if (attemptCount < 5) {
                        pollPaymentStatus(transactionId, doctorId, amount, attemptCount + 1, finalCallback);
                    } else {
                        isProcessingPayment = false;
                        if (finalCallback != null) {
                            finalCallback.onError("Network error: " + error);
                        }
                    }
                }
            });
        }, STATUS_CHECK_INTERVAL);
    }

    private void handlePaymentSuccess(int transactionId, String doctorId, double amount,
                                      FirebaseHelper.OnCompleteListener<Boolean> finalCallback) {
        // Settlement and doctor wallet credit are owned by the payment backend.
        currentTransactionId = -1;
        isProcessingPayment = false;
        if (finalCallback != null) finalCallback.onSuccess(true);
    }

    public void checkPaymentStatus(int transactionId,
                                   FirebaseHelper.OnCompleteListener<PaymentStatusResponse> callback) {
        withFirebaseAuthHeader(new AuthHeaderCallback() {
            @Override
            public void onSuccess(String authHeader) {
                Call<PaymentStatusResponse> call = apiService.checkPaymentStatus(authHeader, transactionId);
                trackCall(call);

                call.enqueue(new Callback<PaymentStatusResponse>() {
                    @Override
                    public void onResponse(Call<PaymentStatusResponse> call, Response<PaymentStatusResponse> response) {
                        untrackCall(call);
                        if (response.isSuccessful() && response.body() != null) {
                            PaymentStatusResponse body = response.body();
                            if (callback != null) callback.onSuccess(body);
                        } else {
                            String error = "Status check failed: " + response.code();
                            if (callback != null) callback.onError(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<PaymentStatusResponse> call, Throwable t) {
                        untrackCall(call);
                        if (call.isCanceled()) return;
                        if (callback != null) callback.onError(t.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void cancelPayment(int transactionId,
                              FirebaseHelper.OnCompleteListener<PaymentStatusResponse> callback) {
        CancelPaymentRequest request = new CancelPaymentRequest(transactionId);
        withFirebaseAuthHeader(new AuthHeaderCallback() {
            @Override
            public void onSuccess(String authHeader) {
                Call<Void> call = apiService.cancelPayment(authHeader, request);
                trackCall(call);

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        untrackCall(call);
                        if (callback != null) {
                            if (response.isSuccessful() || response.code() == 409) {
                                // Snippe cannot remotely cancel direct card/mobile-money
                                // requests. A 409 means the local payment screen can still
                                // be abandoned; any late provider result is reconciled by
                                // the signed payment webhook.
                                callback.onSuccess(null);
                            } else {
                                callback.onError("Cancel failed: " + response.code());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        untrackCall(call);
                        if (callback != null) callback.onError(t.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public int getCurrentTransactionId() {
        return currentTransactionId;
    }

    public void cleanup() {
        synchronized (activeCalls) {
            for (Call<?> call : activeCalls) {
                if (call != null && !call.isCanceled()) {
                    call.cancel();
                }
            }
            activeCalls.clear();
        }
        if (statusCheckHandler != null) {
            statusCheckHandler.removeCallbacksAndMessages(null);
        }
    }

    private void withFirebaseAuthHeader(AuthHeaderCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onError("No signed-in user found");
            return;
        }

        // Force refresh so an old anonymous session cannot send an expired token
        // to the payment API.
        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result != null ? result.getToken() : null;
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError("Unauthorized: A valid Firebase token is required");
                        return;
                    }
                    callback.onSuccess("Bearer " + token);
                })
                .addOnFailureListener(error ->
                        callback.onError("Unauthorized: A valid Firebase token is required"));
    }
}
