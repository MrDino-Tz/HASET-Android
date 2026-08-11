package com.haset.hasetapp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.activities.ChatActivity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications for appointments, chats, and general alerts
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    
    private static final String TAG = "FCMService";
    
    // Notification Channels
    private static final String CHANNEL_APPOINTMENTS = "appointments";
    private static final String CHANNEL_MESSAGES = "messages";
    // Use a versioned channel because Android does not allow an existing
    // channel's importance or sound behavior to be raised after creation.
    private static final String CHANNEL_GENERAL = "general_alerts_v2";
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }
    
    /**
     * Called when a new FCM token is generated
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save token to preferences
        PreferenceManager preferenceManager = new PreferenceManager(this);
        preferenceManager.setFCMToken(token);
        
        // TODO: Send token to your backend server
        sendTokenToServer(token);
    }
    
    /**
     * Called when a message is received
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        // Check if message contains data payload
        boolean handledDataPayload = remoteMessage.getData().size() > 0
                && remoteMessage.getData().get("type") != null;
        if (handledDataPayload) {
            handleDataMessage(remoteMessage.getData());
        }
        
        // Check if message contains notification payload
        if (!handledDataPayload && remoteMessage.getNotification() != null) {
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }
    
    /**
     * Handle data messages
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        
        if (type == null) {
            return;
        }

        // Apply doctor-specific filtering
        PreferenceManager pm = new PreferenceManager(this);
        String userRole = pm.getUserRole();
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            if (!"appointment_reminder".equals(type) && !"chat_message".equals(type)
                    && !"new_appointment".equals(type) && !"admin_broadcast".equals(type)) {
                Log.d(TAG, "Doctor skipping notification type: " + type);
                return;
            }
        }
        
        switch (type) {
            case Constants.NOTIF_TYPE_APPOINTMENT_REMINDER:
                handleAppointmentReminder(data);
                break;
            case Constants.NOTIF_TYPE_APPOINTMENT_STATUS:
                handleAppointmentStatus(data);
                break;
            case Constants.NOTIF_TYPE_CHAT_MESSAGE:
                handleChatMessage(data);
                break;
            case Constants.NOTIF_TYPE_NEW_APPOINTMENT:
                handleNewAppointment(data);
                break;
            case Constants.NOTIF_TYPE_GENERAL:
                handleGeneralNotification(data);
                break;
            case Constants.NOTIF_TYPE_ARTICLE:
                handleArticleNotification(data);
                break;
            default:
                // Admin broadcasts and future informational types must still be
                // visible instead of being dropped by an exhaustive switch.
                handleGeneralNotification(data);
                break;
        }
    }
    
    /**
     * Handle appointment reminder notifications
     */
    private void handleAppointmentReminder(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String appointmentId = data.get("appointmentId");
        String doctorName = data.get("doctorName");
        String appointmentTime = data.get("appointmentTime");
        
        if (title == null) {
            title = getString(R.string.notification_appointment_reminder);
        }
        
        if (message == null) {
            message = String.format(getString(R.string.appointment_reminder_message), 
                doctorName, appointmentTime);
        }
        
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("navigate_to", "appointments");
        intent.putExtra("appointmentId", appointmentId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        showNotificationWithIntent(title, message, intent, CHANNEL_APPOINTMENTS, 1);
    }
    
    /**
     * Handle new appointment notifications (sent to doctor when patient books)
     */
    private void handleNewAppointment(Map<String, String> data) {
        String patientName = data.get("patientName");
        String appointmentDate = data.get("appointmentDate");
        String appointmentTime = data.get("appointmentTime");
        String appointmentId = data.get("appointmentId");

        String title = getString(R.string.notification_new_appointment);
        String message = String.format(getString(R.string.new_appointment_message),
            patientName != null ? patientName : "A patient",
            appointmentDate != null ? appointmentDate : "",
            appointmentTime != null ? appointmentTime : "");

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("navigate_to", "appointments");
        intent.putExtra("appointmentId", appointmentId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notificationId = appointmentId != null ? appointmentId.hashCode() : 10;
        showNotificationWithIntent(title, message, intent, CHANNEL_APPOINTMENTS, notificationId);
    }

    /**
     * Handle appointment status change notifications
     */
    private void handleAppointmentStatus(Map<String, String> data) {
        String status = data.get("status");
        String appointmentId = data.get("appointmentId");
        String doctorName = data.get("doctorName");
        
        String title;
        String message;
        
        if ("approved".equals(status)) {
            title = getString(R.string.notification_appointment_approved);
            message = String.format(getString(R.string.appointment_approved_message), doctorName);
        } else if ("cancelled".equals(status)) {
            title = getString(R.string.notification_appointment_cancelled);
            message = String.format(getString(R.string.appointment_cancelled_message), doctorName);
        } else {
            title = getString(R.string.appointment_status_updated);
            message = data.get("message");
        }
        
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("navigate_to", "appointments");
        intent.putExtra("appointmentId", appointmentId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        showNotificationWithIntent(title, message, intent, CHANNEL_APPOINTMENTS, 2);
    }
    
    /**
     * Handle chat message notifications
     */
    private void handleChatMessage(Map<String, String> data) {
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        String message = data.get("message");

        // Skip if currently chatting with this user
        String activeChatUser = com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).getCurrentlyChattingWith();
        if (senderId != null && senderId.equals(activeChatUser)) {
            Log.d(TAG, "Already in chat with " + senderName + ", skipping notification.");
            return;
        }
        
        String title = senderName != null ? senderName : getString(R.string.notification_new_message);
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_CHAT_USER_ID, senderId);
        intent.putExtra(Constants.EXTRA_CHAT_USER_NAME, senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int notificationId = senderId != null ? senderId.hashCode() : 3;
        showNotificationWithIntent(title, message, intent, CHANNEL_MESSAGES, notificationId);
    }
    
    /**
     * Handle general notifications
     */
    private void handleGeneralNotification(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String notificationId = data.get("notificationId");
        
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("navigate_to", "notifications");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int systemNotificationId = notificationId != null ? notificationId.hashCode() : 4;
        showNotificationWithIntent(title, message, intent, CHANNEL_GENERAL, systemNotificationId);
    }
    
    /**
     * Handle article notifications
     */
    private void handleArticleNotification(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String imageUrl = data.get("imageUrl");
        
        Intent intent = new Intent(this, com.haset.hasetapp.activities.ArticleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int notificationId = (title != null) ? title.hashCode() : 5;
        
        com.haset.hasetapp.utils.NotificationHelper helper = new com.haset.hasetapp.utils.NotificationHelper(this);
        helper.showBigPictureNotification(
            title != null ? title : "New Article", 
            message, 
            imageUrl, 
            intent, 
            CHANNEL_GENERAL, 
            notificationId
        );
    }
    
    /**
     * Show notification with custom intent
     */
    private void showNotificationWithIntent(String title, String message, Intent intent, 
                                           String channelId, int notificationId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            notificationId, 
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.h_10_icon_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(1)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }
    
    /**
     * Simple notification display
     */
    private void showNotification(String title, String message, Map<String, String> data) {
        // Apply doctor-specific filtering for notification payload messages
        PreferenceManager pm = new PreferenceManager(this);
        String userRole = pm.getUserRole();
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            String type = data.get("type");
            if (type == null || (!"appointment_reminder".equals(type)
                    && !"appointment_status".equals(type)
                    && !"chat_message".equals(type)
                    && !"new_appointment".equals(type))) {
                Log.d(TAG, "Doctor skipping simple notification");
                return;
            }
        }
        
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        showNotificationWithIntent(title, message, intent, CHANNEL_GENERAL, 0);
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                return;
            }
            
            // Appointments Channel
            NotificationChannel appointmentsChannel = new NotificationChannel(
                CHANNEL_APPOINTMENTS,
                "Appointments",
                NotificationManager.IMPORTANCE_HIGH
            );
            appointmentsChannel.setDescription("Appointment reminders and updates");
            appointmentsChannel.enableVibration(true);
            notificationManager.createNotificationChannel(appointmentsChannel);
            
            // Messages Channel
            NotificationChannel messagesChannel = new NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Chat messages and notifications");
            messagesChannel.enableVibration(true);
            notificationManager.createNotificationChannel(messagesChannel);
            
            // General Channel
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL,
                "Important information",
                NotificationManager.IMPORTANCE_HIGH
            );
            generalChannel.setDescription("Important information and messages from HASET administrators");
            generalChannel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
            );
            generalChannel.enableVibration(true);
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    /**
     * Send FCM token to backend server
     */
    private void sendTokenToServer(String token) {
        PreferenceManager preferenceManager = new PreferenceManager(this);
        String userId = preferenceManager.getUserId();

        if (userId != null && !userId.isEmpty()) {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("fcmToken")
                .setValue(token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token stored for signed-in user"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store FCM token", e));
        }
    }
}
