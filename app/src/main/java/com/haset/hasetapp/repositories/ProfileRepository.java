package com.haset.hasetapp.repositories;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Doctor;

import java.util.ArrayList;
import java.util.List;

public class ProfileRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<UserEntity> getUserInfo(String userId) {
        MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
        firebaseHelper.getUsersRef().child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserEntity user = new UserEntity();
                    try {
                        user.setUserId(snapshot.getKey());
                        user.setEmail(snapshot.child("email").getValue(String.class));
                        user.setFullName(snapshot.child("fullName").getValue(String.class));
                        
                        Object phoneObj = snapshot.child("phone").getValue();
                        user.setPhone(phoneObj != null ? String.valueOf(phoneObj) : "");
                        
                        user.setRole(snapshot.child("role").getValue(String.class));
                        user.setProfileImage(snapshot.child("profileImage").getValue(String.class));
                        Object ageValue = snapshot.child("age").getValue();
                        if (ageValue instanceof Number) {
                            user.setAge(((Number) ageValue).intValue());
                        } else if (ageValue != null) {
                            try {
                                user.setAge(Integer.parseInt(String.valueOf(ageValue)));
                            } catch (NumberFormatException ignored) {
                                user.setAge(0);
                            }
                        }
                        Object genderValue = snapshot.child("gender").getValue();
                        user.setGender(genderValue == null ? "" : String.valueOf(genderValue));
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
                        user.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
                        
                        userLiveData.postValue(user);
                    } catch (Exception e) {
                        Log.e("ProfileRepository", "Error parsing user data", e);
                        userLiveData.postValue(null);
                    }
                } else {
                    userLiveData.postValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userLiveData.postValue(null);
            }
        });
        return userLiveData;
    }

    public LiveData<Doctor> getDoctorProfessionalInfo(String doctorId) {
        MutableLiveData<Doctor> doctorLiveData = new MutableLiveData<>();
        firebaseHelper.getDoctorsNodeRef().child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Doctor doctor = new Doctor();
                        doctor.setDoctorId(doctorId);
                        doctor.setSpecialty(snapshot.child("specialty").getValue(String.class));
                        Double consultationFee = snapshot.child("consultationFee").getValue(Double.class);
                        doctor.setConsultationFee(consultationFee != null ? consultationFee : 0.0);
                        doctor.setAbout(snapshot.child("about").getValue(String.class));
                        doctor.setLocation(snapshot.child("location").getValue(String.class));
                        doctor.setRegNo(snapshot.child("regNo").getValue(String.class));
                        Boolean verified = snapshot.child("verified").getValue(Boolean.class);
                        // Fallback to isApproved (set by admin when approving doctors)
                        if (verified == null) {
                            verified = snapshot.child("isApproved").getValue(Boolean.class);
                        }
                        doctor.setVerified(verified != null && verified);
                        
                        Object availableTimesObj = snapshot.child("availableTimes").getValue();
                        if (availableTimesObj instanceof List) {
                            doctor.setAvailableTimes((List<String>) availableTimesObj);
                        } else if (availableTimesObj instanceof String) {
                            String timesStr = availableTimesObj.toString();
                            if (timesStr.startsWith("[") && timesStr.endsWith("]")) {
                                timesStr = timesStr.substring(1, timesStr.length() - 1);
                            }
                            String[] timesArray = timesStr.split(", ");
                            List<String> timesList = new ArrayList<>();
                            for (String time : timesArray) {
                                if (!time.trim().isEmpty()) {
                                    timesList.add(time.trim());
                                }
                            }
                            doctor.setAvailableTimes(timesList);
                        }
                        
                        doctorLiveData.postValue(doctor);
                    } catch (Exception e) {
                        Log.e("ProfileRepository", "Error parsing doctor data", e);
                        doctorLiveData.postValue(null);
                    }
                } else {
                    doctorLiveData.postValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                doctorLiveData.postValue(null);
            }
        });
        return doctorLiveData;
    }

    public void deleteAccount(String userId, FirebaseHelper.OnCompleteListener<Void> callback) {
        firebaseHelper.deleteUserAccount(userId, callback);
    }

    public void updateUserInfo(UserEntity user, FirebaseHelper.OnCompleteListener<Void> callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("fullName", user.getFullName());
        updates.put("email", user.getEmail());
        updates.put("phone", user.getPhone());
        updates.put("profileImage", user.getProfileImage() == null ? "" : user.getProfileImage());
        updates.put("age", user.getAge());
        updates.put("gender", user.getGender() == null ? "" : user.getGender());
        firebaseHelper.getUsersRef().child(user.getUserId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
