package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.haset.hasetapp.database.entities.DoctorWalletEntity;

@Dao
public interface DoctorWalletDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DoctorWalletEntity wallet);
    
    @Update
    void update(DoctorWalletEntity wallet);
    
    @Query("SELECT * FROM doctor_wallets WHERE doctorId = :doctorId LIMIT 1")
    DoctorWalletEntity getWalletByDoctorId(String doctorId);
    
    @Query("UPDATE doctor_wallets SET balance = balance + :amount, totalEarnings = totalEarnings + :amount, lastUpdated = :timestamp WHERE doctorId = :doctorId")
    void addToBalance(String doctorId, double amount, long timestamp);
    
    @Query("UPDATE doctor_wallets SET balance = balance - :amount, lastUpdated = :timestamp WHERE doctorId = :doctorId AND balance >= :amount")
    int deductFromBalance(String doctorId, double amount, long timestamp);
    
    @Query("DELETE FROM doctor_wallets WHERE doctorId = :doctorId")
    void deleteWallet(String doctorId);
}

