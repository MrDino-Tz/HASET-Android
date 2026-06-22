package com.haset.hasetapp.api;

import com.haset.hasetapp.utils.Constants;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.Collections;

public class RetrofitClient {
    private static RetrofitClient instance;
    private Retrofit retrofit;
    
    private RetrofitClient() {
        OkHttpClient client = buildSecureClient();
        
        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    
    private OkHttpClient buildSecureClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        // In debug/development mode, allow all hostnames including ngrok
                        if (Constants.IS_DEBUG_MODE) {
                            return true;
                        }
                        // In production, verify against allowed domains
                        return hostname.equals("api.hasetapp.com") || 
                               hostname.equals("firebasestorage.googleapis.com") ||
                               hostname.equals("payments.hasethospital.or.tz");
                    }
                });
        
        // Add interceptor to bypass ngrok browser warning
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder()
                        .addHeader("ngrok-skip-browser-warning", "any")
                        .build();
                return chain.proceed(request);
            }
        });

        // Add logging interceptor only in debug mode
        if (Constants.IS_DEBUG_MODE) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        // Force HTTP/1.1 to avoid "Required SETTINGS preface not received" errors with ngrok
        builder.protocols(Collections.singletonList(Protocol.HTTP_1_1));

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
        return retrofit.create(PaymentApiService.class);
    }
}
