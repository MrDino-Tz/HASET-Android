package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.firebase.FirebaseHelper;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DoctorRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<Doctor> getDoctorById(String doctorId) {
        MutableLiveData<Doctor> doctorLiveData = new MutableLiveData<>();
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorById(doctorId, new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Doctor>() {
            @Override
            public void onSuccess(Doctor doctor) {
                doctorLiveData.postValue(doctor);
            }

            @Override
            public void onError(String error) {
                doctorLiveData.postValue(null);
            }
        });
        return doctorLiveData;
    }

    public LiveData<DoctorEntity> getDoctorEntityById(String doctorId) {
        MutableLiveData<DoctorEntity> doctorLiveData = new MutableLiveData<>();
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorsNodeRef().child(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            DoctorEntity doctor = snapshot.getValue(DoctorEntity.class);
                            doctorLiveData.postValue(doctor);
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

    public void saveDoctorProfile(DoctorEntity doctor, com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Boolean> callback) {
        com.haset.hasetapp.utils.FirebaseHelper.saveOrUpdateDoctor(doctor, callback);
    }

    private final MutableLiveData<List<Doctor>> allDoctorsLiveData = new MutableLiveData<>();
    private com.google.firebase.database.ValueEventListener doctorsRealtimeListener;

    public LiveData<List<Doctor>> getAllDoctors() {
        attachRealtimeDoctorsListener();
        return allDoctorsLiveData;
    }

    private void attachRealtimeDoctorsListener() {
        if (doctorsRealtimeListener != null) {
            return;
        }
        doctorsRealtimeListener = com.haset.hasetapp.utils.FirebaseHelper.observeDoctorsForPatients(new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<List<Doctor>>() {
            @Override
            public void onSuccess(List<Doctor> doctors) {
                if (doctors != null && !doctors.isEmpty()) {
                    allDoctorsLiveData.postValue(doctors);
                } else {
                    loadAllDoctorsFromUsersPath(allDoctorsLiveData);
                }
            }

            @Override
            public void onError(String error) {
                loadAllDoctorsFromUsersPath(allDoctorsLiveData);
            }
        });
    }

    private void loadAllDoctorsFromUsersPath(MutableLiveData<List<Doctor>> liveData) {
        com.haset.hasetapp.utils.FirebaseHelper.getUsersRef().orderByChild("role").equalTo(Constants.ROLE_DOCTOR)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Doctor> doctors = new ArrayList<>();
                        final int totalUsers = (int) dataSnapshot.getChildrenCount();
                        final int[] processedCount = {0};

                        if (totalUsers == 0) {
                            liveData.postValue(doctors);
                            return;
                        }

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            try {
                                UserEntity user = userSnapshot.getValue(UserEntity.class);
                                if (user != null) {
                                    final String doctorId = user.getUserId();
                                    Doctor doctor = new Doctor();
                                    doctor.setDoctorId(doctorId);
                                    doctor.setUserId(doctorId);
                                    doctor.setFullName(user.getFullName() != null ? user.getFullName() : "Unknown Doctor");
                                    doctor.setProfileImage(user.getProfileImage() != null ? user.getProfileImage() : "");
                                    doctor.setEmail(user.getEmail() != null ? user.getEmail() : "");
                                    doctor.setPhone(user.getPhone() != null ? user.getPhone() : "");
                                    
                                    String specialty = userSnapshot.child("specialty").getValue(String.class);
                                    if (specialty == null || specialty.isEmpty()) {
                                        specialty = "Medical Doctor";
                                    }
                                    doctor.setSpecialty(specialty);
                                    
                                    Object ratingValue = userSnapshot.child("rating").getValue();
                                    if (ratingValue instanceof Number) {
                                        doctor.setRating(((Number) ratingValue).floatValue());
                                    } else {
                                        doctor.setRating(4.5f);
                                    }
                                    
                                    Object expValue = userSnapshot.child("experience").getValue();
                                    if (expValue instanceof Number) {
                                        doctor.setExperience(((Number) expValue).intValue());
                                    } else {
                                        doctor.setExperience(5);
                                    }
                                    
                                    Object feeValue = userSnapshot.child("consultationFee").getValue();
                                    if (feeValue instanceof Number) {
                                        doctor.setConsultationFee(((Number) feeValue).doubleValue());
                                    } else if (feeValue instanceof String) {
                                        try {
                                            doctor.setConsultationFee(Double.parseDouble((String) feeValue));
                                        } catch (Exception e) {
                                            doctor.setConsultationFee(0.0);
                                        }
                                    }
                                    
                                    String location = userSnapshot.child("location").getValue(String.class);
                                    doctor.setLocation(location != null ? location : "");
                                    doctor.setCreatedAt(user.getCreatedAt());
                                    
                                    Boolean isDemo = userSnapshot.child("isDemo").getValue(Boolean.class);
                                    doctor.setDemo(isDemo != null && isDemo);

                                    final Doctor finalDoctor = doctor;
                                    com.haset.hasetapp.utils.FirebaseHelper.getDoctorsNodeRef().child(doctorId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot doctorSnapshot) {
                                                    if (doctorSnapshot.exists()) {
                                                        Boolean isOnline = doctorSnapshot.child("online").getValue(Boolean.class);
                                                        String onlineStatus = doctorSnapshot.child("onlineStatus").getValue(String.class);
                                                        finalDoctor.setOnline(isOnline != null && isOnline);
                                                        finalDoctor.setOnlineStatus(onlineStatus != null ? onlineStatus : "offline");
                                                    } else {
                                                        finalDoctor.setOnline(false);
                                                        finalDoctor.setOnlineStatus("offline");
                                                    }
                                                    synchronized (doctors) {
                                                        doctors.add(finalDoctor);
                                                        processedCount[0]++;
                                                        if (processedCount[0] == totalUsers) {
                                                            liveData.postValue(doctors);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    synchronized (doctors) {
                                                        doctors.add(finalDoctor);
                                                        processedCount[0]++;
                                                        if (processedCount[0] == totalUsers) {
                                                            liveData.postValue(doctors);
                                                        }
                                                    }
                                                }
                                            });
                                } else {
                                    processedCount[0]++;
                                    if (processedCount[0] == totalUsers && doctors.isEmpty()) {
                                        liveData.postValue(doctors);
                                    }
                                }
                            } catch (Exception e) {
                                processedCount[0]++;
                                if (processedCount[0] == totalUsers && doctors.isEmpty()) {
                                    liveData.postValue(doctors);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        liveData.postValue(new ArrayList<>());
                    }
                });
    }
    
    public void refreshDoctors() {
        if (doctorsRealtimeListener != null) {
            com.haset.hasetapp.utils.FirebaseHelper.getDoctorsNodeRef().removeEventListener(doctorsRealtimeListener);
            doctorsRealtimeListener = null;
        }
        attachRealtimeDoctorsListener();
    }
}
