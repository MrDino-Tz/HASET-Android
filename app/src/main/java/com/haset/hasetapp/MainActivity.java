package com.haset.hasetapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.LocalizedAppCompatActivity;
import com.haset.hasetapp.utils.AppRatingHelper;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.HealthTipsHelper;
import com.haset.hasetapp.utils.NotificationHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.StatusBarHelper;

public class MainActivity extends LocalizedAppCompatActivity {

    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private HealthTipsHelper healthTipsHelper;
    private AppRatingHelper appRatingHelper;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize helpers
        preferenceManager = new PreferenceManager(this);
        notificationHelper = new NotificationHelper(this);
        healthTipsHelper = new HealthTipsHelper(this);
        appRatingHelper = new AppRatingHelper(this);
        appRatingHelper.initialize();

        // Setup permission launcher
        setupNotificationPermissionLauncher();

        // Check and request notification permission if needed
        checkAndRequestNotificationPermission();
        
        // Configure status bar for better visibility
        StatusBarHelper.configureStatusBar(this);
    }

    private void setupNotificationPermissionLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, show welcome notification
                        if (preferenceManager.isLoggedIn()) {
                            // Schedule health tips for patients
                            if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
                                healthTipsHelper.scheduleDailyHealthTips();
                            }
                        }
                    } else {
                        // Permission denied, show explanation
                        showNotificationPermissionDialog();
                    }
                }
        );
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Permission already granted, show welcome notification
                if (preferenceManager.isLoggedIn()) {
                    // Schedule health tips for patients
                    if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
                        healthTipsHelper.scheduleDailyHealthTips();
                    }
                    // Check and show app rating prompt
                    appRatingHelper.checkAndShowRating(new AppRatingHelper.RatingCallback() {
                        @Override
                        public void onRatingShown() {
                            // Rating flow started
                        }

                        @Override
                        public void onRatingComplete(boolean success) {
                            if (success) {
                                // User completed or dismissed rating
                            }
                        }
                    });
                }
            }
        } else {
            // Android 12 and below, show welcome notification directly
            if (preferenceManager.isLoggedIn()) {
                // Schedule health tips for patients
                if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
                    healthTipsHelper.scheduleDailyHealthTips();
                }
                // Check and show app rating prompt
                appRatingHelper.checkAndShowRating(new AppRatingHelper.RatingCallback() {
                    @Override
                    public void onRatingShown() {
                        // Rating flow started
                    }

                    @Override
                    public void onRatingComplete(boolean success) {
                        if (success) {
                            // User completed or dismissed rating
                        }
                    }
                });
            }
        }
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }
}
