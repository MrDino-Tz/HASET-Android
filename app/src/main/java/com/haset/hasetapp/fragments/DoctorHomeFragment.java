package com.haset.hasetapp.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import de.hdodenhof.circleimageview.CircleImageView;
import com.facebook.shimmer.ShimmerFrameLayout;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.activities.NotificationActivity;
import com.haset.hasetapp.activities.ArticleActivity;
import com.haset.hasetapp.activities.DoctorPatientsActivity;

import com.haset.hasetapp.adapters.AppointmentAdapter;
import com.haset.hasetapp.adapters.RecentAppointmentAdapter;
import com.haset.hasetapp.database.entities.AppointmentEntity; 
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.FileUploadHelper;
import com.haset.hasetapp.utils.ValidationUtils;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.DoctorHomeViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorHomeFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {
    private TextView tvGreeting, tvDoctorName, tvPendingCount, tvApprovedCount, tvCancelledCount, tvWalletBalance, tvQuickRatingCount, tvTodayDate;
    private RecyclerView rvAppointments, rvRecentAppointments;
    private ImageView ivNotification;
    private TextView tvNotificationBadge;
    private AppointmentAdapter appointmentAdapter;
    private RecentAppointmentAdapter recentAppointmentAdapter;
    private PreferenceManager preferenceManager;
    private NetworkUtils.NetworkCallback networkCallback;
    private com.haset.hasetapp.utils.AuditLogger auditLogger;

    // New UI members
    private TextView tvUserInitials;
    private LinearLayout llSchedule, llPatients, llArticlesAction;
    private DoctorHomeViewModel viewModel;
    private LinearLayout emptyState;
    private android.widget.ProgressBar progressBar;
    private com.google.android.material.card.MaterialCardView cardPendingApproval;
    private TextView tvPendingApproval;
    private MaterialButton btnResubmitDocuments;
    private Uri resubmitNinUri;
    private Uri resubmitMctUri;
    private AlertDialog resubmitDocumentsDialog;
    private TextView tvResubmitNinStatus;
    private TextView tvResubmitMctStatus;
    private MaterialButton btnSubmitResubmittedDocuments;

    private final ActivityResultLauncher<String[]> mctResubmitPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                if (!ValidationUtils.isPdfDocument(requireContext().getContentResolver(), uri)) {
                    Toast.makeText(requireContext(), R.string.error_document_pdf_only, Toast.LENGTH_SHORT).show();
                    return;
                }
                persistReadPermission(uri);
                resubmitMctUri = uri;
                updateResubmitDocumentsUi();
                Toast.makeText(requireContext(), R.string.mct_certificate_selected, Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> ninResubmitPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                if (!ValidationUtils.isPdfDocument(requireContext().getContentResolver(), uri)) {
                    Toast.makeText(requireContext(), R.string.error_document_pdf_only, Toast.LENGTH_SHORT).show();
                    return;
                }
                persistReadPermission(uri);
                resubmitNinUri = uri;
                updateResubmitDocumentsUi();
                Toast.makeText(requireContext(), R.string.nin_document_selected, Toast.LENGTH_SHORT).show();
            });

    // Header Profile Components
    private ImageView ivProfileHeader;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerProfileHeader;
    private android.view.View profileImageContainer;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPageLoading;
    private android.widget.LinearLayout layoutHomeContent;

    // Wallet Visibility
    private ImageView ivToggleWalletBalance;
    private boolean isWalletBalanceVisible = false;
    private double currentWalletBalance = 0;

    // Online Status Toggle
    private LinearLayout llOnlineStatus;
    private View statusIndicator;
    private TextView tvOnlineStatus;
    private boolean isOnline = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceManager = new PreferenceManager(requireContext());
        auditLogger = com.haset.hasetapp.utils.AuditLogger.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(DoctorHomeViewModel.class);
        setupObservers();

        // Set doctor name and initials
        String doctorName = preferenceManager.getUserName();
        if (doctorName != null && !doctorName.isEmpty()) {
            tvDoctorName.setText(getString(R.string.dr_prefix, doctorName));
            if (tvUserInitials != null) tvUserInitials.setText(com.haset.hasetapp.utils.ProfilePhotoHelper.getInitials(doctorName));
        }

        refreshHeaderProfile();

        // Set current date & dynamic greeting
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        if (tvTodayDate != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, MMM dd", java.util.Locale.getDefault());
            String dateString = dateFormat.format(calendar.getTime());
            tvTodayDate.setText(dateString);
        }
        
        if (tvGreeting != null) {
            int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 0 && hour < 12) {
                tvGreeting.setText(R.string.good_morning);
            } else if (hour >= 12 && hour < 17) {
                tvGreeting.setText(R.string.good_afternoon);
            } else if (hour >= 17 && hour < 21) {
                tvGreeting.setText(R.string.good_evening);
            } else {
                tvGreeting.setText(R.string.good_night);
            }
        }
    }

    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvCancelledCount = view.findViewById(R.id.tvCancelledCount);
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        tvTodayDate = view.findViewById(R.id.tvTodayDate);
        rvRecentAppointments = view.findViewById(R.id.rvRecentAppointments);
        
        // New UI Initials
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        ivProfileHeader = view.findViewById(R.id.ivProfileHeader);
        shimmerPageLoading = view.findViewById(R.id.shimmerPageLoading);
        layoutHomeContent = view.findViewById(R.id.layoutHomeContent);
        cardPendingApproval = view.findViewById(R.id.cardPendingApproval);
        tvPendingApproval = view.findViewById(R.id.tvPendingApproval);
        btnResubmitDocuments = view.findViewById(R.id.btnResubmitDocuments);
        shimmerProfileHeader = view.findViewById(R.id.shimmerProfileHeader);
        profileImageContainer = view.findViewById(R.id.profileImageContainer);
        
        // Initial state: Shimmer ON, Content OFF
        if (shimmerPageLoading != null) shimmerPageLoading.setVisibility(View.VISIBLE);
        if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.GONE);

        if (profileImageContainer != null) {
            profileImageContainer.setOnClickListener(v -> navigateToProfile());
        }
        
        refreshHeaderProfile();
        loadApprovalBanner();
        if (btnResubmitDocuments != null) {
            btnResubmitDocuments.setOnClickListener(v -> showResubmitDocumentsPrompt());
        }

        ivNotification = view.findViewById(R.id.ivNotification);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        
        // Online Status Toggle
        llOnlineStatus = view.findViewById(R.id.llOnlineStatus);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus);
        
        setupOnlineStatusToggle();
        loadDoctorOnlineStatus();
        
