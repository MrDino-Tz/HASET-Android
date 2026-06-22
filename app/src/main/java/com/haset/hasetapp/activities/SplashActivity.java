package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.TypewriterAnimation;
import com.haset.hasetapp.fragments.NoInternetBottomSheet;
import com.haset.hasetapp.models.AppConfig;
import com.haset.hasetapp.utils.FirebaseHelper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends BaseActivity {
    private static final int SPLASH_DELAY = 2500;
    private PreferenceManager preferenceManager;
    private Handler splashHandler;
    private TypewriterAnimation typewriterAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);
        if (!preferenceManager.isOnboardingSeen()) {
            Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        // Theme is initialized globally in HASETApplication
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        // Start typing animation for app name
        TextView tvAppName = findViewById(R.id.tvAppName);
        if (tvAppName != null) {
            String appName = getString(R.string.app_name);
            typewriterAnimation = new TypewriterAnimation(tvAppName, appName, 150);
            typewriterAnimation.start();
        }

        if (NetworkUtils.isNetworkAvailable(this)) {
            checkAppConfiguration();
        } else {
            showNoInternetBottomSheet();
        }
    }

    @Override
    protected void onDestroy() {
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
            splashHandler = null;
        }
        if (typewriterAnimation != null) {
            typewriterAnimation.stop();
        }
        super.onDestroy();
    }

    private void startSplashFlow() {
        splashHandler = new Handler(Looper.getMainLooper());
        splashHandler.postDelayed(() -> {
            if (preferenceManager.isLoggedIn()) {
                String role = preferenceManager.getUserRole();
                Intent intent;
                if (Constants.ROLE_ADMIN.equals(role)) {
                    intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, DashboardActivity.class);
                }
                startActivity(intent);
            } else {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
            finish();
        }, SPLASH_DELAY);
    }

    @Override
    public void onRetryConnection() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            dismissNoInternetBottomSheet();
            checkAppConfiguration();
        } else {
            Toast.makeText(this, R.string.no_internet_still, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNetworkAvailable() {
        dismissNoInternetBottomSheet();
        checkAppConfiguration();
    }

    @Override
    public void onNetworkUnavailable() {
        showNoInternetBottomSheet();
    }

    @Override
    public void onBottomSheetDismissed() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            finish();
        } else {
            checkAppConfiguration();
        }
    }

    private void checkAppConfiguration() {
        FirebaseHelper.getAppConfig(new FirebaseHelper.OnCompleteListener<AppConfig>() {
            @Override
            public void onSuccess(AppConfig config) {
                if (config == null) {
                    startSplashFlow();
                    return;
                }

                if (config.isMaintenanceMode()) {
                    showBlockingDialog("Maintenance Mode", 
                        config.getMaintenanceMessage() != null ? config.getMaintenanceMessage() : "HASET App is undergoing maintenance. Please try again later.", 
                        null);
                    return;
                }

                if (config.getMinVersionCode() > getAppVersionCode()) {
                    showBlockingDialog("Update Required", 
                        "A new version of HASET is available. Please update to continue.", 
                        config.getUpdateUrl());
                    return;
                }

                startSplashFlow();
            }

            @Override
            public void onError(String error) {
                startSplashFlow();
            }
        });
    }

    private void showBlockingDialog(String title, String message, String url) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false);

        if (url != null && !url.isEmpty()) {
            builder.setPositiveButton("Update Now", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open update link", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        builder.setNegativeButton("Exit", (dialog, which) -> finish());
        builder.show();
    }
    
    private int getAppVersionCode() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }
}
