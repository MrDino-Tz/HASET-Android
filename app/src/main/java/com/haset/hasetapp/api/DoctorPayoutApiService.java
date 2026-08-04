package com.haset.hasetapp.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface DoctorPayoutApiService {
    @GET("mobile/doctor/withdrawals")
    Call<JsonObject> listWithdrawals(@Header("Authorization") String authorization);
    @POST("mobile/doctor/withdrawals")
    Call<JsonObject> requestWithdrawal(
            @Header("Authorization") String authorization,
            @Header("X-MFA-Action-Token") String mfaActionToken,
            @Body JsonObject body
    );
}
