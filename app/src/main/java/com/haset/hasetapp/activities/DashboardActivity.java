package com.haset.hasetapp.activities;

import com.haset.hasetapp.utils.CustomDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.haset.hasetapp.HASETApplication;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.AppointmentsFragment;
import com.haset.hasetapp.fragments.ChatListFragment;
import com.haset.hasetapp.fragments.DoctorHomeFragment;
import com.haset.hasetapp.fragments.PatientHomeFragment;
import com.haset.hasetapp.fragments.ProfileFragment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.DoctorNotificationManager;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.NotificationBadgeHelper;
import com.haset.hasetapp.utils.PatientNotificationManager;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.StatusBarHelper;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.fragments.NoInternetBottomSheet;
import com.haset.hasetapp.workers.TrendingArticlesWorker;

public class DashboardActivity extends BaseActivity {
    private BottomNavigationView bottomNavigation;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Theme is initialized globally in HASETApplication
        
        preferenceManager = new PreferenceManager(this);
        
        setContentView(R.layout.activity_dashboard);
        
        // Configure status bar for better visibility
        StatusBarHelper.configureStatusBar(this);

        // Clear notification badge on app open (Option 2: Auto-clear on app open)
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(this);
        badgeHelper.onAppOpened();

        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupBottomNavigation();
        loadInitialFragment();
        
        // Handle navigation intents
        handleIntent(getIntent());
        
        // Initialize network monitoring is handled by BaseActivity
        // setupNetworkMonitoring();
        
        // Trigger role-specific notifications
        triggerPatientNotifications();
        triggerDoctorNotifications();
    }
    
    private void triggerPatientNotifications() {
        if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            // Get patient notification manager from application
            PatientNotificationManager notificationManager = 
                ((HASETApplication) getApplication()).getPatientNotificationManager();
            
            if (notificationManager != null) {
                String userName = preferenceManager.getUserName();
                notificationManager.onPatientLogin(userName);
            }
            
            // Schedule trending articles background check
            TrendingArticlesWorker.schedule(this);
        }
    }
    
    private void triggerDoctorNotifications() {
        if (Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole())) {
            // Get doctor notification manager from application
            DoctorNotificationManager notificationManager = 
                ((HASETApplication) getApplication()).getDoctorNotificationManager();
            
            if (notificationManager != null) {
                String userName = preferenceManager.getUserName();
                notificationManager.onDoctorLogin(userName);
            }
            
            // Schedule trending articles background check
            TrendingArticlesWorker.schedule(this);
        }
    }
    
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = getHomeFragment();
            }
            else if (itemId == R.id.nav_appointments) {
                fragment = new AppointmentsFragment();
            }

            else if (itemId == R.id.nav_chat) {
                fragment = new ChatListFragment();
            }
            else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    private void loadInitialFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                .replace(R.id.fragmentContainer, getHomeFragment())
                .commit();
    }

    /**
     * Get the bottom navigation view for fragment navigation
     */
    public BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }

    private Fragment getHomeFragment() {
        String role = preferenceManager.getUserRole();
        if (Constants.ROLE_DOCTOR.equals(role)) {
            return new DoctorHomeFragment();
        } else {
            return new PatientHomeFragment();
        }
    }

    // Network monitoring handled by BaseActivity

    @Override
    protected void onResume() {
        super.onResume();
        
        // Refresh message badge in home fragment when returning from other activities
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment instanceof PatientHomeFragment) {
            ((PatientHomeFragment) currentFragment).refreshMessageBadge();
        }
    }

    // Custom onRetryConnection if needed, otherwise uses BaseActivity's implementation

    @Override
    public void onNetworkAvailable() {
        // Network is available - can proceed with Firebase operations
        dismissNoInternetBottomSheet();
        // Refresh current fragment data if needed
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            // Trigger data refresh in the current fragment
        }
    }

    @Override
    public void onNetworkUnavailable() {
        // Network is unavailable - show offline state
        showNoInternetBottomSheet();
    }

    @Override
    public void onBottomSheetDismissed() {
        super.onBottomSheetDismissed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "prescription_detail".equals(intent.getStringExtra("navigate_to"))) {
            String prescriptionId = intent.getStringExtra("prescription_id");
            if (prescriptionId != null) {
                Fragment fragment = com.haset.hasetapp.fragments.PrescriptionDetailFragment.newInstance(prescriptionId);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    public void onBackPressed() {
        CustomDialog exitDialog = new CustomDialog(this)
                .setDialogType(CustomDialog.DialogType.WARNING)
                .setTitle(getString(R.string.exit_app))
                .setMessage(String.valueOf(R.string.exit_app_confirm))
                .setPositiveButtonColor(R.color.colorError);
        
        exitDialog.setPositiveButton("Exit", v -> {
            exitDialog.dismiss();
            finish();
        });
        
        exitDialog.setNegativeButton("Stay", v -> exitDialog.dismiss());
        
        exitDialog.show();
    }
}
