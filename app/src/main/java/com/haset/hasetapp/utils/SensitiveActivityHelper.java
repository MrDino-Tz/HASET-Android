package com.haset.hasetapp.utils;

import android.app.Activity;
import android.view.WindowManager;

/**
 * Helper class for selective screenshot blocking.
 * 
 * Screenshots are blocked only in SENSITIVE areas to protect user privacy
 * while allowing screenshots in PUBLIC content areas.
 * 
 * SENSITIVE (Screenshots Blocked):
 * - Payment screens (financial data)
 * - Chat conversations (private messages)
 * - Prescription details (medical data)
 * - Personal health information
 * 
 * PUBLIC (Screenshots Allowed):
 * - Articles & News
 * - Doctor profiles
 * - Health tips
 * - Appointment confirmations
 * - Public announcements
 */
public class SensitiveActivityHelper {

    /**
     * Block screenshots for sensitive activities
     * Call this in onCreate() of sensitive activities
     */
    public static void blockScreenshots(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Allow screenshots for public content
     * Call this in onCreate() if you want to ensure screenshots are allowed
     */
    public static void allowScreenshots(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Check if screenshots are blocked for an activity
     */
    public static boolean areScreenshotsBlocked(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            int flags = activity.getWindow().getAttributes().flags;
            return (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
        }
        return false;
    }
}
