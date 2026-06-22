package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.DoctorRatingEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;

import java.io.Serializable;
import java.util.UUID;

/* DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
public class RateDoctorBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_APPOINTMENT = "appointment";
    
    private Appointment appointment;
    private PreferenceManager preferenceManager;
    
    private ImageView[] starViews;
    private TextView tvRatingText, tvDoctorName;
    private TextInputEditText etComment;
    private MaterialButton btnSubmit, btnCancel;
    
    private int selectedRating = 0;
    private String existingRatingId = null; // Store existing ID for updates

    public static RateDoctorBottomSheet newInstance(Appointment appointment) {
        RateDoctorBottomSheet fragment = new RateDoctorBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_APPOINTMENT, (Serializable) appointment);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointment = (Appointment) getArguments().getSerializable(ARG_APPOINTMENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_rate_doctor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (appointment == null) {
            dismiss();
            return;
        }

        preferenceManager = new PreferenceManager(requireContext());

        initViews(view);
        setupStarRating();
        checkExistingRating();
    }

    private void initViews(View view) {
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvRatingText = view.findViewById(R.id.tvRatingText);
        etComment = view.findViewById(R.id.etComment);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnCancel = view.findViewById(R.id.btnCancel);
        
        starViews = new ImageView[5];
        starViews[0] = view.findViewById(R.id.ivStar1);
        starViews[1] = view.findViewById(R.id.ivStar2);
        starViews[2] = view.findViewById(R.id.ivStar3);
        starViews[3] = view.findViewById(R.id.ivStar4);
        starViews[4] = view.findViewById(R.id.ivStar5);
        
        tvDoctorName.setText(getString(R.string.dr_prefix, appointment.getDoctorName()));
        
        btnSubmit.setOnClickListener(v -> submitRating());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void setupStarRating() {
        for (int i = 0; i < starViews.length; i++) {
            final int rating = i + 1;
            starViews[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStarDisplay();
                updateRatingText();
            });
        }
    }

    private void updateStarDisplay() {
        for (int i = 0; i < starViews.length; i++) {
            if (i < selectedRating) {
                starViews[i].setImageResource(R.drawable.ic_star_filled);
                starViews[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.star_yellow));
            } else {
                starViews[i].setImageResource(R.drawable.ic_star_outline);
                starViews[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.star_gray));
            }
        }
    }

    private void updateRatingText() {
        String[] ratingTexts = {
            getString(R.string.tap_to_rate),
            getString(R.string.rating_poor),
            getString(R.string.rating_fair),
            getString(R.string.rating_good),
            getString(R.string.rating_very_good),
            getString(R.string.rating_excellent)
        };
        
        if (selectedRating >= 0 && selectedRating < ratingTexts.length) {
            tvRatingText.setText(ratingTexts[selectedRating]);
        }
    }

    private void checkExistingRating() {
        if (appointment.getAppointmentId() == null) return;
        
        // Use FirebaseHelper to check for existing rating by appointment ID
        com.haset.hasetapp.utils.FirebaseHelper.getRatingByAppointment(appointment.getAppointmentId(), new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<DoctorRatingEntity>() {
            @Override
            public void onSuccess(DoctorRatingEntity existingRating) {
                if (existingRating != null) {
                    // Load existing rating
                    existingRatingId = existingRating.getRatingId();
                    selectedRating = (int) existingRating.getRating();
                    updateStarDisplay();
                    updateRatingText();
                    if (existingRating.getComment() != null) {
                        etComment.setText(existingRating.getComment());
                    }
                    // Optional: Disable submission if you only want to allow one rating per appointment without edits
                    // btnSubmit.setEnabled(false);
                    // btnSubmit.setText("Rated");
                }
            }

            @Override
            public void onError(String error) {
                // No existing rating or error, start fresh
            }
        });
    }

    private void submitRating() {
        if (selectedRating == 0) {
            Toast.makeText(requireContext(), R.string.please_select_rating, Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnSubmit.setEnabled(false); // Prevent double submission

        String patientId = preferenceManager.getUserId();
        String patientName = preferenceManager.getUserName();
        String doctorId = appointment.getDoctorId();
        String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

        DoctorRatingEntity rating = new DoctorRatingEntity();
        rating.setRatingId(existingRatingId != null ? existingRatingId : UUID.randomUUID().toString());
        rating.setDoctorId(doctorId);
        rating.setPatientId(patientId);
        rating.setPatientName(patientName);
        rating.setRating(selectedRating);
        rating.setComment(comment);
        rating.setAppointmentId(appointment.getAppointmentId());
        rating.setCreatedAt(System.currentTimeMillis());

        // Use FirebaseHelper to submit rating
        com.haset.hasetapp.utils.FirebaseHelper.submitDoctorRating(rating, new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), R.string.thank_you_rating, Toast.LENGTH_SHORT).show();
                AuditLogger.getInstance(requireContext()).logAction("RATE_DOCTOR", 
                    "Rated doctor: " + appointment.getDoctorName() + " (" + selectedRating + " stars)", 
                    "DOCTOR", doctorId);
                dismiss();
            }

            @Override
            public void onError(String error) {
                btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.failed_to_submit_rating, error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
*/

// Placeholder class - Rating system disabled for V1
public class RateDoctorBottomSheet extends BottomSheetDialogFragment {
    public static RateDoctorBottomSheet newInstance(Appointment appointment) {
        return new RateDoctorBottomSheet();
    }
}
