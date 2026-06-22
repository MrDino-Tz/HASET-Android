package com.haset.hasetapp.models;

import java.io.Serializable;

public class AuditLog implements Serializable {
    private String logId;
    private String userId;
    private String userName;
    private String userRole;
    private String action; // e.g., "LOGIN", "LOGOUT", "CREATE_APPOINTMENT", "UPDATE_PROFILE", etc.
    private String description; // Detailed description of the action
    private String entityType; // e.g., "USER", "APPOINTMENT", "PROFILE", etc.
    private String entityId; // ID of the affected entity
    private long timestamp;
    private String ipAddress; // Optional: IP address if available
    private String deviceInfo; // Optional: Device information

    public AuditLog() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public AuditLog(String userId, String userName, String userRole, String action, String description) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.action = action;
        this.description = description;
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
}

