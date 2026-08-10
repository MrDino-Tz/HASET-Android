package com.haset.hasetapp.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MobileMfaApiService {
    @GET("mobile/mfa/status") Call<JsonObject> status(@Header("Authorization") String auth);
    @POST("mobile/mfa/setup") Call<JsonObject> setup(@Header("Authorization") String auth);
    @POST("mobile/mfa/confirm") Call<JsonObject> confirm(@Header("Authorization") String auth, @Body JsonObject body);
    @POST("mobile/mfa/verify") Call<JsonObject> verify(@Header("Authorization") String auth, @Body JsonObject body);
    @POST("mobile/mfa/disable") Call<JsonObject> disable(@Header("Authorization") String auth, @Body JsonObject body);
}
