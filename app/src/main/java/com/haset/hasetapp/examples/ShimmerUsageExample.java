package com.haset.hasetapp.examples;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.ShimmerHelper;

/**
 * Example implementation of ShimmerHelper
 * Shows different ways to use skeleton loading effects
 */
public class ShimmerUsageExample extends Fragment {

    private ShimmerFrameLayout appointmentShimmer;
    private LinearLayout appointmentContainer;
    private LinearLayout doctorListContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a simple layout for demonstration
        View rootView = inflater.inflate(R.layout.fragment_example_shimmer, container, false);
        
        initializeViews(rootView);
        setupShimmerEffects();
        simulateDataLoading();
        
        return rootView;
    }

    private void initializeViews(View rootView) {
        appointmentContainer = rootView.findViewById(R.id.appointmentContainer);
        doctorListContainer = rootView.findViewById(R.id.doctorListContainer);
    }

    private void setupShimmerEffects() {
        // Method 1: Create shimmer for appointment cards
        appointmentShimmer = ShimmerHelper.createAppointmentShimmer(requireContext());
        
        // Method 2: Show shimmer for doctor list
        ShimmerHelper.showListShimmer(
                requireContext(),
                doctorListContainer,
                3, // Show 3 shimmer items
                R.layout.shimmer_layout_doctor_list
        );
    }

    private void simulateDataLoading() {
        // Show shimmer while loading
        if (appointmentShimmer != null && appointmentContainer != null) {
            ShimmerHelper.showShimmer(appointmentShimmer, appointmentContainer);
        }
        
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Hide shimmer and show actual content
            if (appointmentShimmer != null && appointmentContainer != null) {
                ShimmerHelper.hideShimmer(appointmentShimmer, appointmentContainer);
            }
            ShimmerHelper.hideListShimmer(doctorListContainer);
            
            // Load actual data
            loadAppointments();
            loadDoctors();
        }, 2000); // 2 second delay
    }

    private void loadAppointments() {
        // Add actual appointment views to container
        if (appointmentContainer != null) {
            appointmentContainer.addView(createAppointmentItem("Dr. John Smith", "10:00 AM"));
            appointmentContainer.addView(createAppointmentItem("Dr. Sarah Johnson", "2:30 PM"));
        }
    }

    private void loadDoctors() {
        // Add actual doctor views to container
        if (doctorListContainer != null) {
            doctorListContainer.addView(createDoctorItem("Dr. Michael Brown", "Cardiology"));
            doctorListContainer.addView(createDoctorItem("Dr. Emily Davis", "Pediatrics"));
            doctorListContainer.addView(createDoctorItem("Dr. Robert Wilson", "Orthopedics"));
        }
    }

    private View createAppointmentItem(String doctorName, String time) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);
        
        // Create doctor name TextView
        android.widget.TextView nameView = new android.widget.TextView(requireContext());
        nameView.setText(doctorName);
        nameView.setTextSize(16);
        nameView.setTextColor(getResources().getColor(R.color.text_primary));
        
        // Create time TextView
        android.widget.TextView timeView = new android.widget.TextView(requireContext());
        timeView.setText(time);
        timeView.setTextSize(14);
        timeView.setTextColor(getResources().getColor(R.color.text_secondary));
        
        item.addView(nameView);
        item.addView(timeView);
        
        return item;
    }

    private View createDoctorItem(String doctorName, String specialty) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);
        
        // Create doctor name TextView
        android.widget.TextView nameView = new android.widget.TextView(requireContext());
        nameView.setText(doctorName);
        nameView.setTextSize(16);
        nameView.setTextColor(getResources().getColor(R.color.text_primary));
        
        // Create specialty TextView
        android.widget.TextView specialtyView = new android.widget.TextView(requireContext());
        specialtyView.setText(specialty);
        specialtyView.setTextSize(14);
        specialtyView.setTextColor(getResources().getColor(R.color.text_secondary));
        
        item.addView(nameView);
        item.addView(specialtyView);
        
        return item;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up shimmer animations
        if (appointmentShimmer != null) {
            appointmentShimmer.stopShimmer();
        }
        
        // Stop all shimmer animations in containers
        stopShimmerInContainer(appointmentContainer);
        stopShimmerInContainer(doctorListContainer);
    }

    private void stopShimmerInContainer(ViewGroup container) {
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof ShimmerFrameLayout) {
                    ((ShimmerFrameLayout) child).stopShimmer();
                }
            }
        }
    }
}
