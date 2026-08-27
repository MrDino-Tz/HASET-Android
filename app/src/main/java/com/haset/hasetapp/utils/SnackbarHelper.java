package com.haset.hasetapp.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;

/**
 * Centralised Snackbar presentation for the auth flow so every screen shows
 * transient feedback consistently:
 *   - errors are red and anchored to a view
 *   - validation is inline (field-level) and not emitted here
 *   - messages are always taken from string resources (never raw literals)
 *
 * Use {@link #error(View, String)} for failures and {@link #success(View, String)}
 * for completed operations.
 */
public final class SnackbarHelper {

    private SnackbarHelper() {
    }

    /** Red error Snackbar (LENGTH_SHORT) from a string resource. */
    public static void error(@NonNull View root, @StringRes int messageRes) {
        show(root, root.getContext().getString(messageRes), R.color.colorError, Snackbar.LENGTH_SHORT);
    }

    /** Red error Snackbar (LENGTH_SHORT) from a raw string. */
    public static void error(@NonNull View root, @Nullable String message) {
        show(root, message, R.color.colorError, Snackbar.LENGTH_SHORT);
    }

    /** Green success Snackbar (LENGTH_LONG). */
    public static void success(@NonNull View root, @Nullable String message) {
        show(root, message, R.color.green_primary, Snackbar.LENGTH_LONG);
    }

    /** Neutral/default Snackbar (LENGTH_LONG). */
    public static void info(@NonNull View root, @Nullable String message) {
        show(root, message, R.color.text_primary, Snackbar.LENGTH_LONG);
    }

    private static void show(@NonNull View root, @Nullable String message, int colorRes, int duration) {
        if (message == null || message.isEmpty()) {
            return;
        }
        try {
            Snackbar.make(root, message, duration)
                    .setBackgroundTint(root.getContext().getColor(colorRes))
                    .show();
        } catch (RuntimeException ignored) {
            // Root view not attached; do not crash.
        }
    }
}
