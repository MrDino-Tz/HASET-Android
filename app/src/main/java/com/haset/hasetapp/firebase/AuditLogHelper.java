package com.haset.hasetapp.firebase;

import android.content.Context;
import com.google.firebase.database.DatabaseReference;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.utils.PreferenceManager;
import java.util.UUID;

/**
 * Helper class to record and manage audit logs in Firebase.
 */
public class AuditLogHelper {
    private static AuditLogHelper instance;
    private DatabaseReference auditLogsRef;

    private AuditLogHelper() {
        auditLogsRef = FirebaseHelper.getInstance().getAuditLogsRef();
    }

    public static synchronized AuditLogHelper getInstance() {
        if (instance == null) {
            instance = new AuditLogHelper();
        }
        return instance;
    }

    /**
     * Record a new activity in the audit logs.
     * 
     * @param context Application context
     * @param activityType The type of activity (e.g., "LOGIN", "VIEW_POST")
     * @param description Brief description of what happened
     */
    public void logActivity(Context context, String activityType, String description) {
        PreferenceManager prefManager = new PreferenceManager(context);
        String userId = prefManager.getUserId();
        String userEmail = prefManager.getUserEmail();
        String userName = prefManager.getUserName();
        String userRole = prefManager.getUserRole();

        if (userId == null || userId.isEmpty()) return;

        String logId = UUID.randomUUID().toString();
        AuditLogEntity log = new AuditLogEntity(
            logId,
            userId,
            userEmail,
            userName,
            userRole,
            activityType,
            description,
            System.currentTimeMillis()
        );

        auditLogsRef.child(logId).setValue(log)
            .addOnFailureListener(e -> android.util.Log.e("AuditLogHelper", "Failed to record log: " + e.getMessage()));
    }

    /**
     * Specialized logging for login events (where prefs might not be set yet)
     */
    public void logLogin(String userId, String email, String name, String role, String description) {
        String logId = UUID.randomUUID().toString();
        AuditLogEntity log = new AuditLogEntity(
            logId,
            userId,
            email,
            name,
            role,
            "LOGIN",
            description,
            System.currentTimeMillis()
        );

        auditLogsRef.child(logId).setValue(log);
    }
}
