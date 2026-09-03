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
import com.haset.hasetapp.utils.HealthTipsHelper;
import com.haset.hasetapp.utils.StatusBarHelper;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.utils.FirebaseHelper;
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
        redirectUnpaidDoctorIfNeeded();
        
        setContentView(R.layout.activity_dashboard);
        overridePendingTransition(R.anim.anim_slide_up, 0);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
        
        // Configure status bar for better visibility
        StatusBarHelper.configureStatusBar(this);

        // Clear notification badge on app open (Option 2: Auto-clear on app open)
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(this);
        badgeHelper.onAppOpened();

        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupBottomNavigation();
        loadInitialFragment();

        // Token refresh can happen before authentication. Always bind the
        // current installation token to the signed-in user on dashboard entry.
        syncFcmToken();

        // The application process can start before authentication is restored;
        // start the conversation listener again after the logged-in user is
        // available so chat notifications are not silently missed.
        com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).startListening();
        
        // Handle navigation intents
        handleIntent(getIntent());
        
        // Initialize network monitoring is handled by BaseActivity
        // setupNetworkMonitoring();
        
        // Trigger role-specific notifications
        triggerPatientNotifications();
        triggerDoctorNotifications();
    }

    private void redirectUnpaidDoctorIfNeeded() {
        if (!Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole())) {
            return;
        }
        FirebaseHelper.isDoctorRegistrationPending(preferenceManager.getUserId(),
            new FirebaseHelper.OnCompleteListener<Boolean>() {
                @Override
                public void onSuccess(Boolean pending) {
                    if (Boolean.TRUE.equals(pending) && !isFinishing()) {
                        sendUnpaidDoctorToLogin();
                    }
                }

                @Override
                public void onError(String error) {
                    // Leave paid doctors online if the check fails.
                }
            });
    }

    private void sendUnpaidDoctorToLogin() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        preferenceManager.setLoggedIn(false);
        Intent login = new Intent(DashboardActivity.this, LoginActivity.class);
        login.putExtra("unpaid_doctor", true);
        login.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        finish();
    }

    private void syncFcmToken() {
        String userId = preferenceManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) return;

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.trim().isEmpty()) return;
                    preferenceManager.setFCMToken(token);
                    com.haset.hasetapp.utils.FirebaseHelper.getUsersRef()
                            .child(userId)
                            .child("fcmToken")
                            .setValue(token);
                });
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
        if (intent != null && "appointments".equals(intent.getStringExtra("navigate_to"))) {
            // Notification taps must open the actionable appointments screen,
            // including when DashboardActivity was already running.
            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_appointments);
            }
        } else if (intent != null && "notifications".equals(intent.getStringExtra("navigate_to"))) {
            startActivity(new Intent(this, NotificationActivity.class));
        } else if (intent != null && "chat".equals(intent.getStringExtra("navigate_to"))) {
            String senderId = intent.getStringExtra("senderId");
            if (senderId != null && !senderId.trim().isEmpty()) {
                Intent chatIntent = new Intent(this, ChatActivity.class);
                chatIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, senderId);
                chatIntent.putExtra(Constants.EXTRA_CHAT_USER_NAME, intent.getStringExtra("senderName"));
                startActivity(chatIntent);
            }
        } else if (intent != null && "prescription_detail".equals(intent.getStringExtra("navigate_to"))) {
            String prescriptionId = intent.getStringExtra("prescription_id");
            if (prescriptionId != null) {
                Fragment fragment = com.haset.hasetapp.fragments.PrescriptionDetailFragment.newInstance(prescriptionId);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        } else if (intent != null && HealthTipsHelper.NAVIGATE_TO_HEALTH_TIP.equals(
                intent.getStringExtra("navigate_to"))) {
            String title = intent.getStringExtra(HealthTipsHelper.EXTRA_HEALTH_TIP_TITLE);
            String tip = intent.getStringExtra(HealthTipsHelper.EXTRA_HEALTH_TIP_TEXT);
            if (tip != null && !tip.trim().isEmpty()) {
                CustomDialog tipDialog = new CustomDialog(this)
                        .setDialogType(CustomDialog.DialogType.INFO)
                        .setTitle(title == null || title.trim().isEmpty()
                                ? getString(R.string.health_tips) : title)
                        .setMessage(tip)
                        .hideNegativeButton();
                tipDialog.setPositiveButton(getString(android.R.string.ok), v -> tipDialog.dismiss());
                tipDialog.show();
            }

            // Avoid showing the same tip again after an activity recreation.
            intent.removeExtra("navigate_to");
            intent.removeExtra(HealthTipsHelper.EXTRA_HEALTH_TIP_TITLE);
            intent.removeExtra(HealthTipsHelper.EXTRA_HEALTH_TIP_TEXT);
        }
    }

    private void showExitDialog() {
        CustomDialog exitDialog = new CustomDialog(this)
                .setDialogType(CustomDialog.DialogType.WARNING)
                .setTitle(getString(R.string.exit_app))
                .setMessage(getString(R.string.exit_app_confirm))
                .setPositiveButtonColor(R.color.colorError);
        
        exitDialog.setPositiveButton("Exit", v -> {
            exitDialog.dismiss();
            finish();
        });
        
        exitDialog.setNegativeButton("Stay", v -> exitDialog.dismiss());
        
        exitDialog.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
