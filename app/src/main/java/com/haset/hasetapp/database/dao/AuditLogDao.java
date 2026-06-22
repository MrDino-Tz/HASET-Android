package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.haset.hasetapp.database.entities.AuditLogEntity;

import java.util.List;

@Dao
public interface AuditLogDao {
    
    @Insert
    void insert(AuditLogEntity auditLog);
    
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    List<AuditLogEntity> getAllAuditLogs();
    
    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    List<AuditLogEntity> getAuditLogsByUser(String userId);
    
    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC")
    List<AuditLogEntity> getAuditLogsByAction(String action);
    
    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType ORDER BY timestamp DESC")
    List<AuditLogEntity> getAuditLogsByEntityType(String entityType);
    
    @Query("SELECT * FROM audit_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    List<AuditLogEntity> getAuditLogsByDateRange(long startTime, long endTime);
    
    @Query("DELETE FROM audit_logs WHERE timestamp < :beforeTimestamp")
    void deleteOldLogs(long beforeTimestamp);
    
    @Query("DELETE FROM audit_logs")
    void deleteAll();
}

