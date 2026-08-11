package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import com.haset.hasetapp.utils.AuditLogger;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.ProfileViewModel;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {
//    private MaterialToolbar toolbar;
    private ImageView btnBack;
    private CircleImageView ivProfileImage;
    private TextInputEditText etFullName, etEmail, etPhone, etRole, etAge;
    private AutoCompleteTextView actvGender;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextView tvUserInitials;
    private View layoutProgress;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerProfileImage;
    private android.widget.FrameLayout layoutProfileImage;
    private TextView tvChangePhoto;
    
    private PreferenceManager preferenceManager;
    private UserEntity currentUser;
    private ProfilePhotoHelper profilePhotoHelper;
    private ProfileViewModel viewModel;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        setupObservers();
        loadUserData();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getLoading().observe(this, this::showProgress);
        
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    error, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            }
        });

        viewModel.getUpdateSuccess().observe(this, success -> {
            if (success != null && success) {
                // Update preference manager with new data
                if (currentUser != null) {
                    preferenceManager.saveUserName(currentUser.getFullName());
                    preferenceManager.saveUserEmail(currentUser.getEmail());
                    preferenceManager.saveUserPhone(currentUser.getPhone());
                }
                
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    "Profile updated successfully!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(android.R.color.holo_green_dark))
                    .show();
                AuditLogger.getInstance(EditProfileActivity.this).logProfileUpdated("ALL_FIELDS");
                
                // Delay finish to allow user to see success message
                ivProfileImage.postDelayed(this::finish, 1000);
            }
        });
    }

    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btnBack);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etRole = findViewById(R.id.etRole);
        etAge = findViewById(R.id.etAge);
        actvGender = findViewById(R.id.actvGender);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        layoutProgress = findViewById(R.id.layoutProgress);
        shimmerProfileImage = findViewById(R.id.shimmerProfileImage);
        tvUserInitials = findViewById(R.id.tvUserInitials);
        layoutProfileImage = findViewById(R.id.layoutProfileImage);
        tvChangePhoto = findViewById(R.id.tvChangePhoto);
        
        // Setup gender dropdown
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(genderAdapter);
        
        preferenceManager = new PreferenceManager(this);
//        firebaseHelper = FirebaseHelper.getInstance(); // Initialize FirebaseHelper (removed)
        profilePhotoHelper = new ProfilePhotoHelper(this, new ProfilePhotoHelper.OnPhotoSelectedListener() {
            @Override
            public void onLocalPhotoSelected(Uri localUri) {
                if (localUri != null) {
                    com.bumptech.glide.Glide.with(EditProfileActivity.this)
                            .load(localUri)
                            .placeholder(R.drawable.profile_photo)
                            .into(ivProfileImage);
                }
            }

            @Override
            public void onPhotoSelected(Uri imageUri) {
                if (imageUri != null) {
                    currentPhotoPath = imageUri.toString();
                    // Update current user object so saveProfile() doesn't overwrite it
                    if (currentUser != null) {
                        currentUser.setProfileImage(imageUri.toString());
                    }
                    
                    // After upload completes, reload from Firebase to get the Cloudinary URL
                    String userId = preferenceManager.getUserId();
                    if (userId != null && !userId.isEmpty()) {
                        // Delay slightly to ensure Firebase has saved the URL
                        ivProfileImage.postDelayed(() -> {
                            // Clear Glide cache to force fresh load
                            com.bumptech.glide.Glide.get(EditProfileActivity.this).clearMemory();
                            // Reload profile photo from Firebase (which now has Cloudinary URL) with shimmer
                            ProfilePhotoHelper.loadProfilePhoto(EditProfileActivity.this, userId, ivProfileImage, shimmerProfileImage, tvUserInitials);
                        }, 500); // 500ms delay to ensure Firebase save completes
                    } else {
                        // Fallback: show local URI if userId not available
                        ivProfileImage.setImageURI(imageUri);
                    }
                } else {
                    ivProfileImage.setImageResource(R.drawable.profile_photo);
                }
            }

            @Override
            public void onPhotoError(String error) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    error, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            }
        });
    }

