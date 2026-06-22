package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.utils.CustomDialog;
// import com.google.android.gms.auth.api.signin.GoogleSignIn;
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
// import com.google.android.gms.auth.api.signin.GoogleSignInClient;
// import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
// import com.google.android.gms.common.api.ApiException;
// import com.google.android.gms.tasks.Task;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.activities.AdminDashboardActivity;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ValidationUtils;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.viewmodels.AuthViewModel;

import android.util.Log;

public class RegisterActivity extends BaseActivity {
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etRegNo;
    private com.google.android.material.textfield.TextInputLayout tilRegNo;
    private MaterialButton btnRegister;
    private MaterialCardView btnGoogleLogin;
    private TextView tvLogin, tvRole;
    private String userRole;
    private PreferenceManager preferenceManager;
    private LocalStorageHelper storageHelper;

    // private GoogleSignInClient mGoogleSignInClient;
    // private ActivityResultLauncher<Intent> googleSignInLauncher;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved theme
        // Theme is initialized globally in HASETApplication
        
        setContentView(R.layout.activity_register);

        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            userRole = Constants.ROLE_PATIENT;
        }

        initViews();
        preferenceManager = new PreferenceManager(this);
        storageHelper = LocalStorageHelper.getInstance(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();

        // Language Switcher Toggle logic
        com.haset.hasetapp.utils.LanguageToggleHelper.setup(this, findViewById(android.R.id.content), languageCode -> {
            com.haset.hasetapp.utils.CustomDialog.showLoading(this, getString(R.string.switching_language));
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.haset.hasetapp.utils.LocaleHelper.setLocale(this, languageCode);
                if (preferenceManager != null) {
                    preferenceManager.setLanguage(languageCode);
                }
                com.haset.hasetapp.utils.CustomDialog.hideLoading();
                overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
                recreate();
                overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
            }, 500);
        });

        /*
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        */

        /*
        // Initialize Google Sign-In Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                    .getResult(ApiException.class);
                            if (account != null) {
                                authViewModel.loginWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Log.w("RegisterActivity", "Google sign in failed", e);
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                "Google Sign-In failed: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                                .setBackgroundTint(getResources().getColor(R.color.colorError))
                                .show();
                        }
                    } else {
                        CustomDialog.hideLoading();
                    }
                }
        );
        */

        tvRole.setText(userRole.equals(Constants.ROLE_PATIENT) ? 
                getString(R.string.patient) : getString(R.string.doctor));

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etRegNo = findViewById(R.id.etRegNo);
        tilRegNo = findViewById(R.id.tilRegNo);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        tvRole = findViewById(R.id.tvRole);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        
        if (Constants.ROLE_DOCTOR.equals(userRole) && tilRegNo != null) {
            tilRegNo.setVisibility(android.view.View.VISIBLE);
        }
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        /*
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                CustomDialog.showLoading(this, "Opening Google...");
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }
        */
        if (btnGoogleLogin != null) {
             btnGoogleLogin.setVisibility(android.view.View.GONE);
        }
        
        findViewById(R.id.tvPrivacyPolicy).setOnClickListener(v -> 
            openWebPage(Constants.PRIVACY_POLICY_URL));
            
        findViewById(R.id.tvTermsOfService).setOnClickListener(v -> 
            openWebPage(Constants.TERMS_CONDITIONS_URL));
    }

    private void setupObservers() {
        authViewModel.getAuthState().observe(this, state -> {
            switch (state.status) {
                case LOADING:
                    CustomDialog.showLoading(this, state.message);
                    break;
                case ERROR:
                    CustomDialog.hideLoading();
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                        state.message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(getResources().getColor(R.color.colorError))
                        .show();
                    resetRegisterButton();
                    break;
                case AUTHENTICATED:
                    CustomDialog.hideLoading();
                    UserEntity user = (UserEntity) state.data;
                    onAuthSuccess(user);
                    break;
                /*
                case UNREGISTERED:
                    CustomDialog.hideLoading();
                    String uid = (String) state.data;
                    createNewGoogleUser(uid);
                    break;
                */
                case IDLE:
                    CustomDialog.hideLoading();
                    break;
            }
        });
    }

    private void onAuthSuccess(UserEntity user) {
        preferenceManager.saveUserId(user.getUserId());
        preferenceManager.saveUserRole(user.getRole());
        preferenceManager.saveUserName(user.getFullName());
        preferenceManager.setLoggedIn(true);
        
        AuditLogger.getInstance(this).logRegistration();
        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
            "Registration successful!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getResources().getColor(android.R.color.holo_green_dark))
            .show();

        showSuccessAndNavigate(DashboardActivity.class, "Registration Successful", "Welcome to HASET!");
    }

    /*
    private void createNewGoogleUser(String uid) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        UserEntity newUser = new UserEntity();
        newUser.setUserId(uid);
        newUser.setEmail(fUser.getEmail());
        newUser.setFullName(fUser.getDisplayName());
        newUser.setRole(userRole != null ? userRole : Constants.ROLE_PATIENT);
        newUser.setCreatedAt(System.currentTimeMillis());

        authViewModel.saveUserAndLogin(newUser);
    }
    */

    private void registerUser() {
        final String fullName = etFullName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        final String finalPhoneNumber = phone.startsWith("+255") ? phone : "+255" + phone;

        if (!ValidationUtils.isValidName(fullName)) {
            etFullName.setError(getString(R.string.error_name));
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError(getString(R.string.error_email));
            return;
        }

        if (!ValidationUtils.isValidPhone(finalPhoneNumber)) {
            etPhone.setError(getString(R.string.error_phone));
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError(getString(R.string.error_password));
            return;
        }

        String regNo = "";
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            if (etRegNo != null) {
                regNo = etRegNo.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(regNo)) {
                    etRegNo.setError(getString(R.string.mct_reg_required));
                    return;
                }
            }
        }

        btnRegister.setEnabled(false);
