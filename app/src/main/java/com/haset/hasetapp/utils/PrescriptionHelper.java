package com.haset.hasetapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for prescription operations with Firebase and Cloudinary
 */
public class PrescriptionHelper {
    
    private static final String TAG = "PrescriptionHelper";
    private static final String PRESCRIPTIONS_NODE = "prescriptions";
    
    private final DatabaseReference prescriptionsRef;
    private final AppDatabase database;
    private final Gson gson;
    
    public PrescriptionHelper(Context context) {
        this.prescriptionsRef = FirebaseDatabase.getInstance().getReference(PRESCRIPTIONS_NODE);
        this.database = AppDatabase.getInstance(context);
        this.gson = new Gson();
    }
    
    /**
     * Upload prescription image to Cloudinary
     */
    public void uploadPrescriptionImage(Uri imageUri, UploadCallback callback) {
        String uploadPreset = CloudinaryUploadHelper.getUploadPreset();
        if (uploadPreset == null || uploadPreset.trim().isEmpty()) {
            Log.e(TAG, "Cloudinary upload preset is not initialized");
            return;
        }

        MediaManager.get().upload(imageUri)
            .option("folder", "prescriptions")
            .option("resource_type", "image")
            .unsigned(uploadPreset)
            .callback(callback)
            .dispatch();
    }
    
    /**
     * Create a new prescription
     */
    public void createPrescription(Prescription prescription, final PrescriptionCallback callback) {
        String prescriptionId = prescription.getPrescriptionId();
        if (prescriptionId == null || prescriptionId.isEmpty()) {
            prescriptionId = prescriptionsRef.push().getKey();
            prescription.setPrescriptionId(prescriptionId);
        }
        
        final String finalId = prescriptionId;
        
        // Save to Firebase
        prescriptionsRef.child(finalId).setValue(prescription)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Prescription created successfully: " + finalId);
                
                // Save to local database
                savePrescriptionLocally(prescription);
                
                if (callback != null) {
                    callback.onSuccess(prescription);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create prescription", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Get prescriptions for a patient
     */
    public void getPrescriptionsByPatient(String patientId, final PrescriptionListCallback callback) {
        prescriptionsRef.orderByChild("patientId").equalTo(patientId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Prescription> prescriptions = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Prescription prescription = child.getValue(Prescription.class);
                        if (prescription != null) {
                            prescriptions.add(prescription);
                            savePrescriptionLocally(prescription);
                        }
                    }
                    
                    if (callback != null) {
                        callback.onSuccess(prescriptions);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to get prescriptions", error.toException());
                    
                    // Fallback to local database
                    List<Prescription> localPrescriptions = getLocalPrescriptionsByPatient(patientId);
                    if (callback != null) {
                        callback.onSuccess(localPrescriptions);
                    }
                }
            });
    }
    
    /**
     * Get prescriptions by doctor
     */
    public void getPrescriptionsByDoctor(String doctorId, final PrescriptionListCallback callback) {
        prescriptionsRef.orderByChild("doctorId").equalTo(doctorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Prescription> prescriptions = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Prescription prescription = child.getValue(Prescription.class);
                        if (prescription != null) {
                            prescriptions.add(prescription);
                        }
                    }
                    
                    if (callback != null) {
                        callback.onSuccess(prescriptions);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to get prescriptions", error.toException());
                    if (callback != null) {
                        callback.onError(error.getMessage());
                    }
                }
            });
    }
    
    /**
     * Get a single prescription by ID
     */
    public void getPrescriptionById(String prescriptionId, final PrescriptionCallback callback) {
        prescriptionsRef.child(prescriptionId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Prescription prescription = snapshot.getValue(Prescription.class);
                    if (prescription != null) {
                        savePrescriptionLocally(prescription);
                        if (callback != null) {
                            callback.onSuccess(prescription);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Prescription not found");
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to get prescription", error.toException());
                    
                    // Fallback to local database
                    Prescription localPrescription = getLocalPrescriptionById(prescriptionId);
                    if (localPrescription != null && callback != null) {
                        callback.onSuccess(localPrescription);
                    } else if (callback != null) {
                        callback.onError(error.getMessage());
                    }
                }
            });
    }
    
    /**
     * Update a prescription
     */
    public void updatePrescription(Prescription prescription, final PrescriptionCallback callback) {
        prescriptionsRef.child(prescription.getPrescriptionId()).setValue(prescription)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Prescription updated successfully");
                savePrescriptionLocally(prescription);
                
                if (callback != null) {
                    callback.onSuccess(prescription);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update prescription", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Delete a prescription
     */
    public void deletePrescription(String prescriptionId, final DeleteCallback callback) {
        prescriptionsRef.child(prescriptionId).removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Prescription deleted successfully");
                deleteLocalPrescription(prescriptionId);
                
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete prescription", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    // Local database operations
    
    private void savePrescriptionLocally(Prescription prescription) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            PrescriptionEntity entity = convertToEntity(prescription);
            database.prescriptionDao().insert(entity);
            Log.d(TAG, "Prescription saved locally: " + prescription.getPrescriptionId());
        });
    }
    
    private List<Prescription> getLocalPrescriptionsByPatient(String patientId) {
        List<PrescriptionEntity> entities = database.prescriptionDao().getPrescriptionsByPatient(patientId);
        List<Prescription> prescriptions = new ArrayList<>();
        for (PrescriptionEntity entity : entities) {
            prescriptions.add(convertFromEntity(entity));
        }
        return prescriptions;
    }
    
    private Prescription getLocalPrescriptionById(String prescriptionId) {
        PrescriptionEntity entity = database.prescriptionDao().getPrescriptionById(prescriptionId);
        return entity != null ? convertFromEntity(entity) : null;
    }
    
    private void deleteLocalPrescription(String prescriptionId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.prescriptionDao().deleteById(prescriptionId);
            Log.d(TAG, "Prescription deleted locally: " + prescriptionId);
        });
    }
    
    // Conversion methods
    
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
        
        // Parse medicines JSON
        Type listType = new TypeToken<List<Prescription.Medicine>>(){}.getType();
        List<Prescription.Medicine> medicines = gson.fromJson(entity.getMedicinesJson(), listType);
        prescription.setMedicines(medicines != null ? medicines : new ArrayList<>());
        
        prescription.setInstructions(entity.getInstructions());
        prescription.setImageUrl(entity.getImageUrl());
        prescription.setCreatedAt(entity.getCreatedAt());
        return prescription;
    }
    
    // Callback interfaces
    
    public interface PrescriptionCallback {
        void onSuccess(Prescription prescription);
        void onError(String error);
    }
    
    public interface PrescriptionListCallback {
        void onSuccess(List<Prescription> prescriptions);
        void onError(String error);
    }
    
    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
