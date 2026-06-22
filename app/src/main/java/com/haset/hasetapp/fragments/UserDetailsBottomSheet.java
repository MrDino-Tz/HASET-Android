package com.haset.hasetapp.fragments;

import android.app.AlertDialog;
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
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.AdminUserEditActivity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserDetailsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_USER = "user";
    private static final String ARG_CURRENT_ACTIVITY = "current_activity";
    
    private UserEntity user;
    private String currentActivity;
    private OnUserActionListener listener;
    
    public interface OnUserActionListener {
        void onUserUpdated();
        void onUserDeleted();
        void onDoctorApprovalChanged();
    }
    
    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    public static UserDetailsBottomSheet newInstance(UserEntity user, String currentActivity) {
        UserDetailsBottomSheet fragment = new UserDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, user);
        args.putString(ARG_CURRENT_ACTIVITY, currentActivity);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = (UserEntity) getArguments().getSerializable(ARG_USER);
            currentActivity = getArguments().getString(ARG_CURRENT_ACTIVITY, getString(R.string.all_users));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_user_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (user == null) {
            dismiss();
            return;
        }

        CircleImageView ivUserProfile = view.findViewById(R.id.ivUserProfile);
        com.facebook.shimmer.ShimmerFrameLayout shimmerUserProfile = view.findViewById(R.id.shimmerUserProfile);
        ImageView ivVerified = view.findViewById(R.id.ivVerified);
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        TextView tvUserRole = view.findViewById(R.id.tvUserRole);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        TextView tvUserId = view.findViewById(R.id.tvUserId);
        TextView tvCreatedDate = view.findViewById(R.id.tvCreatedDate);
        TextView tvCurrentActivity = view.findViewById(R.id.tvCurrentActivity);
        MaterialButton btnEdit = view.findViewById(R.id.btnEdit);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);
        MaterialButton btnApproveDoctor = view.findViewById(R.id.btnApproveDoctor);
        MaterialButton btnRejectDoctor = view.findViewById(R.id.btnRejectDoctor);

        // Set user details
        tvUserName.setText(user.getFullName());
        String role = user.getRole();
        if (role == null) role = "patient";
        
        int roleResId;
        int roleBgResId;
        switch (role.toLowerCase()) {
            case "doctor": 
                roleResId = R.string.doctor; 
                roleBgResId = R.drawable.bg_role_doctor;
                break;
            case "admin": 
                roleResId = R.string.admin; 
                roleBgResId = R.drawable.bg_role_admin;
                break;
            default: 
                roleResId = R.string.patient; 
                roleBgResId = R.drawable.bg_role_patient;
                break;
        }
        tvUserRole.setText(getString(roleResId));
        tvUserRole.setBackgroundResource(roleBgResId);
        tvUserRole.setTextColor(android.graphics.Color.WHITE);

        tvEmail.setText(user.getEmail());
        tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : getString(R.string.not_provided));
        tvUserId.setText(user.getUserId());
        tvCurrentActivity.setText(currentActivity);

        // Format created date
        if (user.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvCreatedDate.setText(sdf.format(new Date(user.getCreatedAt())));
        } else {
            tvCreatedDate.setText(getString(R.string.unknown));
        }

        // Load profile photo
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), user.getUserId(), ivUserProfile, shimmerUserProfile);

        // Verified badge logic (only for doctors)
        if ("doctor".equalsIgnoreCase(role)) {
            // We'll update this in fetchDoctorDetails or checkDoctorApprovalStatus
            ivVerified.setVisibility(View.GONE); // Default to hidden
        } else {
            ivVerified.setVisibility(View.GONE);
        }

        // Setup action buttons
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AdminUserEditActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("isEdit", true);
            startActivity(intent);
            dismiss();
        });
        
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        
        // Show/fetch doctor details if role is doctor
        View llDoctorInfo = view.findViewById(R.id.llDoctorInfo);
        if ("doctor".equals(role.toLowerCase())) {
            llDoctorInfo.setVisibility(View.VISIBLE);
            fetchDoctorDetails(view);
            checkDoctorApprovalStatus(btnApproveDoctor, btnRejectDoctor);
        } else {
            llDoctorInfo.setVisibility(View.GONE);
            btnApproveDoctor.setVisibility(View.GONE);
            btnRejectDoctor.setVisibility(View.GONE);
        }
        
        // Bottom sheet can be dismissed by tapping outside or back button
        // No close button needed - user can swipe down or press back
    }

    private void fetchDoctorDetails(View view) {
        TextView tvSpecialty = view.findViewById(R.id.tvDoctorSpecialty);
        TextView tvFee = view.findViewById(R.id.tvDoctorFee);
        TextView tvExperience = view.findViewById(R.id.tvDoctorExperience);
        TextView tvStats = view.findViewById(R.id.tvDoctorStats);
        TextView tvAbout = view.findViewById(R.id.tvDoctorAbout);

        FirebaseHelper.getDoctorDetails(user.getUserId(), new FirebaseHelper.OnCompleteListener<com.haset.hasetapp.database.entities.DoctorEntity>() {
            @Override
            public void onSuccess(com.haset.hasetapp.database.entities.DoctorEntity doctor) {
                if (doctor != null && isAdded()) {
                    tvSpecialty.setText(doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Physician");
                    tvFee.setText(String.format(Locale.getDefault(), "%,.0f TZS", doctor.getConsultationFee()));
                    tvExperience.setText(String.format(Locale.getDefault(), "%d Years", doctor.getExperience()));
                    
                    String stats = String.format(Locale.getDefault(), "%.1f⭐ | %d Patients", 
                        doctor.getAverageRating() != null ? doctor.getAverageRating() : 0.0f,
                        doctor.getPatientsTreated());
                    tvStats.setText(stats);
                    
                    if (doctor.getAbout() != null && !doctor.getAbout().isEmpty()) {
                        tvAbout.setText(doctor.getAbout());
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Keep default values on error
            }
        });
    }
    
    private void checkDoctorApprovalStatus(MaterialButton btnApprove, MaterialButton btnReject) {
        ImageView ivVerified = getView() != null ? getView().findViewById(R.id.ivVerified) : null;
        FirebaseHelper.getDoctorApprovalStatus(user.getUserId(), new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isApproved) {
                if (isApproved) {
                    // Doctor is approved, show reject button
                    btnApprove.setVisibility(View.GONE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnReject.setOnClickListener(v -> rejectDoctor());
                    if (ivVerified != null) ivVerified.setVisibility(View.VISIBLE);
                } else {
                    // Doctor is not approved, show approve button
                    btnApprove.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.GONE);
                    btnApprove.setOnClickListener(v -> approveDoctor());
                    if (ivVerified != null) ivVerified.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                // If error, assume not approved
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.GONE);
                btnApprove.setOnClickListener(v -> approveDoctor());
                if (ivVerified != null) ivVerified.setVisibility(View.GONE);
            }
        });
    }
    
    private void approveDoctor() {
        FirebaseHelper.approveDoctor(user.getUserId(), new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), R.string.doctor_approved, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
                AuditLogger.getInstance(requireContext()).logAction("APPROVE_DOCTOR", "Approved doctor: " + user.getFullName(), "DOCTOR", user.getUserId());
                if (listener != null) {
                    listener.onDoctorApprovalChanged();
                }
                dismiss();
            }

            @Override
            public void onError(String error) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), getString(R.string.doctor_approval_failed, error), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void rejectDoctor() {
        FirebaseHelper.rejectDoctor(user.getUserId(), new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), R.string.doctor_rejected, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
                AuditLogger.getInstance(requireContext()).logAction("REJECT_DOCTOR", "Rejected doctor: " + user.getFullName(), "DOCTOR", user.getUserId());
                if (listener != null) {
                    listener.onDoctorApprovalChanged();
                }
                dismiss();
            }

            @Override
            public void onError(String error) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), getString(R.string.doctor_rejection_failed, error), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_user)
            .setMessage(getString(R.string.delete_user_confirmation, user.getFullName()))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteUser())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void deleteUser() {
        FirebaseHelper.deleteUserAccount(user.getUserId(), new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), R.string.user_deleted_success, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
                AuditLogger.getInstance(requireContext()).logAction("DELETE_USER", "Deleted user: " + user.getFullName(), "USER", user.getUserId());
                if (listener != null) {
                    listener.onUserDeleted();
                }
                dismiss();
            }

            @Override
            public void onError(String error) {
                if (getView() != null) {
                    com.google.android.material.snackbar.Snackbar.make(getView(), getString(R.string.user_deletion_failed, error), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
}

