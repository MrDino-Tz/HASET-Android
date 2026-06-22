package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.AuditLogger;

import java.util.UUID;

public class AdminUserEditActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etRole;
    private MaterialButton btnSave, btnCancel;
    private LinearLayout layoutProgress;
    private ProgressBar progressBar;
    
    private LocalStorageHelper localStorageHelper;
    private UserEntity currentUser;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_edit);

        localStorageHelper = LocalStorageHelper.getInstance(this);

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etRole = findViewById(R.id.etRole);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        layoutProgress = findViewById(R.id.layoutProgress);
        progressBar = findViewById(R.id.progressBar);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        currentUser = (UserEntity) getIntent().getSerializableExtra("user");
        isEditMode = getIntent().getBooleanExtra("isEdit", false);
        
        if (isEditMode && currentUser != null) {
            // Edit mode - populate fields
            etFullName.setText(currentUser.getFullName());
            etEmail.setText(currentUser.getEmail());
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
            etRole.setText(currentUser.getRole());
            etRole.setEnabled(false); // Role cannot be changed
            etPassword.setHint(getString(R.string.leave_blank_password));
            findViewById(R.id.tvTitle).setVisibility(View.GONE);
            ((android.widget.TextView) findViewById(R.id.tvTitle)).setText(R.string.edit_user);
        } else {
            // Create mode
            etRole.setEnabled(true);
            findViewById(R.id.tvTitle).setVisibility(View.GONE);
            ((android.widget.TextView) findViewById(R.id.tvTitle)).setText(R.string.create_new_user);
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveUser());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveUser() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String role = etRole.getText() != null ? etRole.getText().toString().trim().toLowerCase() : "";

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError(getString(R.string.error_full_name_required));
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_valid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError(getString(R.string.error_phone_required));
            etPhone.requestFocus();
            return;
        }

        if (!isEditMode && TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required_new_users));
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(role) || (!role.equals("patient") && !role.equals("doctor") && !role.equals("admin"))) {
            etRole.setError(getString(R.string.error_valid_role));
            etRole.requestFocus();
            return;
        }

        // Show progress
        showProgress(true);

        if (isEditMode && currentUser != null) {
            // Update existing user
            currentUser.setFullName(fullName);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            
            // Only update password if provided
            if (!TextUtils.isEmpty(password)) {
                // Hash password
                String hashedPassword = hashPassword(password);
                currentUser.setPassword(hashedPassword);
            }
            
            localStorageHelper.updateUser(currentUser, new LocalStorageHelper.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showProgress(false);
                    Toast.makeText(AdminUserEditActivity.this, R.string.user_updated_success, Toast.LENGTH_SHORT).show();
                    AuditLogger.getInstance(AdminUserEditActivity.this).logAction("UPDATE_USER", "Updated user: " + fullName, "USER", currentUser.getUserId());
                    finish();
                }

                @Override
                public void onError(String error) {
                    showProgress(false);
                    Toast.makeText(AdminUserEditActivity.this, getString(R.string.failed_to_update_user, error), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create new user
            UserEntity newUser = new UserEntity();
            newUser.setUserId(UUID.randomUUID().toString());
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setRole(role);
            
            // Hash password
            String hashedPassword = hashPassword(password);
            newUser.setPassword(hashedPassword);
            newUser.setCreatedAt(System.currentTimeMillis());
            
            localStorageHelper.createUser(newUser, new LocalStorageHelper.OnCompleteListener<UserEntity>() {
                @Override
                public void onSuccess(UserEntity user) {
                    showProgress(false);
                    Toast.makeText(AdminUserEditActivity.this, R.string.user_created_success, Toast.LENGTH_SHORT).show();
                    AuditLogger.getInstance(AdminUserEditActivity.this).logAction("CREATE_USER", "Created user: " + fullName, "USER", user.getUserId());
                    finish();
                }

                @Override
                public void onError(String error) {
                    showProgress(false);
                    Toast.makeText(AdminUserEditActivity.this, getString(R.string.failed_to_create_user, error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return password; // Fallback
        }
    }

    private void showProgress(boolean show) {
        if (layoutProgress != null) {
            layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
}

