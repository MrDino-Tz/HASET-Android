package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.AppointmentNotificationAdapter;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ShimmerHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminHomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminNotificationActivity extends AppCompatActivity implements 
        AppointmentNotificationAdapter.OnNotificationClickListener {

    private ImageView btnBack;
    private ImageView btnClearNotifications;
    private TabLayout tabs;
    private RecyclerView rvPendingAppointments;
    private RecyclerView rvSystemNotifications;
    private RecyclerView rvActivityLogs;
    private View shimmerContainerPending;
    private View shimmerContainerSystem;
    private View shimmerContainerActivity;
    
    private AppointmentNotificationAdapter appointmentAdapter;
    private com.haset.hasetapp.adapters.AuditLogAdapter auditLogAdapter;
    private PreferenceManager preferenceManager;
    private AdminHomeViewModel viewModel;
    private com.haset.hasetapp.viewmodels.AuditLogsViewModel auditViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top + 100, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupAdapters();
        setupTabListener();
        
        viewModel = new ViewModelProvider(this).get(AdminHomeViewModel.class);
        auditViewModel = new ViewModelProvider(this).get(com.haset.hasetapp.viewmodels.AuditLogsViewModel.class);
        setupObservers();
        loadSystemNotifications();
    }

    private void setupObservers() {
        showShimmerPending();
        viewModel.getAllAppointments().observe(this, appointmentEntities -> {
            hideShimmerPending();
            if (appointmentEntities != null) {
                // Filter for pending appointments only
                List<Appointment> pendingAppointments = new ArrayList<>();
                for (com.haset.hasetapp.database.entities.AppointmentEntity entity : appointmentEntities) {
                    if ("pending".equals(entity.getStatus())) {
                        pendingAppointments.add(new Appointment(entity)); // Convert to Appointment model
                    }
                }
                appointmentAdapter.setAppointments(pendingAppointments);
            }
        });

        showShimmerActivity();
        auditViewModel.getAuditLogs().observe(this, entities -> {
            hideShimmerActivity();
            if (entities != null && !entities.isEmpty()) {
                java.util.Collections.sort(entities, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                auditLogAdapter.setAuditLogs(entities);
            }
        });
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearNotifications = findViewById(R.id.btnClearNotifications);
        tabs = findViewById(R.id.tabs);
        rvPendingAppointments = findViewById(R.id.rvPendingAppointments);
        rvSystemNotifications = findViewById(R.id.rvSystemNotifications);
        rvActivityLogs = findViewById(R.id.rvActivityLogs);
        shimmerContainerPending = findViewById(R.id.shimmerContainerPending);
        shimmerContainerSystem = findViewById(R.id.shimmerContainerSystem);
        shimmerContainerActivity = findViewById(R.id.shimmerContainerActivity);
        
        preferenceManager = new PreferenceManager(this);
        // storageHelper = LocalStorageHelper.getInstance(this); // Removed
        
        btnBack.setOnClickListener(v -> finish());
        btnClearNotifications.setOnClickListener(v -> showClearNotificationsMenu());
    }

    private void setupAdapters() {
        appointmentAdapter = new AppointmentNotificationAdapter(this, this);
        
        rvPendingAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvPendingAppointments.setAdapter(appointmentAdapter);
        
        auditLogAdapter = new com.haset.hasetapp.adapters.AuditLogAdapter(entity -> {
            // Re-use logic from AuditLogsActivity if needed, or just show brief info
        });
        rvActivityLogs.setLayoutManager(new LinearLayoutManager(this));
        rvActivityLogs.setAdapter(auditLogAdapter);
        
        // System notifications adapter can be added later if needed
        rvSystemNotifications.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabListener() {
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Pending Appointments
                        rvPendingAppointments.setVisibility(View.VISIBLE);
                        shimmerContainerPending.setVisibility(View.GONE);
                        rvSystemNotifications.setVisibility(View.GONE);
                        shimmerContainerSystem.setVisibility(View.GONE);
                        rvActivityLogs.setVisibility(View.GONE);
                        shimmerContainerActivity.setVisibility(View.GONE);
                        break;
                    case 1: // System Notifications
                        rvPendingAppointments.setVisibility(View.GONE);
                        shimmerContainerPending.setVisibility(View.GONE);
                        rvSystemNotifications.setVisibility(View.VISIBLE);
                        shimmerContainerSystem.setVisibility(View.GONE);
                        rvActivityLogs.setVisibility(View.GONE);
                        shimmerContainerActivity.setVisibility(View.GONE);
                        break;
                    case 2: // Activity Logs
                        rvPendingAppointments.setVisibility(View.GONE);
                        shimmerContainerPending.setVisibility(View.GONE);
                        rvSystemNotifications.setVisibility(View.GONE);
                        shimmerContainerSystem.setVisibility(View.GONE);
                        rvActivityLogs.setVisibility(View.VISIBLE);
                        shimmerContainerActivity.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadNotifications() {
        // Handled by setupObservers
    }

    private void loadPendingAppointments() {
        // Handled by setupObservers
    }

    private void loadSystemNotifications() {
        // System notifications can be loaded from audit logs or other sources
        // For now, this is a placeholder
        hideShimmerSystem();
    }

    @Override
    public void onNotificationClick(Appointment appointment) {
        // Handle appointment notification click - could open appointment details
        // or navigate to AdminManagementActivity with appointments tab
        android.content.Intent intent = new android.content.Intent(this, AdminManagementActivity.class);
        intent.putExtra(AdminManagementActivity.EXTRA_SELECTED_TAB, 3); // Appointments tab
        startActivity(intent);
    }

    private void showClearNotificationsMenu() {
        PopupMenu popup = new PopupMenu(this, btnClearNotifications);
        popup.getMenuInflater().inflate(R.menu.clear_admin_notifications_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_clear_pending) {
                clearPendingAppointments();
                return true;
            } else if (id == R.id.action_clear_system) {
                clearSystemNotifications();
                return true;
            } else if (id == R.id.action_clear_all) {
                clearAllNotifications();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void clearPendingAppointments() {
        // For admin, we might want to approve/decline instead of delete
        // This is a placeholder - actual implementation depends on requirements
        appointmentAdapter.setAppointments(new ArrayList<>());
        android.widget.Toast.makeText(this, R.string.pending_appointments_cleared, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void clearSystemNotifications() {
        // Clear system notifications
        android.widget.Toast.makeText(this, R.string.system_notifications_cleared, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void clearAllNotifications() {
        clearPendingAppointments();
        clearSystemNotifications();
    }
    
    private void showShimmerPending() {
        shimmerContainerPending.setVisibility(View.VISIBLE);
        rvPendingAppointments.setVisibility(View.GONE);
        if (shimmerContainerPending instanceof android.view.ViewGroup) {
            ShimmerHelper.showListShimmer(this, (android.view.ViewGroup) shimmerContainerPending, 5, R.layout.shimmer_notification_list);
        }
    }
    
    private void hideShimmerPending() {
        if (shimmerContainerPending instanceof android.view.ViewGroup) {
            ShimmerHelper.hideListShimmer((android.view.ViewGroup) shimmerContainerPending);
        }
        shimmerContainerPending.setVisibility(View.GONE);
        rvPendingAppointments.setVisibility(View.VISIBLE);
    }
    
    private void showShimmerSystem() {
        shimmerContainerSystem.setVisibility(View.VISIBLE);
        rvSystemNotifications.setVisibility(View.GONE);
        if (shimmerContainerSystem instanceof android.view.ViewGroup) {
            ShimmerHelper.showListShimmer(this, (android.view.ViewGroup) shimmerContainerSystem, 4, R.layout.shimmer_notification_list);
        }
    }
    
    private void hideShimmerSystem() {
        if (shimmerContainerSystem instanceof android.view.ViewGroup) {
            ShimmerHelper.hideListShimmer((android.view.ViewGroup) shimmerContainerSystem);
        }
        shimmerContainerSystem.setVisibility(View.GONE);
        rvSystemNotifications.setVisibility(View.VISIBLE);
    }
    
    private void showShimmerActivity() {
        shimmerContainerActivity.setVisibility(View.VISIBLE);
        rvActivityLogs.setVisibility(View.GONE);
        if (shimmerContainerActivity instanceof android.view.ViewGroup) {
            ShimmerHelper.showListShimmer(this, (android.view.ViewGroup) shimmerContainerActivity, 5, R.layout.shimmer_audit_log_list);
        }
    }
    
    private void hideShimmerActivity() {
        if (shimmerContainerActivity instanceof android.view.ViewGroup) {
            ShimmerHelper.hideListShimmer((android.view.ViewGroup) shimmerContainerActivity);
        }
        shimmerContainerActivity.setVisibility(View.GONE);
        rvActivityLogs.setVisibility(View.VISIBLE);
    }
}

