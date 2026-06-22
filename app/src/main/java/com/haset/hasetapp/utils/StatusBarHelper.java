package com.haset.hasetapp.utils;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.haset.hasetapp.R;

/**
 * StatusBarHelper - Utility class for managing status bar appearance
 * Handles status bar configuration for all Android versions
 */
public class StatusBarHelper {

    /**
     * Configure status bar for light mode with light icons
     * Works across all Android versions
     */
    public static void setLightStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Clear FLAG_TRANSLUCENT_STATUS flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        
        // Add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        // Set status bar color to green_primary
        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.green_primary));
        
        // Handle status bar icon color based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ (API 23+) - remove light status bar flag for light icons
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
        // For Android 5.0, light icons work well with green background
    }
    
    /**
     * Configure status bar for dark mode with light icons
     * Works across all Android versions
     */
    public static void setDarkStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Clear FLAG_TRANSLUCENT_STATUS flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        
        // Add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        // Set status bar color to green_primary
        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.green_primary));
        
        // Handle status bar icon color based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ (API 23+) - remove light status bar flag for dark icons
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
        // For Android 5.0, light icons work well with green background
    }
    
    /**
     * Configure status bar with green_primary color
     * Uses consistent green status bar for all themes
     */
    public static void configureStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        // Clear FLAG_TRANSLUCENT_STATUS flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        
        // Add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        // Set status bar color to green_primary
        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.green_primary));
        
        // Handle status bar icon color based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ (API 23+) - remove light status bar flag for light icons
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
        // For Android 5.0, light icons work well with green background
    }
    
    /**
     * Set status bar color with automatic icon color handling
     * @param color The color resource to use for status bar
     */
    public static void setStatusBarColor(Activity activity, int color) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        if (window == null) return;
        
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        
        int resolvedColor = ContextCompat.getColor(activity, color);
        window.setStatusBarColor(resolvedColor);
        
        // Automatically determine if icons should be light or dark based on background color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            
            // Simple luminance check - you can make this more sophisticated
            boolean isLightColor = isLightColor(resolvedColor);
            
            if (isLightColor) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            
            decorView.setSystemUiVisibility(flags);
        }
    }
    
    /**
     * Simple method to determine if a color is light or dark
     * @param color The color to check
     * @return true if the color is light, false if dark
     */
    private static boolean isLightColor(int color) {
        // Calculate luminance using standard formula
        double darkness = 1 - (0.299 * (color >> 16 & 0xff) + 0.587 * (color >> 8 & 0xff) + 0.114 * (color & 0xff)) / 255;
        return darkness < 0.5;
    }
}
