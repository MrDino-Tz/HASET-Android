package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.haset.hasetapp.database.entities.DoctorEntity;

@Dao
public interface DoctorDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DoctorEntity doctor);
    
    @Update
    void update(DoctorEntity doctor);
    
    @Query("SELECT * FROM doctors WHERE doctorId = :doctorId LIMIT 1")
    DoctorEntity getDoctorById(String doctorId);
    
    @Query("DELETE FROM doctors WHERE doctorId = :doctorId")
    void deleteDoctor(String doctorId);
}

