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
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.activities.DoctorWalletActivity;
import com.haset.hasetapp.activities.NotificationActivity;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.activities.ChatActivity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Notification Manager specifically for DOCTOR role
 * Handles appointment reminders, patient updates, and practice management notifications
 */
public class DoctorNotificationManager {
    
    private static final String TAG = "DoctorNotificationManager";
    
    // Notification channels
    private static final String CHANNEL_APPOINTMENTS = "doctor_appointments_channel";
    private static final String CHANNEL_PATIENT_UPDATES = "doctor_patient_updates_channel";
    private static final String CHANNEL_ONLINE_CHAT = "doctor_online_chat_channel";
    private static final String CHANNEL_TRENDING_ARTICLES = "doctor_trending_articles_channel";
    
    // SharedPreferences keys
    private static final String PREF_DOCTOR_NOTIFICATIONS = "doctor_notifications";
    private static final String PREF_APPOINTMENT_REMINDERS_ENABLED = "appointment_reminders_enabled";
    
    // Notification IDs
    private static final int APPOINTMENT_REMINDER_ID = 4002;
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences preferences;
    private final PreferenceManager preferenceManager;
    
    private boolean isAppActive = false;
    
    public DoctorNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences(PREF_DOCTOR_NOTIFICATIONS, Context.MODE_PRIVATE);
        this.preferenceManager = new PreferenceManager(context);
        
        createNotificationChannels();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Appointments channel
            NotificationChannel appointmentsChannel = new NotificationChannel(
                    CHANNEL_APPOINTMENTS,
                    "Doctor Appointments",
                    NotificationManager.IMPORTANCE_HIGH
            );
            appointmentsChannel.setDescription("Appointment reminders and updates for doctors");
            appointmentsChannel.enableLights(true);
            appointmentsChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(appointmentsChannel);
            
            // Patient updates channel
            NotificationChannel patientUpdatesChannel = new NotificationChannel(
                    CHANNEL_PATIENT_UPDATES,
                    "Patient Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            patientUpdatesChannel.setDescription("Patient-related notifications and updates");
            patientUpdatesChannel.enableLights(true);
            patientUpdatesChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(patientUpdatesChannel);

            // Online Chat channel
            NotificationChannel onlineChatChannel = new NotificationChannel(
                    CHANNEL_ONLINE_CHAT,
                    "Online Chat Appointments",
                    NotificationManager.IMPORTANCE_HIGH
            );
            onlineChatChannel.setDescription("Notifications for new online chat appointments");
            onlineChatChannel.enableLights(true);
            onlineChatChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(onlineChatChannel);

            // Trending Articles channel
            NotificationChannel trendingChannel = new NotificationChannel(
                    CHANNEL_TRENDING_ARTICLES,
                    "Trending Articles",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            trendingChannel.setDescription("Notifications about trending/popular articles");
            trendingChannel.enableLights(true);
            trendingChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(trendingChannel);
        }
    }
    
    /**
     * Called when doctor logs in - shows welcome notification and checks pending appointments
     */
    public void onDoctorLogin(String userName) {
        Log.d(TAG, "Doctor login detected: " + userName);
        
        // Check if user is actually a doctor
        if (!Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole())) {
            Log.d(TAG, "User is not a doctor, skipping notifications");
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
        
        // Check for pending appointments
        checkPendingAppointments();
        
        // Start withdrawal listener
        startListeningForWithdrawals(preferenceManager.getUserId());
        
        // Subscribe to FCM Topics
        subscribeToTopics();
    }
    
