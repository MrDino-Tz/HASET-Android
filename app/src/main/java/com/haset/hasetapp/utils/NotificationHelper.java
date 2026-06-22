package com.haset.hasetapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.MainActivity;

public class NotificationHelper {
    
    private static final int WELCOME_NOTIFICATION_ID = 1001;

    
    // Notification grouping


    private static final String CHANNEL_ID_MESSAGES = "hcare_messages_channel";
    private static final String CHANNEL_NAME_MESSAGES = "HASET Messages";
    private static final String CHANNEL_DESCRIPTION_MESSAGES = "New message notifications for HASET";
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final PreferenceManager preferenceManager;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferenceManager = new PreferenceManager(context);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            // Messages Channel
            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    CHANNEL_NAME_MESSAGES,
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription(CHANNEL_DESCRIPTION_MESSAGES);
            notificationManager.createNotificationChannel(messagesChannel);
        }
    }
    


    public void showMessageNotification(String senderId, String senderName, String messageBody, String chatId) {
        if (!preferenceManager.isNotificationEnabled()) {
            return;
        }

        int notificationId = (int) System.currentTimeMillis();

        // Intent to open chat when notification is tapped
        Intent chatIntent = new Intent(context, com.haset.hasetapp.activities.ChatActivity.class);
        chatIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, senderId);
        chatIntent.putExtra(Constants.EXTRA_CHAT_USER_NAME, senderName);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent for Reply Action
        Intent replyIntent = new Intent(context, com.haset.hasetapp.receivers.DirectReplyReceiver.class);
        replyIntent.setAction(Constants.ACTION_REPLY);
        replyIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, senderId);
        replyIntent.putExtra("notificationId", notificationId);
        replyIntent.putExtra("senderId", preferenceManager.getUserId()); // Current user ID for reply context

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE // Mutable needed for remote input
        );

        // Reply Action
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_send, // Using standard icon, ensure it exists or use android.R.drawable.ic_menu_send
                "Jibu",
                replyPendingIntent)
                .addRemoteInput(new androidx.core.app.RemoteInput.Builder(Constants.KEY_TEXT_REPLY).setLabel("Jibu").build())
                .build();

        // Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.h_10_icon_notification)
                .setContentTitle(senderName)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(replyAction); // Add reply action

        notificationManager.notify(notificationId, builder.build());
    }

    public void showBigPictureNotification(String title, String message, String imageUrl, Intent intent, String channelId, int notificationId) {
        if (!preferenceManager.isNotificationEnabled()) {
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        new Thread(() -> {
            Bitmap bitmap = null;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                bitmap = getBitmapFromURL(imageUrl);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.h_10_icon_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(message));
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }

            notificationManager.notify(notificationId, builder.build());
        }).start();
    }

    private Bitmap getBitmapFromURL(String strURL) {
        try {
            java.net.URL url = new java.net.URL(strURL);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            java.io.InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error loading image: " + e.getMessage());
            return null;
        }
    }
    
    // ... rest of existing methods ...

}
