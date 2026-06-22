package com.haset.hasetapp.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.AdminManagementActivity;
import com.haset.hasetapp.activities.AuditLogsActivity;
import com.haset.hasetapp.activities.LoginActivity;
import com.haset.hasetapp.activities.SettingsActivity;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import com.haset.hasetapp.viewmodels.ProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.workers.TrendingArticlesWorker;

public class AdminProfileFragment extends Fragment {

    private TextView tvAdminName, tvAdminEmail, tvAppVersion, tvServerStatus;
    private CircleImageView ivAdminImage;
    private View btnManageDoctors, btnManagePatients, btnManageAppointments, btnViewAuditLogs;
    private View btnForceSync, btnManageTips, btnChangePassword, btnLogout;
    
    private PreferenceManager preferenceManager;
    private ProfileViewModel viewModel;
    
    private androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher;

    public AdminProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && isAdded()) {
                    // Preview locally instantly
                    ivAdminImage.setImageURI(uri);
                    uploadAdminProfilePhoto(uri);
                }
            }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);

        preferenceManager = new PreferenceManager(requireContext());
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initViews(view);
        setupAdminInfo();
        setupClickListeners();
        setupObservers();

        return view;
    }

    private void initViews(View view) {
        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail);
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        tvServerStatus = view.findViewById(R.id.tvServerStatus);
        ivAdminImage = view.findViewById(R.id.ivAdminImage);
        
        android.widget.FrameLayout flProfileImage = view.findViewById(R.id.flProfileImage);
        if(flProfileImage != null){
            flProfileImage.setOnClickListener(v -> {
                if (imagePickerLauncher != null) {
                    imagePickerLauncher.launch("image/*");
                }
            });
        }

        btnManageDoctors = view.findViewById(R.id.btnManageDoctors);
        btnManagePatients = view.findViewById(R.id.btnManagePatients);
        btnManageAppointments = view.findViewById(R.id.btnManageAppointments);
        btnViewAuditLogs = view.findViewById(R.id.btnViewAuditLogs);

        btnForceSync = view.findViewById(R.id.btnForceSync);
        btnManageTips = view.findViewById(R.id.btnManageTips);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);
        
        // Set app version info
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0);
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            tvAppVersion.setText("HASET App v" + versionName + " (Build " + versionCode + ")");
        } catch (Exception e) {
            tvAppVersion.setText("HASET App Admin Edition");
        }
    }

    private void setupAdminInfo() {
        tvAdminName.setText(preferenceManager.getUserName());
        tvAdminEmail.setText(preferenceManager.getUserEmail());
        
        // Load existing profile photo
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), preferenceManager.getUserId(), ivAdminImage);
        
        // Check server status (simplified for proof of concept)
        FirebaseHelper.getFirebaseDatabase().getReference(".info/connected")
            .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    if (tvServerStatus != null && isAdded()) {
                        tvServerStatus.setText("Server Connection: " + (connected ? "Active" : "Disconnected"));
                        tvServerStatus.setTextColor(getResources().getColor(connected ? R.color.green_primary : R.color.red_primary));
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void setupClickListeners() {
        btnManageDoctors.setOnClickListener(v -> navigateToManagement(1));
        btnManagePatients.setOnClickListener(v -> navigateToManagement(2));
        btnManageAppointments.setOnClickListener(v -> navigateToManagement(3));
        btnViewAuditLogs.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AuditLogsActivity.class);
            startActivity(intent);
        });

        btnForceSync.setOnClickListener(v -> forceDatabaseSync());

        btnManageTips.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> logout());
    }

    private void setupObservers() {
        // Observers for profile updates if needed
    }

    private void navigateToManagement(int tabIndex) {
        Intent intent = new Intent(requireContext(), AdminManagementActivity.class);
        intent.putExtra(AdminManagementActivity.EXTRA_SELECTED_TAB, tabIndex);
        startActivity(intent);
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        
        TextInputLayout tilCurrentPassword = dialogView.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirmPassword = dialogView.findViewById(R.id.tilConfirmPassword);

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            // Reset errors
            tilCurrentPassword.setError(null);
            tilNewPassword.setError(null);
            tilConfirmPassword.setError(null);

            if (currentPass.isEmpty()) {
                tilCurrentPassword.setError(getString(R.string.error_current_password_required));
                return;
            }
            if (newPass.length() < 6) {
                tilNewPassword.setError(getString(R.string.error_min_6_chars));
                return;
            }
            if (!newPass.equals(confirmPass)) {
                tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
                return;
            }

            updatePassword(currentPass, newPass, dialog);
        });

        dialog.show();
    }

    private void updatePassword(String currentPass, String newPass, AlertDialog dialog) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), R.string.user_session_expired, Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
        
        Toast.makeText(requireContext(), R.string.verifying_credentials, Toast.LENGTH_SHORT).show();
        
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Now update password
                user.updatePassword(newPass).addOnCompleteListener(passTask -> {
                    if (passTask.isSuccessful()) {
                        Toast.makeText(requireContext(), R.string.password_updated, Toast.LENGTH_LONG).show();
                        AuditLogger.getInstance(requireContext()).logAction(
                            "CHANGE_PASSWORD", 
                            "Admin changed their password", 
                            "SECURITY", 
                            preferenceManager.getUserId()
                        );
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.update_failed) + ": " + passTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(requireContext(), R.string.current_password_incorrect, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void forceDatabaseSync() {

        View rootView = getView();
        if (rootView == null) return;
        
        Toast.makeText(requireContext(), R.string.starting_db_sync, Toast.LENGTH_SHORT).show();
        
        // Force sync by triggering Firebase to refresh
        FirebaseHelper.getFirebaseDatabase().goOnline();
        
        // Get reference to root and force refresh
        FirebaseHelper.getFirebaseDatabase().getReference(".info/connected")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    Boolean connected = snapshot.getValue(Boolean.class);
                    if (connected != null && connected) {
                        // Clear local cache to force refresh
                        LocalStorageHelper.getInstance(requireContext()).clearAllData(new LocalStorageHelper.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(requireContext(), R.string.db_cleared_syncing, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                // Ignore error
                            }
                        });
                        
                        Toast.makeText(requireContext(), R.string.db_synced_success, Toast.LENGTH_LONG).show();
                        
                        // Log the action
                        AuditLogger.getInstance(requireContext()).logAction(
                            "FORCE_SYNC", 
                            "Admin forced database sync", 
                            "SYSTEM", 
                            preferenceManager.getUserId()
                        );
                    } else {
                        Toast.makeText(requireContext(), R.string.no_internet_cannot_sync, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Toast.makeText(requireContext(), getString(R.string.sync_failed, error.getMessage()), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void uploadAdminProfilePhoto(android.net.Uri imageUri) {
        String adminId = preferenceManager.getUserId();
        if (adminId == null || adminId.isEmpty()) return;

        Toast.makeText(requireContext(), "Uploading profile photo...", Toast.LENGTH_SHORT).show();
        String fileName = adminId + "_profile_admin.jpg";

        CloudinaryUploadHelper.uploadFile(requireContext(), imageUri, "image", fileName, "profile_photos",
            new CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {}

                @Override
                public void onUploadProgress(double progress) {}

                @Override
                public void onUploadSuccess(String downloadUrl, String uploadedFileName) {
                    if (!isAdded()) return;
                    
                    // Save to Firebase database
                    FirebaseHelper.getUsersRef().child(adminId).child("profileImage").setValue(downloadUrl)
                        .addOnSuccessListener(aVoid -> {
                            preferenceManager.saveProfilePhotoPath(downloadUrl);
                            if (isAdded()) Toast.makeText(requireContext(), "Profile photo updated successfully", Toast.LENGTH_SHORT).show();
                        });
                }

                @Override
                public void onUploadError(String error) {
                    if (isAdded()) Toast.makeText(requireContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void logout() {
        AuditLogger.getInstance(requireContext()).logLogout();
        
        // Cancel trending articles background worker
        TrendingArticlesWorker.cancel(requireContext());
        
        FirebaseHelper.getFirebaseAuth().signOut();
        preferenceManager.clearPreferences();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
        requireActivity().finish();
    }
}
