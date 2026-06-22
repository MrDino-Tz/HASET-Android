package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.AppointmentAdapter;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.haset.hasetapp.utils.FirebaseHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AppointmentsViewModel;

import java.util.ArrayList;
import java.util.List;

public class CancelledAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {
    private RecyclerView rvAppointments;
    private AppointmentAdapter appointmentAdapter;
    private PreferenceManager preferenceManager;
    private View rootView;
    private LinearLayout shimmerContainer;
    private View emptyStateCard;
    private TextView tvEmptyStateTitle;
    private TextView tvEmptyStateSubtitle;
    private ImageView ivEmptyStateIcon;
    private AppointmentsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        rvAppointments = view.findViewById(R.id.rvAppointments);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        tvEmptyStateTitle = emptyStateCard.findViewById(R.id.tvEmptyStateTitle);
        tvEmptyStateSubtitle = emptyStateCard.findViewById(R.id.tvEmptyStateSubtitle);
        ivEmptyStateIcon = emptyStateCard.findViewById(R.id.ivEmptyStateIcon);

        preferenceManager = new PreferenceManager(requireContext());
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentsViewModel.class);
        setupObservers();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload appointments when fragment becomes visible
        loadAppointments();
    }

    private void setupRecyclerView() {
        String userRole = preferenceManager.getUserRole();
        boolean showActions = Constants.ROLE_DOCTOR.equals(userRole);
        appointmentAdapter = new AppointmentAdapter(this, showActions, userRole);
        rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAppointments.setAdapter(appointmentAdapter);
    }

    private void setupObservers() {
        showShimmerLoading();
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        
        viewModel.setUserInfo(userId, role);
        viewModel.getCancelledAppointments().observe(getViewLifecycleOwner(), appointments -> {
            if (isAdded() && rootView != null) {
                hideShimmerLoading();
                if (appointmentAdapter != null) {
                    appointmentAdapter.setAppointments(appointments);
                }

                if (appointments == null || appointments.isEmpty()) {
                    showEmptyState(getString(R.string.no_canceled_appointments_title),
                            getString(R.string.no_canceled_appointments_desc),
                            R.drawable.ic_no_data);
                } else {
                    hideEmptyState();
                }
            }
        });
    }

    public void loadAppointments() {
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void showShimmerLoading() {
        shimmerContainer.setVisibility(View.VISIBLE);
        rvAppointments.setVisibility(View.GONE);
        emptyStateCard.setVisibility(View.GONE); // Hide empty state when loading
        ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 4, R.layout.shimmer_layout_appointment_card);
    }
    private void hideShimmerLoading() {
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
        // rvAppointments.setVisibility(View.VISIBLE); // Visibility handled by empty state logic
    }

    private void showEmptyState(String title, String subtitle, int iconRes) {
        emptyStateCard.setVisibility(View.VISIBLE);
        rvAppointments.setVisibility(View.GONE);

        tvEmptyStateTitle.setText(title);
        tvEmptyStateSubtitle.setText(subtitle);
        ivEmptyStateIcon.setImageResource(iconRes);
    }

    private void hideEmptyState() {
        emptyStateCard.setVisibility(View.GONE);
        rvAppointments.setVisibility(View.VISIBLE);
    }

    public List<Appointment> getCurrentAppointments() {
        return appointmentAdapter != null ? appointmentAdapter.getAppointments() : new ArrayList<>();
    }

    @Override
    public void onApprove(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            showSnackbar(getString(R.string.invalid_appointment));
            return;
        }

        viewModel.updateStatus(appointment, Constants.STATUS_APPROVED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                showSnackbar(getString(R.string.appointment_approved_success));
            }

            @Override
            public void onError(String error) {
                showSnackbar(getString(R.string.failed_to_approve_appointment, error));
            }
        });
    }

    @Override
    public void onDecline(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            showSnackbar(getString(R.string.invalid_appointment));
            return;
        }

        viewModel.updateStatus(appointment, Constants.STATUS_DECLINED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                showSnackbar(getString(R.string.appointment_declined_success));
            }

            @Override
            public void onError(String error) {
                showSnackbar(getString(R.string.failed_to_decline_appointment, error));
            }
        });
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Handle appointment click - do nothing for cancelled appointments
    }

    @Override
    public void onCancel(Appointment appointment) {
        if (preferenceManager == null || !Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            return;
        }
        if (appointment == null || appointment.getAppointmentId() == null) {
            showSnackbar("Invalid appointment");
            return;
        }

            viewModel.updateStatus(appointment, Constants.STATUS_CANCELLED, new FirebaseHelper.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showSnackbar(getString(R.string.appointment_cancelled_success));
                }

                @Override
                public void onError(String error) {
                    showSnackbar(getString(R.string.failed_to_cancel_appointment, error));
                }
            });
    }

    @Override
    public void onReschedule(Appointment appointment) {
        showSnackbar(getString(R.string.reschedule_soon));
    }

    private void showSnackbar(String message) {
        if (isAdded() && getView() != null) {
            com.google.android.material.snackbar.Snackbar.make(getView(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRateDoctor(Appointment appointment) {
        // Rating only available for completed appointments
    }

    @Override
    public void onStartSession(Appointment appointment) {
        // Not applicable for canceled appointments
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clear adapter
        if (rvAppointments != null) {
            rvAppointments.setAdapter(null);
        }
        appointmentAdapter = null;
        
        // Null out view references
        rvAppointments = null;
        shimmerContainer = null;
        emptyStateCard = null;
        tvEmptyStateTitle = null;
        tvEmptyStateSubtitle = null;
        ivEmptyStateIcon = null;
        rootView = null;
    }
}
