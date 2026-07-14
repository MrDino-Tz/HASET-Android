package com.haset.hasetapp.utils;

import android.app.AlarmManager;
import android.app.Notification;
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

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.receivers.HealthTipsReceiver;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.haset.hasetapp.database.entities.ArticlePostEntity;

/**
 * Enhanced notification manager specifically for patient notifications
 * Handles recurring health tips
 */
public class PatientNotificationManager {
    
    private static final String TAG = "PatientNotificationManager";
    
    // Notification channels

    private static final String CHANNEL_HEALTH_TIPS = "patient_health_tips_channel";
    private static final String CHANNEL_APPOINTMENTS = "patient_appointments_channel";
    private static final String CHANNEL_TRENDING_ARTICLES = "patient_trending_articles_channel";
    
    // SharedPreferences keys
    private static final String PREF_PATIENT_NOTIFICATIONS = "patient_notifications";
    private static final String PREF_NOTIFIED_APPOINTMENTS = "notified_appointments"; // Track already notified approvals
    private static final String PREF_NOTIFIED_PRESCRIPTIONS = "notified_prescriptions"; // Track already notified prescriptions

    private static final String PREF_LAST_HEALTH_TIP_TIME = "last_health_tip_time";
    private static final String PREF_HEALTH_TIPS_ENABLED = "health_tips_enabled";
    private static final String PREF_APP_ACTIVE_TIPS_ENABLED = "app_active_tips_enabled";
    
    // Notification IDs

    private static final int LOGIN_HEALTH_TIP_ID = 3002;
    private static final int APP_ACTIVE_TIP_BASE_ID = 3100;
    
    // Health tip intervals (12 hours = 43,200,000 milliseconds)
    private static final long HEALTH_TIP_INTERVAL_MS = TimeUnit.HOURS.toMillis(12);
    private static final long HEALTH_TIP_INTERVAL_APP_ACTIVE_MS = TimeUnit.HOURS.toMillis(12);
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences preferences;
    private final PreferenceManager preferenceManager;
    private final HealthTipsHelper healthTipsHelper;
    private final NotificationHelper notificationHelper;
    
    // Handler for app-active health tips
    private Handler healthTipHandler;
    private Runnable healthTipRunnable;
    private boolean isAppActive = false;
    
    public PatientNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences(PREF_PATIENT_NOTIFICATIONS, Context.MODE_PRIVATE);
        this.preferenceManager = new PreferenceManager(context);
        this.healthTipsHelper = new HealthTipsHelper(context);
        this.notificationHelper = new NotificationHelper(context);
        
        createNotificationChannels();
        initializeHealthTipScheduler();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            
            // Health tips channel
            NotificationChannel healthTipsChannel = new NotificationChannel(
                    CHANNEL_HEALTH_TIPS,
                    "Patient Health Tips",
                    NotificationManager.IMPORTANCE_HIGH
            );
            healthTipsChannel.setDescription("Health tips and wellness reminders for patients");
            healthTipsChannel.enableLights(true);
            healthTipsChannel.setLightColor(android.graphics.Color.BLUE);
            healthTipsChannel.enableVibration(true);
            healthTipsChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(healthTipsChannel);

            // Appointment updates channel
            NotificationChannel appointmentsChannel = new NotificationChannel(
                    CHANNEL_APPOINTMENTS,
                    "Appointments",
                    NotificationManager.IMPORTANCE_HIGH
            );
            appointmentsChannel.setDescription("Notifications about your appointment status");
            appointmentsChannel.enableLights(true);
            appointmentsChannel.setLightColor(android.graphics.Color.GREEN);
            appointmentsChannel.enableVibration(true);
            appointmentsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(appointmentsChannel);

