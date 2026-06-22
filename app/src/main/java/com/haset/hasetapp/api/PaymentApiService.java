package com.haset.hasetapp.api;

import com.haset.hasetapp.models.CancelPaymentRequest;
import com.haset.hasetapp.models.PaymentRequest;
import com.haset.hasetapp.models.PaymentResponse;
import com.haset.hasetapp.models.PaymentStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PaymentApiService {
    
    @POST("payment/initiate")
    Call<PaymentResponse> initiatePayment(@Body PaymentRequest request);
    
    @GET("payment/status")
    Call<PaymentStatusResponse> checkPaymentStatus(@Query("transaction_id") int transactionId);
    
    @POST("payment/cancel")
    Call<Void> cancelPayment(@Body CancelPaymentRequest request);
    
    @POST("payment/payout")
    Call<com.haset.hasetapp.models.PaymentResponse> disburseFunds(@Body com.haset.hasetapp.models.PayoutRequest request);

    @GET("payment/balance")
    Call<com.haset.hasetapp.models.PaymentResponse> getGatewayBalance();
}
