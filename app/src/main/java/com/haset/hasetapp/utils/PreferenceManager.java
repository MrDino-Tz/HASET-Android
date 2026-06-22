package com.haset.hasetapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    
    // Theme preference constants
    private static final String KEY_THEME = "app_theme";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    
    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    public void saveUserId(String userId) {
        editor.putString(Constants.KEY_USER_ID, userId);
        editor.apply();
    }
    
    public String getUserId() {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null);
    }
    
    public void saveUserRole(String role) {
        editor.putString(Constants.KEY_USER_ROLE, role);
        editor.apply();
    }
    
    public String getUserRole() {
        return sharedPreferences.getString(Constants.KEY_USER_ROLE, null);
    }
    
    public void saveUserName(String name) {
        editor.putString(Constants.KEY_USER_NAME, name);
        editor.apply();
    }
    
    public String getUserName() {
        return sharedPreferences.getString(Constants.KEY_USER_NAME, null);
    }
    
    public void saveUserEmail(String email) {
        editor.putString(Constants.KEY_USER_EMAIL, email);
        editor.apply();
    }
    
    public String getUserEmail() {
        return sharedPreferences.getString(Constants.KEY_USER_EMAIL, null);
    }
    
    public void saveUserPhone(String phone) {
        editor.putString(Constants.KEY_USER_PHONE, phone);
        editor.apply();
    }
    
    public String getUserPhone() {
        return sharedPreferences.getString(Constants.KEY_USER_PHONE, null);
    }
    
    public void saveProfilePhotoPath(String path) {
        editor.putString(Constants.KEY_PROFILE_PHOTO_PATH, path);
        editor.apply();
    }
    
    public String getProfilePhotoPath() {
        return sharedPreferences.getString(Constants.KEY_PROFILE_PHOTO_PATH, null);
    }
    
    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }
    
    private static final String KEY_IS_DEMO_DOCTOR = "is_demo_doctor";
    
    public void setIsDemoDoctor(boolean isDemoDoctor) {
        editor.putBoolean(KEY_IS_DEMO_DOCTOR, isDemoDoctor);
        editor.apply();
    }
    
    public boolean isDemoDoctor() {
        return sharedPreferences.getBoolean(KEY_IS_DEMO_DOCTOR, false);
    }
    
    public void clearPreferences() {
        // Preserve app-level preferences that should persist across logouts
        boolean onboardingSeen = isOnboardingSeen();
        int theme = getTheme();
        String language = getLanguage();
        boolean notificationEnabled = isNotificationEnabled();
        
        // Clear all preferences
        editor.clear();
        
        // Restore app-level preferences
        editor.putBoolean(KEY_ONBOARDING_SEEN, onboardingSeen);
        editor.putInt(KEY_THEME, theme);
        editor.putString(KEY_LANGUAGE, language);
        editor.putBoolean(KEY_NOTIFICATION_ENABLED, notificationEnabled);
        
        editor.apply();
    }
    
    // Theme preference methods
    public void setTheme(int theme) {
        editor.putInt(KEY_THEME, theme);
        editor.apply();
    }
    
    public int getTheme() {
        return sharedPreferences.getInt(KEY_THEME, THEME_SYSTEM); // Default to system theme
    }
    
    // Notification preferences
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_ARTICLE_NOTIFICATIONS = "article_notifications_enabled";
    private static final String KEY_HEALTH_TIP_NOTIFICATIONS = "health_tip_notifications_enabled";
    
    public void setNotificationEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATION_ENABLED, enabled);
        editor.apply();
    }
    
    public boolean isNotificationEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setArticleNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_ARTICLE_NOTIFICATIONS, enabled);
        editor.apply();
    }

    public boolean isArticleNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_ARTICLE_NOTIFICATIONS, true);
    }

    public void setHealthTipNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_HEALTH_TIP_NOTIFICATIONS, enabled);
        editor.apply();
    }

    public boolean isHealthTipNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_HEALTH_TIP_NOTIFICATIONS, true);
    }
    
    // Location permission preference (for doctors)
    private static final String KEY_LOCATION_ENABLED = "location_enabled";
    
    public void setLocationEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_LOCATION_ENABLED, enabled);
        editor.apply();
    }
    
    public boolean isLocationEnabled() {
        return sharedPreferences.getBoolean(KEY_LOCATION_ENABLED, true); // Default to enabled
    }
    
    // Language preferences
    private static final String KEY_LANGUAGE = "language";
    
    public void setLanguage(String language) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LANGUAGE, language);
        editor.apply();
    }
    
    public String getLanguage() {
        return sharedPreferences.getString(KEY_LANGUAGE, "en"); // Default to English code
    }
    
    // Appointment filter preference
    private static final String KEY_APPOINTMENT_FILTER_STATUS = "appointment_filter_status";
    
    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }
    
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Onboarding preference
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    public boolean isOnboardingSeen() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_SEEN, false);
    }
    public void setOnboardingSeen(boolean seen) {
        editor.putBoolean(KEY_ONBOARDING_SEEN, seen);
        editor.commit();
    }
    
    // FCM Token preference
    private static final String KEY_FCM_TOKEN = "fcm_token";
    
    public void setFCMToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }
    
    public String getFCMToken() {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null);
    }
}
