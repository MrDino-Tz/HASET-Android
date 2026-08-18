package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.database.entities.WithdrawalRequest;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.FirebaseHelper;

import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.api.DoctorPayoutApiService;
import com.haset.hasetapp.api.MobileMfaApiService;
import com.haset.hasetapp.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DoctorHomeRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<List<Appointment>> getAppointments(String doctorId) {
        MutableLiveData<List<Appointment>> appointmentsLiveData = new MutableLiveData<>();

        // Single indexed query over /appointments (rules allow doctorId queries).
        // Avoids the N+1 per-appointment reads of the old map-based path.
        FirebaseHelper.getAppointmentsRef().orderByChild("doctorId").equalTo(doctorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Appointment> appointments = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Appointment appointment = snapshot.getValue(Appointment.class);
                            if (appointment != null) {
                                appointments.add(appointment);
                            }
                        }
                        sortAndPost(appointments, appointmentsLiveData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Fallback for rule versions that deny the query: use the
                        // doctor_appointments map plus per-appointment reads.
                        loadAppointmentsFromMap(doctorId, appointmentsLiveData);
                    }
                });

        return appointmentsLiveData;
    }

    private void loadAppointmentsFromMap(String doctorId, MutableLiveData<List<Appointment>> appointmentsLiveData) {
        firebaseHelper.getDoctorAppointmentsRef(doctorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    appointmentsLiveData.postValue(new ArrayList<>());
                    return;
                }

                List<Appointment> appointments = new ArrayList<>();
                int totalChildren = (int) dataSnapshot.getChildrenCount();
                final int[] processed = {0};

                for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                    String appointmentId = idSnapshot.getKey();
                    if (appointmentId != null) {
                        FirebaseHelper.getAppointmentsRef().child(appointmentId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Appointment appointment = snapshot.getValue(Appointment.class);
                                if (appointment != null) {
                                    appointments.add(appointment);
                                }
                                processed[0]++;
                                if (processed[0] == totalChildren) {
                                    sortAndPost(appointments, appointmentsLiveData);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                processed[0]++;
                                if (processed[0] == totalChildren) {
                                    sortAndPost(appointments, appointmentsLiveData);
                                }
                            }
                        });
                    } else {
                        processed[0]++;
                        if (processed[0] == totalChildren) {
                            sortAndPost(appointments, appointmentsLiveData);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sortAndPost(List<Appointment> appointments, MutableLiveData<List<Appointment>> liveData) {
        Collections.sort(appointments, (a1, a2) -> {
            if (a1.getDate() == null && a2.getDate() == null) return 0;
            if (a1.getDate() == null) return 1;
            if (a2.getDate() == null) return -1;
            return a2.getDate().compareTo(a1.getDate());
        });
        liveData.postValue(appointments);
    }

    public void fetchWalletBalance(String doctorId, FirebaseHelper.OnCompleteListener<DoctorWalletEntity> callback) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) {
            callback.onError("Authentication expired. Please sign in again.");
            return;
        }
        user.getIdToken(true).addOnSuccessListener(token ->
            RetrofitClient.getInstance().getDoctorPayoutApiService()
                .getWallet("Bearer " + token.getToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError(errorMessage(response, "Unable to load the doctor wallet."));
                            return;
                        }
                        DoctorWalletEntity wallet = new DoctorWalletEntity();
                        wallet.setDoctorId(doctorId);
                        wallet.setBalance(0);
                        wallet.setTotalEarnings(0);
                        wallet.setLastUpdated(System.currentTimeMillis());
                        JsonObject envelope = response.body();
                        Log.d("HASET_WALLET", "wallet raw response: " + envelope);
                        JsonObject data = jsonObject(envelope, "data");
                        JsonObject json = firstJsonObject(
                                jsonObject(envelope, "wallet"),
                                jsonObject(data, "wallet"),
                                envelope
                        );
                        if (json != null) {
                            wallet.setDoctorId(jsonString(json, "doctor_id", doctorId));
                            double available = jsonDouble(json, "available_balance");
                            double reserved = jsonDouble(json, "reserved_balance");
                            double paidOut = jsonDouble(json, "paid_out_balance");
                            wallet.setBalance(available);
                            wallet.setTotalEarnings(available + reserved + paidOut);
                            wallet.setLastUpdated(parseIsoTimestamp(jsonString(json, "updated_at", null)));
                        }
                        JsonObject destinations = firstJsonObject(
                                jsonObject(envelope, "payout_destinations"),
                                jsonObject(envelope, "payoutDestinations"),
                                jsonObject(data, "payout_destinations"),
                                jsonObject(data, "payoutDestinations"),
                                jsonObject(json, "payout_destinations"),
                                jsonObject(json, "payoutDestinations"),
                                jsonObject(json, "destinations"),
                                jsonObject(data, "destinations")
                        );
                        if (destinations != null) {
                            JsonObject mobile = firstJsonObject(
                                    jsonObject(destinations, "mobile_money"),
                                    jsonObject(destinations, "mobileMoney"),
                                    jsonObject(destinations, "mobile")
                            );
                            JsonObject bank = jsonObject(destinations, "bank");
                            if (mobile != null) {
                                String status = jsonString(mobile, "status", "");
                                boolean destinationAvailable = jsonBoolean(mobile, "available", isAvailableStatus(status));
                                String provider = jsonString(mobile, "provider", "");
                                String account = firstString(mobile, "masked_account", "maskedAccount", "phone_number_masked", "phoneNumberMasked", "phone_number", "phoneNumber");
                                wallet.setMobileMoneyAvailable(destinationAvailable);
                                wallet.setMobileMoneyPending(!destinationAvailable
                                        && ("pending".equalsIgnoreCase(status) || !provider.isEmpty() || !account.isEmpty()));
                                if (!provider.isEmpty() || !account.isEmpty()) wallet.setMobileMoneyLabel((provider + "  " + account).trim());
                            }
                            if (bank != null) {
                                String status = jsonString(bank, "status", "");
                                boolean destinationAvailable = jsonBoolean(bank, "available", isAvailableStatus(status));
                                String bankCode = firstString(bank, "bank_code", "bankCode");
                                String account = firstString(bank, "masked_account", "maskedAccount", "bank_account_masked", "bankAccountMasked", "bank_account", "bankAccount");
                                wallet.setBankAvailable(destinationAvailable);
                                wallet.setBankPending(!destinationAvailable
                                        && ("pending".equalsIgnoreCase(status) || !bankCode.isEmpty() || !account.isEmpty()));
                                if (!bankCode.isEmpty() || !account.isEmpty()) wallet.setBankLabel((bankCode + "  " + account).trim());
                            }
                        }
                        com.google.gson.JsonArray destinationArray = firstJsonArray(
                                jsonArray(envelope, "payout_destinations"),
                                jsonArray(envelope, "payoutDestinations"),
                                jsonArray(data, "payout_destinations"),
                                jsonArray(data, "payoutDestinations"),
                                jsonArray(json, "payout_destinations"),
                                jsonArray(json, "payoutDestinations"),
                                jsonArray(json, "destinations"),
                                jsonArray(data, "destinations")
                        );
                        if (destinationArray != null) {
                            for (com.google.gson.JsonElement element : destinationArray) {
                                if (element.isJsonObject()) {
                                    applyPayoutDestinationObject(wallet, element.getAsJsonObject());
                                }
                            }
                        }
                        JsonObject singularDestination = firstJsonObject(
                                jsonObject(envelope, "payout_destination"),
                                jsonObject(envelope, "payoutDestination"),
                                jsonObject(data, "payout_destination"),
                                jsonObject(data, "payoutDestination"),
                                jsonObject(json, "payout_destination"),
                                jsonObject(json, "payoutDestination")
                        );
                        if (singularDestination != null) {
                            applyPayoutDestinationObject(wallet, singularDestination);
                        }
                        if (json != null) {
                            applyFlatPayoutDestinationFields(wallet, json);
                        }
                        callback.onSuccess(wallet);
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable throwable) {
                        callback.onError("Network error while loading the doctor wallet.");
                    }
                })
        ).addOnFailureListener(error -> callback.onError("Authentication expired. Please sign in again."));
    }

    public void updateAppointmentStatus(Appointment appointment, String status, FirebaseHelper.OnCompleteListener<Void> callback) {
        AppointmentEntity entity = new AppointmentEntity(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getReason(),
                status,
                appointment.getAppointmentType()
        );
        entity.setCreatedAt(appointment.getCreatedAt());
        FirebaseHelper.updateAppointment(entity, callback);
    }

    public LiveData<Integer> getNotificationCount(String userId, String role) {
        MutableLiveData<Integer> countLiveData = new MutableLiveData<>();
        FirebaseHelper.getAppointmentsByUser(userId, role, new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> appointmentEntities) {
                countLiveData.postValue(appointmentEntities.size());
            }

            @Override
            public void onError(String error) {
                countLiveData.postValue(0);
            }
        });
        return countLiveData;
    }

    public LiveData<Integer> getRatingCount(Context context, String doctorId) {
        MutableLiveData<Integer> ratingCountLiveData = new MutableLiveData<>();
        LocalStorageHelper storageHelper = LocalStorageHelper.getInstance(context);
        storageHelper.getRatingCount(doctorId, new LocalStorageHelper.OnCompleteListener<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                ratingCountLiveData.postValue(count != null ? count : 0);
            }

            @Override
            public void onError(String error) {
                ratingCountLiveData.postValue(0);
            }
        });
        return ratingCountLiveData;
    }

    public void requestWithdrawalSecure(double amount, String reason, String payoutMethod, String mfaCode, FirebaseHelper.OnCompleteListener<Boolean> callback) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) { callback.onError("Authentication expired. Please sign in again."); return; }
        user.getIdToken(true).addOnSuccessListener(result -> {
            String bearer = "Bearer " + result.getToken();
            JsonObject codeBody = new JsonObject(); codeBody.addProperty("code", mfaCode);
            RetrofitClient.getInstance().getMobileMfaApiService().verify(bearer, codeBody).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null || !response.body().has("mfa_action_token")) { callback.onError(response.code() == 429 ? "Too many MFA attempts. Please wait and retry." : "Invalid or expired MFA code."); return; }
                    String actionToken = response.body().get("mfa_action_token").getAsString();
                    JsonObject body = new JsonObject(); body.addProperty("request_id", "WR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)); body.addProperty("amount", Math.round(amount)); body.addProperty("reason", reason); body.addProperty("payout_method", payoutMethod);
                    RetrofitClient.getInstance().getDoctorPayoutApiService().requestWithdrawal(bearer, actionToken, body).enqueue(new Callback<JsonObject>() {
                        public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                            if (r.isSuccessful()) callback.onSuccess(true);
                            else callback.onError(r.code() == 429
                                    ? "Too many requests. Please retry later."
                                    : errorMessage(r, "Payout request failed."));
                        }
                        public void onFailure(Call<JsonObject> c, Throwable t) { callback.onError("Network error while submitting payout request."); }
                    });
                }
                public void onFailure(Call<JsonObject> call, Throwable t) { callback.onError("Network error while verifying MFA."); }
            });
        }).addOnFailureListener(e -> callback.onError("Authentication expired. Please sign in again."));
    }

    public void fetchWithdrawalRequests(String doctorId, FirebaseHelper.OnCompleteListener<List<WithdrawalRequest>> callback) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) {
            callback.onError("Authentication expired. Please sign in again.");
            return;
        }
        user.getIdToken(true).addOnSuccessListener(token -> RetrofitClient.getInstance().getDoctorPayoutApiService().listWithdrawals("Bearer " + token.getToken()).enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(errorMessage(response, "Unable to load withdrawal history."));
                    return;
                }
                List<WithdrawalRequest> list = new ArrayList<>();
                JsonObject envelope = response.body();
                JsonObject data = jsonObject(envelope, "data");
                com.google.gson.JsonArray withdrawals = firstJsonArray(
                        jsonArray(envelope, "withdrawals"),
                        jsonArray(data, "withdrawals"),
                        envelope.has("data") && envelope.get("data").isJsonArray() ? envelope.getAsJsonArray("data") : null
                );
                if (withdrawals != null) {
                    for (com.google.gson.JsonElement element : withdrawals) {
                        JsonObject w = element.getAsJsonObject();
                        WithdrawalRequest item = new WithdrawalRequest();
                        item.setRequestId(w.has("request_id") ? w.get("request_id").getAsString() : "");
                        item.setDoctorId(doctorId); item.setAmount(w.has("amount") ? w.get("amount").getAsDouble() : 0);
                        item.setFeeAmount(w.has("fee_amount") && !w.get("fee_amount").isJsonNull() ? w.get("fee_amount").getAsDouble() : 0);
                        item.setStatus(w.has("status") ? w.get("status").getAsString() : "pending");
                        String payoutMethod = jsonString(w, "payout_method", "mobile_money");
                        item.setMethod("bank".equals(payoutMethod) ? WithdrawalRequest.METHOD_BANK : WithdrawalRequest.METHOD_MOBILE_MONEY);
                        boolean bank = "bank".equals(payoutMethod);
                        item.setAccountNumber(bank
                                ? firstString(w, "bank_account_masked", "bank_account", "masked_account", "account_number", "accountNumber")
                                : firstString(w, "phone_number_masked", "phone_number", "masked_account", "account_number", "accountNumber"));
                        item.setBankName(bank ? firstString(w, "bank_code", "bank_name", "bankName") : firstString(w, "provider", "payout_provider", "mobile_money_provider"));
                        item.setRequestedAt(parseIsoTimestamp(jsonString(w, "created_at", null)));
                        item.setRejectionReason(w.has("failure_reason") && !w.get("failure_reason").isJsonNull() ? w.get("failure_reason").getAsString() : null);
                        list.add(item);
                    }
                }
                callback.onSuccess(list);
            }
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error while loading withdrawal history.");
            }
        })).addOnFailureListener(e -> callback.onError("Authentication expired. Please sign in again."));
    }

    private static String errorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                JsonObject error = com.google.gson.JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                if (error.has("message") && !error.get("message").isJsonNull()) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String jsonString(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static String firstString(JsonObject json, String... keys) {
        if (json == null) return "";
        for (String key : keys) {
            String value = jsonString(json, key, "");
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static JsonObject jsonObject(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull() || !json.get(key).isJsonObject()) return null;
        return json.getAsJsonObject(key);
    }

    private static com.google.gson.JsonArray jsonArray(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull() || !json.get(key).isJsonArray()) return null;
        return json.getAsJsonArray(key);
    }

    private static JsonObject firstJsonObject(JsonObject... objects) {
        for (JsonObject object : objects) {
            if (object != null) return object;
        }
        return null;
    }

    private static com.google.gson.JsonArray firstJsonArray(com.google.gson.JsonArray... arrays) {
        for (com.google.gson.JsonArray array : arrays) {
            if (array != null) return array;
        }
        return null;
    }

    private static boolean jsonBoolean(JsonObject json, String key, boolean fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean firstBoolean(JsonObject json, boolean fallback, String... keys) {
        if (json == null) return fallback;
        for (String key : keys) {
            if (json.has(key) && !json.get(key).isJsonNull()) {
                try {
                    return json.get(key).getAsBoolean();
                } catch (Exception ignored) {
                }
            }
        }
        return fallback;
    }

    private static boolean isAvailableStatus(String status) {
        return "approved".equalsIgnoreCase(status)
                || "verified".equalsIgnoreCase(status)
                || "active".equalsIgnoreCase(status)
                || "enabled".equalsIgnoreCase(status);
    }

    private static boolean isPendingDestination(String status, String labelOne, String labelTwo) {
        return "pending".equalsIgnoreCase(status)
                || "requested".equalsIgnoreCase(status)
                || "review".equalsIgnoreCase(status)
                || (!labelOne.isEmpty() || !labelTwo.isEmpty());
    }

    private static void applyPayoutDestinationObject(DoctorWalletEntity wallet, JsonObject destination) {
        if (destination == null) return;
        String type = firstString(destination, "type", "destination_type", "method", "payout_method");
        String status = firstString(destination, "status", "state");
        boolean statusApproved = isAvailableStatus(status);
        if ("bank".equalsIgnoreCase(type)) {
            String bankCode = firstString(destination, "bank_code", "bankCode", "bank_name", "bankName");
            String account = firstString(destination, "masked_account", "maskedAccount", "bank_account_masked", "bankAccountMasked", "bank_account", "bankAccount", "account_number", "accountNumber");
            if (!bankCode.isEmpty() || !account.isEmpty()) {
                boolean available = firstBoolean(destination, statusApproved, "available", "is_approved", "approved", "verified", "is_verified");
                wallet.setBankAvailable(available);
                wallet.setBankPending(!available && isPendingDestination(status, bankCode, account));
                wallet.setBankLabel((bankCode + "  " + account).trim());
            }
        } else if ("mobile_money".equalsIgnoreCase(type) || "mobile".equalsIgnoreCase(type) || "mobileMoney".equalsIgnoreCase(type)) {
            String provider = firstString(destination, "provider", "mobile_money_provider", "mobileMoneyProvider", "payout_provider", "payoutProvider");
            String account = firstString(destination, "masked_account", "maskedAccount", "phone_number_masked", "phoneNumberMasked", "phone_number", "phoneNumber", "account_number", "accountNumber");
            if (!provider.isEmpty() || !account.isEmpty()) {
                boolean available = firstBoolean(destination, statusApproved, "available", "is_approved", "approved", "verified", "is_verified");
                wallet.setMobileMoneyAvailable(available);
                wallet.setMobileMoneyPending(!available && isPendingDestination(status, provider, account));
                wallet.setMobileMoneyLabel((provider + "  " + account).trim());
            }
        }
    }

    private static void applyFlatPayoutDestinationFields(DoctorWalletEntity wallet, JsonObject json) {
        String mobileStatus = firstString(json,
                "payout_mobile_status", "payoutMobileStatus",
                "mobile_money_status", "mobileMoneyStatus",
                "destination_status", "destinationStatus",
                "payout_status", "payoutStatus");
        String mobileProvider = firstString(json,
                "payout_provider", "payoutProvider",
                "mobile_money_provider", "mobileMoneyProvider",
                "provider");
        String mobileAccount = firstString(json,
                "payout_phone_masked", "payoutPhoneMasked",
                "phone_number_masked", "phoneNumberMasked",
                "masked_phone_number", "maskedPhoneNumber",
                "masked_account", "maskedAccount",
                "phone_number", "phoneNumber",
                "accountNumber", "account_number");
        if (!mobileProvider.isEmpty() || !mobileAccount.isEmpty()) {
            boolean mobileAvailable = firstBoolean(json, isAvailableStatus(mobileStatus),
                    "payout_mobile_available", "payoutMobileAvailable",
                    "mobile_money_available", "mobileMoneyAvailable",
                    "destination_available", "destinationAvailable");
            wallet.setMobileMoneyAvailable(mobileAvailable);
            wallet.setMobileMoneyPending(!mobileAvailable && isPendingDestination(mobileStatus, mobileProvider, mobileAccount));
            wallet.setMobileMoneyLabel((mobileProvider + "  " + mobileAccount).trim());
        }

        String bankStatus = firstString(json,
                "payout_bank_status", "payoutBankStatus",
                "bank_status", "bankStatus");
        String bankCode = firstString(json,
                "payout_bank_code", "payoutBankCode",
                "bank_code", "bankCode");
        String bankAccount = firstString(json,
                "payout_bank_account_masked", "payoutBankAccountMasked",
                "bank_account_masked", "bankAccountMasked",
                "masked_bank_account", "maskedBankAccount",
                "bank_account", "bankAccount");
        if (!bankCode.isEmpty() || !bankAccount.isEmpty()) {
            boolean bankAvailable = firstBoolean(json, isAvailableStatus(bankStatus),
                    "payout_bank_available", "payoutBankAvailable",
                    "bank_available", "bankAvailable");
            wallet.setBankAvailable(bankAvailable);
            wallet.setBankPending(!bankAvailable && isPendingDestination(bankStatus, bankCode, bankAccount));
            wallet.setBankLabel((bankCode + "  " + bankAccount).trim());
        }
    }

    private static double jsonDouble(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static long parseIsoTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) return System.currentTimeMillis();
        String normalized = value.trim().replaceFirst("(\\.\\d{3})\\d+(?=Z|[+-])", "$1");
        String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsed = format.parse(normalized);
                if (parsed != null) return parsed.getTime();
            } catch (ParseException ignored) {
            }
        }
        return System.currentTimeMillis();
    }
}
