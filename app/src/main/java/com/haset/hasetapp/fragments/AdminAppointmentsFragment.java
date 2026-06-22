package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.AppointmentAdapter;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.haset.hasetapp.utils.FirebaseHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminHomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {
    private RecyclerView rvAppointments;
    private AppointmentAdapter appointmentAdapter;
    private View rootView;
    private LinearLayout shimmerContainer;
    private AdminHomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        rvAppointments = view.findViewById(R.id.rvAdminContent);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(this).get(AdminHomeViewModel.class);
        setupObservers();
    }

    private void setupObservers() {
        showShimmerLoading();
        viewModel.getAllAppointments().observe(getViewLifecycleOwner(), appointmentEntities -> {
            hideShimmerLoading();
            if (appointmentEntities != null) {
                List<Appointment> appointments = new ArrayList<>();
                for (com.haset.hasetapp.database.entities.AppointmentEntity entity : appointmentEntities) {
                    appointments.add(new Appointment(entity));
                }
                appointmentAdapter.setAppointments(appointments);
                if (appointments.isEmpty()) {
                    Snackbar.make(rootView, R.string.no_appointments_found, Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(rootView, R.string.failed_to_load_appointments, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupRecyclerView() {
        PreferenceManager preferenceManager = new PreferenceManager(requireContext());
        appointmentAdapter = new AppointmentAdapter(this, false, preferenceManager.getUserRole());
        rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAppointments.setAdapter(appointmentAdapter);
    }

    private void loadAppointments() {
        // Handled by setupObservers
    }

    private void showShimmerLoading() {
        shimmerContainer.setVisibility(View.VISIBLE);
        rvAppointments.setVisibility(View.GONE);
        ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 5, R.layout.shimmer_layout_appointment_card);
    }

    private void hideShimmerLoading() {
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
        rvAppointments.setVisibility(View.VISIBLE);
    }

    // Implement AppointmentAdapter.OnAppointmentActionListener methods
    @Override
    public void onApprove(Appointment appointment) {}

    @Override
    public void onDecline(Appointment appointment) {}

    @Override
    public void onAppointmentClick(Appointment appointment) {}

    @Override
    public void onCancel(Appointment appointment) {}

    @Override
    public void onReschedule(Appointment appointment) {}

    @Override
    public void onRateDoctor(Appointment appointment) {
        // Admins cannot rate doctors
    }

    @Override
    public void onStartSession(Appointment appointment) {
        // Admins don't partcipate in sessions
    }
}

