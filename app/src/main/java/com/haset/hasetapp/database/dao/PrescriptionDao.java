package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.haset.hasetapp.database.entities.PrescriptionEntity;

import java.util.List;

/**
 * DAO for prescription database operations
 */
@Dao
public interface PrescriptionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PrescriptionEntity prescription);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PrescriptionEntity> prescriptions);
    
    @Update
    void update(PrescriptionEntity prescription);
    
    @Delete
    void delete(PrescriptionEntity prescription);
    
    @Query("SELECT * FROM prescriptions WHERE patientId = :patientId ORDER BY createdAt DESC")
    List<PrescriptionEntity> getPrescriptionsByPatient(String patientId);
    
    @Query("SELECT * FROM prescriptions WHERE prescriptionId = :prescriptionId")
    PrescriptionEntity getPrescriptionById(String prescriptionId);
    
    @Query("SELECT * FROM prescriptions WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    List<PrescriptionEntity> getPrescriptionsByDoctor(String doctorId);
    
    @Query("SELECT * FROM prescriptions WHERE appointmentId = :appointmentId")
    List<PrescriptionEntity> getPrescriptionsByAppointment(String appointmentId);
    
    @Query("DELETE FROM prescriptions WHERE prescriptionId = :prescriptionId")
    void deleteById(String prescriptionId);
    
    @Query("DELETE FROM prescriptions WHERE patientId = :patientId")
    void deleteByPatient(String patientId);
    
    @Query("DELETE FROM prescriptions")
    void deleteAll();
    
    @Query("SELECT COUNT(*) FROM prescriptions WHERE patientId = :patientId")
    int getCountByPatient(String patientId);
}
