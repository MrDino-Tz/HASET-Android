package com.haset.hasetapp.api;

import com.haset.hasetapp.utils.Constants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance;
    private Retrofit retrofit;
    private Retrofit paymentRetrofit;
    
    private RetrofitClient() {
        OkHttpClient client = buildSecureClient();
        
        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        paymentRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.PAYMENT_API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    
    private OkHttpClient buildSecureClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        return builder.build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }
    
    public PaymentApiService getPaymentApiService() {
        return paymentRetrofit.create(PaymentApiService.class);
    }
}
