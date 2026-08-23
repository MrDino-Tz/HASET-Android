package com.haset.hasetapp.utils;

import android.util.Log;

/**
 * Lightweight, dependency-free error logging.
 *
 * Centralises where failures are recorded so a crash-reporting backend
 * (e.g. Firebase Crashlytics) can be plugged in from one place without
 * touching every call site.
 */
public final class ErrorLogger {

    private static final String TAG = "HASET_ERROR";

    private ErrorLogger() {
    }

    public static void log(String userMessage, String raw) {
        Log.w(TAG, "error: " + (raw != null ? raw : userMessage));
    }

    public static void log(Throwable throwable) {
        if (throwable == null) return;
        Log.w(TAG, "exception: " + throwable.getMessage(), throwable);
    }
}
