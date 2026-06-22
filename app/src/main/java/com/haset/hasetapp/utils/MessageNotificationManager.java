package com.haset.hasetapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.haset.hasetapp.models.Conversation;

public class MessageNotificationManager {
    private static final String TAG = "MessageNotifManager";
    private static MessageNotificationManager instance;
    private final Context context;
    private final FirebaseHelper firebaseHelper;
    private final PreferenceManager preferenceManager;
    private final NotificationHelper notificationHelper;
    private ChildEventListener conversationListener;
    private String currentUserId;
    private String currentlyChattingWithUserId = null;

    private MessageNotificationManager(Context context) {
        this.context = context;
        this.firebaseHelper = FirebaseHelper.getInstance();
        this.preferenceManager = new PreferenceManager(context);
        this.notificationHelper = new NotificationHelper(context);
    }

    public static synchronized MessageNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new MessageNotificationManager(context.getApplicationContext());
        }
        return instance;
    }

    public void startListening() {
        currentUserId = preferenceManager.getUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.d(TAG, "No user logged in, cannot listen for messages.");
            return;
        }

        if (conversationListener != null) {
            // Already listening
            return;
        }

        Log.d(TAG, "Starting to listen for message notifications for user: " + currentUserId);

        conversationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Initial load or new conversation. 
                // We typically don't want to notify for all historical conversations on startup.
                // However, if a new conversation starts (new child added) while app is running, we might.
                // For simplicity, we can treat ChildAdded similar to ChildChanged but check timestamp validity.
                processConversationUpdate(snapshot, true);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                processConversationUpdate(snapshot, false);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Conversation listener cancelled: " + error.getMessage());
            }
        };

        FirebaseHelper.getUserConversationsRef().child(currentUserId).addChildEventListener(conversationListener);
    }

    public void stopListening() {
        if (conversationListener != null && currentUserId != null) {
            FirebaseHelper.getUserConversationsRef().child(currentUserId).removeEventListener(conversationListener);
            conversationListener = null;
            Log.d(TAG, "Stopped listening for message notifications.");
        }
    }

    public void setCurrentlyChattingWith(String userId) {
        this.currentlyChattingWithUserId = userId;
        Log.d(TAG, "User currently chatting with: " + userId);
    }

    public String getCurrentlyChattingWith() {
        return currentlyChattingWithUserId;
    }

    private void processConversationUpdate(DataSnapshot snapshot, boolean isInitialLoad) {
        try {
            Conversation conversation = snapshot.getValue(Conversation.class);
            if (conversation == null) return;

            // Important: We only notify if the last message was NOT sent by the current user
            String lastSenderId = conversation.getLastMessageSenderId();
            
            // If sender ID is missing (legacy data) or equals current user, don't notify
            if (lastSenderId == null || lastSenderId.equals(currentUserId)) {
                return;
            }

            // Check if we are currently looking at this chat
            String otherUserId = snapshot.getKey(); // The key is the other user's ID
            if (otherUserId != null && otherUserId.equals(currentlyChattingWithUserId)) {
                return; // Don't notify if chat is open
            }

            // 10 second threshold for "new" messages to avoid notifying on stale data 
            // especially during initial load (ChildAdded)
            long timeDiff = System.currentTimeMillis() - conversation.getLastMessageTimestamp();
            if (timeDiff > 10000) { // older than 10 seconds
               return; 
            }

            // Trigger Notification
            notificationHelper.showMessageNotification(
                    lastSenderId,  // The actual sender of the message
                    conversation.getOtherUserName(),  // Sender's name
                    conversation.getLastMessage(),  // Message content
                    currentUserId + "_" + otherUserId  // Chat room ID
            );

        } catch (Exception e) {
            Log.e(TAG, "Error processing conversation update", e);
        }
    }
}
