package com.haset.hasetapp.activities;

import com.haset.hasetapp.utils.CustomDialog;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.haset.hasetapp.HASETApplication;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.AdminHomeFragment;
import com.haset.hasetapp.fragments.AdminProfileFragment;
import com.haset.hasetapp.utils.AdminNotificationManager;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.fragments.NoInternetBottomSheet;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminDashboardActivity extends BaseActivity {
    private BottomNavigationView bottomNavigation;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton fabMenu;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Theme is initialized globally in HASETApplication
        
        setContentView(R.layout.activity_admin_dashboard);

        preferenceManager = new PreferenceManager(this);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        fabMenu = findViewById(R.id.fabMenu);

        setupFloatingMenu();
        setupNavigationDrawer();
        setupBottomNavigation();
        loadInitialFragment();
        updateNavHeader();
        
        // Initialize network monitoring is handled by BaseActivity
        // setupNetworkMonitoring();
        
        // Trigger admin notifications
        triggerAdminNotifications();
    }
    
    private void setupFloatingMenu() {
        fabMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }
    
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new AdminHomeFragment());
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_audit_logs) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_AUDIT_LOGS", "Admin accessed Audit Logs", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AuditLogsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_reports) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_REPORTS", "Admin accessed Reports", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminReportActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_article_center) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_ARTICLE_CENTER", "Admin accessed Article Center", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, ArticleCenterActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_promo_banners) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_PROMO_BANNERS", "Admin accessed Promo Banners", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminBannersActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_health_quotes) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_HEALTH_QUOTES", "Admin accessed Health Quotes", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminQuotesActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_demo_doctors) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_DEMO_DOCTORS", "Admin accessed Demo Doctors", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminManageDemoDoctorsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_wallet_management) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_WALLET_MANAGEMENT", "Admin accessed Wallet Management", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminWalletManagementActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_support_tickets) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_SUPPORT_TICKETS", "Admin accessed Support Tickets", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminSupportTicketsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_manage_hospitals) {
                AuditLogger.getInstance(this).logAction("NAVIGATE_MANAGE_HOSPITALS", "Admin accessed Manage Hospitals", "ADMIN_ACTION", null);
                Intent intent = new Intent(this, AdminManageHospitalsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_logout) {
                performLogout();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            
            return false;
        });
    }
    
    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        CircleImageView navHeaderProfileImage = headerView.findViewById(R.id.navHeaderProfileImage);
        TextView navHeaderUserName = headerView.findViewById(R.id.navHeaderUserName);
        TextView navHeaderUserEmail = headerView.findViewById(R.id.navHeaderUserEmail);
        com.facebook.shimmer.ShimmerFrameLayout shimmerNavHeaderProfile = headerView.findViewById(R.id.shimmerNavHeaderProfile);
        
        String userName = preferenceManager.getUserName();
        String userEmail = preferenceManager.getUserEmail();
        String userId = preferenceManager.getUserId();
        
        navHeaderUserName.setText(userName != null ? userName : "Admin");
        navHeaderUserEmail.setText(userEmail != null ? userEmail : "admin@hasetapp.com");
        
        if (userId != null) {
            ProfilePhotoHelper.loadProfilePhoto(this, userId, navHeaderProfileImage, shimmerNavHeaderProfile);
        }
    }
    
    private void performLogout() {
        // Log logout action before clearing preferences
        AuditLogger.getInstance(this).logLogout();
        
        preferenceManager.setLoggedIn(false);
        preferenceManager.clearPreferences();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
        finish();
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
            .replace(R.id.fragment_container, fragment)
            .commit();
        
        // Animate FAB position based on fragment
        animateFabPosition(fragment);
    }
    
    private void animateFabPosition(Fragment fragment) {
        // Wait for layout to be measured
        fabMenu.post(() -> {
            // Calculate screen width for animation
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int fabWidth = fabMenu.getWidth();
            int margin = (int) (16 * getResources().getDisplayMetrics().density);
            
            // Calculate distance to move
            float distanceToMove;
            if (fragment instanceof AdminProfileFragment) {
                // Move from left to right: screen width - fab width - 2*margin (left margin + right margin)
                distanceToMove = screenWidth - fabWidth - (2 * margin);
            } else {
                // Move back to left: reset translation
                distanceToMove = 0;
            }
            
            // Animate translation
            ObjectAnimator animator = ObjectAnimator.ofFloat(fabMenu, "translationX", 
                fabMenu.getTranslationX(), distanceToMove);
            animator.setDuration(300);
            animator.start();
        });
    }
    
    private void triggerAdminNotifications() {
        // Get admin notification manager from application
        AdminNotificationManager notificationManager = 
            ((HASETApplication) getApplication()).getAdminNotificationManager();
        
        if (notificationManager != null) {
            String userName = preferenceManager.getUserName();
            notificationManager.onAdminLogin(userName);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new AdminHomeFragment();
            } else if (itemId == R.id.nav_management) {
                Intent intent = new Intent(this, AdminManagementActivity.class);
                startActivity(intent);
                return false; // Don't select the item as it launches an activity
            } else if (itemId == R.id.nav_profile) {
                fragment = new AdminProfileFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                    .replace(R.id.fragment_container, fragment)
                    .commit();
                
                // Animate FAB position based on fragment
                animateFabPosition(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadInitialFragment() {
        // Load admin home fragment by default
        AdminHomeFragment homeFragment = new AdminHomeFragment();
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
            .replace(R.id.fragment_container, homeFragment)
            .commit();
        
        // Set selected item in bottom navigation
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        
        // FAB starts on left for AdminHomeFragment
        fabMenu.post(() -> {
            fabMenu.setTranslationX(0);
        });
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            CustomDialog exitDialog = new CustomDialog(this)
                    .setDialogType(CustomDialog.DialogType.WARNING)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButtonColor(R.color.colorError);
            
            exitDialog.setPositiveButton("Exit", v -> {
                exitDialog.dismiss();
                finish();
            });
            
            exitDialog.setNegativeButton("Stay", v -> exitDialog.dismiss());
            
            exitDialog.show();
        }
    }

    // Network monitoring handled by BaseActivity

    @Override
    protected void onResume() {
        super.onResume();
    }

    // NetworkStateCallback implementation
    @Override
    public void onRetryConnection() {
        // Handle retry connection - check network again
        if (NetworkUtils.isNetworkAvailable(this)) {
            dismissNoInternetBottomSheet();
            onNetworkAvailable();
        } else {
            onNetworkUnavailable();
        }
    }

    @Override
    public void onNetworkAvailable() {
        // Network is available - can proceed with Firebase operations
        dismissNoInternetBottomSheet();
        // Refresh current fragment data if needed
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
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
}
