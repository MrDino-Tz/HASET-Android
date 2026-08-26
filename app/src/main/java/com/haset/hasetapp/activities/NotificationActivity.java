package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.NotificationBadgeHelper;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.haset.hasetapp.adapters.GenericNotificationAdapter;
import com.haset.hasetapp.database.entities.NotificationEntity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationActivity extends LocalizedAppCompatActivity implements 
        AppointmentNotificationAdapter.OnNotificationClickListener {
    private static final String PREF_NOTIFICATION_CLEAR_STATE = "notification_clear_state";
    private static final String KEY_HIDDEN_APPOINTMENT_IDS = "hidden_appointment_ids";

    private ImageView btnBack;
    private ImageView btnClearNotifications;
    private TabLayout tabs;
    private RecyclerView rvAppointmentNotifications;
    private RecyclerView rvPaymentNotifications;
    private LinearLayout shimmerAppointment;
    private LinearLayout shimmerPayment;
    
    private AppointmentNotificationAdapter appointmentAdapter;
    private GenericNotificationAdapter paymentAdapter;
    private PreferenceManager preferenceManager;
    private NotificationBadgeHelper badgeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupAdapters();
        setupTabListener();
        
        badgeHelper = new NotificationBadgeHelper(this);
        
        loadNotifications();
        
        badgeHelper.markGeneralNotificationsAsRead();
        badgeHelper.markAllTabsAsRead();
        clearAllTabBadges();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearNotifications = findViewById(R.id.btnClearNotifications);
        tabs = findViewById(R.id.tabs);
        rvAppointmentNotifications = findViewById(R.id.rvAppointmentNotifications);
        rvPaymentNotifications = findViewById(R.id.rvPaymentNotifications);
        
        shimmerAppointment = findViewById(R.id.shimmerAppointment);
        shimmerPayment = findViewById(R.id.shimmerPayment);
        
        preferenceManager = new PreferenceManager(this);
        
        btnBack.setOnClickListener(v -> finish());
        btnClearNotifications.setOnClickListener(v -> showClearNotificationsMenu());
    }

    private void setupAdapters() {
        appointmentAdapter = new AppointmentNotificationAdapter(this, this);
        
        rvAppointmentNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvAppointmentNotifications.setAdapter(appointmentAdapter);

        paymentAdapter = new GenericNotificationAdapter();
        rvPaymentNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvPaymentNotifications.setAdapter(paymentAdapter);
    }

    private void setupTabListener() {
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                clearTabBadge(tab.getPosition());
                
                switch (tab.getPosition()) {
                    case 0: // Appointment
                        rvAppointmentNotifications.setVisibility(View.VISIBLE);
                        rvPaymentNotifications.setVisibility(View.GONE);
                        break;
                    case 1: // Payment
                        rvAppointmentNotifications.setVisibility(View.GONE);
                        rvPaymentNotifications.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTabBadge(int tabIndex, int count) {
        TabLayout.Tab tab = tabs.getTabAt(tabIndex);
        if (tab != null) {
            com.google.android.material.badge.BadgeDrawable badge = tab.getOrCreateBadge();
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
                badge.setBackgroundColor(getResources().getColor(R.color.red_primary, getTheme()));
                badge.setVerticalOffset(-8);
            } else {
                badge.setVisible(false);
            }
        }
    }
    
    private void clearTabBadge(int tabIndex) {
        TabLayout.Tab tab = tabs.getTabAt(tabIndex);
        if (tab != null) {
            com.google.android.material.badge.BadgeDrawable badge = tab.getBadge();
            if (badge != null) {
                badge.setVisible(false);
                badge.clearNumber();
            }
        }
        if (badgeHelper != null) {
            if (tabIndex == 0) {
                badgeHelper.markAppointmentsAsRead();
            } else if (tabIndex == 1) {
                badgeHelper.markPaymentsAsRead();
            }
        }
    }
    
    private void clearAllTabBadges() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            clearTabBadge(i);
        }
        if (badgeHelper != null) {
            badgeHelper.markGeneralNotificationsAsRead();
            badgeHelper.markAllTabsAsRead();
        }
    }

    private void loadNotifications() {
        showAppointmentShimmer();
        showPaymentShimmer();
        loadAppointmentNotifications();
        loadPaymentNotifications();
    }

    private void loadAppointmentNotifications() {
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        
        FirebaseHelper.getAppointmentsByUser(userId, role, new FirebaseHelper.OnCompleteListener<List<com.haset.hasetapp.database.entities.AppointmentEntity>>() {
            @Override
            public void onSuccess(List<com.haset.hasetapp.database.entities.AppointmentEntity> appointmentEntities) {
                hideAppointmentShimmer();
                List<Appointment> appointments = new ArrayList<>();
                Set<String> hiddenAppointmentIds = getHiddenAppointmentIds();
                for (com.haset.hasetapp.database.entities.AppointmentEntity entity : appointmentEntities) {
                    if (entity != null && !hiddenAppointmentIds.contains(entity.getAppointmentId())) {
                        appointments.add(new Appointment(entity));
                    }
                }
                appointmentAdapter.setAppointments(appointments);
                int unreadCount = getUnreadAppointmentsCount(appointmentEntities, hiddenAppointmentIds);
                updateTabBadge(0, unreadCount);
                if (badgeHelper != null) {
                    badgeHelper.setAppointmentsUnreadCount(unreadCount);
                }
                updateHomeBadge();
            }

            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                hideAppointmentShimmer();
            }
        });
    }

    private void loadPaymentNotifications() {
        String userId = preferenceManager.getUserId();
        FirebaseHelper.getNotificationsRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hidePaymentShimmer();
                List<NotificationEntity> notifications = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationEntity notification = child.getValue(NotificationEntity.class);
                    if (notification != null) {
                        notifications.add(notification);
                    }
                }
                Collections.sort(notifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                paymentAdapter.setNotifications(notifications);
                int unreadCount = getUnreadNotificationsCount(notifications);
                updateTabBadge(1, unreadCount);
                if (badgeHelper != null) {
                    badgeHelper.setPaymentsUnreadCount(unreadCount);
                }
                updateHomeBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hidePaymentShimmer();
            }
        });
    }
    
    private int getUnreadAppointmentsCount(
            List<com.haset.hasetapp.database.entities.AppointmentEntity> appointments,
            Set<String> hiddenAppointmentIds) {
        int count = 0;
        for (com.haset.hasetapp.database.entities.AppointmentEntity appointment : appointments) {
            if (appointment != null
                    && !hiddenAppointmentIds.contains(appointment.getAppointmentId())
                    && !"read".equalsIgnoreCase(appointment.getStatus())) {
                count++;
            }
        }
        return count;
    }
    
    private int getUnreadNotificationsCount(List<NotificationEntity> notifications) {
        int count = 0;
        for (NotificationEntity notification : notifications) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }
    
    private void updateHomeBadge() {
        int total = badgeHelper.getTotalFromAllTabs();
        badgeHelper.setGeneralNotificationsUnreadCount(total);
    }

    @Override
    public void onNotificationClick(Appointment appointment) {
        android.content.Intent intent = new android.content.Intent(this, com.haset.hasetapp.activities.DashboardActivity.class);
        intent.putExtra("navigate_to", "appointments");
        intent.putExtra("appointment_id", appointment.getAppointmentId());
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void showClearNotificationsMenu() {
        PopupMenu popup = new PopupMenu(this, btnClearNotifications);
        popup.getMenuInflater().inflate(R.menu.clear_notifications_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_clear_appointments) {
                clearAppointmentNotifications();
                return true;
            } else if (id == R.id.action_clear_all) {
                clearAllNotifications();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void clearAppointmentNotifications() {
        Set<String> hiddenIds = getHiddenAppointmentIds();
        for (Appointment appointment : appointmentAdapter.getAppointments()) {
            if (appointment != null && appointment.getAppointmentId() != null) {
                hiddenIds.add(appointment.getAppointmentId());
            }
        }
        saveHiddenAppointmentIds(hiddenIds);
        appointmentAdapter.setAppointments(new ArrayList<>());
        updateTabBadge(0, 0);
        if (badgeHelper != null) {
            badgeHelper.setAppointmentsUnreadCount(0);
            updateHomeBadge();
        }
        android.widget.Toast.makeText(this, R.string.appointments_cleared, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void clearAllNotifications() {
        clearAppointmentNotifications();
        String userId = preferenceManager.getUserId();
        FirebaseHelper.getNotificationsRef(userId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    paymentAdapter.setNotifications(new ArrayList<>());
                    updateTabBadge(1, 0);
                    if (badgeHelper != null) {
                        badgeHelper.setPaymentsUnreadCount(0);
                        badgeHelper.markGeneralNotificationsAsRead();
                        updateHomeBadge();
                    }
                })
                .addOnFailureListener(error -> android.widget.Toast.makeText(
                        this,
                        getString(R.string.failed_to_clear_notifications, error.getMessage()),
                        android.widget.Toast.LENGTH_SHORT).show());
    }

    private Set<String> getHiddenAppointmentIds() {
        return new HashSet<>(getSharedPreferences(PREF_NOTIFICATION_CLEAR_STATE, MODE_PRIVATE)
                .getStringSet(KEY_HIDDEN_APPOINTMENT_IDS, new HashSet<>()));
    }

    private void saveHiddenAppointmentIds(Set<String> hiddenIds) {
        getSharedPreferences(PREF_NOTIFICATION_CLEAR_STATE, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_HIDDEN_APPOINTMENT_IDS, new HashSet<>(hiddenIds))
                .apply();
    }

    private void showAppointmentShimmer() {
        if (shimmerAppointment != null) {
            shimmerAppointment.setVisibility(View.VISIBLE);
            rvAppointmentNotifications.setVisibility(View.GONE);
            ShimmerHelper.showListShimmer(this, shimmerAppointment, 5, R.layout.shimmer_notification_list);
        }
    }

    private void hideAppointmentShimmer() {
        if (shimmerAppointment != null) {
            ShimmerHelper.hideListShimmer(shimmerAppointment);
            shimmerAppointment.setVisibility(View.GONE);
            rvAppointmentNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void showPaymentShimmer() {
        if (shimmerPayment != null) {
            shimmerPayment.setVisibility(View.VISIBLE);
            rvPaymentNotifications.setVisibility(View.GONE);
            ShimmerHelper.showListShimmer(this, shimmerPayment, 4, R.layout.shimmer_notification_list);
        }
    }

    private void hidePaymentShimmer() {
        if (shimmerPayment != null) {
            ShimmerHelper.hideListShimmer(shimmerPayment);
            shimmerPayment.setVisibility(View.GONE);
            rvPaymentNotifications.setVisibility(View.VISIBLE);
        }
    }
}
