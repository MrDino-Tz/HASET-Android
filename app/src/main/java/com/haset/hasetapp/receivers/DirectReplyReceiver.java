package com.haset.hasetapp.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.haset.hasetapp.R;

import androidx.core.app.RemoteInput;

import com.haset.hasetapp.database.entities.MessageEntity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.FirebaseHelper;

public class DirectReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constants.ACTION_REPLY.equals(intent.getAction())) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence(Constants.KEY_TEXT_REPLY);
                if (replyText != null) {
                    processReply(context, intent, replyText.toString());
                }
            }
        }
    }

    private void processReply(Context context, Intent intent, String replyText) {
        String recipientId = intent.getStringExtra(Constants.EXTRA_CHAT_USER_ID);
        String senderId = intent.getStringExtra("senderId"); // We might need to pass this or get from prefs
        
        // If senderId is missing (e.g. app killed), try getting from prefs
        if (senderId == null) {
            // Ideally use PreferenceManager, but here we can try FirebaseHelper if initialized
             senderId = FirebaseHelper.getInstance().getCurrentUserId();
        }

        if (recipientId != null && senderId != null) {
            MessageEntity message = new MessageEntity();
            message.setSenderId(senderId);
            message.setReceiverId(recipientId);
            message.setMessage(replyText);
            message.setTimestamp(System.currentTimeMillis());
            message.setType("text");
            message.setRead(false);

            // Send message via FirebaseHelper (make sure to use the static context or similar)
            FirebaseHelper.sendMessage(message, new FirebaseHelper.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Update notification to show "Replied" or cancel it
                    updateNotification(context, intent.getIntExtra("notificationId", 0));
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(context, R.string.failed_to_send_reply, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateNotification(Context context, int notificationId) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        
        // Optionally, you could update the notification to show a spinner or "Sent" status logic
        // But canceling is standard behavior after a reply is processed
    }
}
