package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
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
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ValidationUtils;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.viewmodels.AuthViewModel;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.models.AppConfig;
import com.haset.hasetapp.utils.FirebaseHelper;

public class RegisterActivity extends BaseActivity {
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etRegNo;
    private com.google.android.material.textfield.TextInputLayout tilRegNo;
    private MaterialButton btnRegister;
    private MaterialCardView btnGoogleLogin;
    private TextView tvLogin, tvRole;
    private String userRole;
    private PreferenceManager preferenceManager;

    // private GoogleSignInClient mGoogleSignInClient;
    // private ActivityResultLauncher<Intent> googleSignInLauncher;

    private AuthViewModel authViewModel;
    private ActivityResultLauncher<Intent> doctorPaymentLauncher;
    private UserEntity pendingDoctorUser;
    private String pendingDoctorEmail;
    private String pendingDoctorPassword;
    private boolean pendingDoctorPaymentRequiresAnonymousAuth = false;
    private FirebaseUser pendingAnonymousPaymentUser;

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

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();
        setupPaymentLauncher();

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
                    String registerDetail = com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(RegisterActivity.this, state.message);
                    com.haset.hasetapp.utils.ErrorLogger.log(registerDetail, state.message);
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                        registerDetail, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
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

        if (!ValidationUtils.isStrongPassword(password)) {
            etPassword.setError(getString(R.string.error_strong_password));
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
            fetchDoctorRegistrationFeeAndShowPaymentDialog(email, password, newUser);
        } else {
            authViewModel.register(email, password, newUser);
        }
    }

    private void fetchDoctorRegistrationFeeAndShowPaymentDialog(String email, String password, UserEntity newUser) {
        FirebaseHelper.getAppConfig(new FirebaseHelper.OnCompleteListener<AppConfig>() {
            @Override
            public void onSuccess(AppConfig config) {
                // Zero is a valid admin-configured fee (free registration).
                // Only fall back when app_config itself could not be loaded.
                double fee = config != null
                    ? Math.max(0.0, config.getDoctorRegistrationFee())
                    : 500.0;
                if (fee == 0.0) {
                    authViewModel.register(email, password, newUser);
                    return;
                }
                ensurePaymentAuthThenShowDoctorRegistrationPaymentDialog(email, password, newUser, fee);
            }

            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                ensurePaymentAuthThenShowDoctorRegistrationPaymentDialog(email, password, newUser, 500.0);
            }
        });
    }

    private void ensurePaymentAuthThenShowDoctorRegistrationPaymentDialog(String email, String password, UserEntity newUser, double fee) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            pendingDoctorPaymentRequiresAnonymousAuth = currentUser.isAnonymous();
            pendingAnonymousPaymentUser = currentUser.isAnonymous() ? currentUser : null;
            showDoctorRegistrationPaymentDialog(email, password, newUser, fee);
            return;
        }

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener(result -> {
                pendingDoctorPaymentRequiresAnonymousAuth = true;
                pendingAnonymousPaymentUser = result.getUser();
                showDoctorRegistrationPaymentDialog(email, password, newUser, fee);
            })
            .addOnFailureListener(error -> {
                resetRegisterButton();
                CustomDialog.hideLoading();
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                    error.getMessage() != null ? error.getMessage() : "Unable to prepare payment session",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            });
    }

    private void showDoctorRegistrationPaymentDialog(String email, String password, UserEntity newUser, double fee) {
        pendingDoctorUser = newUser;
        pendingDoctorEmail = email;
        pendingDoctorPassword = password;

        Doctor paymentDoctor = new Doctor("doctor_registration", "doctor_registration", newUser.getFullName(), "Doctor Registration");
        paymentDoctor.setConsultationFee(fee);
        paymentDoctor.setVerified(false);

        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("doctor", paymentDoctor);
        paymentIntent.putExtra("consultation_fee", fee);
        paymentIntent.putExtra("buyer_email", newUser.getEmail());
        paymentIntent.putExtra("buyer_name", newUser.getFullName());
        paymentIntent.putExtra("buyer_phone", newUser.getPhone());
        doctorPaymentLauncher.launch(paymentIntent);
    }

    private void setupPaymentLauncher() {
        doctorPaymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && pendingDoctorUser != null) {
                    // A successful payment must still result in a normal credential-backed
                    // account. Never promote an anonymous Firebase session to a doctor.
                    UserEntity doctorUser = pendingDoctorUser;
                    String doctorEmail = pendingDoctorEmail;
                    String doctorPassword = pendingDoctorPassword;
                    CustomDialog.showLoading(RegisterActivity.this, getString(R.string.creating_account));
                    cleanupAnonymousPaymentSession(() ->
                        authViewModel.register(doctorEmail, doctorPassword, doctorUser));
                } else {
                    cleanupAnonymousPaymentSession(null);
                    CustomDialog.hideLoading();
                    resetRegisterButton();
                }

                pendingDoctorUser = null;
                pendingDoctorEmail = null;
                pendingDoctorPassword = null;
                pendingDoctorPaymentRequiresAnonymousAuth = false;
                pendingAnonymousPaymentUser = null;
            }
        );
    }

    private void cleanupAnonymousPaymentSession(@androidx.annotation.Nullable Runnable onComplete) {
        FirebaseUser anonymousUser = pendingAnonymousPaymentUser;
        if (!pendingDoctorPaymentRequiresAnonymousAuth
                || anonymousUser == null
                || !anonymousUser.isAnonymous()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        String anonymousUid = anonymousUser.getUid();
        java.util.Map<String, Object> cleanupUpdates = new java.util.HashMap<>();
        cleanupUpdates.put("users/" + anonymousUid, null);
        cleanupUpdates.put("doctors/" + anonymousUid, null);
        cleanupUpdates.put("notifications/" + anonymousUid, null);
        cleanupUpdates.put("saved_articles/" + anonymousUid, null);

        FirebaseHelper.getDatabaseReference().updateChildren(cleanupUpdates)
            .addOnCompleteListener(ignored -> deleteAnonymousUser(anonymousUser, onComplete));
    }

    private void deleteAnonymousUser(FirebaseUser anonymousUser,
                                     @androidx.annotation.Nullable Runnable onComplete) {
        anonymousUser.delete()
            .addOnCompleteListener(task -> {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null && currentUser.isAnonymous()
                        && currentUser.getUid().equals(anonymousUser.getUid())) {
                    auth.signOut();
                }
                if (onComplete != null) onComplete.run();
            });
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
