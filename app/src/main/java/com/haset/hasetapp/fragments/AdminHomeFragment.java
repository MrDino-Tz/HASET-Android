package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.AdminManagementActivity;
import com.haset.hasetapp.activities.AdminNotificationActivity;
import com.haset.hasetapp.adapters.UserAdapter;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.FirebaseHelper; // Added FirebaseHelper import
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminHomeViewModel;

import java.util.List;

/**
 * Home dashboard for Admin users.
 * <p>
 * <b>Memory Management:</b>
 * Implements {@link #onDestroyView()} to clear RecyclerView adapters and view references.
 * This ensures large lists of users/appointments don't stay in memory when switching tabs.
 */
public class AdminHomeFragment extends Fragment {
    private TextView tvTotalUsers, tvTotalDoctors, tvTotalPatients, tvTotalAppointments;
    private MaterialCardView cardViewUsers, cardViewDoctors, cardViewPatients, cardViewAppointments;
    private RecyclerView recyclerViewUsers, recyclerViewAuditLogs;
    private UserAdapter userAdapter;
    private com.haset.hasetapp.adapters.AuditLogAdapter auditLogAdapter;
    private PreferenceManager preferenceManager;
    private ImageView ivNotification;
    private TextView tvNotificationBadge, tvAdminTip, tvAlertMessage;
    private View llSystemAlerts;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private AdminHomeViewModel viewModel;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPageLoading;
    private View llMainContent;
    private boolean statsLoaded = false;
    private boolean usersLoaded = false;

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        preferenceManager = new PreferenceManager(requireContext());

        initViews(view);
        
        viewModel = new ViewModelProvider(this).get(AdminHomeViewModel.class);
        setupObservers();
        setupClickListeners();

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void initViews(View view) {
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalDoctors = view.findViewById(R.id.tvTotalDoctors);
        tvTotalPatients = view.findViewById(R.id.tvTotalPatients);
        tvTotalAppointments = view.findViewById(R.id.tvTotalAppointments);
        tvAdminTip = view.findViewById(R.id.tvAdminTip);
        tvAlertMessage = view.findViewById(R.id.tvAlertMessage);
        llSystemAlerts = view.findViewById(R.id.llSystemAlerts);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        llMainContent = view.findViewById(R.id.llMainContent);

        cardViewUsers = view.findViewById(R.id.cardViewUsers);
        cardViewDoctors = view.findViewById(R.id.cardViewDoctors);
        cardViewPatients = view.findViewById(R.id.cardViewPatients);
        cardViewAppointments = view.findViewById(R.id.cardViewAppointments);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        recyclerViewAuditLogs = view.findViewById(R.id.recyclerViewAuditLogs);
        recyclerViewAuditLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Initialize adapters
        userAdapter = new UserAdapter();
        userAdapter.setOnUserClickListener(user -> {
            UserDetailsBottomSheet bottomSheet = UserDetailsBottomSheet.newInstance(user, "Recent Registrations");
            bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
        });
        recyclerViewUsers.setAdapter(userAdapter);
        
        auditLogAdapter = new com.haset.hasetapp.adapters.AuditLogAdapter(log -> {
            // Optional: show audit log details
        });
        recyclerViewAuditLogs.setAdapter(auditLogAdapter);
        
        // Initialize notification views
        ivNotification = view.findViewById(R.id.ivNotification);
        shimmerPageLoading = view.findViewById(R.id.shimmerPageLoading);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);

