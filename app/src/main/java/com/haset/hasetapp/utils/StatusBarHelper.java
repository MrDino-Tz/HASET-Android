package com.haset.hasetapp.utils;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.haset.hasetapp.R;

/**
 * StatusBarHelper - Utility class for managing status bar appearance.
 * Uses WindowInsetsControllerCompat instead of the deprecated
 * Window.setStatusBarColor / View.setSystemUiVisibility APIs.
 * Bar colors themselves come from the theme (android:statusBarColor),
 * which Android 15+ ignores in favour of edge-to-edge transparency.
 */
public class StatusBarHelper {

    /**
     * Configure status bar for light mode (white icons on the green bar)
     */
    public static void setLightStatusBar(Activity activity) {
        applyIconAppearance(activity, true);
    }

    /**
     * Configure status bar for dark mode (white icons on the green bar)
     */
    public static void setDarkStatusBar(Activity activity) {
        applyIconAppearance(activity, true);
    }

    /**
     * Configure status bar with the app's standard appearance
     * (green bar via theme, white icons)
     */
    public static void configureStatusBar(Activity activity) {
        applyIconAppearance(activity, true);
    }

    /**
     * Set icon appearance automatically based on the given bar color.
     * The bar color itself is provided by the activity theme.
     * @param color The color resource intended for the status bar
     */
    public static void setStatusBarColor(Activity activity, int color) {
        if (activity == null) return;
        int resolvedColor = ContextCompat.getColor(activity, color);
        applyIconAppearance(activity, !isLightColor(resolvedColor));
    }

    private static void applyIconAppearance(Activity activity, boolean lightIcons) {
        if (activity == null) return;

        Window window = activity.getWindow();
        if (window == null) return;

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // lightIcons == true → white icons → appearanceLightStatusBars == false
        controller.setAppearanceLightStatusBars(!lightIcons);
    }

    /**
     * Simple method to determine if a color is light or dark
     * @param color The color to check
     * @return true if the color is light, false if dark
     */
    private static boolean isLightColor(int color) {
        double darkness = 1 - (0.299 * (color >> 16 & 0xff) + 0.587 * (color >> 8 & 0xff) + 0.114 * (color & 0xff)) / 255;
        return darkness < 0.5;
    }
}
