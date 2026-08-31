package com.haset.hasetapp.utils;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Lightweight, dependency-free error logging.
 *
 * Centralises where failures are recorded so a crash-reporting backend
 * (e.g. Firebase Crashlytics) can be plugged in from one place without
 * touching every call site.
 *
 * Failures are written to Logcat (tag HASET_ERROR) and, where there is a
 * {@link Throwable}, forwarded to Crashlytics as a non-fatal report so it
 * can be investigated in the Firebase console.
 */
public final class ErrorLogger {

    private static final String TAG = "HASET_ERROR";

    private ErrorLogger() {
    }

    public static void log(String userMessage, String raw) {
        String detail = raw != null ? raw : userMessage;
        Log.w(TAG, "error: " + detail);
        FirebaseCrashlytics.getInstance().log(detail);
    }

    public static void log(Throwable throwable) {
        if (throwable == null) return;
        Log.w(TAG, "exception: " + throwable.getMessage(), throwable);
        FirebaseCrashlytics.getInstance().recordException(throwable);
    }
}