        swipeRefresh.setOnRefreshListener(() -> {
            statsLoaded = false;
            usersLoaded = false;
            viewModel.refresh();
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }, 1000);
        });
    }

    private void setupObservers() {
        showPageShimmer();
        
        viewModel.getDashboardStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvTotalUsers.setText(String.valueOf(stats.totalUsers));
                tvTotalDoctors.setText(String.valueOf(stats.totalDoctors));
                tvTotalPatients.setText(String.valueOf(stats.totalPatients));
                statsLoaded = true;
                checkDataLoaded();
            }
        });

        viewModel.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null && userAdapter != null) {
                // Focus on most recent registrations
                java.util.Collections.sort(users, (u1, u2) -> Long.compare(u2.getCreatedAt(), u1.getCreatedAt()));
                List<UserEntity> recentUsers = users.size() > 5 ? users.subList(0, 5) : users;
                userAdapter.setUsers(recentUsers);
                usersLoaded = true;
                checkDataLoaded();
            }
        });

        viewModel.getAllAppointments().observe(getViewLifecycleOwner(), appointments -> {
            if (appointments != null) {
                tvTotalAppointments.setText(String.valueOf(appointments.size()));
                
                int pendingCount = 0;
                for (com.haset.hasetapp.database.entities.AppointmentEntity a : appointments) {
                    if ("pending".equalsIgnoreCase(a.getStatus())) {
                        pendingCount++;
                    }
                }
                updateNotificationBadgeUI(pendingCount);
                updateSystemAlertsUI(pendingCount);
            }
        });

        viewModel.getAuditLogs().observe(getViewLifecycleOwner(), logs -> {
            if (logs != null && auditLogAdapter != null) {
                // Show latest logs first
                java.util.Collections.sort(logs, (l1, l2) -> Long.compare(l2.getTimestamp(), l1.getTimestamp()));
                List<com.haset.hasetapp.database.entities.AuditLogEntity> recentLogs = logs.size() > 10 ? logs.subList(0, 10) : logs;
                auditLogAdapter.setAuditLogs(recentLogs);
            }
        });

        viewModel.getAdminTip().observe(getViewLifecycleOwner(), tip -> {
            if (tip != null && tvAdminTip != null) {
                tvAdminTip.setText(tip);
            }
        });
    }
    
    private void checkDataLoaded() {
        if (statsLoaded && usersLoaded) {
            hidePageShimmer();
        }
    }

    private void showPageShimmer() {
        if (shimmerPageLoading != null) {
            shimmerPageLoading.startShimmer();
            shimmerPageLoading.setVisibility(View.VISIBLE);
            if (llMainContent != null) {
                llMainContent.setVisibility(View.GONE);
            }
        }
    }

    private void hidePageShimmer() {
        if (shimmerPageLoading != null) {
            shimmerPageLoading.stopShimmer();
            shimmerPageLoading.setVisibility(View.GONE);
            if (llMainContent != null) {
                llMainContent.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateSystemAlertsUI(int pendingCount) {
        if (pendingCount > 0) {
            llSystemAlerts.setVisibility(View.VISIBLE);
            tvAlertMessage.setText(String.format(java.util.Locale.getDefault(), 
                "You have %d pending appointments requiring immediate review.", pendingCount));
        } else {
            llSystemAlerts.setVisibility(View.GONE);
        }
    }

    private void updateNotificationBadgeUI(int count) {
        if (count > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        cardViewUsers.setOnClickListener(v -> navigateToManagement(0));
        cardViewDoctors.setOnClickListener(v -> navigateToManagement(1));
        cardViewPatients.setOnClickListener(v -> navigateToManagement(2));
        cardViewAppointments.setOnClickListener(v -> navigateToManagement(3));
        
        ivNotification.setOnClickListener(v -> {
            // Clear notification badge when opening notifications
            if (tvNotificationBadge != null) {
                tvNotificationBadge.setVisibility(View.GONE);
            }
            
            Intent intent = new Intent(requireContext(), AdminNotificationActivity.class);
            startActivity(intent);
        });

        llSystemAlerts.setOnClickListener(v -> navigateToManagement(3));
    }

    private void navigateToManagement(int tabIndex) {
        Intent intent = new Intent(requireContext(), AdminManagementActivity.class);
        intent.putExtra(AdminManagementActivity.EXTRA_SELECTED_TAB, tabIndex);
        startActivity(intent);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        if (recyclerViewUsers != null) recyclerViewUsers.setAdapter(null);
        if (recyclerViewAuditLogs != null) recyclerViewAuditLogs.setAdapter(null);
        
        userAdapter = null;
        auditLogAdapter = null;
        
        tvTotalUsers = null;
        tvTotalDoctors = null; 
        tvTotalPatients = null; 
        tvTotalAppointments = null;
        tvAdminTip = null;
        tvAlertMessage = null;
        llSystemAlerts = null;
        swipeRefresh = null;
        llMainContent = null;
        cardViewUsers = null;
        cardViewDoctors = null;
        cardViewPatients = null;
        cardViewAppointments = null;
        recyclerViewUsers = null;
        recyclerViewAuditLogs = null;
        ivNotification = null;
        tvNotificationBadge = null;
    }
}