            // Trending Articles channel
            NotificationChannel trendingChannel = new NotificationChannel(
                    CHANNEL_TRENDING_ARTICLES,
                    "Trending Articles",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            trendingChannel.setDescription("Notifications about trending/popular articles");
            trendingChannel.enableLights(true);
            trendingChannel.setLightColor(android.graphics.Color.GREEN);
            trendingChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(trendingChannel);
        }
    }
    
    /**
     * Called when patient logs in - shows welcome notification and immediate health tip
     */
    public void onPatientLogin(String userName) {
        Log.d(TAG, "Patient login detected: " + userName);
        
        // Check if user is actually a patient
        if (!Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            Log.d(TAG, "User is not a patient, skipping notifications");
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
        

        
        // Show immediate health tip on login
        showLoginHealthTip();
        
        // Start appointment approval listener
        startListeningForApprovals(preferenceManager.getUserId());
        
        // Start prescription listener
        startListeningForPrescriptions(preferenceManager.getUserId());
        
        // Start app-active health tip scheduler
        startAppActiveHealthTips();
        
        // Start article listener
        startListeningForArticles();
        
        // Subscribe to FCM Topics (Articles and All Users)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ARTICLES)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Subscribed to articles topic");
                }
            });
            
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Subscribed to all_users topic");
                }
            });
        
        // Subscribe to trending articles topic
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_TRENDING_ARTICLES)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Subscribed to trending articles topic");
                }
            });
        
        // Check for trending articles
        checkTrendingArticles();
    }
    
    private void checkTrendingArticles() {
        com.haset.hasetapp.firebase.ArticlePostHelper.getInstance().getTrendingArticles(5, 
            new com.haset.hasetapp.firebase.ArticlePostHelper.OnCompleteListener<List<com.haset.hasetapp.database.entities.ArticlePostEntity>>() {
            @Override
            public void onSuccess(List<ArticlePostEntity> result) {
                if (result != null && !result.isEmpty()) {
                    for (com.haset.hasetapp.database.entities.ArticlePostEntity article : result) {
                        checkAndNotifyTrendingArticle(article);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching trending articles: " + error);
            }
        });
    }
    
    private void checkAndNotifyTrendingArticle(com.haset.hasetapp.database.entities.ArticlePostEntity article) {
        if (article == null || article.getPostId() == null) return;
        
        SharedPreferences notifiedPrefs = context.getSharedPreferences(PREF_PATIENT_NOTIFICATIONS + "_trending", Context.MODE_PRIVATE);
        long lastNotifiedViews = notifiedPrefs.getLong(article.getPostId() + "_views", 0);
        
        if (article.getViews() > lastNotifiedViews && article.getViews() >= com.haset.hasetapp.utils.Constants.TRENDING_VIEWS_THRESHOLD) {
            showTrendingArticleNotification(article);
            notifiedPrefs.edit().putLong(article.getPostId() + "_views", article.getViews()).apply();
        }
    }
    
    private void showTrendingArticleNotification(com.haset.hasetapp.database.entities.ArticlePostEntity article) {
        String title = "Trending Article: " + article.getTitle();
        String description = article.getDescription() != null ? article.getDescription() : "";
        String summary = description.length() > 100 ? description.substring(0, 97) + "..." : description;
        
        Intent intent = new Intent(context, com.haset.hasetapp.activities.ArticleActivity.class);
        intent.putExtra("article_id", article.getPostId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        notificationHelper.showBigPictureNotification(title, summary, article.getImageUrl(), intent, CHANNEL_TRENDING_ARTICLES, ("patient_trending_" + article.getPostId()).hashCode());
        Log.d(TAG, "Trending article notification shown: " + article.getTitle() + " (" + article.getViews() + " views)");
    }

    public void startListeningForArticles() {
        Log.d(TAG, "Starting article listener");
        // Use "article_posts" node as it is used for articles in the app
        com.google.firebase.database.DatabaseReference articlesRef = 
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("article_posts");
        
        articlesRef.addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                com.haset.hasetapp.database.entities.ArticlePostEntity article = 
                    snapshot.getValue(com.haset.hasetapp.database.entities.ArticlePostEntity.class);
                if (article != null && "published".equalsIgnoreCase(article.getStatus())) {
                    checkAndNotifyArticle(article);
                }
            }

            @Override
            public void onChildChanged(@NonNull com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                com.haset.hasetapp.database.entities.ArticlePostEntity article = 
                    snapshot.getValue(com.haset.hasetapp.database.entities.ArticlePostEntity.class);
                if (article != null && "published".equalsIgnoreCase(article.getStatus())) {
                    checkAndNotifyArticle(article);
                }
            }

            @Override
            public void onChildRemoved(@NonNull com.google.firebase.database.DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Article listener cancelled: " + error.getMessage());
            }
        });
    }

    private void checkAndNotifyArticle(com.haset.hasetapp.database.entities.ArticlePostEntity article) {
        if (article == null || article.getPostId() == null) return;
        
        SharedPreferences notifiedPrefs = context.getSharedPreferences(PREF_PATIENT_NOTIFICATIONS + "_articles", Context.MODE_PRIVATE);
        boolean alreadyNotified = notifiedPrefs.getBoolean(article.getPostId(), false);

        if (!alreadyNotified) {
            // Only notify if created recently (e.g. within last 24 hours) to avoid back-filling old articles
            long ageLimit = TimeUnit.HOURS.toMillis(24);
            if (System.currentTimeMillis() - article.getCreatedAt() < ageLimit) {
                showArticleNotification(article);
                notifiedPrefs.edit().putBoolean(article.getPostId(), true).apply();
            }
        }
    }

    private void showArticleNotification(com.haset.hasetapp.database.entities.ArticlePostEntity article) {
        if (article == null) return;
        
        String title = "New Article: " + article.getTitle();
        String description = article.getDescription() != null ? article.getDescription() : "";
        String summary = description;
        if (summary.length() > 100) {
            summary = summary.substring(0, 97) + "...";
        }
        
        // Intent to open ArticleActivity
        Intent intent = new Intent(context, com.haset.hasetapp.activities.ArticleActivity.class);
        intent.putExtra("article_id", article.getPostId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        notificationHelper.showBigPictureNotification(title, summary, article.getImageUrl(), intent, CHANNEL_HEALTH_TIPS, article.getPostId().hashCode());
        Log.d(TAG, "Article notification shown: " + article.getTitle());
    }
    

    
    /**
     * Show health tip immediately after login
     */
    private void showLoginHealthTip() {
        String[] loginTips = {
            "🌅 Habari ya asubuhi! Anza siku yako kwa glasi ya maji na dakika 5 za kujinyoosha.",
            "💧 Kumbuka kunywa maji! Lengo ni glasi 8 za maji kutwa nzima.",
            "🥗 Kifungua kinywa chenye afya humpa nguvu mwili na akili yako kwa siku nzima.",
            "🚶‍♀️ Chukua matembezi mafupi leo - hata dakika 10 zinaweza kuboresha hisia na nguvu.",
            "🧘 Vuta pumzi ndefu kwa dakika 2 kupunguza msongo na kuongeza umakini.",
            "🍎 Tufaha moja kwa siku humweka daktari mbali! Chagua matunda freshi kwa vitafunwa.",
            "😴 Usingizi mzuri ni muhimu kwa afya. Lenga masaa 7-9 usiku huu.",
            "📱 Pumzika mara kwa mara kutoka kwenye skrini ili kupumzisha macho na akili.",
            "💊 Usisahau dawa ulizoandikiwa - weka vikumbusho ikiwa inahitajika.",
            "🏥 Uchunguzi wa kawaida husaidia kuzuia matatizo ya afya kabla hayajawa makubwa."
        };
        
        Random random = new Random();
        String tip = loginTips[random.nextInt(loginTips.length)];
        
        // Create intent to open dashboard
        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_HEALTH_TIPS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle("Dokezo la Afya la Sasa")
                .setContentText("Hiki hapa kikumbusho chako cha afya!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(tip))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(LOGIN_HEALTH_TIP_ID, builder.build());
        
        Log.d(TAG, "Login health tip shown");
    }
    
    /**
     * Start scheduler for health tips every 3 hours when app is active
     */
    private void startAppActiveHealthTips() {
        if (!isAppActiveHealthTipsEnabled()) {
            Log.d(TAG, "App-active health tips disabled");
            return;
        }
        
        isAppActive = true;
        
        if (healthTipHandler == null) {
            healthTipHandler = new Handler(Looper.getMainLooper());
        }
        
        // Cancel any existing runnable
        if (healthTipRunnable != null) {
            healthTipHandler.removeCallbacks(healthTipRunnable);
        }
        
        // Create new runnable for recurring health tips
        healthTipRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAppActive && isPatientUser()) {
                    showAppActiveHealthTip();
                    
                    // Schedule next tip
                    healthTipHandler.postDelayed(this, HEALTH_TIP_INTERVAL_APP_ACTIVE_MS);
                }
            }
        };
        
        // Schedule first tip after 3 hours from now
        healthTipHandler.postDelayed(healthTipRunnable, HEALTH_TIP_INTERVAL_APP_ACTIVE_MS);
        
        Log.d(TAG, "App-active health tips scheduler started");
    }
    
    /**
     * Show health tip when app is active (every 3 hours)
     */
    private void showAppActiveHealthTip() {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }
        
        // Check if enough time has passed since last tip
        long lastTipTime = preferences.getLong(PREF_LAST_HEALTH_TIP_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastTipTime < HEALTH_TIP_INTERVAL_APP_ACTIVE_MS) {
            Log.d(TAG, "Not enough time passed since last health tip");
            return;
        }
        
        // Get health tip from HealthTipsHelper
        String tip = getRandomHealthTip();
        int notificationId = APP_ACTIVE_TIP_BASE_ID + new Random().nextInt(100);
        
        // Create intent to open dashboard
        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_HEALTH_TIPS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle("Kikumbusho cha Afya")
                .setContentText("Muda wako wa kujijali kiafya!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(tip))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(notificationId, builder.build());
        
        // Save last tip time
        preferences.edit()
                .putLong(PREF_LAST_HEALTH_TIP_TIME, currentTime)
                .apply();
        
        Log.d(TAG, "App-active health tip shown");
    }
    
    /**
     * Call this when app goes to background
     */
    public void onAppBackgrounded() {
        isAppActive = false;
        
        // Stop app-active health tip scheduler
        if (healthTipHandler != null && healthTipRunnable != null) {
            healthTipHandler.removeCallbacks(healthTipRunnable);
        }
        
        // Resume regular scheduled health tips via AlarmManager
        healthTipsHelper.scheduleDailyHealthTips();
        
        Log.d(TAG, "App backgrounded, stopped app-active health tips");
    }
    
    /**
     * Call this when app comes to foreground
     */
    public void onAppForegrounded() {
        // Check if user is patient
        if (!Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            return;
        }
        
        // Restart app-active health tips
        startAppActiveHealthTips();
        
        Log.d(TAG, "App foregrounded, started app-active health tips");
    }
    
    // Helper methods
    
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    private boolean isPatientUser() {
        return Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole());
    }
    

    

    
    private boolean isAppActiveHealthTipsEnabled() {
        return preferences.getBoolean(PREF_APP_ACTIVE_TIPS_ENABLED, true);
    }
    
    private String getCurrentDateString() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
    }
    

    
    private String getRandomHealthTip() {
        String[] tips = {
            "💧 Kaa na maji mwilini! Kunywa glasi ya maji sasa.",
            "🚶‍♀️ Simama na ujinyooshe kwa dakika 2.",
            "👀 Pumzisha macho yako kwenye skrini kwa sekunde 20.",
            "🧘 Vuta pumzi 3 ndefu ili kupumzisha akili yako.",
            "🍎 Chukua kitafunwa chenye afya ikiwa unahisi njaa.",
            "😊 Tabasamu! Inaweza kuboresha hisia zako mara moja.",
            "📱 Weka simu yako chini kwa mapumziko ya dakika 5.",
            "🎵 Sikiliza wimbo uupendao ili kuboresha hisia zako.",
            "🌿 Fungua dirisha upate hewa safi.",
            "✨ Unafanya vizuri! Endelea kujijali."
        };
        
        Random random = new Random();
        return tips[random.nextInt(tips.length)];
    }
    
    private void initializeHealthTipScheduler() {
        // This will be called when app starts
        // We'll start the scheduler when user logs in
    }
    
    /**
     * Enable/disable app-active health tips
     */
    public void setAppActiveHealthTipsEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(PREF_APP_ACTIVE_TIPS_ENABLED, enabled)
                .apply();
        
        if (enabled && isAppActive) {
            startAppActiveHealthTips();
        } else if (!enabled && healthTipHandler != null) {
            healthTipHandler.removeCallbacks(healthTipRunnable);
        }
    }
    
    public void startListeningForApprovals(String patientId) {
        if (patientId == null) return;

        Log.d(TAG, "Starting approval listener for patient: " + patientId);
        com.google.firebase.database.DatabaseReference appointmentsRef = 
            com.haset.hasetapp.utils.FirebaseHelper.getAppointmentsRef();
        
        com.google.firebase.database.Query query = appointmentsRef.orderByChild("patientId").equalTo(patientId);
        
        query.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                for (com.google.firebase.database.DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    String status = appointmentSnapshot.child("status").getValue(String.class);
                    String appointmentId = appointmentSnapshot.getKey();
                    String doctorName = appointmentSnapshot.child("doctorName").getValue(String.class);
                    String type = appointmentSnapshot.child("appointmentType").getValue(String.class);

                    if (Constants.STATUS_APPROVED.equalsIgnoreCase(status)) {
                        String doctorId = appointmentSnapshot.child("doctorId").getValue(String.class);
                        checkAndNotifyApproval(appointmentId, doctorName, doctorId, type);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Approval listener cancelled: " + error.getMessage());
            }
        });
    }

    private void checkAndNotifyApproval(String appointmentId, String doctorName, String doctorId, String type) {
        // Use a separate set of prefs to track notified appointments to avoid spamming on every data change
        SharedPreferences notifiedPrefs = context.getSharedPreferences(PREF_NOTIFIED_APPOINTMENTS, Context.MODE_PRIVATE);
        boolean alreadyNotified = notifiedPrefs.getBoolean(appointmentId, false);

        if (!alreadyNotified) {
            showApprovalNotification(doctorName, doctorId, type);
            notifiedPrefs.edit().putBoolean(appointmentId, true).apply();
        }
    }

    private void showApprovalNotification(String doctorName, String doctorId, String type) {
        String title = "Miadi Imeidhinishwa! ✅";
        String message = "Dkt. " + doctorName + " ameidhinisha " + (type != null ? type : "miadi") + " yako. Unaweza kuanza kikao cha mazungumzo sasa.";
        
        // Main intent to open Dashboard (fallback)
        Intent mainIntent = new Intent(context, DashboardActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context,
                new Random().nextInt(1000),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action intent to open ChatActivity directly
        Intent chatIntent = new Intent(context, com.haset.hasetapp.activities.ChatActivity.class);
        chatIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, doctorId);
        chatIntent.putExtra(Constants.EXTRA_CHAT_USER_NAME, doctorName);
        chatIntent.putExtra(Constants.EXTRA_APPOINTMENT_APPROVED_AT, System.currentTimeMillis());
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent chatPendingIntent = PendingIntent.getActivity(
                context,
                new Random().nextInt(1000),
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS)
                .setSmallIcon(R.drawable.logo_v1_notify)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(chatPendingIntent)
                .addAction(R.drawable.messages_24, "Soga sasa", chatPendingIntent);

        notificationManager.notify(new Random().nextInt(10000), builder.build());
        Log.d(TAG, "Approval notification shown with chat action for doctor: " + doctorName);
    }

    public void startListeningForPrescriptions(String patientId) {
        if (patientId == null) return;

        Log.d(TAG, "Starting prescription listener for patient: " + patientId);
        com.google.firebase.database.DatabaseReference prescriptionsRef = 
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("prescriptions");
        
        com.google.firebase.database.Query query = prescriptionsRef.orderByChild("patientId").equalTo(patientId);
        
        query.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                for (com.google.firebase.database.DataSnapshot prescriptionSnapshot : snapshot.getChildren()) {
                    String prescriptionId = prescriptionSnapshot.getKey();
                    String doctorName = prescriptionSnapshot.child("doctorName").getValue(String.class);
                    
                    if (prescriptionId != null) {
                        checkAndNotifyPrescription(prescriptionId, doctorName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Prescription listener cancelled: " + error.getMessage());
            }
        });
    }

    private void checkAndNotifyPrescription(String prescriptionId, String doctorName) {
        SharedPreferences notifiedPrefs = context.getSharedPreferences(PREF_NOTIFIED_PRESCRIPTIONS, Context.MODE_PRIVATE);
        boolean alreadyNotified = notifiedPrefs.getBoolean(prescriptionId, false);

        if (!alreadyNotified) {
            showPrescriptionNotification(doctorName);
            notifiedPrefs.edit().putBoolean(prescriptionId, true).apply();
        }
    }

    private void showPrescriptionNotification(String doctorName) {
        String title = "Maelekezo Mapya ya Dawa! 💊";
        String message = "Dkt. " + doctorName + " amekupa maelekezo mapya ya dawa. Angalia maelezo katika sehemu ya Maelekezo ya Dawa.";
        
        // Intent to open Dashboard
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                new Random().nextInt(1000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(new Random().nextInt(10000), builder.build());
        Log.d(TAG, "Prescription notification shown for doctor: " + doctorName);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (healthTipHandler != null && healthTipRunnable != null) {
            healthTipHandler.removeCallbacks(healthTipRunnable);
        }
        isAppActive = false;
    }
}
