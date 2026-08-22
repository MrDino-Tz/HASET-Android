package com.haset.hasetapp.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AdminPayoutApiService {
    @Headers("Accept: application/json")
    @GET("admin/payout-destinations")
    Call<JsonObject> listPayoutDestinations(
            @Header("Authorization") String authorization,
            @Query("status") String status
    );

    @Headers("Accept: application/json")
    @PUT("admin/wallets/{doctor_id}/destination")
    Call<JsonObject> setVerifiedPayoutDestination(
            @Header("Authorization") String authorization,
            @Path("doctor_id") String doctorId,
            @Body JsonObject body
    );

    @Headers("Accept: application/json")
    @POST("admin/wallets/{doctor_id}/destination/{change}/approve")
    Call<JsonObject> approvePayoutDestination(
            @Header("Authorization") String authorization,
            @Path("doctor_id") String doctorId,
            @Path("change") String changePublicId,
            @Body JsonObject body
    );

    @Headers("Accept: application/json")
    @POST("admin/wallets/{doctor_id}/destination/{change}/reject")
    Call<JsonObject> rejectPayoutDestination(
            @Header("Authorization") String authorization,
            @Path("doctor_id") String doctorId,
            @Path("change") String changePublicId,
            @Body JsonObject body
    );
}
