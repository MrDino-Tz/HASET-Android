package com.haset.hasetapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.haset.hasetapp.database.entities.WithdrawalRequest;

import java.util.List;

@Dao
public interface WithdrawalRequestDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WithdrawalRequest request);
    
    @Update
    void update(WithdrawalRequest request);
    
    @Delete
    void delete(WithdrawalRequest request);
    
    @Query("SELECT * FROM withdrawal_requests WHERE requestId = :requestId")
    WithdrawalRequest getRequestById(String requestId);
    
    @Query("SELECT * FROM withdrawal_requests WHERE doctorId = :doctorId ORDER BY requestedAt DESC")
    List<WithdrawalRequest> getRequestsByDoctorId(String doctorId);
    
    @Query("SELECT * FROM withdrawal_requests WHERE status = :status ORDER BY requestedAt DESC")
    List<WithdrawalRequest> getRequestsByStatus(String status);
    
    @Query("SELECT * FROM withdrawal_requests ORDER BY requestedAt DESC")
    List<WithdrawalRequest> getAllRequests();
    
    @Query("SELECT * FROM withdrawal_requests WHERE status = 'pending' ORDER BY requestedAt ASC")
    List<WithdrawalRequest> getPendingRequests();
    
    @Query("SELECT COUNT(*) FROM withdrawal_requests WHERE status = 'pending'")
    int getPendingRequestCount();
    
    @Query("DELETE FROM withdrawal_requests WHERE requestId = :requestId")
    void deleteById(String requestId);
}
