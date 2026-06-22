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

    public void requestWithdrawal(String doctorId, String doctorName, double amount, String method, 
                                 String accountNumber, String accountName, String bankName,
                                 FirebaseHelper.OnCompleteListener<Boolean> callback) {
        // Create a withdrawal request instead of directly deducting
        String requestId = "WR_" + System.currentTimeMillis();
        WithdrawalRequest request = new WithdrawalRequest(requestId, doctorId, doctorName, amount, method, accountNumber);
        request.setAccountName(accountName);
        request.setBankName(bankName);
        
        FirebaseHelper.createWithdrawalRequest(request, new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Failed to create withdrawal request");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void withdrawFunds(String doctorId, double amount, FirebaseHelper.OnCompleteListener<Boolean> callback) {
        // Deprecated - use requestWithdrawal instead
        FirebaseHelper.deductFromDoctorWallet(doctorId, amount, callback);
    }

    public LiveData<List<WithdrawalRequest>> getWithdrawalRequests(String doctorId) {
        MutableLiveData<List<WithdrawalRequest>> requestsLiveData = new MutableLiveData<>();
        FirebaseHelper.getWithdrawalRequestsByDoctor(doctorId, new FirebaseHelper.OnCompleteListener<List<WithdrawalRequest>>() {
            @Override
            public void onSuccess(List<WithdrawalRequest> requests) {
                requestsLiveData.postValue(requests);
            }

            @Override
            public void onError(String error) {
                requestsLiveData.postValue(null);
            }
        });
        return requestsLiveData;
    }
}
