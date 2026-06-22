package com.haset.hasetapp.utils;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.firebase.FirebaseHelper;

import java.util.UUID;

/**
 * AuditLogger - Utility class for logging user actions
 * Provides easy methods to log various admin-tracked activities
 */
public class AuditLogger {
    
    private static AuditLogger instance;
    private DatabaseReference auditLogsRef;
    private PreferenceManager preferenceManager;
    
    private AuditLogger(Context context) {
        auditLogsRef = FirebaseHelper.getInstance().getAuditLogsRef();
        preferenceManager = new PreferenceManager(context);
    }
    
    public static synchronized AuditLogger getInstance(Context context) {
        if (instance == null) {
            instance = new AuditLogger(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Log a user action
     */
    public void logAction(String action, String description, String entityType, String entityId) {
        String userId = preferenceManager.getUserId();
        String userName = preferenceManager.getUserName();
        String userRole = preferenceManager.getUserRole();
        
        if (userId == null || userName == null || userRole == null) {
            return; // Can't log without user info
        }
        
        AuditLogEntity log = new AuditLogEntity();
        log.setLogId(UUID.randomUUID().toString());
        log.setUserId(userId);
        log.setUserName(userName);
        log.setUserRole(userRole);
        log.setUserEmail(preferenceManager.getUserEmail());
        log.setAction(action);
        log.setDescription(description);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setTimestamp(System.currentTimeMillis());
        log.setPlatform("Android");
        log.setDeviceInfo(android.os.Build.MODEL + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        
        // Save to Firebase
        if (auditLogsRef != null) {
            auditLogsRef.child(log.getLogId()).setValue(log)
                .addOnFailureListener(e -> android.util.Log.e("AuditLogger", "Failed to save log: " + e.getMessage()));
        }
    }
    
    /**
     * Log user login
     */
    public void logLogin() {
        logAction("LOGIN", "User logged in successfully", "USER", preferenceManager.getUserId());
    }
    
    /**
     * Log user registration
     */
    public void logRegistration() {
        logAction("REGISTER", "User registered a new account", "USER", preferenceManager.getUserId());
    }
    
    /**
     * Log user logout
     */
    public void logLogout() {
        logAction("LOGOUT", "User logged out", "USER", preferenceManager.getUserId());
    }
    
    /**
     * Log account deletion
     */
    public void logAccountDeleted() {
        logAction("DELETE_ACCOUNT", "User deleted their account", "USER", preferenceManager.getUserId());
    }
    
    /**
     * Log appointment creation
     */
    public void logAppointmentCreated(String appointmentId, String doctorName) {
        logAction("CREATE_APPOINTMENT", "Created appointment with " + doctorName, 
                 "APPOINTMENT", appointmentId);
    }
    
    /**
     * Log appointment update
     */
    public void logAppointmentUpdated(String appointmentId, String action, String details) {
        logAction("UPDATE_APPOINTMENT", action + ": " + details, 
                 "APPOINTMENT", appointmentId);
    }
    
    /**
     * Log profile update
     */
    public void logProfileUpdated(String field) {
        logAction("UPDATE_PROFILE", "User updated profile information: " + field, 
                 "PROFILE", preferenceManager.getUserId());
    }

    /**
     * Log settings update
     */
    public void logSettingsUpdated(String details) {
        logAction("UPDATE_SETTINGS", details, "SETTINGS", preferenceManager.getUserId());
    }

    /**
     * Log post creation
     */
    public void logPostCreated(String postId, String title, String type) {
        String role = preferenceManager.getUserRole();
        String actor = "admin".equals(role) ? "Admin" : "User";
        logAction("CREATE_POST", actor + " created " + type + " post: " + title, "POST", postId);
    }

    /**
     * Log post update
     */
    public void logPostUpdated(String postId, String title, String type) {
        String role = preferenceManager.getUserRole();
        String actor = "admin".equals(role) ? "Admin" : "User";
        logAction("UPDATE_POST", actor + " updated " + type + " post: " + title, "POST", postId);
    }

    /**
     * Log post like/unlike
     */
    public void logPostLiked(String postId, boolean isLiked, String type) {
        String action = isLiked ? "LIKE_POST" : "UNLIKE_POST";
        String description = isLiked ? "User liked a " + type + " post" : "User unliked a " + type + " post";
        logAction(action, description, "POST", postId);
    }

    /**
     * Log post comment
     */
    public void logPostCommented(String postId, String type) {
        logAction("COMMENT_POST", "User commented on a " + type + " post", "POST", postId);
    }

    /**
     * Log post share
     */
    public void logPostShared(String postId, String type) {
        logAction("SHARE_POST", "User shared a " + type + " post", "POST", postId);
    }

    /**
     * Log post deletion
     */
    public void logPostDeleted(String postId, String title, String type) {
        logAction("DELETE_POST", "Admin deleted " + type + " post: " + title, "POST", postId);
    }
    
    /**
     * Log post-related action (backwards compatibility)
     */
    @Deprecated
    public void logPostAction(String action, String postId, String type) {
        String description = "User performed " + action + " on " + type + " post";
        if (action.contains("CREATE")) description = "User created " + type + " post";
        else if (action.contains("UPDATE")) description = "User updated " + type + " post";
        else if (action.contains("DELETE")) description = "Admin deleted " + type + " post";
        
        logAction(action, description, "POST", postId);
    }

    /**
     * Log user management action
     */
    public void logUserManagement(String action, String targetUserId, String details) {
        logAction("USER_MANAGEMENT", action + ": " + details, "USER", targetUserId);
    }
    
    /**
     * Log data export
     */
    public void logDataExport(String reportType, int recordCount) {
        logAction("EXPORT_DATA", "Admin exported " + recordCount + " records for " + reportType, 
                 "REPORT", reportType);
    }

    /**
     * Log prescription issued
     */
    public void logPrescriptionIssued(String prescriptionId, String patientName) {
        logAction("PRESCRIPTION_ISSUED", "Prescription issued for patient: " + patientName, 
                 "PRESCRIPTION", prescriptionId);
    }
}

