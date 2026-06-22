package com.haset.hasetapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.AdminDashboardActivity;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.UserEntity;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Notification Manager specifically for ADMIN role
 * Handles system alerts, user management notifications, and administrative reminders
 */
public class AdminNotificationManager {
    
    private static final String TAG = "AdminNotificationManager";
    
    // Notification channels
    private static final String CHANNEL_SYSTEM_ALERTS = "admin_system_alerts_channel";
    private static final String CHANNEL_USER_MANAGEMENT = "admin_user_management_channel";
    private static final String CHANNEL_ADMIN_TIPS = "admin_admin_tips_channel";
    
    // SharedPreferences keys
    private static final String PREF_ADMIN_NOTIFICATIONS = "admin_notifications";
    private static final String PREF_LAST_LOGIN_DATE = "last_login_date";
    private static final String PREF_SYSTEM_ALERTS_ENABLED = "system_alerts_enabled";
    private static final String PREF_ADMIN_TIPS_ENABLED = "admin_tips_enabled";
    private static final String PREF_LAST_ADMIN_TIP_TIME = "last_admin_tip_time";
    
    // Notification IDs
    private static final int SYSTEM_ALERT_ID = 5002;
    private static final int ADMIN_TIP_BASE_ID = 5100;
    
    // Admin tip intervals (12 hours = 43,200,000 milliseconds)
    private static final long ADMIN_TIP_INTERVAL_MS = TimeUnit.HOURS.toMillis(12);
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences preferences;
    private final PreferenceManager preferenceManager;
    private final LocalStorageHelper storageHelper;
    
    // Handler for admin tips
    private Handler adminTipHandler;
    private Runnable adminTipRunnable;
    private boolean isAppActive = false;
    
