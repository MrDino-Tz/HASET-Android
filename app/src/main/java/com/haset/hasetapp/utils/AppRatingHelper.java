package com.haset.hasetapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.haset.hasetapp.R;

public class AppRatingHelper {
    private static final String TAG = "AppRatingHelper";
    private static final String PREF_NAME = "app_rating_prefs";
    private static final String KEY_RATING_REQUESTED = "rating_requested";
    private static final String KEY_LAUNCH_COUNT = "launch_count";
    private static final String KEY_FIRST_LAUNCH_TIME = "first_launch_time";
    private static final int LAUNCHES_BEFORE_PROMPT = 5;
    private static final long DAYS_BEFORE_PROMPT = 3;

    private final Activity activity;
    private final SharedPreferences prefs;

    public AppRatingHelper(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
    }

    public void initialize() {
        incrementLaunchCount();
    }

    public void checkAndShowRating(RatingCallback callback) {
        if (isRatingRequested()) {
            Log.d(TAG, "Rating already requested, skipping");
            if (callback != null) callback.onRatingComplete(false);
            return;
        }

        if (!shouldShowRating()) {
            Log.d(TAG, "Conditions not met for rating prompt");
            if (callback != null) callback.onRatingComplete(false);
            return;
        }

        showRatingDialog(callback);
    }

    public void showRatingDialog(RatingCallback callback) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.rate_haset_title))
            .setMessage(activity.getString(R.string.rate_haset_message))
            .setPositiveButton("Rate Now", (dialog, which) -> {
                openPlayStore();
                markRatingRequested();
                if (callback != null) callback.onRatingComplete(true);
            })
            .setNegativeButton("Later", (dialog, which) -> {
                if (callback != null) callback.onRatingComplete(false);
                dialog.dismiss();
            })
            .setNeutralButton("No Thanks", (dialog, which) -> {
                markRatingRequested();
                if (callback != null) callback.onRatingComplete(false);
                dialog.dismiss();
            })
            .setCancelable(false)
            .show();
    }

    private void openPlayStore() {
        try {
            String packageName = activity.getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=" + packageName));
            
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
            } else {
                intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                activity.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Play Store: " + e.getMessage());
        }
    }

    private void incrementLaunchCount() {
        long firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, 0);
        if (firstLaunchTime == 0) {
            firstLaunchTime = System.currentTimeMillis();
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, firstLaunchTime).apply();
        }

        int launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0);
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount + 1).apply();
    }

    private boolean shouldShowRating() {
        int launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0);
        long firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis());

        long daysSinceFirstLaunch = (System.currentTimeMillis() - firstLaunchTime) / (1000 * 60 * 60 * 24);

        return launchCount >= LAUNCHES_BEFORE_PROMPT && daysSinceFirstLaunch >= DAYS_BEFORE_PROMPT;
    }

    private boolean isRatingRequested() {
        return prefs.getBoolean(KEY_RATING_REQUESTED, false);
    }

    private void markRatingRequested() {
        prefs.edit().putBoolean(KEY_RATING_REQUESTED, true).apply();
    }

    public void resetRatingState() {
        prefs.edit()
            .putBoolean(KEY_RATING_REQUESTED, false)
            .putInt(KEY_LAUNCH_COUNT, 0)
            .putLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis())
            .apply();
    }

    public int getLaunchCount() {
        return prefs.getInt(KEY_LAUNCH_COUNT, 0);
    }

    public boolean hasRated() {
        return isRatingRequested();
    }

    public interface RatingCallback {
        void onRatingShown();
        void onRatingComplete(boolean success);
    }
}
