package com.haset.hasetapp.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.ApiError;

/**
 * Centralised error presentation so every screen shows failures consistently.
 *
 * Pass either a plain message (already produced by the repository) or a typed
 * {@link ApiError}; both use the same "Operation failed: &lt;detail&gt;" Toast
 * format that the rest of the app expects, and both are localised where a
 * well-known category is recognised.
 */
public final class ErrorDisplay {

    /** Message repositories use when a Firebase session can no longer be verified. */
    public static final String AUTH_EXPIRED = "Authentication expired. Please sign in again.";

    private ErrorDisplay() {
    }

    public static void toast(@NonNull Context context, @Nullable String message) {
        String detail = localize(context, message);
        Toast.makeText(context, context.getString(R.string.operation_failed, detail), Toast.LENGTH_LONG).show();
        ErrorLogger.log(detail, message);
    }

    public static void toast(@NonNull Context context, @NonNull ApiError error) {
        toast(context, error.getUserMessage());
    }

    public static void toast(@NonNull Context context, @StringRes int messageRes) {
        Toast.makeText(context, context.getString(R.string.operation_failed, context.getString(messageRes)), Toast.LENGTH_LONG).show();
    }

    public static void snackbar(@NonNull View root, @Nullable String message) {
        String detail = localize(root.getContext(), message);
        Snackbar.make(root, detail, Snackbar.LENGTH_LONG).show();
        ErrorLogger.log(detail, message);
    }

    public static void snackbar(@NonNull View root, @NonNull ApiError error) {
        snackbar(root, error.getUserMessage());
    }

    /** Localised text for callers that render the message inside their own UI. */
    public static String localizeMessage(@NonNull Context context, @Nullable String message) {
        return localize(context, message);
    }

    public static boolean isAuthError(@Nullable String message) {
        return AUTH_EXPIRED.equals(message);
    }

    /** Send the user back to login — used when the session has expired. */
    public static void navigateToLogin(@NonNull Context context) {
        try {
            Intent intent = new Intent(context, com.haset.hasetapp.activities.SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
            // SplashActivity not available in this build flavour; ignore.
        }
    }

    /**
     * One-call handler for any {@code onError} site. Localises the message, logs it,
     * shows a Toast, and sends the user to login when the session has expired.
     * Safe to call from Activities and arbitrary contexts (falls back to a Toast).
     */
    public static void report(@NonNull Context context, @Nullable String message) {
        if (message == null || message.isEmpty()) {
            toast(context, (String) null);
            return;
        }
        toast(context, message);
        if (isAuthError(message) && context instanceof android.app.Activity) {
            navigateToLogin(context);
        }
    }

    /**
     * Fragment-friendly variant that surfaces the error as a Snackbar anchored to a view.
     */
    public static void report(@NonNull View root, @Nullable String message) {
        if (message == null || message.isEmpty()) {
            snackbar(root, (String) null);
            return;
        }
        snackbar(root, message);
        if (isAuthError(message) && root.getContext() instanceof android.app.Activity) {
            navigateToLogin(root.getContext());
        }
    }

    private static String localize(@NonNull Context context, @Nullable String message) {
        if (message == null || message.isEmpty()) {
            return context.getString(R.string.error_generic);
        }
        if (AUTH_EXPIRED.equals(message)) {
            return context.getString(R.string.error_auth_expired);
        }

        String lower = message.toLowerCase();

        // Network / connectivity — Firebase and generic IO exceptions.
        if (lower.contains("network error") || lower.contains("unable to resolve host")
                || lower.contains("failed to connect") || lower.contains("timeout")
                || lower.contains("no internet") || lower.contains("connection")) {
            return context.getString(R.string.error_network);
        }

        // Firebase auth error codes / messages.
        if (lower.contains("email address is already in use") || lower.contains("email_exists")) {
            return context.getString(R.string.error_account_exists);
        }
        if (lower.contains("no user record") || lower.contains("user not found")
                || lower.contains("user does not exist")) {
            return context.getString(R.string.error_user_not_found);
        }
        if (lower.contains("user account has been disabled") || lower.contains("user_disabled")) {
            return context.getString(R.string.error_user_disabled);
        }
        if (lower.contains("password is invalid") || lower.contains("invalid_credential")
                || lower.contains("wrong password") || lower.contains("invalid-credential")
                || lower.contains("credential is incorrect")) {
            return context.getString(R.string.error_invalid_credential);
        }
        if (lower.contains("weak_password") || lower.contains("password should be at least")
                || lower.contains("password-does-not-meet-requirements") || lower.contains("missing password requirements")) {
            return context.getString(R.string.error_weak_password);
        }
        if (lower.contains("badly formatted") || lower.contains("invalid_email")
                || lower.contains("email address is invalid")) {
            return context.getString(R.string.error_email_bad_format);
        }
        if (lower.contains("too many") && lower.contains("attempt")) {
            return context.getString(R.string.error_too_many_requests);
        }
        if (lower.contains("email is not verified") || lower.contains("email_not_verified")
                || lower.contains("verify your email")) {
            return context.getString(R.string.error_email_not_verified);
        }
        if (lower.contains("not enabled") && lower.contains("sign")) {
            return context.getString(R.string.error_signin_not_enabled);
        }
        if (lower.contains("credential is no longer valid") || lower.contains("must sign in again")
                || lower.contains("session has expired") || lower.contains("auth expired")) {
            return context.getString(R.string.error_session_expired);
        }

        if (lower.contains("server error") || lower.contains("on our servers")
                || lower.contains("internal error") || lower.contains("unknown error")) {
            return context.getString(R.string.error_server);
        }
        if ("password_change_not_available".equals(lower)
                || lower.contains("password_change_not_available")
                || (lower.contains("operation is not allowed") && lower.contains("provider"))) {
            return context.getString(R.string.error_password_not_available);
        }
        if ("doctor_profile_update_denied".equals(lower)
                || lower.contains("doctor_profile_update_denied")) {
            return context.getString(R.string.error_doctor_profile_update);
        }
        if (lower.contains("permission") || lower.contains("not allowed")
                || lower.contains("forbidden")) {
            return context.getString(R.string.error_forbidden);
        }
        if (lower.contains("not found")) {
            return context.getString(R.string.error_not_found);
        }

        return message;
    }
}