    public AdminNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences(PREF_ADMIN_NOTIFICATIONS, Context.MODE_PRIVATE);
        this.preferenceManager = new PreferenceManager(context);
        this.storageHelper = LocalStorageHelper.getInstance(context);
    }
    
    public void onAdminLogin(String userName) {}
    
    public void onAppForegrounded() {}
    
    public void onAppBackgrounded() {}
    
    public void onNewUserRegistration(UserEntity user) {}
    
    public void onSystemAlert(String title, String message) {}
    
    public void onWithdrawalRequest(String doctorName, double amount) {}
    
    /* DISABLED - All notification logic
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // System alerts channel
            NotificationChannel systemAlertsChannel = new NotificationChannel(
                    CHANNEL_SYSTEM_ALERTS,
                    "System Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            systemAlertsChannel.setDescription("Critical system alerts and notifications for administrators");
            systemAlertsChannel.enableLights(true);
            systemAlertsChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(systemAlertsChannel);
            
            // User management channel
            NotificationChannel userManagementChannel = new NotificationChannel(
                    CHANNEL_USER_MANAGEMENT,
                    "User Management",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            userManagementChannel.setDescription("User registration and management notifications");
            userManagementChannel.enableLights(true);
            userManagementChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(userManagementChannel);
            
            // Admin tips channel
            NotificationChannel adminTipsChannel = new NotificationChannel(
                    CHANNEL_ADMIN_TIPS,
                    "Admin Tips",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            adminTipsChannel.setDescription("Administrative tips and reminders for system management");
            adminTipsChannel.enableLights(true);
            adminTipsChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(adminTipsChannel);
        }
    }
    */
    
    /**
     * Called when admin logs in - shows welcome notification and admin tip
     */
    /* DISABLED - All notification logic
    public void onAdminLogin(String userName) {
        Log.d(TAG, "Admin login detected: " + userName);
        
        // Check if user is actually an admin
        if (!Constants.ROLE_ADMIN.equals(preferenceManager.getUserRole())) {
            Log.d(TAG, "User is not an admin, skipping notifications");
            return;
        }
        
        // Check notification permissions
        if (!hasNotificationPermission()) {
            Log.d(TAG, "Notification permission not granted");
            return;
        }
        
        // Check if notifications are enabled
        if (!preferenceManager.isNotificationEnabled()) {
            Log.d(TAG, "Notifications disabled in preferences");
            return;
        }
        

        // Show System status overview on login
        checkSystemStatus();
        
        // Start recurring admin tip scheduler (6 hours)
        startAdminTips();
        
        // Subscribe to FCM Topics
        subscribeToTopics();
    }
    */
    
    /* DISABLED - All notification logic
    private void subscribeToTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ADMIN)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to admin topic successfully");
                    } else {
                        Log.e(TAG, "Failed to subscribe to admin topic");
                    }
                });
        
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL);
        
        // Also subscribe to articles
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ARTICLES);
    }
    */
    

    
    /**
     * Show admin tip immediately after login
     */
    // showLoginAdminTip removed as per request to focus on system status
    
    /**
     * Check system status and notify if needed
     */
    /* DISABLED - All notification logic
    private void checkSystemStatus() {
        // Get global user statistics from Firebase
        FirebaseHelper.getAllUsers(new FirebaseHelper.OnCompleteListener<List<UserEntity>>() {
            @Override
            public void onSuccess(List<UserEntity> users) {
                int totalUsers = users.size();
                int doctors = 0;
                int patients = 0;
                
                for (UserEntity user : users) {
                    if (Constants.ROLE_DOCTOR.equals(user.getRole())) {
                        doctors++;
                    } else if (Constants.ROLE_PATIENT.equals(user.getRole())) {
                        patients++;
                    }
                }
                
                showSystemStatusNotification(totalUsers, doctors, patients);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking global system status: " + error);
                
                // Fallback to local data if Firebase fails
                storageHelper.getAllUsers(new LocalStorageHelper.OnCompleteListener<List<UserEntity>>() {
                    @Override
                    public void onSuccess(List<UserEntity> localUsers) {
                        int total = localUsers.size();
                        int drs = 0, pts = 0;
                        for (UserEntity u : localUsers) {
                            if (Constants.ROLE_DOCTOR.equals(u.getRole())) drs++;
                            else if (Constants.ROLE_PATIENT.equals(u.getRole())) pts++;
                        }
                        showSystemStatusNotification(total, drs, pts);
                    }
                    @Override
                    public void onError(String ignored) {}
                });
            }
        });
    }
    */
    
    /**
     * Show system status notification
     */
    /* DISABLED - All notification logic
    private void showSystemStatusNotification(int totalUsers, int doctors, int patients) {
        String title = "Muhtasari wa Hali ya Mfumo wa HASET";
        String message = String.format("Takwimu za Sasa: Watumiaji Jumla %d\n👨‍⚕️ Madaktari %d | 👥 Wagonjwa %d", 
                                      totalUsers, doctors, patients);
        
        // Create intent to open admin dashboard
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SYSTEM_ALERTS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle(title)
                .setContentText("Gusa kutazama takwimu za jukwaa")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(message + "\n\nJukwaa linafanya kazi kawaida. Afya ya mfumo ni bora."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(SYSTEM_ALERT_ID, builder.build());
        
        Log.d(TAG, "System status notification shown");
    }
    */
    
    /**
     * Start scheduler for admin tips every 6 hours when app is active
     */
    /* DISABLED - All notification logic
    private void startAdminTips() {
        if (!isAdminTipsEnabled()) {
            Log.d(TAG, "Admin tips disabled");
            return;
        }
        
        isAppActive = true;
        
        if (adminTipHandler == null) {
            adminTipHandler = new Handler(Looper.getMainLooper());
        }
        
        // Cancel any existing runnable
        if (adminTipRunnable != null) {
            adminTipHandler.removeCallbacks(adminTipRunnable);
        }
        
        // Create new runnable for recurring admin tips
        adminTipRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAppActive && isAdminUser()) {
                    showAdminTip();
                    
                    // Schedule next tip
                    adminTipHandler.postDelayed(this, ADMIN_TIP_INTERVAL_MS);
                }
            }
        };
        
        // Schedule first tip after 6 hours from now
        adminTipHandler.postDelayed(adminTipRunnable, ADMIN_TIP_INTERVAL_MS);
        
        Log.d(TAG, "Admin tips scheduler started");
    }
    */
    
    /**
     * Show admin tip when app is active (every 6 hours)
     */
    /* DISABLED - All notification logic
    private void showAdminTip() {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }
        
        // Check if enough time has passed since last tip
        long lastTipTime = preferences.getLong(PREF_LAST_ADMIN_TIP_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastTipTime < ADMIN_TIP_INTERVAL_MS) {
            Log.d(TAG, "Not enough time passed since last admin tip");
            return;
        }
        
        // Get admin tip
        String tip = getRandomAdminTip();
        int notificationId = ADMIN_TIP_BASE_ID + new java.util.Random().nextInt(100);
        
        // Create intent to open admin dashboard
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ADMIN_TIPS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle("Kikumbusho cha Msimamizi")
                .setContentText("Muda wa kujikagua kiutawala!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(tip))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(notificationId, builder.build());
        
        // Save last tip time
        preferences.edit()
                .putLong(PREF_LAST_ADMIN_TIP_TIME, currentTime)
                .apply();
        
        Log.d(TAG, "Admin tip shown");
    }
    */
    
    /**
     * Call this when app goes to background
     */
    /* DISABLED - All notification logic
    public void onAppBackgrounded() {
        isAppActive = false;
        
        // Stop admin tip scheduler
        if (adminTipHandler != null && adminTipRunnable != null) {
            adminTipHandler.removeCallbacks(adminTipRunnable);
        }
        
        Log.d(TAG, "App backgrounded, stopped admin tips");
    }
    */
    
    /**
     * Call this when app comes to foreground
     */
    /* DISABLED - All notification logic
    public void onAppForegrounded() {
        // Check if user is admin
        if (!Constants.ROLE_ADMIN.equals(preferenceManager.getUserRole())) {
            return;
        }
        
        // Restart admin tips
        startAdminTips();
        
        Log.d(TAG, "App foregrounded, started admin tips");
    }
    */
    
    /**
     * Notify about new user registration
     */
    /* DISABLED - All notification logic
    public void onNewUserRegistration(UserEntity user) {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }

        // Only notify for doctors and patients as per request
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
        if (!role.equals("doctor") && !role.equals("patient")) {
            return;
        }
        
        String roleDisplay = role.substring(0, 1).toUpperCase() + role.substring(1);
        String message = "Amesajiliwa " + roleDisplay + " mpya: " + user.getFullName();
        
        // Create intent to open admin dashboard
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_USER_MANAGEMENT)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle("Usajili Mpya wa " + roleDisplay)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\nBarua pepe: " + user.getEmail() + "\nHali: Inasubiri Ukaguzi"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification - using distinct ID to avoid overwrites
        notificationManager.notify((int)System.currentTimeMillis(), builder.build());
        
        Log.d(TAG, "New " + role + " registration notification shown");
    }
    */
    
    /**
     * Notify about system alerts
     */
    /* DISABLED - All notification logic
    public void onSystemAlert(String title, String message) {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }
        
        // Create intent to open admin dashboard
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SYSTEM_ALERTS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(SYSTEM_ALERT_ID + 2, builder.build());
        
        Log.d(TAG, "System alert notification shown: " + title);
    }
    */
    
    /**
     * Notify about new withdrawal request
     */
    /* DISABLED - All notification logic
    public void onWithdrawalRequest(String doctorName, double amount) {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }
        
        String title = "Ombi Mpya la Kutoa Pesa";
        String amountFormatted = String.format(java.util.Locale.getDefault(), "%,.0f TZS", amount);
        String message = doctorName + " ameomba kutoa " + amountFormatted;
        
        // Create intent to open wallet management
        Intent intent = new Intent(context, com.haset.hasetapp.activities.AdminWalletManagementActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SYSTEM_ALERTS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(message + "\n\nHali: Inasubiri Idhini\nKitendo Kinahitajika: Kagua na uchakata malipo."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(SYSTEM_ALERT_ID + 3, builder.build());
        
        Log.d(TAG, "Withdrawal request notification shown for: " + doctorName);
    }
    */
    
    // Helper methods
    
    /* DISABLED - All notification logic
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    private boolean isAdminUser() {
        return Constants.ROLE_ADMIN.equals(preferenceManager.getUserRole());
    }
    */
    

    

    
    /* DISABLED - All notification logic
    private boolean isAdminTipsEnabled() {
        return preferences.getBoolean(PREF_ADMIN_TIPS_ENABLED, true);
    }
    
    private String getCurrentDateString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        return calendar.get(java.util.Calendar.YEAR) + "-" + 
               (calendar.get(java.util.Calendar.MONTH) + 1) + "-" + 
               calendar.get(java.util.Calendar.DAY_OF_MONTH);
    }
    */
    

    
    /* DISABLED - All notification logic
    private String getRandomAdminTip() {
        String[] tips = {
            "🔐 Kagua mipangilio ya usalama na uhakikishe udhibiti sahihi wa ufikiaji upo.",
            "📊 Angalia vipimo vya utendaji wa mfumo na usuluhishe vikwazo vyovyote.",
            "💾 Hakikisha kama nakala za chelezo otomatiki zinakamilika kwa mafanikio.",
            "👥 Fuatilia mienendo ya usajili wa watumiaji kwa shughuli zozote zisizo za kawaida.",
            "🏥 Kagua maombi ya uthibitishaji wa madaktari na shughulikia maombi yanayosubiri.",
            "📱 Jaribu mifumo ya arifa kuhakikisha inafanya kazi ipasavyo.",
            "🌐 Fuatilia muda ambao seva iko mtandaoni na kasi ya majibu.",
            "📈 Chambua takwimu za ushiriki wa watumiaji na tambua fursa za kuboresha.",
            "🔧 Angalia visasisho vya mfumo na viraka vya usalama.",
            "📝 Pitia kumbukumbu za msimamizi kwa makosa au maonyo yoyote.",
            "🛡️ Fanya ukaguzi wa usalama wa ruhusa za watumiaji.",
            "📧 Angalia mifumo ya uwasilishaji wa barua pepe kwa arifa.",
            "🚀 Fuatilia utendaji wa programu na ripoti za matatizo.",
            "🎯 Pitia takwimu za matumizi ya jukwaa na mielekeo.",
            "⚡ Boresha maswali ya hifadhidata kwa utendaji bora."
        };
        
        return tips[new java.util.Random().nextInt(tips.length)];
    }
    */
    
    /* DISABLED - All notification logic
    private void initializeAdminTipScheduler() {
        // Will be initialized when admin logs in
    }
    */
    
    /**
     * Enable/disable admin tips
     */
    /* DISABLED - All notification logic
    public void setAdminTipsEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(PREF_ADMIN_TIPS_ENABLED, enabled)
                .apply();
        
        if (enabled && isAppActive) {
            startAdminTips();
        } else if (!enabled && adminTipHandler != null) {
            adminTipHandler.removeCallbacks(adminTipRunnable);
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (adminTipHandler != null && adminTipRunnable != null) {
            adminTipHandler.removeCallbacks(adminTipRunnable);
        }
        isAppActive = false;
    }

}