//        CustomDialog.showLoading(this, getString(R.string.creating_account));

        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setPhone(finalPhoneNumber);
        newUser.setRole(userRole);
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            newUser.setRegNo(regNo);
        }
        newUser.setCreatedAt(System.currentTimeMillis());

        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            showDoctorRegistrationPaymentDialog(email, password, newUser);
        } else {
            authViewModel.register(email, password, newUser);
        }
    }

    private void showDoctorRegistrationPaymentDialog(String email, String password, UserEntity newUser) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_payment_success, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        MaterialButton btnDone = dialogView.findViewById(R.id.btnDone);
        
        if (tvTitle != null) tvTitle.setText(R.string.doctor_reg_fee_title);
        if (tvMessage != null) tvMessage.setText(R.string.doctor_reg_fee_message);
        if (btnDone != null) btnDone.setText(R.string.pay_via_ussd);

        btnDone.setOnClickListener(v -> {
            btnDone.setEnabled(false);
            btnDone.setText("Processing payment...");
            
            // Simulate 3 seconds payment processing delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                dialog.dismiss();
                // Proceed with registration after successful "payment"
                CustomDialog.showLoading(RegisterActivity.this, getString(R.string.creating_account));
                authViewModel.register(email, password, newUser);
            }, 3000);
        });

        // Add a cancel button dynamically to dialog_payment_success layout container
        android.view.ViewGroup layout = dialogView.findViewById(R.id.btnDone).getParent() instanceof android.view.ViewGroup ? 
                                        (android.view.ViewGroup) dialogView.findViewById(R.id.btnDone).getParent() : null;
        if (layout != null) {
            MaterialButton btnCancel = new MaterialButton(this);
            btnCancel.setText(R.string.cancel);
            btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 0);
            btnCancel.setLayoutParams(params);
            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();
                resetRegisterButton();
            });
            layout.addView(btnCancel);
        }

        dialog.show();
    }

    private void resetRegisterButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText(R.string.sign_up);
    }

    private void showSuccessAndNavigate(Class<?> targetActivity, String title, String message) {
        // Go directly to dashboard
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(RegisterActivity.this, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            startActivity(intent);
            overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
            finish();
        }, 500);
    }
}
