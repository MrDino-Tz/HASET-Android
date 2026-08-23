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
import com.haset.hasetapp.utils.PreferenceManager;

public class DirectReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constants.ACTION_REPLY.equals(intent.getAction())) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence(Constants.KEY_TEXT_REPLY);
                if (replyText != null) {
                    PendingResult pendingResult = goAsync();
                    processReply(context.getApplicationContext(), intent,
                            replyText.toString(), pendingResult);
                }
            }
        }
    }

    private void processReply(Context context, Intent intent, String replyText,
                              PendingResult pendingResult) {
        String recipientId = intent.getStringExtra(Constants.EXTRA_CHAT_USER_ID);
        String recipientName = intent.getStringExtra(Constants.EXTRA_CHAT_USER_NAME);
        String senderName = new PreferenceManager(context).getUserName();
        String cleanReply = replyText == null ? "" : replyText.trim();

        if (recipientId != null && !recipientId.isEmpty() && !cleanReply.isEmpty()) {
            FirebaseHelper.sendDirectReply(recipientId, senderName, recipientName, cleanReply,
                    new FirebaseHelper.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    updateNotification(context, intent.getIntExtra("notificationId", 0));
                    pendingResult.finish();
                }

                @Override
                public void onError(String error) {
                    if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                    Toast.makeText(context, R.string.failed_to_send_reply, Toast.LENGTH_SHORT).show();
                    pendingResult.finish();
                }
            });
        } else {
            pendingResult.finish();
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
