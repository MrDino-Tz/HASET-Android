package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

import com.haset.hasetapp.database.entities.DoctorRatingEntity;

import java.util.List;

@Dao
public interface DoctorRatingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoctorRatingEntity rating);
    
    @Query("SELECT * FROM doctor_ratings WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    List<DoctorRatingEntity> getRatingsByDoctorId(String doctorId);
    
    @Query("SELECT * FROM doctor_ratings WHERE doctorId = :doctorId AND patientId = :patientId LIMIT 1")
    DoctorRatingEntity getRatingByDoctorAndPatient(String doctorId, String patientId);
    
    @Query("SELECT AVG(rating) FROM doctor_ratings WHERE doctorId = :doctorId")
    Double getAverageRating(String doctorId);
    
    @Query("SELECT COUNT(*) FROM doctor_ratings WHERE doctorId = :doctorId")
    int getRatingCount(String doctorId);
    
    @Query("SELECT * FROM doctor_ratings WHERE appointmentId = :appointmentId LIMIT 1")
    DoctorRatingEntity getRatingByAppointmentId(String appointmentId);
    
    @Query("DELETE FROM doctor_ratings WHERE ratingId = :ratingId")
    void deleteRating(String ratingId);
}