//    private void setupToolbar() {
//        toolbar.setNavigationOnClickListener(v -> finish());
//    }

    private void loadUserData() {
        String userId = preferenceManager.getUserId();
        if (userId != null && !userId.isEmpty()) {
            viewModel.getUserInfo(userId).observe(this, user -> {
                if (user != null) {
                    currentUser = user;
                    populateFields();
                }
            });
        } else {
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                "No user logged in", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
            finish();
        }
    }

    private void populateFields() {
        if (currentUser != null) {
            etFullName.setText(currentUser.getFullName());
            etEmail.setText(currentUser.getEmail());
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
            etRole.setText(currentUser.getRole() != null ? 
                currentUser.getRole().substring(0, 1).toUpperCase() + currentUser.getRole().substring(1).toLowerCase() : 
                "Unknown");
            
            // Load age
            if (currentUser.getAge() > 0) {
                etAge.setText(String.valueOf(currentUser.getAge()));
            }
            
            // Load gender
            if (currentUser.getGender() != null && !currentUser.getGender().isEmpty()) {
                actvGender.setText(currentUser.getGender(), false);
            }
            
            // Load profile photo with shimmer and initials fallback
            ProfilePhotoHelper.loadProfilePhoto(this, currentUser.getUserId(), ivProfileImage, shimmerProfileImage, tvUserInitials);
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());

        btnBack.setOnClickListener(v-> finish());
        
        ivProfileImage.setOnClickListener(v -> {
            profilePhotoHelper.showPhotoSelectionDialog();
        });
        
        if (layoutProfileImage != null) {
            layoutProfileImage.setOnClickListener(v -> profilePhotoHelper.showPhotoSelectionDialog());
        }
        if (tvChangePhoto != null) {
            tvChangePhoto.setOnClickListener(v -> profilePhotoHelper.showPhotoSelectionDialog());
        }
        
//        findViewById(R.id.btnAboutUs).setOnClickListener(v -> {
//            Intent intent = new Intent(this, AboutUsActivity.class);
//            startActivity(intent);
//            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
//        });
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();

        // Validation
        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.error_full_name_required));
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_valid_email));
            etEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError(getString(R.string.error_phone_required));
            etPhone.requestFocus();
            return;
        }

        if (!ageStr.isEmpty()) {
            try {
                int age = Integer.parseInt(ageStr);
                if (age < 1 || age > 120) {
                    etAge.setError(getString(R.string.error_valid_age));
                    etAge.requestFocus();
                    return;
                }
            } catch (NumberFormatException error) {
                etAge.setError(getString(R.string.error_valid_age));
                etAge.requestFocus();
                return;
            }
        }

        // Show progress
        showProgress(true);

        // Update user entity and save to Firebase
        if (currentUser != null) {
            currentUser.setFullName(fullName);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            
            // Set age
            if (!ageStr.isEmpty()) {
                try {
                    currentUser.setAge(Integer.parseInt(ageStr));
                } catch (NumberFormatException e) {
                    currentUser.setAge(0);
                }
            } else {
                currentUser.setAge(0);
            }
            
            // Set gender
            if (!gender.isEmpty()) {
                currentUser.setGender(gender.toLowerCase());
            } else {
                currentUser.setGender("");
            }

            if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                Log.d("EditProfile", "Saving profile photo to database: " + currentPhotoPath);
                currentUser.setProfileImage(currentPhotoPath);
            }

            viewModel.updateUserInfo(currentUser);
        } else {
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                "Error: Current user data not available.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
            showProgress(false);
        }
    }

    private void showProgress(boolean show) {
        if (layoutProgress != null) {
            layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (profilePhotoHelper != null) {
            profilePhotoHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (profilePhotoHelper != null) {
            profilePhotoHelper.handlePermissionResult(requestCode, grantResults);
        }
    }
}
