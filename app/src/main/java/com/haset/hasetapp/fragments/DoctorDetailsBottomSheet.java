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
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.BookAppointmentActivity;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.util.List;
import java.util.Locale;

// DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
// import com.haset.hasetapp.viewmodels.ReviewsViewModel;

public class DoctorDetailsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "DoctorDetailsBottomSheet";
    private static final String ARG_DOCTOR = "doctor";

    private Doctor doctor;
    private boolean isFavorite = false;
    
    // UI Components
    private MaterialToolbar toolbar;
    private ImageView ivDoctorImage;
    private TextView tvDoctorName, tvSpecialty, tvPhoneNumber, tvEmail, tvAddress, tvAvailableTime, tvConsultationFee, tvBio, tvUserInitials;
    private MaterialButton btnBookAppointment;
    private ImageView ivVerified;
    private ImageView ivVerifiedImage;
    private TextView tvDemoBadge;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerDoctorImage;

    public static DoctorDetailsBottomSheet newInstance(Doctor doctor) {
        DoctorDetailsBottomSheet fragment = new DoctorDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOCTOR, doctor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            doctor = (Doctor) getArguments().getSerializable(ARG_DOCTOR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_doctor_details, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbarDoctorDetails);
        ivDoctorImage = view.findViewById(R.id.ivDoctorImage);
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvSpecialty = view.findViewById(R.id.tvSpecialty);
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvAvailableTime = view.findViewById(R.id.tvAvailableTime);
        tvConsultationFee = view.findViewById(R.id.tvConsultationFee);
        btnBookAppointment = view.findViewById(R.id.btnBookAppointment);
        ivVerified = view.findViewById(R.id.ivVerified);
        ivVerifiedImage = view.findViewById(R.id.ivVerifiedImage);
        tvDemoBadge = view.findViewById(R.id.tvDemoBadge);
        tvBio = view.findViewById(R.id.tvBio);
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        shimmerDoctorImage = view.findViewById(R.id.shimmerDoctorImage);

        // Pre-populate with passed object if available
        if (doctor != null) {
            updateUI(doctor);
            // Fetch fresh data in background to ensure all details are loaded
            refreshDoctorData();
        }

        toolbar.setNavigationOnClickListener(v -> dismiss());
    }

    private void updateUI(Doctor doctor) {
        if (doctor == null || getContext() == null) return;

        tvDoctorName.setText(getString(R.string.dr_prefix, doctor.getFullName()));
        
        String specialty = doctor.getSpecialty();
        tvSpecialty.setText(specialty != null && !specialty.isEmpty() && !specialty.equals("Specialty") ? specialty : "Loading...");
        
        String phone = doctor.getPhone();
        tvPhoneNumber.setText(phone != null && !phone.isEmpty() ? phone : "Not available");
        
        String email = doctor.getEmail();
        tvEmail.setText(email != null && !email.isEmpty() ? email : "Not available");
        
        String location = doctor.getLocation();
        tvAddress.setText(location != null && !location.isEmpty() && !location.contains("City") ? location : "Location not specified");
        
        double fee = doctor.getConsultationFee();
        tvConsultationFee.setText(String.format(java.util.Locale.getDefault(), "%,.0f TZS", fee));
        
        if (tvBio != null) {
            String bio = doctor.getAbout();
            tvBio.setText(bio != null && !bio.isEmpty() ? bio : getString(R.string.no_bio_available));
        }

        if (ivVerified != null) {
            ivVerified.setVisibility(doctor.isVerified() ? View.VISIBLE : View.GONE);
        }
        if (ivVerifiedImage != null) {
            ivVerifiedImage.setVisibility(doctor.isVerified() ? View.VISIBLE : View.GONE);
        }
        if (tvDemoBadge != null) {
            tvDemoBadge.setVisibility(doctor.isDemo() ? View.VISIBLE : View.GONE);
        }

        // Format available times
        if (tvAvailableTime != null) {
            if (doctor.getAvailableTimes() != null && !doctor.getAvailableTimes().isEmpty()) {
                List<String> times = doctor.getAvailableTimes();
                String firstTime = times.get(0);
                String lastTime = times.get(times.size() - 1);
                tvAvailableTime.setText(firstTime + " - " + lastTime);
            } else {
                tvAvailableTime.setText(R.string.contact_for_availability);
            }
        }

        // Load profile photo with initials fallback
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), doctor.getDoctorId(), ivDoctorImage, shimmerDoctorImage, tvUserInitials);

        btnBookAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BookAppointmentActivity.class);
            intent.putExtra(Constants.EXTRA_DOCTOR_ID, doctor.getDoctorId());
            intent.putExtra("doctor", doctor); // Pass full object for instant loading
            intent.putExtra("doctor_name", doctor.getFullName());
            startActivity(intent);
            dismiss();
        });
    }

    private void refreshDoctorData() {
        if (doctor == null || doctor.getDoctorId() == null) return;
        
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorById(doctor.getDoctorId(), new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Doctor>() {
            @Override
            public void onSuccess(Doctor fullDoctor) {
                if (isAdded() && fullDoctor != null) {
                    doctor = fullDoctor;
                    updateUI(fullDoctor);
                }
            }

            @Override
            public void onError(String error) {
                // Log or handle error if needed, but we still have the initial data
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
            }
        });
    }
}
