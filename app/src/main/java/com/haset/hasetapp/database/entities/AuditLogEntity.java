package com.haset.hasetapp.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.firebase.database.PropertyName;

@Entity(tableName = "audit_logs")
public class AuditLogEntity {
    @PrimaryKey
    @NonNull
    private String logId;
    private String userId;
    private String userEmail;
    private String userName;
    private String userRole; // "admin", "doctor", "patient"
    private String action; // "LOGIN", "CREATE_POST", "DELETE_POST", "APPOINTMENT_BOOKING", etc.
    
    @PropertyName("activityType")
    public String getActivityType() {
        return action;
    }
    
    public void setActivityType(String activityType) {
        this.action = activityType;
    }
    
    private String description;
    private String entityType; // "POST", "USER", "APPOINTMENT", etc.
    private String entityId; // ID of the affected entity
    private String platform; // "Android"
    private long timestamp;
    private String ipAddress; // Optional, might be available via Firebase or helper
    private String deviceInfo;

    public AuditLogEntity() {
    }

    @Ignore
    public AuditLogEntity(@NonNull String logId, String userId, String userEmail, String userName, String userRole, 
                          String action, String description, long timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userRole = userRole;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
        this.platform = "Android";
        this.deviceInfo = android.os.Build.MODEL + " (API " + android.os.Build.VERSION.SDK_INT + ")";
    }

    // Getters and Setters
    @NonNull
    public String getLogId() { return logId; }
    public void setLogId(@NonNull String logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
}
