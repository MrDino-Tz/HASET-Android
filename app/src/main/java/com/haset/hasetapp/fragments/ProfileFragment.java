package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import androidx.core.content.FileProvider;
import de.hdodenhof.circleimageview.CircleImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import androidx.lifecycle.ViewModelProvider;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.ServiceAgreementActivity;
import com.haset.hasetapp.viewmodels.ProfileViewModel;
import com.google.android.material.card.MaterialCardView;

import com.haset.hasetapp.activities.AboutUsActivity;
import com.haset.hasetapp.activities.EditProfileActivity;
import com.haset.hasetapp.activities.LoginActivity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.AppRatingHelper;
import com.haset.hasetapp.utils.StylishQRCodeGenerator;
import com.haset.hasetapp.utils.BottomSheetHelper;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.workers.TrendingArticlesWorker;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvUserEmail, tvUserRole, tvUserPhone, tvUserAge, tvUserGender, tvSpecialization, tvConsultationFee, tvAvailableTimes, tvBio, tvLocation, tvMedicalRecordsTitle, tvUserInitials, tvUserRegNo;
    private TextView tvProfessionalInfoTitle;
    private ImageView ivVerified;
    private LinearLayout btnLogout, btnDeleteAccount, btnEditProfessionalInfo, btnDoctorPolicy;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerProfileImage;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPageLoading;
    private android.widget.LinearLayout layoutProfileContent;
    private ImageView btnShare;
    private MaterialCardView cardMedicalInfo, cardMedicalRecords;
    private LinearLayout cardMedicalInfo2, layoutConsultationFee, layoutAvailableTimes;
    private View dividerConsultationFee, dividerAvailableTimes;
    private CircleImageView ivProfileImage;
    private LinearLayout about_app, serviceAgree, contactUs, btnMyPrescriptions, rateApp;
    private com.google.android.material.button.MaterialButton btnEditProfileMain;
    private ImageView btnSettings;
    private PreferenceManager preferenceManager;
    private ProfileViewModel viewModel;
    private String registeredRegNo;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Block screenshots for profile (sensitive - personal health info)
        if (getActivity() != null) {
//            com.haset.hasetapp.utils.SensitiveActivityHelper.blockScreenshots(getActivity());
        }

        preferenceManager = new PreferenceManager(requireContext());

        initializeViews(view);

        // Initial state: Shimmer ON, Content OFF
        if (shimmerPageLoading != null) shimmerPageLoading.setVisibility(View.VISIBLE);
        if (layoutProfileContent != null) layoutProfileContent.setVisibility(View.GONE);
        
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        setupObservers();
        setupClickListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Reload doctor professional info if user is a doctor from Firebase
        String userRole = preferenceManager.getUserRole();
        String userId = preferenceManager.getUserId();
        if ("doctor".equals(userRole) && userId != null) {
            loadDoctorProfessionalInfo(userId);
        }

        // Reload profile photo to ensure it's up-to-date
        // Load profile photo with initials fallback
        refreshProfilePhoto();
    }
    
    private void refreshProfilePhoto() {
        if (ivProfileImage == null || preferenceManager == null) return;
        String userId = preferenceManager.getUserId();
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), userId, ivProfileImage, shimmerProfileImage, tvUserInitials);
    }

    private void hidePageShimmer() {
        if (shimmerPageLoading != null && shimmerPageLoading.getVisibility() == View.VISIBLE) {
            shimmerPageLoading.stopShimmer();
            shimmerPageLoading.setVisibility(View.GONE);
            if (layoutProfileContent != null) {
                layoutProfileContent.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initializeViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvUserAge = view.findViewById(R.id.tvUserAge);
        tvUserGender = view.findViewById(R.id.tvUserGender);
        tvUserRegNo = view.findViewById(R.id.tvUserRegNo);

        tvSpecialization = view.findViewById(R.id.tvSpecialization);
        tvConsultationFee = view.findViewById(R.id.tvConsultationFee);
        tvAvailableTimes = view.findViewById(R.id.tvAvailableTimes);
        btnEditProfileMain = view.findViewById(R.id.btnEditProfileMain);
        // btnShare = view.findViewById(R.id.btnShare);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        cardMedicalInfo = view.findViewById(R.id.cardMedicalInfo);
        cardMedicalInfo2 = view.findViewById(R.id.cardMedicalInfo2);
        layoutConsultationFee = view.findViewById(R.id.layoutConsultationFee);
        layoutAvailableTimes = view.findViewById(R.id.layoutAvailableTimes);
        btnEditProfessionalInfo = view.findViewById(R.id.btnEditProfessionalInfo);
        tvProfessionalInfoTitle = view.findViewById(R.id.tvProfessionalInfoTitle);
        dividerConsultationFee = view.findViewById(R.id.dividerConsultationFee);
        dividerAvailableTimes = view.findViewById(R.id.dividerAvailableTimes);
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        ivVerified = view.findViewById(R.id.ivVerified);
        shimmerProfileImage = view.findViewById(R.id.shimmerProfileImage);
        shimmerPageLoading = view.findViewById(R.id.shimmerPageLoading);
        layoutProfileContent = view.findViewById(R.id.layoutProfileContent);
        about_app = view.findViewById(R.id.about_app);
        serviceAgree = view.findViewById(R.id.serviceAgree);
        contactUs = view.findViewById(R.id.contactUs);
        rateApp = view.findViewById(R.id.rateApp);
        
        // Initialize new UI elements
        tvBio = view.findViewById(R.id.tvBio);
        tvLocation = view.findViewById(R.id.tvLocation);
        btnDoctorPolicy = view.findViewById(R.id.btnDoctorPolicy);
        btnMyPrescriptions = view.findViewById(R.id.btnMyPrescriptions);
        tvMedicalRecordsTitle = view.findViewById(R.id.tvMedicalRecordsTitle);
        cardMedicalRecords = view.findViewById(R.id.cardMedicalRecords);
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
    }

    private void setupObservers() {
        String userId = preferenceManager.getUserId();
        if (userId == null) {
            loadFromPreferencesFallback();
            return;
        }

        viewModel.getUserInfo(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUserUI(user);
            } else {
                loadFromPreferencesFallback();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                CustomDialog.showLoading(requireContext(), "Processing...");
            } else {
                CustomDialog.hideLoading();
            }
        });

        viewModel.getDeleteAccountSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                // Log account deletion action
                AuditLogger.getInstance(requireContext()).logAccountDeleted();
                
                // Hide loading if showing
                CustomDialog.hideLoading();
                
                // Clear preferences
                preferenceManager.clearPreferences();
                
                CustomDialog.showSuccess(
                        requireContext(),
                        getString(R.string.account_deleted),
                        getString(R.string.account_deleted_msg),
                        getString(R.string.done),
                        v -> {
                            Activity activity = getActivity();
                            if (activity != null) {
                                Intent intent = new Intent(activity, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                activity.overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
                                activity.finish();
                            }
                        }
                );
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null) return;
            CustomDialog.hideLoading();
            if (com.haset.hasetapp.utils.ErrorDisplay.isAuthError(error)) {
                com.haset.hasetapp.utils.ErrorDisplay.navigateToLogin(requireContext());
                return;
            }
            if (getView() != null) {
                com.haset.hasetapp.utils.ErrorDisplay.snackbar(getView(), error);
            } else {
                com.haset.hasetapp.utils.ErrorDisplay.toast(requireContext(), error);
            }
            viewModel.clearError();
        });

        viewModel.getPasswordChangeSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success && getView() != null) {
                CustomDialog.hideLoading();
                Snackbar.make(getView(), R.string.password_updated, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserUI(UserEntity user) {
        tvUserName.setText(user.getFullName());
        tvUserEmail.setText(Constants.displayEmail(user.getEmail()));
        tvUserPhone.setText(user.getPhone());
        
        String role = user.getRole();
        
        // Display age if available (for all users - patient, doctor, admin)
        if (user.getAge() > 0 && tvUserAge != null) {
            tvUserAge.setText(getString(R.string.age_label, user.getAge()));
            tvUserAge.setVisibility(View.VISIBLE);
        } else if (tvUserAge != null) {
            tvUserAge.setVisibility(View.GONE);
        }
        
        // Display gender if available (for all users - patient, doctor, admin)
        if (user.getGender() != null && !user.getGender().isEmpty() && tvUserGender != null) {
            String gender = user.getGender().substring(0, 1).toUpperCase() + user.getGender().substring(1).toLowerCase();
            tvUserGender.setText(getString(R.string.gender_label, gender));
            tvUserGender.setVisibility(View.VISIBLE);
        } else if (tvUserGender != null) {
            tvUserGender.setVisibility(View.GONE);
        }
        
        // Always hide badge and RegNo initially - will show only for verified doctors
        if (ivVerified != null) ivVerified.setVisibility(View.GONE);
        if (tvUserRegNo != null) tvUserRegNo.setVisibility(View.GONE);

        // Only doctors can have verified badge - not patients or admins
        if (Constants.ROLE_DOCTOR.equals(role)) {
            registeredRegNo = user.getRegNo();
            showMctRegistration(registeredRegNo);

            if (tvProfessionalInfoTitle != null) tvProfessionalInfoTitle.setVisibility(View.VISIBLE);
            cardMedicalInfo.setVisibility(View.VISIBLE);
            cardMedicalInfo2.setVisibility(View.VISIBLE);
            if (layoutConsultationFee != null) layoutConsultationFee.setVisibility(View.VISIBLE);
            if (layoutAvailableTimes != null) layoutAvailableTimes.setVisibility(View.VISIBLE);
            if (dividerConsultationFee != null) dividerConsultationFee.setVisibility(View.VISIBLE);
            if (dividerAvailableTimes != null) dividerAvailableTimes.setVisibility(View.VISIBLE);
            
            if (tvMedicalRecordsTitle != null) tvMedicalRecordsTitle.setVisibility(View.GONE);
            if (cardMedicalRecords != null) cardMedicalRecords.setVisibility(View.GONE);
            
            // Fetch doctor info if we haven't already for this session
            String currentId = user.getUserId();
            viewModel.getDoctorInfo(currentId).observe(getViewLifecycleOwner(), doctor -> {
                if (doctor != null) {
                    updateDoctorUI(doctor);
                }
            });
        } else {
            tvSpecialization.setText(R.string.patient);
            if (tvProfessionalInfoTitle != null) tvProfessionalInfoTitle.setVisibility(View.GONE);
            cardMedicalInfo.setVisibility(View.GONE);
            cardMedicalInfo2.setVisibility(View.GONE);
            
            if (tvMedicalRecordsTitle != null) tvMedicalRecordsTitle.setVisibility(View.VISIBLE);
            if (cardMedicalRecords != null) cardMedicalRecords.setVisibility(View.VISIBLE);
        }

        preferenceManager.saveUserName(user.getFullName());
        preferenceManager.saveUserEmail(user.getEmail());
        preferenceManager.saveUserPhone(user.getPhone());
        preferenceManager.saveUserRole(user.getRole());

        refreshProfilePhoto();
        hidePageShimmer();
    }

    private void observeDoctorInfo(String doctorId) {
        viewModel.getDoctorInfo(doctorId).observe(getViewLifecycleOwner(), doctor -> {
            if (doctor != null) {
                updateDoctorUI(doctor);
            }
        });
    }

    private void updateDoctorUI(Doctor doctor) {
        String specialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Physician";
        tvSpecialization.setText(specialty);

        double fee = doctor.getConsultationFee() > 0 ? doctor.getConsultationFee() : 0.0;
        String formattedFee = String.format(java.util.Locale.getDefault(), getString(R.string.currency_format), fee);
        tvConsultationFee.setText(formattedFee);

        if (doctor.getAvailableTimes() != null && !doctor.getAvailableTimes().isEmpty()) {
            String timesStr = android.text.TextUtils.join(", ", doctor.getAvailableTimes());
            tvAvailableTimes.setText(timesStr);
        }
        
        if (tvBio != null) tvBio.setText(doctor.getAbout());
        if (tvLocation != null) tvLocation.setText(doctor.getLocation());
        
        String regNo = doctor.getRegNo() != null && !doctor.getRegNo().trim().isEmpty()
                ? doctor.getRegNo()
                : registeredRegNo;
        showMctRegistration(regNo);
        
        if (doctor.isVerified()) {
            if (ivVerified != null) {
                ivVerified.setVisibility(View.VISIBLE);
                ivVerified.bringToFront();
                Log.d("ProfileFragment", "Doctor is verified - showing badge");
            }
            tvUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Clear old logic
        } else {
            if (ivVerified != null) {
                ivVerified.setVisibility(View.GONE);
                Log.d("ProfileFragment", "Doctor is NOT verified - hiding badge");
            }
            tvUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void showMctRegistration(String regNo) {
        if (tvUserRegNo == null) return;
        if (regNo != null && !regNo.trim().isEmpty()) {
            tvUserRegNo.setText(getString(R.string.mct_reg_no_label, regNo.trim()));
            tvUserRegNo.setVisibility(View.VISIBLE);
        } else {
            tvUserRegNo.setVisibility(View.GONE);
        }
    }

    private void setupUserInfo() {
        // Handled by setupObservers
    }

    private void loadFromPreferencesFallback() {
        Log.d("ProfileFragment", "Loading user info from local preferences (fallback).");
        // This method will now strictly be a fallback if Firebase fails or userId is null
        String userName = preferenceManager.getUserName();
        String userEmail = preferenceManager.getUserEmail();
        String userPhone = preferenceManager.getUserPhone();
        String userRole = preferenceManager.getUserRole();

        if (userName != null) tvUserName.setText(userName);
        if (userEmail != null) tvUserEmail.setText(Constants.displayEmail(userEmail));
        if (userPhone != null) tvUserPhone.setText(userPhone);
        if (userRole != null) {

            if (Constants.ROLE_DOCTOR.equals(userRole)) {
                if (tvProfessionalInfoTitle != null) {
                    tvProfessionalInfoTitle.setVisibility(View.VISIBLE);
                }
                cardMedicalInfo.setVisibility(View.VISIBLE);
                cardMedicalInfo2.setVisibility(View.VISIBLE);
                if (layoutConsultationFee != null) {
                    layoutConsultationFee.setVisibility(View.VISIBLE);
                }
                if (layoutAvailableTimes != null) {
                    layoutAvailableTimes.setVisibility(View.VISIBLE);
                }
                if (dividerConsultationFee != null) {
                    dividerConsultationFee.setVisibility(View.VISIBLE);
                }
                if (dividerAvailableTimes != null) {
                    dividerAvailableTimes.setVisibility(View.VISIBLE);
                }
                if (tvMedicalRecordsTitle != null) {
                    tvMedicalRecordsTitle.setVisibility(View.GONE);
                }
                if (cardMedicalRecords != null) {
                    cardMedicalRecords.setVisibility(View.GONE);
                }
                String userId = preferenceManager.getUserId();
                if (userId != null) {
                    loadDoctorProfessionalInfo(userId);
                }
            } else { // Patient
                tvSpecialization.setText(R.string.patient);
                if (tvProfessionalInfoTitle != null) {
                    tvProfessionalInfoTitle.setVisibility(View.GONE);
                }
                cardMedicalInfo.setVisibility(View.GONE);
                cardMedicalInfo2.setVisibility(View.GONE);
            }
        }
        // Load profile photo from preferences if available, otherwise default
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), preferenceManager.getUserId(), ivProfileImage, shimmerProfileImage);
        hidePageShimmer();
    }

    private void loadDoctorProfessionalInfo(String doctorId) {
        // Handled by setupObservers
    }

    private void setupClickListeners() {
        if (serviceAgree != null) {
            serviceAgree.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ServiceAgreementActivity.class);
                startActivity(intent);
            });
        }

        if (btnDoctorPolicy != null) {
            btnDoctorPolicy.setOnClickListener(v -> showDoctorPolicyBottomSheet());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Log the logout action
                AuditLogger.getInstance(requireContext()).logLogout();
                
                // Cancel trending articles background worker
                TrendingArticlesWorker.cancel(requireContext());
                
                // Sign out from Firebase Auth
                FirebaseHelper.getFirebaseAuth().signOut();
                
                // Clear preferences
                preferenceManager.clearPreferences();

                // Navigate to login and finish activity
                Activity activity = getActivity();
                if (activity != null) {
                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    activity.overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
                    activity.finish();
                }
            });
        }

        if (about_app != null) {
            about_app.setOnClickListener(v-> {
                Intent intent = new Intent(requireContext(), AboutUsActivity.class);
                startActivity(intent);
            });
        }

        if (serviceAgree != null) {
            serviceAgree.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ServiceAgreementActivity.class);
                startActivity(intent);
            });
        }

        if (contactUs != null) {
            contactUs.setOnClickListener(v -> {
                BottomSheetHelper.showContactUsBottomSheet(requireContext());
            });
        }

        if (rateApp != null) {
            rateApp.setOnClickListener(v -> {
                AppRatingHelper ratingHelper = new AppRatingHelper(requireActivity());
                ratingHelper.showRatingDialog(null);
            });
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        }
        
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> showProfileQRBottomSheet());
        }
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnEditProfileMain != null) {
            btnEditProfileMain.setOnClickListener(v -> {
                // DISABLED FOR V1 - Doctor edit not implemented yet
                // Both doctor and patient use EditProfileActivity for now
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                startActivity(intent);
                /* Original code for V2:
                String userRole = preferenceManager.getUserRole();
                if ("doctor".equals(userRole)) {
                    Intent intent = new Intent(requireContext(), DoctorEditActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                    startActivity(intent);
                }
                */
            });
        }

        if (btnMyPrescriptions != null) {
            btnMyPrescriptions.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.PrescriptionActivity.class);
                startActivity(intent);
            });
        }

        if (btnEditProfessionalInfo != null) {
            btnEditProfessionalInfo.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.DoctorEditActivity.class);
                startActivity(intent);
            });
        }
    }


    private void shareApp() {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_app));
            startActivity(shareIntent);
        } catch (Exception e) {
            if (isAdded() && getView() != null) {
                Snackbar.make(getView(), R.string.error_occurred, Snackbar.LENGTH_SHORT).show();
            }
        }
    }


    private void showDeleteAccountDialog() {
        CustomDialog.showWarning(
            requireContext(),
            getString(R.string.delete_account),
            getString(R.string.delete_account_msg),
            getString(R.string.delete_account),
            v -> showFinalDeleteConfirmation(),
            getString(R.string.cancel_btn),
            null
        );
    }
    
    private void showFinalDeleteConfirmation() {
        CustomDialog.showError(
            requireContext(),
            getString(R.string.final_confirmation),
            getString(R.string.final_delete_msg),
            getString(R.string.yes_delete_all),
            v -> deleteAccount()
        );
    }
    
    private void deleteAccount() {
        String userId = preferenceManager.getUserId();
        
        if (userId != null && getView() != null) {
            Snackbar.make(getView(), R.string.deleting_account, Snackbar.LENGTH_SHORT).show();
            viewModel.deleteAccount(userId);
        } else {
            CustomDialog.showError(
                requireContext(),
                getString(R.string.error_occurred),
                getString(R.string.error_identify_user),
                getString(R.string.done),
                null
            );
        }
    }
    
    private void showProfileQRBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_profile_qr, null);
        
        // Initialize views
        ImageView ivQRCode = view.findViewById(R.id.ivQRCode);
        TextView tvQRUserName = view.findViewById(R.id.tvQRUserName);
        TextView tvQRUserRole = view.findViewById(R.id.tvQRUserRole);
        LinearLayout llCopyLink = view.findViewById(R.id.llCopyLink);
        LinearLayout llSaveQR = view.findViewById(R.id.llSaveQR);
        LinearLayout llShareQR = view.findViewById(R.id.llShareQR);
        LinearLayout llShareApp = view.findViewById(R.id.llShareApp);
        
        // Get current user info
        String userName = preferenceManager.getUserName();
        String userRole = preferenceManager.getUserRole();
        String userId = preferenceManager.getUserId();
        
        // Set user info
        tvQRUserName.setText(userName != null && !userName.isEmpty() ? userName : getString(R.string.user_label));
        
        String roleDisplay = getString(R.string.user_label);
        if (userRole != null && !userRole.isEmpty()) {
            int roleResId;
            switch (userRole.toLowerCase()) {
                case "doctor": roleResId = R.string.doctor; break;
                default: roleResId = R.string.patient; break;
            }
            roleDisplay = getString(roleResId);
        }
        tvQRUserRole.setText(roleDisplay);
        
        // Generate QR code with profile link
        String profileLink = "https://hasetapp.com/profile/" + userId;
        Bitmap qrBitmap = generateQRCode(profileLink);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        }
        
        // Set click listeners
        llCopyLink.setOnClickListener(v -> {
            copyProfileLink(profileLink);
            bottomSheetDialog.dismiss();
        });
        
        llSaveQR.setOnClickListener(v -> {
            if (qrBitmap != null) {
                saveQRCode(qrBitmap, userName);
            }
            bottomSheetDialog.dismiss();
        });
        
        llShareQR.setOnClickListener(v -> {
            if (qrBitmap != null) {
                shareQRCode(qrBitmap, userName, userRole);
            }
            bottomSheetDialog.dismiss();
        });

        llShareApp.setOnClickListener(v -> {
            shareApp();
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    
    private Bitmap generateQRCode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        return StylishQRCodeGenerator.generateHASETQR(requireContext(), text);
    }

    private void copyProfileLink(String link) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(requireContext().CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.copy_profile_link), link);
        clipboard.setPrimaryClip(clip);
        if (getView() != null) {
            Snackbar.make(getView(), R.string.profile_link_copied, Snackbar.LENGTH_SHORT).show();
        }
    }
    
    private void saveQRCode(Bitmap qrBitmap, String userName) {
        try {
            // Add some padding to the QR code before saving
            Bitmap paddedBitmap = Bitmap.createBitmap(qrBitmap.getWidth() + 40, qrBitmap.getHeight() + 40, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(paddedBitmap);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(qrBitmap, 20, 20, null);

            // Sanitize userName for filename
            String safeName = (userName != null ? userName : "user").replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "Afya_Plus_QR_" + safeName + "_" + System.currentTimeMillis() + ".png";

            java.io.OutputStream outputStream;

            // Use modern MediaStore for Android 10+ (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentResolver resolver = requireContext().getContentResolver();
                android.content.ContentValues contentValues = new android.content.ContentValues();
                contentValues.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/HASET");

                Uri imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null) throw new IOException("Failed to create MediaStore entry.");
                outputStream = resolver.openOutputStream(imageUri);
            } else {
                // Fallback for older Android Versions
                File imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                File hasetDir = new File(imagesDir, "HASET");
                if (!hasetDir.exists()) hasetDir.mkdirs();
                File imageFile = new File(hasetDir, fileName);
                outputStream = new FileOutputStream(imageFile);
                
                // Trigger scanner so it appears in gallery immediately
                android.media.MediaScannerConnection.scanFile(requireContext(), new String[]{imageFile.getAbsolutePath()}, null, null);
            }

            if (outputStream != null) {
                paddedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.qr_code_saved, Snackbar.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.e("ProfileFragment", "Error saving QR code", e);
            if (getView() != null) {
                Snackbar.make(getView(), R.string.qr_code_save_failed, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
    
    private void shareQRCode(Bitmap qrBitmap, String userName, String userRole) {
        try {
            if (!isAdded()) return;
            
            // Sanitize userName for filename
            String safeName = (userName != null ? userName : "user").replaceAll("[^a-zA-Z0-9]", "_");
            
            // Create a file in the app's cache directory
            File qrDirectory = new File(requireContext().getCacheDir(), "qr_codes");
            if (!qrDirectory.exists() && !qrDirectory.mkdirs()) {
                throw new java.io.IOException("Unable to create QR code directory");
            }
            File fileName = new File(qrDirectory, "qr_code_" + safeName + ".png");
            FileOutputStream outputStream = new FileOutputStream(fileName);
            
            // Add some padding to the QR code
            Bitmap paddedBitmap = Bitmap.createBitmap(qrBitmap.getWidth() + 40, qrBitmap.getHeight() + 40, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(paddedBitmap);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(qrBitmap, 20, 20, null);
            
            paddedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            
            // Get URI using FileProvider
            Uri imageUri = FileProvider.getUriForFile(requireContext(), 
                requireContext().getPackageName() + ".fileprovider", fileName);
            
            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            
            String roleText = getString(R.string.user_label);
            if (userRole != null && !userRole.isEmpty()) {
                int roleResId;
                switch (userRole.toLowerCase()) {
                    case "doctor": roleResId = R.string.doctor; break;
                    default: roleResId = R.string.patient; break;
                }
                roleText = getString(roleResId);
            }
            String nameText = userName != null && !userName.isEmpty() ? userName : getString(R.string.user_label);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_qr_text, roleText, nameText));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_qr_code)));
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error sharing QR code", e);
            if (isAdded() && getView() != null) {
                Snackbar.make(getView(), R.string.qr_code_share_failed, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showDoctorPolicyBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_doctor_policy, null);
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Null out all view references
        tvUserName = null;
        tvUserEmail = null; 
        tvUserRole = null; 
        tvUserPhone = null; 
        tvSpecialization = null; 
        tvConsultationFee = null; 
        tvAvailableTimes = null; 
        tvBio = null; 
        tvLocation = null;
        tvProfessionalInfoTitle = null;
        
        btnLogout = null; 
        btnDeleteAccount = null; 
        btnEditProfessionalInfo = null; 
        btnDoctorPolicy = null;
        
        shimmerProfileImage = null;
        // btnShare = null;
        cardMedicalInfo = null;
        cardMedicalInfo2 = null;
        layoutConsultationFee = null; 
        layoutAvailableTimes = null;
        dividerConsultationFee = null; 
        dividerAvailableTimes = null;
        ivProfileImage = null;
        
        about_app = null; 
        serviceAgree = null; 
        contactUs = null;
        btnMyPrescriptions = null;
        tvMedicalRecordsTitle = null;
        cardMedicalRecords = null;
        shimmerPageLoading = null;
        layoutProfileContent = null;
    }
}
