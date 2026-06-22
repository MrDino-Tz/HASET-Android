package com.haset.hasetapp.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.utils.FirebaseHelper;

import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AdminRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<List<UserEntity>> getAllUsers() {
        MutableLiveData<List<UserEntity>> usersLiveData = new MutableLiveData<>();
        FirebaseHelper.getAllUsers(new FirebaseHelper.OnCompleteListener<List<UserEntity>>() {
            @Override
            public void onSuccess(List<UserEntity> result) {
                usersLiveData.postValue(result);
            }

            @Override
            public void onError(String error) {
                usersLiveData.postValue(null);
            }
        });
        return usersLiveData;
    }

    public LiveData<List<AppointmentEntity>> getAllAppointments() {
        MutableLiveData<List<AppointmentEntity>> appointmentsLiveData = new MutableLiveData<>();
        FirebaseHelper.getAllAppointments(new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> result) {
                appointmentsLiveData.postValue(result);
            }

            @Override
            public void onError(String error) {
                appointmentsLiveData.postValue(null);
            }
        });
        return appointmentsLiveData;
    }
    public LiveData<List<PatientBannerAdapter.BannerItem>> getBanners() {
        MutableLiveData<List<PatientBannerAdapter.BannerItem>> bannersLiveData = new MutableLiveData<>();
        firebaseHelper.getDatabaseReference().child(Constants.BANNERS_PATH)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<PatientBannerAdapter.BannerItem> banners = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        PatientBannerAdapter.BannerItem item = ds.getValue(PatientBannerAdapter.BannerItem.class);
                        if (item != null) {
                            item.key = ds.getKey();
                            banners.add(item);
                        }
                    }
                    bannersLiveData.postValue(banners);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    bannersLiveData.postValue(null);
                }
            });
        return bannersLiveData;
    }

    public void addBanner(PatientBannerAdapter.BannerItem banner, FirebaseHelper.OnCompleteListener<Void> callback) {
        firebaseHelper.getDatabaseReference().child(Constants.BANNERS_PATH).push().setValue(banner)
            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateBanner(String key, PatientBannerAdapter.BannerItem banner, FirebaseHelper.OnCompleteListener<Void> callback) {
        firebaseHelper.getDatabaseReference().child(Constants.BANNERS_PATH).child(key).setValue(banner)
            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteBanner(String key, FirebaseHelper.OnCompleteListener<Void> callback) {
        firebaseHelper.getDatabaseReference().child(Constants.BANNERS_PATH).child(key).removeValue()
            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public LiveData<List<AuditLogEntity>> getAuditLogs() {
        MutableLiveData<List<AuditLogEntity>> logsLiveData = new MutableLiveData<>();
        FirebaseHelper.getAllAuditLogs(new FirebaseHelper.OnCompleteListener<List<AuditLogEntity>>() {
            @Override
            public void onSuccess(List<AuditLogEntity> result) {
                logsLiveData.postValue(result);
            }

            @Override
            public void onError(String error) {
                logsLiveData.postValue(null);
            }
        });
        return logsLiveData;
    }
}
