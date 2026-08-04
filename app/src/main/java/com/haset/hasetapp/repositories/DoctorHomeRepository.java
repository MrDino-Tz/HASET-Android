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

public class DoctorHomeRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<List<Appointment>> getAppointments(String doctorId) {
        MutableLiveData<List<Appointment>> appointmentsLiveData = new MutableLiveData<>();
        
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
        
        return appointmentsLiveData;
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

    public LiveData<DoctorWalletEntity> getWalletBalance(String doctorId) {
        MutableLiveData<DoctorWalletEntity> walletLiveData = new MutableLiveData<>();
        FirebaseHelper.getDoctorWallet(doctorId, new FirebaseHelper.OnCompleteListener<DoctorWalletEntity>() {
            @Override
            public void onSuccess(DoctorWalletEntity wallet) {
                walletLiveData.postValue(wallet);
            }

            @Override
            public void onError(String error) {
                walletLiveData.postValue(null);
            }
        });
        return walletLiveData;
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

    public void requestWithdrawalSecure(double amount, String reason, String mfaCode, FirebaseHelper.OnCompleteListener<Boolean> callback) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) { callback.onError("Authentication expired. Please sign in again."); return; }
        user.getIdToken(true).addOnSuccessListener(result -> {
            String bearer = "Bearer " + result.getToken();
            JsonObject codeBody = new JsonObject(); codeBody.addProperty("code", mfaCode);
            RetrofitClient.getInstance().getMobileMfaApiService().verify(bearer, codeBody).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null || !response.body().has("mfa_action_token")) { callback.onError(response.code() == 429 ? "Too many MFA attempts. Please wait and retry." : "Invalid or expired MFA code."); return; }
                    String actionToken = response.body().get("mfa_action_token").getAsString();
                    JsonObject body = new JsonObject(); body.addProperty("request_id", "WR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)); body.addProperty("amount", Math.round(amount)); body.addProperty("reason", reason);
                    RetrofitClient.getInstance().getDoctorPayoutApiService().requestWithdrawal(bearer, actionToken, body).enqueue(new Callback<JsonObject>() {
                        public void onResponse(Call<JsonObject> c, Response<JsonObject> r) { if (r.isSuccessful()) callback.onSuccess(true); else callback.onError(r.code() == 429 ? "Too many requests. Please retry later." : "Payout request failed."); }
                        public void onFailure(Call<JsonObject> c, Throwable t) { callback.onError("Network error while submitting payout request."); }
                    });
                }
                public void onFailure(Call<JsonObject> call, Throwable t) { callback.onError("Network error while verifying MFA."); }
            });
        }).addOnFailureListener(e -> callback.onError("Authentication expired. Please sign in again."));
    }

    public LiveData<List<WithdrawalRequest>> getWithdrawalRequests(String doctorId) {
        MutableLiveData<List<WithdrawalRequest>> requestsLiveData = new MutableLiveData<>();
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) { requestsLiveData.postValue(null); return requestsLiveData; }
        user.getIdToken(true).addOnSuccessListener(token -> RetrofitClient.getInstance().getDoctorPayoutApiService().listWithdrawals("Bearer " + token.getToken()).enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) { requestsLiveData.postValue(null); return; }
                List<WithdrawalRequest> list = new ArrayList<>();
                if (response.body().has("withdrawals") && response.body().get("withdrawals").isJsonArray()) {
                    for (com.google.gson.JsonElement element : response.body().getAsJsonArray("withdrawals")) {
                        JsonObject w = element.getAsJsonObject();
                        WithdrawalRequest item = new WithdrawalRequest();
                        item.setRequestId(w.has("request_id") ? w.get("request_id").getAsString() : "");
                        item.setDoctorId(doctorId); item.setAmount(w.has("amount") ? w.get("amount").getAsDouble() : 0);
                        item.setStatus(w.has("status") ? w.get("status").getAsString() : "pending");
                        item.setMethod("mobile"); item.setRequestedAt(w.has("created_at") ? 0 : System.currentTimeMillis());
                        item.setRejectionReason(w.has("failure_reason") && !w.get("failure_reason").isJsonNull() ? w.get("failure_reason").getAsString() : null);
                        list.add(item);
                    }
                }
                requestsLiveData.postValue(list);
            }
            public void onFailure(Call<JsonObject> call, Throwable t) { requestsLiveData.postValue(null); }
        })).addOnFailureListener(e -> requestsLiveData.postValue(null));
        return requestsLiveData;
    }
}
