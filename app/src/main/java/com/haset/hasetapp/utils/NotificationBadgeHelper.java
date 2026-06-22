package com.haset.hasetapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.haset.hasetapp.R;

/**
 * Utility class for managing notification badges for chat messages and general notifications
 */
public class NotificationBadgeHelper {
    
    private static final String PREFS_NAME = "notification_badges";
    private static final String TOTAL_UNREAD_KEY = "total_unread_messages";
    private static final String GENERAL_NOTIFICATIONS_KEY = "general_notifications_unread";
    private static final String TAB_APPOINTMENTS_KEY = "tab_appointments_unread";
    private static final String TAB_REMINDERS_KEY = "tab_reminders_unread";
    private static final String TAB_PAYMENTS_KEY = "tab_payments_unread";
    private static final String LAST_APP_OPEN_KEY = "last_app_open_timestamp";
    private static final String NEW_NOTIFICATIONS_COUNT_KEY = "new_notifications_since_last_open";
    
    private SharedPreferences preferences;
    
    public NotificationBadgeHelper(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // ===== App Session Badge Management =====
    
    /**
     * Called when app opens - clears new notification badge so user isn't annoyed
     * by old notifications. Only NEW notifications after this point will show.
     */
    public void onAppOpened() {
        long currentTime = System.currentTimeMillis();
        long lastOpen = preferences.getLong(LAST_APP_OPEN_KEY, 0);
        
        if (lastOpen > 0) {
            int newCount = getNewNotificationsSinceLastOpen();
            if (newCount > 0) {
                Log.d("NotificationBadgeHelper", "App opened with " + newCount + " new notifications since last open");
            }
        }
        
        preferences.edit()
            .putLong(LAST_APP_OPEN_KEY, currentTime)
            .putInt(NEW_NOTIFICATIONS_COUNT_KEY, 0)
            .apply();
        
        Log.d("NotificationBadgeHelper", "App session started, badge cleared");
    }
    
    /**
     * Get count of new notifications since last app open
     */
    public int getNewNotificationsSinceLastOpen() {
        return preferences.getInt(NEW_NOTIFICATIONS_COUNT_KEY, 0);
    }
    
    /**
     * Increment new notifications count (called when new notification arrives)
     */
    public void incrementNewNotifications() {
        int current = preferences.getInt(NEW_NOTIFICATIONS_COUNT_KEY, 0);
        preferences.edit().putInt(NEW_NOTIFICATIONS_COUNT_KEY, current + 1).apply();
    }
    
    /**
     * Get the timestamp of when app was last opened
     */
    public long getLastAppOpenTime() {
        return preferences.getLong(LAST_APP_OPEN_KEY, 0);
    }
    
    /**
     * Check if badge should be shown (only show for new notifications since last open)
     */
    public boolean shouldShowBadge() {
        int newCount = getNewNotificationsSinceLastOpen();
        return newCount > 0;
    }
    
    // ===== General Notifications =====
    
    /**
     * Get unread count for general notifications (non-chat)
     */
    public int getGeneralNotificationsUnreadCount() {
        return preferences.getInt(GENERAL_NOTIFICATIONS_KEY, 0);
    }
    
    /**
     * Set unread count for general notifications
     */
    public void setGeneralNotificationsUnreadCount(int count) {
        preferences.edit().putInt(GENERAL_NOTIFICATIONS_KEY, Math.max(0, count)).apply();
    }
    
    /**
     * Increment general notifications unread count
     */
    public void incrementGeneralNotifications() {
        int current = getGeneralNotificationsUnreadCount();
        setGeneralNotificationsUnreadCount(current + 1);
    }
    
    /**
     * Mark all general notifications as read (reset to 0)
     */
    public void markGeneralNotificationsAsRead() {
        setGeneralNotificationsUnreadCount(0);
        markAllTabsAsRead();
    }
    
    // ===== Tab-specific Badges =====
    
    /**
     * Get unread count for Appointments tab
     */
    public int getAppointmentsUnreadCount() {
        return preferences.getInt(TAB_APPOINTMENTS_KEY, 0);
    }
    
    /**
     * Set unread count for Appointments tab
     */
    public void setAppointmentsUnreadCount(int count) {
        preferences.edit().putInt(TAB_APPOINTMENTS_KEY, Math.max(0, count)).apply();
    }
    
    /**
     * Increment appointments unread count
     */
    public void incrementAppointmentsUnread() {
        int current = getAppointmentsUnreadCount();
        setAppointmentsUnreadCount(current + 1);
    }
    
    /**
     * Mark appointments as read
     */
    public void markAppointmentsAsRead() {
        setAppointmentsUnreadCount(0);
    }
    
    /**
     * Get unread count for Reminders tab
     */
    public int getRemindersUnreadCount() {
        return preferences.getInt(TAB_REMINDERS_KEY, 0);
    }
    
    /**
     * Set unread count for Reminders tab
     */
    public void setRemindersUnreadCount(int count) {
        preferences.edit().putInt(TAB_REMINDERS_KEY, Math.max(0, count)).apply();
    }
    
    /**
     * Increment reminders unread count
     */
    public void incrementRemindersUnread() {
        int current = getRemindersUnreadCount();
        setRemindersUnreadCount(current + 1);
    }
    
    /**
     * Mark reminders as read
     */
    public void markRemindersAsRead() {
        setRemindersUnreadCount(0);
    }
    
    /**
     * Get unread count for Payments tab
     */
    public int getPaymentsUnreadCount() {
        return preferences.getInt(TAB_PAYMENTS_KEY, 0);
    }
    
    /**
     * Set unread count for Payments tab
     */
    public void setPaymentsUnreadCount(int count) {
        preferences.edit().putInt(TAB_PAYMENTS_KEY, Math.max(0, count)).apply();
    }
    
    /**
     * Increment payments unread count
     */
    public void incrementPaymentsUnread() {
        int current = getPaymentsUnreadCount();
        setPaymentsUnreadCount(current + 1);
    }
    
    /**
     * Mark payments as read
     */
    public void markPaymentsAsRead() {
        setPaymentsUnreadCount(0);
    }
    
    /**
     * Get total from all tabs
     */
    public int getTotalFromAllTabs() {
        return getAppointmentsUnreadCount() + getRemindersUnreadCount() + getPaymentsUnreadCount();
    }
    
    /**
     * Mark all tabs as read
     */
    public void markAllTabsAsRead() {
        setAppointmentsUnreadCount(0);
        setRemindersUnreadCount(0);
        setPaymentsUnreadCount(0);
    }
    
    /**
     * Update message icon badge with unread count
     */
    public static void updateMessageBadge(TextView badgeView, int unreadCount) {
        if (badgeView == null) {
            Log.d("NotificationBadgeHelper", "Badge view is null");
            return;
        }
        
        Log.d("NotificationBadgeHelper", "Updating badge with count: " + unreadCount);
        
        if (unreadCount > 0) {
            badgeView.setVisibility(View.VISIBLE);
            if (unreadCount > 99) {
                badgeView.setText("99+");
            } else {
                badgeView.setText(String.valueOf(unreadCount));
            }
        } else {
            badgeView.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update the conversation item badge with unread count
     */
    public static void updateConversationBadge(TextView badgeView, int unreadCount) {
        if (badgeView == null) {
            Log.d("NotificationBadgeHelper", "Conversation badge view is null");
            return;
        }
        
        Log.d("NotificationBadgeHelper", "Updating conversation badge with count: " + unreadCount);
        
        if (unreadCount > 0) {
            badgeView.setVisibility(View.VISIBLE);
            if (unreadCount > 99) {
                badgeView.setText("99+");
            } else {
                badgeView.setText(String.valueOf(unreadCount));
            }
        } else {
            badgeView.setVisibility(View.GONE);
        }
    }
    
    /**
     * Get total unread message count
     */
    public int getTotalUnreadCount() {
        return preferences.getInt(TOTAL_UNREAD_KEY, 0);
    }
    
    /**
     * Set total unread message count
     */
    public void setTotalUnreadCount(int count) {
        preferences.edit().putInt(TOTAL_UNREAD_KEY, Math.max(0, count)).commit(); // Use commit for immediate write
        Log.d("NotificationBadgeHelper", "Total unread count set to: " + count);
    }
    
    /**
     * Increment total unread count
     */
    public void incrementUnreadCount() {
        int currentCount = getTotalUnreadCount();
        setTotalUnreadCount(currentCount + 1);
    }
    
    /**
     * Decrement total unread count
     */
    public void decrementUnreadCount() {
        int currentCount = getTotalUnreadCount();
        setTotalUnreadCount(Math.max(0, currentCount - 1));
    }
    
    /**
     * Reset total unread count (when user reads all messages)
     */
    public void resetUnreadCount() {
        setTotalUnreadCount(0);
    }
    
    /**
     * Get unread count for a specific conversation
     */
    public int getConversationUnreadCount(String conversationId) {
        return preferences.getInt("unread_" + conversationId, 0);
    }
    
    /**
     * Set unread count for a specific conversation
     */
    public void setConversationUnreadCount(String conversationId, int count) {
        preferences.edit().putInt("unread_" + conversationId, Math.max(0, count)).commit(); // Use commit for immediate write
    }
    
    /**
     * Increment unread count for a specific conversation
     */
    public void incrementConversationUnread(String conversationId) {
        int currentCount = getConversationUnreadCount(conversationId);
        setConversationUnreadCount(conversationId, currentCount + 1);
        incrementUnreadCount(); // Also increment total
    }
    
    /**
     * Mark conversation as read (reset unread count for that conversation)
     */
    public void markConversationAsRead(String conversationId) {
        int conversationCount = getConversationUnreadCount(conversationId);
        setConversationUnreadCount(conversationId, 0);
        
        // Subtract from total count
        int currentTotal = getTotalUnreadCount();
        setTotalUnreadCount(Math.max(0, currentTotal - conversationCount));
    }
}
