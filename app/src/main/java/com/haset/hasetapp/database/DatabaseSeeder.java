package com.haset.hasetapp.database;

import android.content.Context;

import com.haset.hasetapp.database.entities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Database Seeder - Adds test users for development/testing
 */
public class DatabaseSeeder {
    
    private final AppDatabase database;
    private final ExecutorService executorService;
    
    public DatabaseSeeder(Context context) {
        database = AppDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Seeds the database with test users
     * Call this once on first app launch
     */
    public void seedTestUsers(OnSeedCompleteListener listener) {
        executorService.execute(() -> {
            try {
                // Check if users already exist
                if (database.userDao().getAllUsers().size() > 0) {
                    if (listener != null) {
                        listener.onComplete("Users already exist");
                    }
                    return;
                }
                
                // Create test patient
//                UserEntity patient = new UserEntity();
//                patient.setUserId("patient-001");
//                patient.setEmail("patient@test.com");
//                patient.setPassword(hashPassword("password123"));
//                patient.setFullName("John Patient");
//                patient.setPhone("+1234567890");
//                patient.setRole("patient");
//                patient.setCreatedAt(System.currentTimeMillis());
//                database.userDao().insert(patient);
                
                // Create test doctor
//                UserEntity doctor = new UserEntity();
//                doctor.setUserId("doctor-001");
//                doctor.setEmail("doctor@test.com");
//                doctor.setPassword(hashPassword("password123"));
//                doctor.setFullName("Dr. Sarah Smith");
//                doctor.setPhone("+1234567891");
//                doctor.setRole("doctor");
//                doctor.setCreatedAt(System.currentTimeMillis());
//                database.userDao().insert(doctor);
                
                // Create another doctor
//                UserEntity doctor2 = new UserEntity();
//                doctor2.setUserId("doctor-002");
//                doctor2.setEmail("doctor2@test.com");
//                doctor2.setPassword(hashPassword("password123"));
//                doctor2.setFullName("Dr. Michael Johnson");
//                doctor2.setPhone("+1234567892");
//                doctor2.setRole("doctor");
//                doctor2.setCreatedAt(System.currentTimeMillis());
//                database.userDao().insert(doctor2);
                
                // Create another doctor for testing
//                UserEntity doctor3 = new UserEntity();
//                doctor3.setUserId("doctor-003");
//                doctor3.setEmail("doctor3@test.com");
//                doctor3.setPassword(hashPassword("password123"));
//                doctor3.setFullName("Dr. Emily Davis");
//                doctor3.setPhone("+1234567893");
//                doctor3.setRole("doctor");
//                doctor3.setCreatedAt(System.currentTimeMillis());
//                database.userDao().insert(doctor3);
                
                if (listener != null) {
                    listener.onComplete("Test users created successfully");
                }
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onComplete("Error: " + e.getMessage());
                }
            }
        });
    }
    
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }
    
    public interface OnSeedCompleteListener {
        void onComplete(String message);
    }
}
