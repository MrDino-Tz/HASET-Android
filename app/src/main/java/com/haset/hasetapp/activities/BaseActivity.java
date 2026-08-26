package com.haset.hasetapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.NoInternetBottomSheet;
import com.haset.hasetapp.utils.MemoryMonitor;
import com.haset.hasetapp.utils.NetworkUtils;

/**
 * Base Activity that provides network monitoring functionality to all activities.
 * Activities should extend this class instead of AppCompatActivity to automatically
 * get network error bottom sheet functionality.
 */
public abstract class BaseActivity extends AppCompatActivity implements NoInternetBottomSheet.NetworkStateCallback, NetworkUtils.NetworkCallback {

    private NetworkUtils.NetworkCallback networkCallback;
    protected NoInternetBottomSheet noInternetBottomSheet;
    private String attachedLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        attachedLanguage = com.haset.hasetapp.utils.LocaleHelper.getLanguage(newBase);
        super.attachBaseContext(com.haset.hasetapp.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    public void applyOverrideConfiguration(android.content.res.Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNetworkMonitoring();
    }

    /**
     * Sets up network monitoring to show/hide the no internet bottom sheet.
     */
    private void setupNetworkMonitoring() {
        // Use our new NetworkUtils callback system
        NetworkUtils.addNetworkCallback(this, this);

        // Check initial network state
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetBottomSheet();
            onNetworkUnavailable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister from our modern callback system
        NetworkUtils.removeNetworkCallback(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguage = com.haset.hasetapp.utils.LocaleHelper.getLanguage(this);
        if (attachedLanguage != null && !attachedLanguage.equals(currentLanguage)) {
            recreate();
            return;
        }

        // Log memory usage for monitoring
        MemoryMonitor.logMemoryUsageThrottled(getClass().getSimpleName() + "_onResume");
        
        // Check network state when resuming
        if (NetworkUtils.isNetworkAvailable(this)) {
            if (noInternetBottomSheet != null && noInternetBottomSheet.isAdded()) {
                noInternetBottomSheet.dismiss();
            }
            onNetworkAvailable();
        } else {
            if (noInternetBottomSheet == null || !noInternetBottomSheet.isAdded()) {
                noInternetBottomSheet = new NoInternetBottomSheet();
                noInternetBottomSheet.show(getSupportFragmentManager(), NoInternetBottomSheet.TAG);
            }
            onNetworkUnavailable();
        }
    }

    // NetworkCallback implementation
    @Override
    public void onNetworkAvailable() {
        runOnUiThread(() -> {
            dismissNoInternetBottomSheet();
        });
    }

    @Override
    public void onNetworkLost() {
        runOnUiThread(() -> {
            showNoInternetBottomSheet();
        });
    }

    // NetworkStateCallback implementation
    @Override
    public void onRetryConnection() {
        // Handle retry connection - check network again
        if (NetworkUtils.isNetworkAvailable(this)) {
            onNetworkAvailable();
        } else {
            onNetworkUnavailable();
        }
    }

    @Override
    public void onNetworkUnavailable() {
        // Network is unavailable - override in child activities if needed
        // This method can be used to show offline state or disable features
    }

    protected void showNoInternetBottomSheet() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (noInternetBottomSheet == null || !noInternetBottomSheet.isAdded()) {
                noInternetBottomSheet = new NoInternetBottomSheet();
                noInternetBottomSheet.show(getSupportFragmentManager(), NoInternetBottomSheet.TAG);
            }
        } catch (IllegalStateException e) {
            // FragmentManager state issue - ignore
        }
    }

    protected void dismissNoInternetBottomSheet() {
        if (isFinishing() || isDestroyed()) return;
        try {
            if (noInternetBottomSheet != null && noInternetBottomSheet.isAdded()) {
                noInternetBottomSheet.dismiss();
            }
        } catch (IllegalStateException e) {
            // FragmentManager state issue - ignore
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_fade_enter, R.anim.slide_fade_exit);
    }

    @Override
    public void startActivity(Intent intent, android.os.Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(R.anim.slide_fade_enter, R.anim.slide_fade_exit);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode, null);
        overridePendingTransition(R.anim.slide_fade_enter, R.anim.slide_fade_exit);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, android.os.Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        overridePendingTransition(R.anim.slide_fade_enter, R.anim.slide_fade_exit);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_fade_pop_enter, R.anim.slide_fade_pop_exit);
    }

    @Override
    public void onBottomSheetDismissed() {
        noInternetBottomSheet = null;
    }

    /**
     * Opens a web page using Chrome Custom Tabs.
     * @param url The URL to open.
     */
    public void openWebPage(String url) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(this, R.color.green_primary));
            builder.setShowTitle(true);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            // Fallback to standard browser if Custom Tabs fails
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
}