//        tvQuickRatingCount = view.findViewById(R.id.tvQuickRatingCount);
        
        // New Action Icons
        llSchedule = view.findViewById(R.id.llSchedule);
        llPatients = view.findViewById(R.id.llPatients);
        llArticlesAction = view.findViewById(R.id.llArticlesAction);
        
        if (llSchedule != null) {
            llSchedule.setOnClickListener(v -> navigateToAppointments(null));
        }
        
        if (llPatients != null) {
            llPatients.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), DoctorPatientsActivity.class)));
        }
        
        if (llArticlesAction != null) {
            llArticlesAction.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ArticleActivity.class);
                startActivity(intent);
            });
        }

        // Setup notification click listener
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                if (!isAdded()) return;
                android.content.Context context = getContext();
                if (context == null) return;
                
                // Clear notification badge when opening notifications
                if (viewModel != null) {
                    viewModel.clearNotificationCount();
                }
                
                Intent intent = new Intent(context, NotificationActivity.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    // Get the center coordinates of the clicked view
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int centerX = location[0] + v.getWidth() / 2;
                    int centerY = location[1] + v.getHeight() / 2;
                    
                    android.app.ActivityOptions options = android.app.ActivityOptions.makeClipRevealAnimation(
                        v, centerX, centerY, 0, 0);
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            });
        }
        
        // Wallet click listener
        View llWallet = view.findViewById(R.id.llWallet);
        ivToggleWalletBalance = view.findViewById(R.id.ivToggleWalletBalance);
        
        if (llWallet != null) {
            llWallet.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.DoctorWalletActivity.class);
                startActivity(intent);
            });
        }

        if (ivToggleWalletBalance != null) {
            ivToggleWalletBalance.setOnClickListener(v -> {
                isWalletBalanceVisible = !isWalletBalanceVisible;
                animateDisplay(tvWalletBalance, ivToggleWalletBalance, isWalletBalanceVisible, currentWalletBalance);
            });
        }
        
        // Setup filter button click listener
        View btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }
        
        // Setup "View All" button click listener
        View btnViewAll = view.findViewById(R.id.btnViewAll);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> navigateToAppointments(null));
        }
        
        // Initialize empty state and progress bar
        emptyState = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Setup stats card click listeners
        setupStatsCardListeners(view);
    }
    
    private void setupOnlineStatusToggle() {
        if (llOnlineStatus != null) {
            llOnlineStatus.setOnClickListener(v -> toggleOnlineStatus());
        }
    }
    
    private void loadDoctorOnlineStatus() {
        String doctorId = preferenceManager.getUserId();
        if (doctorId == null) return;
        
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorsNodeRef().child(doctorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Boolean online = snapshot.child("online").getValue(Boolean.class);
                            isOnline = online != null && online;
                        } else {
                            isOnline = false;
                        }
                        updateOnlineStatusUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isOnline = false;
                        updateOnlineStatusUI();
                    }
                });
    }
    
    private void toggleOnlineStatus() {
        isOnline = !isOnline;
        updateOnlineStatusUI();
        saveOnlineStatus();
    }
    
    private void updateOnlineStatusUI() {
        if (getContext() == null) return;
        
        if (isOnline) {
            if (statusIndicator != null) {
                statusIndicator.setBackgroundResource(R.drawable.bg_status_dot_online);
            }
            if (tvOnlineStatus != null) {
                tvOnlineStatus.setText(R.string.status_online);
                tvOnlineStatus.setTextColor(getResources().getColor(R.color.green_primary, null));
            }
        } else {
            if (statusIndicator != null) {
                statusIndicator.setBackgroundResource(R.drawable.bg_status_dot_offline);
            }
            if (tvOnlineStatus != null) {
                tvOnlineStatus.setText(R.string.status_offline);
                tvOnlineStatus.setTextColor(getResources().getColor(R.color.text_hint, null));
            }
        }
    }
    
    private void saveOnlineStatus() {
        String doctorId = preferenceManager.getUserId();
        if (doctorId == null) return;
        
        java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
        updates.put("online", isOnline);
        updates.put("onlineStatus", isOnline ? "online" : "offline");
        
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorsNodeRef().child(doctorId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (getView() != null && isAdded()) {
                        if (task.isSuccessful()) {
                            com.google.android.material.snackbar.Snackbar.make(getView(),
                                    isOnline ? R.string.status_online : R.string.status_offline,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        } else {
                            com.google.android.material.snackbar.Snackbar.make(getView(),
                                    R.string.error_generic,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                            isOnline = !isOnline;
                            updateOnlineStatusUI();
                        }
                    }
                });
    }
    
    private void setupStatsCardListeners(View view) {
        // Pending appointments card - navigate to appointments with pending filter
        view.findViewById(R.id.cardPending).setOnClickListener(v -> {
            navigateToAppointments(Constants.STATUS_PENDING);
        });
        
        // Completed appointments card - navigate to the completed tab
        view.findViewById(R.id.cardApproved).setOnClickListener(v -> {
            navigateToAppointments(Constants.STATUS_COMPLETED);
        });
        
        // Cancelled appointments card - navigate to appointments with cancelled filter
        view.findViewById(R.id.cardCancelled).setOnClickListener(v -> {
            navigateToAppointments(Constants.STATUS_CANCELLED);
        });
    }
    
    private void navigateToAppointments(String statusFilter) {
        if (getActivity() instanceof DashboardActivity) {
            DashboardActivity activity = (DashboardActivity) getActivity();
            // Store filter status to be used by AppointmentsFragment
            if (statusFilter != null) {
                preferenceManager.saveString("appointment_filter_status", statusFilter);
            } else {
                preferenceManager.saveString("appointment_filter_status", "all");
            }
            // Navigate to appointments tab
            activity.getBottomNavigation().setSelectedItemId(R.id.nav_appointments);
        }
    }
    
    private void navigateToProfile() {
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).getBottomNavigation().setSelectedItemId(R.id.nav_profile);
        }
    }

    private void setupRecyclerView() {
        // Setup recent appointments list (horizontal)
        recentAppointmentAdapter = new RecentAppointmentAdapter(appointment -> {
            // Handle recent appointment click
            onAppointmentClick(appointment);
        });
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvRecentAppointments.setLayoutManager(horizontalLayoutManager);
        rvRecentAppointments.setAdapter(recentAppointmentAdapter);
        
        // Setup main appointments list (vertical) - only if rvAppointments exists
        if (rvAppointments != null) {
            appointmentAdapter = new AppointmentAdapter(this, true, Constants.ROLE_DOCTOR);
            rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAppointments.setAdapter(appointmentAdapter);
        } else {
            // Initialize adapter for potential future use
            appointmentAdapter = new AppointmentAdapter(this, true, Constants.ROLE_DOCTOR);
        }
    }

    private void setupObservers() {
        String doctorId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        
        // Show shimmer initially
        if (shimmerPageLoading != null) {
            shimmerPageLoading.setVisibility(View.VISIBLE);
            shimmerPageLoading.startShimmer();
        }
        if (layoutHomeContent != null) {
            layoutHomeContent.setVisibility(View.GONE);
        }

        // Never leave the user on the loading shimmer indefinitely. Reveal the
        // page after a short grace period; each section still fills in as its
        // data arrives (appointments, wallet, rating, notifications).
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) hidePageShimmer();
        }, 2500);

        // Observe Appointments
        viewModel.getAppointments(doctorId).observe(getViewLifecycleOwner(), appointments -> {
            if (appointments != null) {
                int pending = 0, completed = 0, cancelled = 0;
                for (Appointment a : appointments) {
                    switch (a.getStatus()) {
                        case Constants.STATUS_PENDING: pending++; break;
                        case Constants.STATUS_COMPLETED: completed++; break;
                        case Constants.STATUS_CANCELLED: cancelled++; break;
                    }
                }
                updateUIWithAppointments(appointments, pending, completed, cancelled);
                hidePageShimmer();
            }
        });

        // Observe Wallet
        viewModel.getWalletBalance(doctorId).observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                currentWalletBalance = wallet.getBalance();
                updateWalletBalanceDisplay();
            } else {
                currentWalletBalance = 0;
                updateWalletBalanceDisplay();
            }
        });

        // Observe Rating Count
        viewModel.getRatingCount(doctorId).observe(getViewLifecycleOwner(), count -> {
            if (tvQuickRatingCount != null) {
                tvQuickRatingCount.setText(String.valueOf(count));
            }
        });

        // Observe Notification Count
        viewModel.getNotificationCount(doctorId, role).observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }
    
    private void hidePageShimmer() {
        if (shimmerPageLoading != null && shimmerPageLoading.getVisibility() == View.VISIBLE) {
            shimmerPageLoading.stopShimmer();
            shimmerPageLoading.setVisibility(View.GONE);
            if (layoutHomeContent != null) {
                layoutHomeContent.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateUIWithAppointments(List<Appointment> appointments, int pendingCount, int completedCount, int cancelledCount) {
        tvPendingCount.setText(String.valueOf(pendingCount));
        tvApprovedCount.setText(String.valueOf(completedCount));
        tvCancelledCount.setText(String.valueOf(cancelledCount));

        // Sort appointments by date (most recent first)
        Collections.sort(appointments, (a1, a2) -> {
            if (a1.getDate() == null && a2.getDate() == null) return 0;
            if (a1.getDate() == null) return 1;
            if (a2.getDate() == null) return -1;
            return a2.getDate().compareTo(a1.getDate()); // Descending order
        });

        // Set all appointments to main list (if RecyclerView exists)
        if (appointmentAdapter != null) {
            appointmentAdapter.setAppointments(appointments);
        }

        // Set recent appointments (limit to 3 most recent)
        List<Appointment> recentAppointments = new ArrayList<>();
        int count = Math.min(3, appointments.size());
        for (int i = 0; i < count; i++) {
            recentAppointments.add(appointments.get(i));
        }
        recentAppointmentAdapter.setAppointments(recentAppointments);
        
        // Show/hide empty state based on appointment count
        if (emptyState != null && rvRecentAppointments != null) {
            if (appointments.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rvRecentAppointments.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rvRecentAppointments.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onApprove(Appointment appointment) {
        // Convert Appointment model to AppointmentEntity for FirebaseHelper
        AppointmentEntity appointmentEntity = new AppointmentEntity(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getReason(),
                Constants.STATUS_APPROVED, // Set status to APPROVED
                appointment.getAppointmentType()
        );
        appointmentEntity.setCreatedAt(appointment.getCreatedAt());

        viewModel.updateAppointmentStatus(appointment, Constants.STATUS_APPROVED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                AuditLogger.getInstance(requireContext()).logAppointmentUpdated(appointment.getAppointmentId(), "APPROVE", "Approved appointment with " + appointment.getPatientName());
                if (getView() != null) {
                    showApprovalDialog(appointment);
                }
            }

            @Override
            public void onError(String error) {
                if (getView() != null) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(getView(), error);
                }
            }
        });
    }

    private void showApprovalDialog(Appointment appointment) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chat_start);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvPatientInfo = dialog.findViewById(R.id.tvPatientInfo);
        TextView tvAppointmentDetails = dialog.findViewById(R.id.tvAppointmentDetails);
        TextView tvCountdown = dialog.findViewById(R.id.tvCountdown);
        MaterialButton btnStartChat = dialog.findViewById(R.id.btnStartChat);
        TextView tvSessionExpired = dialog.findViewById(R.id.tvSessionExpired);

        tvPatientInfo.setText(getString(R.string.appointment_with_patient, appointment.getPatientName()));
        tvAppointmentDetails.setText(String.format("%s at %s",
                appointment.getDate() != null ? appointment.getDate() : "",
                appointment.getTime() != null ? appointment.getTime() : ""));

        long approvedAt = System.currentTimeMillis();

        btnStartChat.setOnClickListener(v -> {
            dialog.dismiss();
            startChatWithPatient(appointment, approvedAt);
        });

        // 60-second countdown
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] secondsLeft = {60};
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (secondsLeft[0] <= 0) {
                    tvCountdown.setText("00:00");
                    btnStartChat.setVisibility(View.GONE);
                    tvSessionExpired.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> { if (dialog.isShowing()) dialog.dismiss(); }, 3000);
                    return;
                }
                int min = secondsLeft[0] / 60;
                int sec = secondsLeft[0] % 60;
                tvCountdown.setText(String.format("%02d:%02d", min, sec));
                secondsLeft[0]--;
                handler.postDelayed(this, 1000);
            }
        });

        dialog.show();
    }

    private void startChatWithPatient(Appointment appointment, long approvedAt) {
        Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ChatActivity.class);
        intent.putExtra(Constants.EXTRA_CHAT_USER_ID, appointment.getPatientId());
        intent.putExtra(Constants.EXTRA_CHAT_USER_NAME, appointment.getPatientName());
        intent.putExtra(Constants.EXTRA_APPOINTMENT_ID, appointment.getAppointmentId());
        intent.putExtra(Constants.EXTRA_IS_FROM_APPOINTMENT, true);
        intent.putExtra(Constants.EXTRA_APPOINTMENT_APPROVED_AT, approvedAt);
        startActivity(intent);
    }

    @Override
    public void onDecline(Appointment appointment) {
        // Convert Appointment model to AppointmentEntity for FirebaseHelper
        AppointmentEntity appointmentEntity = new AppointmentEntity(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getReason(),
                Constants.STATUS_DECLINED, // Set status to DECLINED
                appointment.getAppointmentType()
        );
        appointmentEntity.setCreatedAt(appointment.getCreatedAt());

        viewModel.updateAppointmentStatus(appointment, Constants.STATUS_DECLINED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                AuditLogger.getInstance(requireContext()).logAppointmentUpdated(appointment.getAppointmentId(), "DECLINE", "Declined appointment with " + appointment.getPatientName());
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), R.string.appointment_declined, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (getView() != null) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(getView(), error);
                }
            }
        });
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Navigate to appointments tab when clicking on recent appointment
        navigateToAppointments(null);
    }
    
    private void showFilterDialog() {
        String[] filterOptions = {
            getString(R.string.all),
            getString(R.string.pending),
            getString(R.string.approved),
            getString(R.string.canceled),
            getString(R.string.declined)
        };
        
        final String[] filterValues = {null, Constants.STATUS_PENDING, Constants.STATUS_APPROVED, Constants.STATUS_CANCELLED, Constants.STATUS_DECLINED};
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.filter_appointments)
                .setItems(filterOptions, (dialog, which) -> {
                    navigateToAppointments(filterValues[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onCancel(Appointment appointment) {
        // Doctors don't cancel appointments
    }

    @Override
    public void onReschedule(Appointment appointment) {
        // Doctors don't reschedule appointments
    }

    @Override
    public void onRateDoctor(Appointment appointment) {
        // Doctors cannot rate themselves
    }

    @Override
    public void onStartSession(Appointment appointment) {
        if (appointment == null) return;
        
        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(appointment.getAppointmentType())) {
            Intent chatIntent = new Intent(requireContext(), com.haset.hasetapp.activities.ChatActivity.class);
            chatIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, appointment.getPatientId());
            chatIntent.putExtra(Constants.EXTRA_CHAT_USER_NAME, appointment.getPatientName());
            startActivity(chatIntent);
        }
    }

    private void animateDisplay(TextView textView, ImageView toggleIcon, boolean isVisible, double amount) {
        if (textView == null || toggleIcon == null) return;

        textView.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction(() -> {
                if (isVisible) {
                    textView.setText(String.format(java.util.Locale.getDefault(), getString(R.string.currency_format), amount));
                    toggleIcon.setAlpha(1.0f);
                } else {
                    textView.setText("•••••••• TZS");
                    toggleIcon.setAlpha(0.4f);
                }
                
                textView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start();
            })
            .start();
    }

    private void updateWalletBalanceDisplay() {
        if (tvWalletBalance == null) return;
        tvWalletBalance.setText(isWalletBalanceVisible ? 
            String.format(java.util.Locale.getDefault(), getString(R.string.currency_format), currentWalletBalance) : "•••••••• TZS");
        if (ivToggleWalletBalance != null) ivToggleWalletBalance.setAlpha(isWalletBalanceVisible ? 1.0f : 0.4f);
    }

    private void updateNotificationBadge() {
        String userId = preferenceManager.getUserId();
        if (userId == null) return;
        
        FirebaseDatabase.getInstance().getReference(Constants.NOTIFICATIONS_PATH)
                .child(userId)
                .orderByChild("read")
                .equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long unreadCount = snapshot.getChildrenCount();
                        if (tvNotificationBadge != null) {
                            if (unreadCount > 0) {
                                tvNotificationBadge.setVisibility(View.VISIBLE);
                                tvNotificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                            } else {
                                tvNotificationBadge.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DoctorHomeFragment", "Failed to load notification count: " + error.getMessage());
                    }
                });
    }

    private void loadRatingStats() {
        String doctorId = preferenceManager.getUserId();
        if (doctorId == null) return;
        
        FirebaseDatabase.getInstance().getReference(Constants.DOCTOR_RATINGS_PATH)
                .orderByChild("doctorId")
                .equalTo(doctorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long ratingCount = snapshot.getChildrenCount();
                        if (tvQuickRatingCount != null) {
                            tvQuickRatingCount.setText(String.valueOf(ratingCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DoctorHomeFragment", "Failed to load rating stats: " + error.getMessage());
                        if (tvQuickRatingCount != null) {
                            tvQuickRatingCount.setText("0");
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Initialize network monitoring
        initializeNetworkMonitoring();
        
        // Data is handled by observers automatically or can be manually triggered if needed
        
        // Set name and initials
        String doctorName = preferenceManager.getUserName();
        if (doctorName != null && !doctorName.isEmpty()) {
            tvDoctorName.setText(getString(R.string.dr_prefix, doctorName));
            if (tvUserInitials != null) tvUserInitials.setText(com.haset.hasetapp.utils.ProfilePhotoHelper.getInitials(doctorName));
        }

        // Set current date
        if (tvTodayDate != null) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, MMM dd", java.util.Locale.getDefault());
            String dateString = dateFormat.format(calendar.getTime());
            tvTodayDate.setText(dateString);
        }
        refreshHeaderProfile();
        loadDoctorOnlineStatus();
        loadApprovalBanner();
    }

    private void loadApprovalBanner() {
        if (cardPendingApproval == null || preferenceManager == null) return;
        String userId = preferenceManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            cardPendingApproval.setVisibility(View.GONE);
            return;
        }
        FirebaseHelper.getDoctorsNodeRef().child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || cardPendingApproval == null) return;
                String approvalStatus = snapshot.child("approvalStatus").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                boolean rejected = "rejected".equalsIgnoreCase(approvalStatus)
                        || "rejected".equalsIgnoreCase(status)
                        || Boolean.TRUE.equals(snapshot.child("rejected").getValue(Boolean.class));
                String rejectionReason = snapshot.child("rejectionReason").getValue(String.class);

                if (!rejected) {
                    boolean approved = Boolean.TRUE.equals(snapshot.child("approved").getValue(Boolean.class));
                    if (approved) {
                        cardPendingApproval.setVisibility(View.GONE);
                        return;
                    }
                }

                cardPendingApproval.setVisibility(View.VISIBLE);
                if (tvPendingApproval != null) {
                    if (rejected) {
                        String message = "Your doctor verification was rejected.";
                        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                            message += "\nReason: " + rejectionReason.trim();
                        }
                        message += "\nUpload new NIN and MCT certificate PDFs for another review.";
                        tvPendingApproval.setText(message);
                    } else {
                        tvPendingApproval.setText(R.string.doctor_awaiting_admin_approval);
                    }
                }
                if (btnResubmitDocuments != null) {
                    btnResubmitDocuments.setVisibility(rejected ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && cardPendingApproval != null) {
                    cardPendingApproval.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showResubmitDocumentsPrompt() {
        resubmitNinUri = null;
        resubmitMctUri = null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_resubmit_documents, null, false);
        MaterialButton btnChooseNin = dialogView.findViewById(R.id.btnChooseNinDocument);
        MaterialButton btnChooseMct = dialogView.findViewById(R.id.btnChooseMctDocument);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelResubmitDocuments);
        btnSubmitResubmittedDocuments = dialogView.findViewById(R.id.btnSubmitResubmitDocuments);
        tvResubmitNinStatus = dialogView.findViewById(R.id.tvResubmitNinStatus);
        tvResubmitMctStatus = dialogView.findViewById(R.id.tvResubmitMctStatus);

        resubmitDocumentsDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnChooseNin.setOnClickListener(v -> ninResubmitPicker.launch(new String[]{"application/pdf"}));
        btnChooseMct.setOnClickListener(v -> mctResubmitPicker.launch(new String[]{"application/pdf"}));
        btnCancel.setOnClickListener(v -> resubmitDocumentsDialog.dismiss());
        btnSubmitResubmittedDocuments.setOnClickListener(v -> uploadResubmittedDocuments());

        resubmitDocumentsDialog.setOnDismissListener(dialog -> {
            tvResubmitNinStatus = null;
            tvResubmitMctStatus = null;
            btnSubmitResubmittedDocuments = null;
            resubmitDocumentsDialog = null;
        });
        resubmitDocumentsDialog.show();
        updateResubmitDocumentsUi();
    }

    private void updateResubmitDocumentsUi() {
        if (tvResubmitNinStatus != null) {
            tvResubmitNinStatus.setText(resubmitNinUri == null
                    ? "No NIN PDF selected"
                    : "Selected: " + getFileName(resubmitNinUri));
        }
        if (tvResubmitMctStatus != null) {
            tvResubmitMctStatus.setText(resubmitMctUri == null
                    ? "No MCT certificate PDF selected"
                    : "Selected: " + getFileName(resubmitMctUri));
        }
        if (btnSubmitResubmittedDocuments != null) {
            btnSubmitResubmittedDocuments.setEnabled(resubmitNinUri != null && resubmitMctUri != null);
        }
    }

    private void uploadResubmittedDocuments() {
        if (resubmitNinUri == null || resubmitMctUri == null) return;
        String userId = preferenceManager.getUserId();
        if (userId == null || userId.isEmpty()) return;

        if (btnSubmitResubmittedDocuments != null) {
            btnSubmitResubmittedDocuments.setEnabled(false);
        }
        Toast.makeText(requireContext(), R.string.uploading_documents, Toast.LENGTH_SHORT).show();
        FileUploadHelper.uploadFile(requireContext(), resubmitNinUri, "document", getFileName(resubmitNinUri),
                "doctor_verification", new FileUploadHelper.OnFileUploadListener() {
                    @Override
                    public void onUploadStart() {}

                    @Override
                    public void onUploadProgress(double progress) {}

                    @Override
                    public void onUploadSuccess(String ninUrl, String fileName, long fileSize) {
                        uploadResubmittedMctDocument(userId, ninUrl);
                    }

                    @Override
                    public void onUploadError(String error) {
                        updateResubmitDocumentsUi();
                        Toast.makeText(requireContext(), error != null ? error : "NIN upload failed", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadResubmittedMctDocument(String userId, String ninUrl) {
        FileUploadHelper.uploadFile(requireContext(), resubmitMctUri, "document", getFileName(resubmitMctUri),
                "doctor_verification", new FileUploadHelper.OnFileUploadListener() {
                    @Override
                    public void onUploadStart() {}

                    @Override
                    public void onUploadProgress(double progress) {}

                    @Override
                    public void onUploadSuccess(String mctUrl, String fileName, long fileSize) {
                        submitVerificationDocuments(userId, ninUrl, mctUrl);
                    }

                    @Override
                    public void onUploadError(String error) {
                        updateResubmitDocumentsUi();
                        Toast.makeText(requireContext(), error != null ? error : "MCT certificate upload failed", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void submitVerificationDocuments(String userId, String ninUrl, String mctUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId + "/ninDocumentUrl", ninUrl);
        updates.put("users/" + userId + "/mctCertificateUrl", mctUrl);
        updates.put("doctors/" + userId + "/ninDocumentUrl", ninUrl);
        updates.put("doctors/" + userId + "/mctCertificateUrl", mctUrl);
        updates.put("doctors/" + userId + "/approved", false);
        updates.put("doctors/" + userId + "/verified", false);
        updates.put("doctors/" + userId + "/rejected", false);
        updates.put("doctors/" + userId + "/approvalStatus", "pending");
        updates.put("doctors/" + userId + "/rejectionReason", null);
        updates.put("doctors/" + userId + "/resubmittedAt", com.google.firebase.database.ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(ignored -> {
                    if (resubmitDocumentsDialog != null && resubmitDocumentsDialog.isShowing()) {
                        resubmitDocumentsDialog.dismiss();
                    }
                    resubmitNinUri = null;
                    resubmitMctUri = null;
                    Toast.makeText(requireContext(), "Documents resubmitted for review", Toast.LENGTH_LONG).show();
                    loadApprovalBanner();
                })
                .addOnFailureListener(error ->
                        {
                            updateResubmitDocumentsUi();
                            Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        });
    }

    private void persistReadPermission(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null && uri != null) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/")) {
                result = result.substring(result.lastIndexOf('/') + 1);
            }
        }
        return result != null ? result : "document.pdf";
    }
    
    @Override
    public void onPause() {
        super.onPause();

        // Stop network monitoring when fragment is not visible
        if (networkCallback != null) {
            NetworkUtils.removeNetworkCallback(requireContext(), networkCallback);
        }
    }
    
    private void initializeNetworkMonitoring() {
        networkCallback = new NetworkUtils.NetworkCallback() {
            @Override
            public void onNetworkAvailable() {
                // Network is available, refresh data
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // In MVVM, observers handle data updates automatically if they use real-time listeners.
                        // Or we can manually trigger a reload in the ViewModel.
                    });
                }
            }
            
            @Override
            public void onNetworkLost() {
                // Network is lost, show message to user
                if (isAdded()) {
                    android.view.View view = getView();
                    if (view != null) {
                        try {
                            com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(
                                view, R.string.network_lost, com.google.android.material.snackbar.Snackbar.LENGTH_LONG);
                            snackbar.setBackgroundTint(getResources().getColor(R.color.colorError));
                            snackbar.show();
                        } catch (Exception e) {
                            android.app.Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> Toast.makeText(activity, R.string.network_lost, Toast.LENGTH_SHORT).show());
                            }
                        }
                    }
                }
            }
        };
        
        NetworkUtils.addNetworkCallback(requireContext(), networkCallback);
    }

    private void refreshHeaderProfile() {
        if (ivProfileHeader == null || preferenceManager == null) return;
        String userId = preferenceManager.getUserId();
        com.haset.hasetapp.utils.ProfilePhotoHelper.loadProfilePhoto(requireContext(), userId, ivProfileHeader, shimmerProfileHeader, tvUserInitials);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove network callback to prevent leaks
        if (networkCallback != null) {
            NetworkUtils.removeNetworkCallback(requireContext(), networkCallback);
            networkCallback = null;
        }
        
        // Clear adapters
        if (rvAppointments != null) {
            rvAppointments.setAdapter(null);
        }
        if (rvRecentAppointments != null) {
            rvRecentAppointments.setAdapter(null);
        }
        appointmentAdapter = null;
        recentAppointmentAdapter = null;
        
        // Null out view references
        tvGreeting = null;
        tvDoctorName = null;
        tvPendingCount = null;
        tvApprovedCount = null; 
        tvCancelledCount = null;
        tvWalletBalance = null;
        tvQuickRatingCount = null;
        tvTodayDate = null;
        rvAppointments = null;
        rvRecentAppointments = null;
        ivNotification = null;
        tvNotificationBadge = null;
        
        tvUserInitials = null;
        llSchedule = null;
        llPatients = null;
        ivProfileHeader = null;
        shimmerProfileHeader = null;
        profileImageContainer = null;
    }
}
