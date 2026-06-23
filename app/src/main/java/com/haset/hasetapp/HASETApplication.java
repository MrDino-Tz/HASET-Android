package com.haset.hasetapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import android.view.WindowManager;

import com.haset.hasetapp.database.DatabaseSeeder;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import com.haset.hasetapp.utils.DoctorNotificationManager;
import com.haset.hasetapp.utils.MessageNotificationManager;
import com.haset.hasetapp.utils.PatientNotificationManager;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ThemeHelper;


public class HASETApplication extends Application implements Application.ActivityLifecycleCallbacks {
    
    private static final String TAG = "HCareApplication";
    private static HASETApplication instance;
    private PatientNotificationManager patientNotificationManager;
    private DoctorNotificationManager doctorNotificationManager;
    private int activityCount = 0;
    private boolean isAppInForeground = false;
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.haset.hasetapp.utils.LocaleHelper.onAttach(base, "en"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Apply saved theme
        PreferenceManager preferenceManager = new PreferenceManager(this);
        ThemeHelper.applyTheme(this, preferenceManager.getTheme());
        
        // Initialize Local Storage (Room Database)
        LocalStorageHelper.getInstance(this);
        
        // Initialize Patient Notification Manager
        patientNotificationManager = new PatientNotificationManager(this);
        
        // Initialize Doctor Notification Manager
        doctorNotificationManager = new DoctorNotificationManager(this);
        
        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks(this);
        
        // Seed test users for development/testing
        seedTestUsers();
        
        // Initialize Cloudinary (for media uploads)
        initializeCloudinary();

        // Initialize Message Notification Manager and start listening
        MessageNotificationManager.getInstance(this).startListening();
        
        Log.d(TAG, "HASET Application initialized with notification management");
    }
    
    /**
     * Initialize Cloudinary with credentials (for chat attachments)
     * TODO: Replace with your actual Cloudinary credentials from https://cloudinary.com/console
     */
    private void initializeCloudinary() {
        try {
            // Get credentials from strings.xml or use environment variables
            // For now, using placeholder - you need to replace these with your actual credentials
            String cloudName = getString(R.string.cloudinary_cloud_name);
            String apiKey = getString(R.string.cloudinary_api_key);
            String apiSecret = getString(R.string.cloudinary_api_secret);
            
            if (cloudName != null && !cloudName.isEmpty() && 
                apiKey != null && !apiKey.isEmpty() && 
                apiSecret != null && !apiSecret.isEmpty()) {
                CloudinaryUploadHelper.initialize(this, cloudName, apiKey, apiSecret);
                Log.d(TAG, "Cloudinary initialized successfully");
            } else {
                Log.w(TAG, "Cloudinary credentials not found. Please add them to strings.xml");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary: " + e.getMessage(), e);
        }
    }
    
    private void seedTestUsers() {
        DatabaseSeeder seeder = new DatabaseSeeder(this);
        seeder.seedTestUsers(message -> {
            Log.d(TAG, "Database Seeding: " + message);
        });
    }
    
    // Public access methods
    public static HASETApplication getInstance() {
        return instance;
    }
    
    public PatientNotificationManager getPatientNotificationManager() {
        return patientNotificationManager;
    }
    
    public DoctorNotificationManager getDoctorNotificationManager() {
        return doctorNotificationManager;
    }
    
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
    
    // Activity Lifecycle Callbacks for app state management
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Screenshot blocking is now selective - handled in individual activities
        // See: SensitiveActivityHelper.java for sensitive screen protection
        Log.d(TAG, "Activity created: " + activity.getClass().getSimpleName());
    }
    
    @Override
    public void onActivityStarted(Activity activity) {
        activityCount++;
        Log.d(TAG, "Activity started: " + activity.getClass().getSimpleName() + ", count: " + activityCount);
    }
    
    @Override
    public void onActivityResumed(Activity activity) {
        if (!isAppInForeground) {
            isAppInForeground = true;
            Log.d(TAG, "App moved to foreground");
            
            // Notify all notification managers
            if (patientNotificationManager != null) {
                patientNotificationManager.onAppForegrounded();
            }
            if (doctorNotificationManager != null) {
                doctorNotificationManager.onAppForegrounded();
            }
        }
        Log.d(TAG, "Activity resumed: " + activity.getClass().getSimpleName());
    }
    
    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "Activity paused: " + activity.getClass().getSimpleName());
    }
    
    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
        Log.d(TAG, "Activity stopped: " + activity.getClass().getSimpleName() + ", count: " + activityCount);
    }
    
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "Activity save instance state: " + activity.getClass().getSimpleName());
    }
    
    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "Activity destroyed: " + activity.getClass().getSimpleName());
        
        if (activityCount == 0 && isAppInForeground) {
            isAppInForeground = false;
            Log.d(TAG, "App moved to background");
            
            // Notify all notification managers
            if (patientNotificationManager != null) {
                patientNotificationManager.onAppBackgrounded();
            }
            if (doctorNotificationManager != null) {
                doctorNotificationManager.onAppBackgrounded();
            }
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Clean up all notification managers
        if (patientNotificationManager != null) {
            patientNotificationManager.cleanup();
        }
        if (doctorNotificationManager != null) {
            doctorNotificationManager.cleanup();
        }
        Log.d(TAG, "Application terminated");
    }
}
