package com.haset.hasetapp.api;

import com.haset.hasetapp.models.CancelPaymentRequest;
import com.haset.hasetapp.models.PaymentRequest;
import com.haset.hasetapp.models.PaymentResponse;
import com.haset.hasetapp.models.PaymentStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PaymentApiService {
    
    @Headers("Accept: application/json")
    @POST("mobile/payment/initiate")
    Call<PaymentResponse> initiatePayment(
            @Header("Authorization") String authorization,
            @Header("Idempotency-Key") String idempotencyKey,
            @Body PaymentRequest request
    );
    
    @Headers("Accept: application/json")
    @GET("mobile/payment/status")
    Call<PaymentStatusResponse> checkPaymentStatus(
            @Header("Authorization") String authorization,
            @Query("transaction_id") int transactionId
    );
    
    @Headers("Accept: application/json")
    @POST("mobile/payment/cancel")
    Call<Void> cancelPayment(
            @Header("Authorization") String authorization,
            @Body CancelPaymentRequest request
    );
    
}
