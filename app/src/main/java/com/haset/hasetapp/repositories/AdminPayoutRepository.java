package com.haset.hasetapp.repositories;

import com.google.gson.JsonObject;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.models.ApiError;
import com.haset.hasetapp.utils.ErrorParser;
import com.haset.hasetapp.utils.FirebaseHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPayoutRepository {
    public void listPayoutDestinations(String adminToken, String status,
            FirebaseHelper.OnCompleteListener<JsonObject> callback) {
        RetrofitClient.getInstance().getAdminPayoutApiService()
                .listPayoutDestinations(bearer(adminToken), status)
                .enqueue(jsonCallback(callback, "Unable to load payout destinations."));
    }

    public void setVerifiedPayoutDestination(String adminToken, String doctorId,
            String phoneNumber, String provider, String twoFactorCode,
            FirebaseHelper.OnCompleteListener<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("phone_number", phoneNumber);
        body.addProperty("provider", provider);
        body.addProperty("two_factor_code", twoFactorCode);
        RetrofitClient.getInstance().getAdminPayoutApiService()
                .setVerifiedPayoutDestination(bearer(adminToken), doctorId, body)
                .enqueue(jsonCallback(callback, "Unable to set payout destination."));
    }

    public void approvePayoutDestination(String adminToken, String doctorId,
            String changePublicId, String twoFactorCode,
            FirebaseHelper.OnCompleteListener<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("two_factor_code", twoFactorCode);
        RetrofitClient.getInstance().getAdminPayoutApiService()
                .approvePayoutDestination(bearer(adminToken), doctorId, changePublicId, body)
                .enqueue(jsonCallback(callback, "Unable to approve payout destination."));
    }

    public void rejectPayoutDestination(String adminToken, String doctorId,
            String changePublicId, String reason, String twoFactorCode,
            FirebaseHelper.OnCompleteListener<JsonObject> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("reason", reason);
        body.addProperty("two_factor_code", twoFactorCode);
        RetrofitClient.getInstance().getAdminPayoutApiService()
                .rejectPayoutDestination(bearer(adminToken), doctorId, changePublicId, body)
                .enqueue(jsonCallback(callback, "Unable to reject payout destination."));
    }

    private static String bearer(String token) {
        if (token == null) return "";
        String trimmed = token.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, 7) ? trimmed : "Bearer " + trimmed;
    }

    private static Callback<JsonObject> jsonCallback(FirebaseHelper.OnCompleteListener<JsonObject> callback,
            String fallback) {
        return new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                callback.onError(errorMessage(response, fallback));
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable throwable) {
                callback.onError(fallback);
            }
        };
    }

    private static String errorMessage(Response<?> response, String fallback) {
        ApiError error = ErrorParser.fromResponse(response);
        String message = error.getMessage();
        return (message != null && !message.isEmpty()) ? message : fallback;
    }
}