    private void subscribeToTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_DOCTORS)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to doctor topic successfully");
                    } else {
                        Log.e(TAG, "Failed to subscribe to doctor topic");
                    }
                });
                
           // Subscribe to a general topic too if you send broadcast alerts to everyone
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL);
        
        // Subscribe to articles
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ARTICLES);
        
        // Subscribe to trending articles
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_TRENDING_ARTICLES);
        
        // Check for trending articles
        checkTrendingArticles();
    }
    
    private void checkTrendingArticles() {
        com.haset.hasetapp.firebase.ArticlePostHelper.getInstance().getTrendingArticles(3, 
            new com.haset.hasetapp.firebase.ArticlePostHelper.OnCompleteListener<List<com.haset.hasetapp.database.entities.ArticlePostEntity>>() {
            @Override
            public void onSuccess(List<com.haset.hasetapp.database.entities.ArticlePostEntity> result) {
                if (result != null && !result.isEmpty()) {
                    for (com.haset.hasetapp.database.entities.ArticlePostEntity article : result) {
                        checkAndNotifyTrendingArticle(article);
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
            }
        });
    }
    
    private void checkAndNotifyTrendingArticle(com.haset.hasetapp.database.entities.ArticlePostEntity article) {
        if (article == null || article.getPostId() == null) return;
        
        SharedPreferences notifiedPrefs = context.getSharedPreferences("doctor_trending_notifications", Context.MODE_PRIVATE);
        long lastNotifiedViews = notifiedPrefs.getLong(article.getPostId() + "_views", 0);
        
        if (article.getViews() > lastNotifiedViews && article.getViews() >= Constants.TRENDING_VIEWS_THRESHOLD) {
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
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                new Random().nextInt(1000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TRENDING_ARTICLES)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description + "\n\nViews: " + article.getViews()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(("trending_" + article.getPostId()).hashCode(), builder.build());
        Log.d(TAG, "Trending article notification shown: " + article.getTitle() + " (" + article.getViews() + " views)");
    }
    
    /**
     * Check for pending appointments and notify
     */
    private void checkPendingAppointments() {
        String doctorId = preferenceManager.getUserId();
        
        FirebaseHelper.getAppointmentsByUser(doctorId, Constants.ROLE_DOCTOR, new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> appointmentEntities) {
                int pendingCount = 0;
                for (AppointmentEntity entity : appointmentEntities) {
                    if (Constants.STATUS_PENDING.equals(entity.getStatus())) {
                        pendingCount++;
                    }
                }
                
                if (pendingCount > 0) {
                    showPendingAppointmentsNotification(pendingCount);
                }
            }
            
            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
            }
        });
    }
    
    /**
     * Show notification for pending appointments
     */
    private void showPendingAppointmentsNotification(int pendingCount) {
        String message = "Una miadi " + pendingCount + " inayosubiri kukaguliwa.";
        
        // Create intent to open appointments
        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle("Miadi Inayosubiri")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\nKagua na udhinishe maombi ya miadi ya wagonjwa."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setNumber(pendingCount);
        
        // Show notification
        notificationManager.notify(APPOINTMENT_REMINDER_ID, builder.build());
        
        Log.d(TAG, "Pending appointments notification shown: " + pendingCount);
    }
    
    public void onAppBackgrounded() {
        isAppActive = false;
        Log.d(TAG, "App backgrounded");
    }
    
    public void onAppForegrounded() {
        isAppActive = true;
        Log.d(TAG, "App foregrounded");
    }
    
    /**
     * Notify about new patient appointment
     */
    public void onNewAppointment(Appointment appointment) {
        if (!hasNotificationPermission() || !preferenceManager.isNotificationEnabled()) {
            return;
        }

        String channelId = CHANNEL_PATIENT_UPDATES;
        String title = "Ombi Jipya la Miadi";
        String message = "Ombi jipya la miadi kutoka kwa " + appointment.getPatientName();
        Intent intent = new Intent(context, DashboardActivity.class);
        int notificationId = APPOINTMENT_REMINDER_ID + 1;

        if ("Online Chat".equals(appointment.getAppointmentType())) {
            channelId = CHANNEL_ONLINE_CHAT;
            title = "Miadi Mpya ya Soga Mtandaoni";
            message = "Una miadi mpya ya soga mtandaoni na " + appointment.getPatientName() + " saa " + appointment.getTime() + " tarehe " + appointment.getDate();
            intent = new Intent(context, ChatActivity.class);
            intent.putExtra(Constants.EXTRA_CHAT_USER_ID, appointment.getPatientId());
            intent.putExtra(Constants.EXTRA_CHAT_USER_NAME, appointment.getPatientName());
            intent.putExtra(Constants.EXTRA_APPOINTMENT_APPROVED_AT, System.currentTimeMillis());
            notificationId = (int) System.currentTimeMillis();
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\nMuda: " + appointment.getDate() + " " + appointment.getTime()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(notificationId, builder.build());
   
        Log.d(TAG, "New " + appointment.getAppointmentType() + " appointment notification shown");
    }
    
    public void startListeningForWithdrawals(String doctorId) {
        if (doctorId == null) return;

        Log.d(TAG, "Starting withdrawal listener for doctor: " + doctorId);
        com.google.firebase.database.DatabaseReference requestsRef = 
            FirebaseHelper.getWithdrawalRequestsRef();
        
        com.google.firebase.database.Query query = requestsRef.orderByChild("doctorId").equalTo(doctorId);
        
        query.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                for (com.google.firebase.database.DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String requestId = requestSnapshot.getKey();
                    String status = requestSnapshot.child("status").getValue(String.class);
                    String reason = requestSnapshot.child("rejectionReason").getValue(String.class);
                    Double amount = requestSnapshot.child("amount").getValue(Double.class);
                    
                    if (requestId != null && status != null && !Constants.STATUS_PENDING.equals(status)) {
                        checkAndNotifyWithdrawal(requestId, status, reason, amount != null ? amount : 0.0);
                    }
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Withdrawal listener cancelled: " + error.getMessage());
            }
        });
    }

    private void checkAndNotifyWithdrawal(String requestId, String status, String reason, double amount) {
        SharedPreferences notifiedPrefs = context.getSharedPreferences("notified_withdrawals", Context.MODE_PRIVATE);
        // Store both requestId and status to avoid re-notifying for the same status
        String key = requestId + "_" + status;
        boolean alreadyNotified = notifiedPrefs.getBoolean(key, false);

        if (!alreadyNotified) {
            showWithdrawalNotification(status, reason, amount);
            notifiedPrefs.edit().putBoolean(key, true).apply();
        }
    }

    private void showWithdrawalNotification(String status, String reason, double amount) {
        String title, message;
        int icon;
        
        if (Constants.STATUS_APPROVED.equals(status) || "completed".equals(status)) {
            title = "Umefanikiwa Kutoa Pesa! 💰";
            message = String.format("Ombi lako la kutoa TZS %,.0f limeidhinishwa.", amount);
            icon = R.drawable.haset_logo;
        } else if (com.haset.hasetapp.database.entities.WithdrawalRequest.STATUS_REJECTED.equals(status)) {
            title = "Ombi la Kutoa Pesa Limekataliwa ❌";
            message = String.format("Ombi lako la kutoa TZS %,.0f limekataliwa.", amount);
            if (reason != null && !reason.isEmpty()) {
                message += " Sababu: " + reason;
            }
            icon = R.drawable.haset_logo;
        } else {
            return;
        }

        Intent intent = new Intent(context, DoctorWalletActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PATIENT_UPDATES)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    private boolean isDoctorUser() {
        return Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole());
    }
    
    public void cleanup() {
        isAppActive = false;
    }
}
