package com.haset.hasetapp.repositories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.models.PharmacyProduct;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
    private final PharmacyRepository pharmacyRepository = new PharmacyRepository();

    public LiveData<List<Doctor>> getDoctors() {
        MutableLiveData<List<Doctor>> doctorsLiveData = new MutableLiveData<>();
        
        firebaseHelper.getDoctorsNodeRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Doctor> doctors = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                        Doctor doctor = parseDoctor(doctorSnapshot);
                        // We could add approval logic here if needed
                        Boolean isApproved = doctorSnapshot.child("approved").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isApproved)) {
                            doctors.add(doctor);
                        }
                    }
                }
                
                if (doctors.isEmpty()) {
                    // Fallback to users path if empty
                    loadDoctorsFromUsersPath(doctorsLiveData);
                } else {
                    // Best-effort enrichment of names/contact details from /users
                    FirebaseHelper.mergeDoctorNamesFromUsers(doctors, new FirebaseHelper.OnCompleteListener<List<Doctor>>() {
                        @Override
                        public void onSuccess(List<Doctor> merged) {
                            sortAndPostDoctors(merged, doctorsLiveData);
                        }

                        @Override
                        public void onError(String error) {
                            sortAndPostDoctors(doctors, doctorsLiveData);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDoctorsFromUsersPath(doctorsLiveData);
            }
        });
        
        return doctorsLiveData;
    }

    private void loadDoctorsFromUsersPath(MutableLiveData<List<Doctor>> liveData) {
        Query query = firebaseHelper.getUsersRef().orderByChild("role").equalTo(Constants.ROLE_DOCTOR);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Doctor> doctors = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Doctor doctor = parseDoctor(userSnapshot);
                        Boolean isApproved = userSnapshot.child("approved").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isApproved)) {
                            doctors.add(doctor);
                        }
                    }
                }
                sortAndPostDoctors(doctors, liveData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                liveData.postValue(new ArrayList<>());
            }
        });
    }

    private Doctor parseDoctor(DataSnapshot snapshot) {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(snapshot.getKey());
        
        String name = snapshot.child("fullName").getValue(String.class);
        if (name == null) name = snapshot.child("name").getValue(String.class);
        if (name == null) name = snapshot.child("doctorName").getValue(String.class);
        doctor.setFullName(name);
        
        doctor.setSpecialty(snapshot.child("specialty").getValue(String.class));
        doctor.setProfileImage(snapshot.child("profileImage").getValue(String.class));
        doctor.setLocation(snapshot.child("location").getValue(String.class));
        doctor.setAbout(snapshot.child("about").getValue(String.class));
        
        Boolean isVerified = snapshot.child("verified").getValue(Boolean.class);
        if (isVerified == null) isVerified = snapshot.child("approved").getValue(Boolean.class);
        doctor.setVerified(isVerified != null && isVerified);

        // Rating
        Float rating = 0f;
        Object ratingValue = snapshot.child("rating").getValue();
        if (ratingValue == null) ratingValue = snapshot.child("averageRating").getValue();
        if (ratingValue instanceof Number) {
            rating = ((Number) ratingValue).floatValue();
        }
        doctor.setRating(rating);

        // Fee
        Double fee = 0.0;
        Object feeValue = snapshot.child("consultationFee").getValue();
        if (feeValue instanceof Number) {
            fee = ((Number) feeValue).doubleValue();
        }
        doctor.setConsultationFee(fee);

        // Demo doctor flag
        Boolean isDemo = snapshot.child("isDemo").getValue(Boolean.class);
        doctor.setDemo(isDemo != null && isDemo);

        // Demo doctor visibility check
        Boolean isVisible = snapshot.child("isVisible").getValue(Boolean.class);
        if (isDemo != null && isDemo) {
            doctor.setDemo(isVisible == null || isVisible);
        }

        return doctor;
    }

    private void sortAndPostDoctors(List<Doctor> doctors, MutableLiveData<List<Doctor>> liveData) {
        Collections.sort(doctors, (d1, d2) -> Float.compare(d2.getRating(), d1.getRating()));
        liveData.postValue(doctors);
    }
    
    public LiveData<Integer> getNotificationCount(String userId, String role) {
        MutableLiveData<Integer> countLiveData = new MutableLiveData<>();
        Query query;
        if ("patient".equals(role)) {
            query = firebaseHelper.getAppointmentsRef().orderByChild("patientId").equalTo(userId);
        } else {
            query = firebaseHelper.getAppointmentsRef().orderByChild("doctorId").equalTo(userId);
        }
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                countLiveData.postValue((int) snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        return countLiveData;
    }
    public LiveData<List<ArticlePostEntity>> getPopularArticles() {
        MutableLiveData<List<ArticlePostEntity>> articlesLiveData = new MutableLiveData<>();
        firebaseHelper.getDatabaseReference().child("article_posts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ArticlePostEntity> articles = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ArticlePostEntity article = ds.getValue(ArticlePostEntity.class);
                            if (article != null) {
                                // Check if article should be shown (status = published or no status field)
                                String status = ds.child("status").getValue(String.class);
                                if (status == null || "published".equalsIgnoreCase(status)) {
                                    articles.add(article);
                                }
                            }
                        }
                        // Sort by views descending
                        Collections.sort(articles, (a1, a2) -> Integer.compare(a2.getViews(), a1.getViews()));
                        // Limit to top 5
                        if (articles.size() > 5) {
                            articles = articles.subList(0, 5);
                        }
                        articlesLiveData.postValue(articles);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        articlesLiveData.postValue(new ArrayList<>());
                    }
                });
        return articlesLiveData;
    }

    public LiveData<List<PharmacyProduct>> getFeaturedMedicines() {
        return pharmacyRepository.getAllProducts(); // For now return all, fragment can slice
    }

    public LiveData<List<PatientBannerAdapter.BannerItem>> getBanners() {
        MutableLiveData<List<PatientBannerAdapter.BannerItem>> bannersLiveData = new MutableLiveData<>();
        firebaseHelper.getDatabaseReference().child("promotional_banners")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<PatientBannerAdapter.BannerItem> banners = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        PatientBannerAdapter.BannerItem item = ds.getValue(PatientBannerAdapter.BannerItem.class);
                        if (item != null) {
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
    
    public LiveData<AppointmentEntity> getUpcomingAppointment(String userId, String role) {
        MutableLiveData<AppointmentEntity> appointmentLiveData = new MutableLiveData<>();
        Query query;
        
        if ("patient".equals(role)) {
            query = firebaseHelper.getAppointmentsRef().orderByChild("patientId").equalTo(userId);
        } else {
            query = firebaseHelper.getAppointmentsRef().orderByChild("doctorId").equalTo(userId);
        }
        
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppointmentEntity upcoming = null;
                long currentTime = System.currentTimeMillis();
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentEntity appointment = ds.getValue(AppointmentEntity.class);
                    if (appointment != null) {
                        String status = appointment.getStatus();
                        if (status != null && !status.equalsIgnoreCase("read") && !status.equalsIgnoreCase("completed")) {
                            upcoming = appointment;
                            break;
                        }
                    }
                }
                appointmentLiveData.postValue(upcoming);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                appointmentLiveData.postValue(null);
            }
        });
        
        return appointmentLiveData;
    }
}
