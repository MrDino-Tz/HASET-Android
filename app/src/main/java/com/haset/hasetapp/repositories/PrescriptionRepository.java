package com.haset.hasetapp.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haset.hasetapp.database.AppDatabase;
import com.haset.hasetapp.database.entities.PrescriptionEntity;
import com.haset.hasetapp.models.Prescription;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.CrashMonitor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionRepository {
    private static final String TAG = "PrescriptionRepository";
    private final DatabaseReference prescriptionsRef;
    private final AppDatabase database;
    private final Gson gson;

    public PrescriptionRepository(Context context) {
        this.prescriptionsRef = FirebaseDatabase.getInstance().getReference("prescriptions");
        this.database = AppDatabase.getInstance(context);
        this.gson = new Gson();
    }

    public LiveData<List<Prescription>> getPrescriptionsByPatient(String patientId) {
        MutableLiveData<List<Prescription>> data = new MutableLiveData<>();
        
        // Initial load from local database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<PrescriptionEntity> entities = database.prescriptionDao().getPrescriptionsByPatient(patientId);
            List<Prescription> prescriptions = new ArrayList<>();
            for (PrescriptionEntity entity : entities) {
                prescriptions.add(convertFromEntity(entity));
            }
            data.postValue(prescriptions);
        });

        // Sync with Firebase
        prescriptionsRef.orderByChild("patientId").equalTo(patientId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Prescription> prescriptions = new ArrayList<>();
                    List<PrescriptionEntity> entities = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Prescription p = ds.getValue(Prescription.class);
                        if (p != null) {
                            prescriptions.add(p);
                            entities.add(convertToEntity(p));
                        }
                    }
                    data.postValue(prescriptions);
                    
                    // Update local cache
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.prescriptionDao().insertAll(entities);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase error: " + error.getMessage());
                }
            });

        return data;
    }

    public LiveData<List<Prescription>> getPrescriptionsByDoctor(String doctorId) {
        MutableLiveData<List<Prescription>> data = new MutableLiveData<>();

        prescriptionsRef.orderByChild("doctorId").equalTo(doctorId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Prescription> prescriptions = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Prescription p = ds.getValue(Prescription.class);
                        if (p != null) {
                            prescriptions.add(p);
                        }
                    }
                    data.postValue(prescriptions);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase error: " + error.getMessage());
                }
            });

        return data;
    }

    public LiveData<Prescription> getPrescriptionById(String prescriptionId) {
        MutableLiveData<Prescription> data = new MutableLiveData<>();

        // Check local first
        AppDatabase.databaseWriteExecutor.execute(() -> {
            PrescriptionEntity entity = database.prescriptionDao().getPrescriptionById(prescriptionId);
            if (entity != null) {
                data.postValue(convertFromEntity(entity));
            }
        });

        // Then check Firebase
        prescriptionsRef.child(prescriptionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Prescription p = snapshot.getValue(Prescription.class);
                if (p != null) {
                    data.postValue(p);
                    // Update local
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.prescriptionDao().insert(convertToEntity(p));
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });

        return data;
    }

    public void createPrescription(Prescription prescription, FirebaseHelper.OnCompleteListener<Void> callback) {
        CrashMonitor.step("appointment", "PrescriptionRepository.create",
                "create prescription patient=" + (prescription.getPatientId() != null ? prescription.getPatientId() : "") + " doctor=" + (prescription.getDoctorId() != null ? prescription.getDoctorId() : ""));
        String id = prescription.getPrescriptionId();
        if (id == null || id.isEmpty()) {
            id = prescriptionsRef.push().getKey();
            prescription.setPrescriptionId(id);
        }

        prescriptionsRef.child(id).setValue(prescription)
            .addOnSuccessListener(aVoid -> {
                CrashMonitor.breadcrumb("prescription created id=" + id);
                saveLocally(prescription);
                if (callback != null) callback.onSuccess(null);
            })
            .addOnFailureListener(e -> {
                CrashMonitor.report("appointment", "PrescriptionRepository.create",
                        "prescription write failed id=" + id, e);
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void deletePrescription(String prescriptionId, FirebaseHelper.OnCompleteListener<Void> callback) {
        CrashMonitor.step("appointment", "PrescriptionRepository.delete", "delete prescription id=" + prescriptionId);
        prescriptionsRef.child(prescriptionId).removeValue()
            .addOnSuccessListener(aVoid -> {
                CrashMonitor.breadcrumb("prescription deleted id=" + prescriptionId);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.prescriptionDao().deleteById(prescriptionId);
                });
                if (callback != null) callback.onSuccess(null);
            })
            .addOnFailureListener(e -> {
                CrashMonitor.report("appointment", "PrescriptionRepository.delete",
                        "prescription delete failed id=" + prescriptionId, e);
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    private void saveLocally(Prescription prescription) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.prescriptionDao().insert(convertToEntity(prescription));
        });
    }

    private PrescriptionEntity convertToEntity(Prescription prescription) {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setPrescriptionId(prescription.getPrescriptionId());
        entity.setAppointmentId(prescription.getAppointmentId());
        entity.setPatientId(prescription.getPatientId());
        entity.setPatientName(prescription.getPatientName());
        entity.setDoctorId(prescription.getDoctorId());
        entity.setDoctorName(prescription.getDoctorName());
        entity.setMedicinesJson(gson.toJson(prescription.getMedicines()));
        entity.setInstructions(prescription.getInstructions());
        entity.setImageUrl(prescription.getImageUrl());
        entity.setCreatedAt(prescription.getCreatedAt());
        entity.setUpdatedAt(System.currentTimeMillis());
        return entity;
    }

    private Prescription convertFromEntity(PrescriptionEntity entity) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionId(entity.getPrescriptionId());
        prescription.setAppointmentId(entity.getAppointmentId());
        prescription.setPatientId(entity.getPatientId());
        prescription.setPatientName(entity.getPatientName());
        prescription.setDoctorId(entity.getDoctorId());
        prescription.setDoctorName(entity.getDoctorName());
        
        Type listType = new TypeToken<List<Prescription.Medicine>>(){}.getType();
        List<Prescription.Medicine> medicines = gson.fromJson(entity.getMedicinesJson(), listType);
        prescription.setMedicines(medicines != null ? medicines : new ArrayList<>());
        
        prescription.setInstructions(entity.getInstructions());
        prescription.setImageUrl(entity.getImageUrl());
        prescription.setCreatedAt(entity.getCreatedAt());
        return prescription;
    }
}
