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
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.repositories.DoctorRepository;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.FirebaseHelper;

import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class AdminDemoDoctorActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etSpecialty, etAbout, etExperience, etLocation, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnCreate, btnCancel;
    private LinearLayout layoutProgress;
    private ProgressBar progressBar;

    private DoctorRepository doctorRepository;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_demo_doctor);

        doctorRepository = new DoctorRepository();
        firebaseAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etSpecialty = findViewById(R.id.etSpecialty);
        etAbout = findViewById(R.id.etAbout);
        etExperience = findViewById(R.id.etExperience);
        etLocation = findViewById(R.id.etLocation);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreate = findViewById(R.id.btnCreate);
        btnCancel = findViewById(R.id.btnCancel);
        layoutProgress = findViewById(R.id.layoutProgress);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnCreate.setOnClickListener(v -> createDemoDoctor());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void createDemoDoctor() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String specialty = etSpecialty.getText() != null ? etSpecialty.getText().toString().trim() : "";
        String about = etAbout.getText() != null ? etAbout.getText().toString().trim() : "";
        String experienceStr = etExperience.getText() != null ? etExperience.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError(getString(R.string.error_full_name_required));
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(specialty)) {
            etSpecialty.setError(getString(R.string.error_specialty_required));
            etSpecialty.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_short));
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_passwords_not_match));
            etConfirmPassword.requestFocus();
            return;
        }

        final int experience;
        if (!TextUtils.isEmpty(experienceStr)) {
            try {
                experience = Integer.parseInt(experienceStr);
            } catch (NumberFormatException e) {
                etExperience.setError(getString(R.string.error_invalid_number));
                etExperience.requestFocus();
                return;
            }
        } else {
            experience = 0;
        }

        // Show progress
        showProgress(true);

        String doctorId = UUID.randomUUID().toString();

        // Create Firebase Auth account first
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : doctorId;
                    
                    // Create UserEntity for the demo doctor
                    UserEntity demoUser = new UserEntity();
                    demoUser.setUserId(uid);
                    demoUser.setFullName(fullName);
                    demoUser.setEmail(email);
                    demoUser.setPhone("");
                    demoUser.setRole("doctor");
                    demoUser.setCreatedAt(System.currentTimeMillis());

                    // Create DoctorEntity
                    DoctorEntity doctorEntity = new DoctorEntity();
                    doctorEntity.setDoctorId(uid);
                    doctorEntity.setSpecialty(specialty);
                    doctorEntity.setConsultationFee(0);
                    doctorEntity.setAbout(about);
                    doctorEntity.setLocation(location);
                    doctorEntity.setExperience(experience);
                    doctorEntity.setApproved(true);
                    doctorEntity.setCreatedAt(System.currentTimeMillis());
                    doctorEntity.setLastUpdated(System.currentTimeMillis());
                    doctorEntity.setDemo(true);

                    // Save to Firebase - both user and doctor profile
                    FirebaseHelper.getInstance().getUsersRef().child(uid).setValue(demoUser)
                            .addOnSuccessListener(aVoid -> {
                                FirebaseHelper.getInstance().getDoctorsNodeRef().child(uid).setValue(doctorEntity)
                                        .addOnSuccessListener(aVoid1 -> {
                                            showProgress(false);
                                            Toast.makeText(this, R.string.demo_doctor_created, Toast.LENGTH_SHORT).show();
                                            AuditLogger.getInstance(this).logAction(
                                                    "CREATE_DEMO_DOCTOR",
                                                    "Created demo doctor: " + fullName + " (" + email + ")",
                                                    "DOCTOR",
                                                    uid
                                            );
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            showProgress(false);
                                            Toast.makeText(this, R.string.error_creating_demo_doctor, Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                showProgress(false);
                                Toast.makeText(this, R.string.error_creating_demo_doctor, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("email")) {
                        etEmail.setError("Email already in use");
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(this, errorMsg != null ? errorMsg : getString(R.string.error_creating_demo_doctor), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProgress(boolean show) {
        if (layoutProgress != null) {
            layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnCreate.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
}
