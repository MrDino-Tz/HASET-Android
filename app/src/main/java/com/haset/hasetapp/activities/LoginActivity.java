package com.haset.hasetapp.activities;

import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.R;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.CheckBox;
// import com.google.android.gms.auth.api.signin.GoogleSignIn;
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
// import com.google.android.gms.auth.api.signin.GoogleSignInClient;
// import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
// import com.google.android.gms.common.api.ApiException;
// import com.google.android.gms.tasks.Task;
// import com.google.firebase.auth.AuthCredential;
// import com.google.firebase.auth.GoogleAuthProvider;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.HealthTipsHelper;
import com.haset.hasetapp.utils.NotificationHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.StatusBarHelper;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.utils.ValidationUtils;
import com.haset.hasetapp.viewmodels.AuthViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.fragments.NoInternetBottomSheet;
import com.haset.hasetapp.ui.MfaCodeInputView;

public class LoginActivity extends BaseActivity {
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private android.widget.TextView tvPasswordAuth;
    private MaterialButton btnLogin;
    private MaterialCardView btnGoogleLogin;
    private TextView tvRegister, tvForgotPassword;
    private CheckBox cbRememberMe;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private HealthTipsHelper healthTipsHelper;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private ActivityResultLauncher<Intent> doctorPaymentLauncher;
    private UserEntity loggedInUser; // Store user for notification after permission
    private android.app.Dialog mfaDialog;

    // private GoogleSignInClient mGoogleSignInClient;
    // private ActivityResultLauncher<Intent> googleSignInLauncher;

    private AuthViewModel authViewModel;

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L;
    private int loginAttemptCount;
    private long lockoutUntil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved theme
        // Theme is initialized globally in HASETApplication
        
        setContentView(R.layout.activity_login);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
        
        // Configure status bar for better visibility
        StatusBarHelper.configureStatusBar(this);

        initViews();
        preferenceManager = new PreferenceManager(this);
        notificationHelper = new NotificationHelper(this);
        healthTipsHelper = new HealthTipsHelper(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupDoctorPaymentLauncher();
        setupObservers();
        showUnpaidDoctorMessageIfNeeded();

        // Language Switcher Toggle logic
        com.haset.hasetapp.utils.LanguageToggleHelper.setup(this, findViewById(android.R.id.content), languageCode -> {
            com.haset.hasetapp.utils.CustomDialog.showLoading(this, getString(R.string.switching_language));
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (preferenceManager != null) {
                    preferenceManager.setLanguage(languageCode);
                }
                com.haset.hasetapp.utils.CustomDialog.hideLoading();
                overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
                com.haset.hasetapp.utils.LocaleHelper.applyLanguageChange(this, languageCode);
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
                            Log.w("LoginActivity", "Google sign in failed", e);
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

        // Setup permission launcher
        setupNotificationPermissionLauncher();

        btnLogin.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                loginUser();
            } else {
                showNoInternetBottomSheet();
            }
        });
        
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
        });
        
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
            com.haset.hasetapp.utils.SnackbarHelper.info(findViewById(android.R.id.content),
                getString(R.string.lets_retrieve_account));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvPasswordAuth = findViewById(R.id.tvPasswordAuth);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        
        setupClickListeners();
        setupPasswordAuthWatcher();
    }

    private void setupPasswordAuthWatcher() {
        if (etPassword == null) return;
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePasswordAuth(s != null ? s.toString() : "");
            }
        });
    }

    private void updatePasswordAuth(String password) {
        if (tvPasswordAuth == null) return;
        tilPassword.setError(null);

        if (password == null || password.isEmpty()) {
            tvPasswordAuth.setVisibility(android.view.View.GONE);
            return;
        }

        if (ValidationUtils.isStrongPassword(password)) {
            tvPasswordAuth.setText(getString(R.string.password_auth_ok));
            tvPasswordAuth.setTextColor(getResources().getColor(R.color.green_primary));
        } else {
            tvPasswordAuth.setText(getString(R.string.error_strong_password));
            tvPasswordAuth.setTextColor(getResources().getColor(R.color.red_primary));
        }
        tvPasswordAuth.setVisibility(android.view.View.VISIBLE);
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
                    String loginDetail = com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(LoginActivity.this, state.message);
                    com.haset.hasetapp.utils.ErrorLogger.log(loginDetail, state.message);
                    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content), loginDetail);
                    if (state.credentialFailure) {
                        loginAttemptCount++;
                        if (loginAttemptCount >= MAX_LOGIN_ATTEMPTS) {
                            lockoutUntil = SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS;
                            loginAttemptCount = 0;
                            showLockoutMessage();
                        }
                    }
                    resetLoginButton();
                    break;
                case AUTHENTICATED:
                    CustomDialog.hideLoading();
                    loginAttemptCount = 0;
                    lockoutUntil = 0;
                    UserEntity user = (UserEntity) state.data;
                    onAuthSuccess(user);
                    break;
                case MFA_REQUIRED:
                    CustomDialog.hideLoading();
                    loginAttemptCount = 0;
                    lockoutUntil = 0;
                    showMfaDialog(false);
                    break;
                case MFA_SETUP_REQUIRED:
                    CustomDialog.hideLoading();
                    loginAttemptCount = 0;
                    lockoutUntil = 0;
                    startActivityForResult(new Intent(this, MfaEnrollmentActivity.class), 701);
                    break;
                case MFA_ERROR:
                    CustomDialog.hideLoading();
                    resetLoginButton();
                    showMfaDialog(true);
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

    private void showMfaDialog(boolean invalid) {
        if (mfaDialog != null && mfaDialog.isShowing()) mfaDialog.dismiss();

        mfaDialog = new android.app.Dialog(this, R.style.Theme_HASETApp);
        mfaDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        mfaDialog.setCancelable(false);
        mfaDialog.setContentView(R.layout.dialog_mfa_challenge);

        final MfaCodeInputView input = mfaDialog.findViewById(R.id.mfaCodeInput);
        TextView message = mfaDialog.findViewById(R.id.mfaMessage);
        if (invalid) input.setErrorState(true);
        if (invalid) message.setText("The code was invalid or expired. Try again.");

        mfaDialog.findViewById(R.id.mfaVerify).setOnClickListener(v -> {
            if (!input.isComplete()) { input.setErrorState(true); return; }
            mfaDialog.dismiss();
            authViewModel.verifyMfa(input.getCode());
        });
        mfaDialog.findViewById(R.id.mfaUseRecovery).setOnClickListener(v -> showRecoveryCodeDialog());
        mfaDialog.findViewById(R.id.mfaCancel).setOnClickListener(v -> {
            mfaDialog.dismiss();
            authViewModel.logout();
        });

        mfaDialog.show();
        if (mfaDialog.getWindow() != null) {
            mfaDialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT);
            mfaDialog.getWindow().setBackgroundDrawableResource(R.color.background_primary);
            mfaDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        input.focusFirst();
    }

    private void showRecoveryCodeDialog() {
        final android.widget.EditText recoveryInput = new android.widget.EditText(this);
        recoveryInput.setHint(R.string.recovery_code_hint);
        recoveryInput.setSingleLine(true);
        recoveryInput.setAllCaps(true);
        recoveryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        recoveryInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10)});

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.recovery_code_title)
                .setMessage(R.string.recovery_code_message)
                .setView(recoveryInput)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String code = recoveryInput.getText().toString().trim().toUpperCase(java.util.Locale.US);
                    if (!code.matches("[A-F0-9]{10}")) {
                        recoveryInput.setError(getString(R.string.invalid_recovery_code));
                        return;
                    }
                    dialog.dismiss();
                    if (mfaDialog != null) mfaDialog.dismiss();
                    authViewModel.verifyMfa(code);
                }));
        dialog.show();
        recoveryInput.requestFocus();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 701 && resultCode == RESULT_OK) authViewModel.resumeAfterMfaSetup();
    }

    private void onAuthSuccess(UserEntity user) {
        loggedInUser = user;
        preferenceManager.saveUserId(user.getUserId());
        preferenceManager.saveUserRole(user.getRole());
        preferenceManager.saveUserName(user.getFullName());
        resumeDoctorRegistrationIfNeeded(user);
    }

    private void resumeDoctorRegistrationIfNeeded(UserEntity user) {
        if (!Constants.ROLE_DOCTOR.equals(user.getRole())) {
            completeDoctorOrPatientLogin(user);
            return;
        }

        FirebaseHelper.isDoctorRegistrationPending(user.getUserId(),
            new FirebaseHelper.OnCompleteListener<Boolean>() {
                @Override
                public void onSuccess(Boolean pending) {
                    if (Boolean.TRUE.equals(pending)) {
                        startDoctorRegistrationPayment(user);
                    } else {
                        completeDoctorOrPatientLogin(user);
                    }
                }

                @Override
                public void onError(String error) {
                    startDoctorRegistrationPayment(user);
                }
            });
    }

    private void completeDoctorOrPatientLogin(UserEntity user) {
        preferenceManager.setLoggedIn(true);
        AuditLogger.getInstance(this).logLogin();
        handleNotificationAndNavigation(user);
    }

    private void startDoctorRegistrationPayment(UserEntity user) {
        if (user == null || !Constants.ROLE_DOCTOR.equals(user.getRole())) {
            completeDoctorOrPatientLogin(user);
            return;
        }

        FirebaseHelper.getAppConfig(new FirebaseHelper.OnCompleteListener<com.haset.hasetapp.models.AppConfig>() {
            @Override
            public void onSuccess(com.haset.hasetapp.models.AppConfig config) {
                double fee = config != null
                        ? Math.max(0.0, config.getDoctorRegistrationFee())
                        : 500.0;
                openDoctorRegistrationPayment(user, fee);
            }

            @Override
            public void onError(String error) {
                openDoctorRegistrationPayment(user, 500.0);
            }
        });
    }

    private void openDoctorRegistrationPayment(UserEntity user, double fee) {
        if (user == null || !Constants.ROLE_DOCTOR.equals(user.getRole())) {
            completeDoctorOrPatientLogin(user);
            return;
        }
        if (fee == 0.0) {
            completeDoctorOrPatientLogin(user);
            return;
        }

        Doctor registrationDoctor = new Doctor(
                "doctor_registration", "doctor_registration", user.getFullName(), "Doctor Registration");
        registrationDoctor.setEmail(user.getEmail());
        registrationDoctor.setPhone(user.getPhone());
        registrationDoctor.setRegNo(user.getRegNo());
        registrationDoctor.setConsultationFee(fee);
        registrationDoctor.setVerified(false);

        Intent paymentIntent = new Intent(LoginActivity.this, PaymentActivity.class);
        paymentIntent.putExtra("doctor", registrationDoctor);
        paymentIntent.putExtra("consultation_fee", fee);
        paymentIntent.putExtra("buyer_email", user.getEmail());
        paymentIntent.putExtra("buyer_name", user.getFullName());
        paymentIntent.putExtra("buyer_phone", user.getPhone());
        doctorPaymentLauncher.launch(paymentIntent);
        resetLoginButton();
    }

    private void setupDoctorPaymentLauncher() {
        doctorPaymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && loggedInUser != null) {
                    completeDoctorOrPatientLogin(loggedInUser);
                    return;
                }
                blockUnpaidDoctorLogin();
            });
    }

    private void showUnpaidDoctorMessageIfNeeded() {
        if (getIntent() != null && getIntent().getBooleanExtra("unpaid_doctor", false)) {
            findViewById(android.R.id.content).post(() ->
                com.haset.hasetapp.utils.SnackbarHelper.error(
                    findViewById(android.R.id.content),
                    getString(R.string.doctor_reg_payment_required)));
        }
    }

    private void blockUnpaidDoctorLogin() {
        FirebaseAuth.getInstance().signOut();
        if (preferenceManager != null) {
            preferenceManager.setLoggedIn(false);
        }
        loggedInUser = null;
        resetLoginButton();
        com.haset.hasetapp.utils.SnackbarHelper.error(
            findViewById(android.R.id.content),
            getString(R.string.doctor_reg_payment_required));
    }

    /*
    private void createNewGoogleUser(String uid) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        UserEntity newUser = new UserEntity();
        newUser.setUserId(uid);
        newUser.setEmail(fUser.getEmail());
        newUser.setFullName(fUser.getDisplayName());
        newUser.setRole(Constants.ROLE_PATIENT); // Default role
        newUser.setCreatedAt(System.currentTimeMillis());

        authViewModel.saveUserAndLogin(newUser);
    }
    */

    private void loginUser() {
        if (isLoginLocked()) {
            showLockoutMessage();
            return;
        }

        String identifier = etEmail.getText().toString().trim();
        String email = Constants.resolveLoginEmail(identifier);
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email));
            return;
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            tilPassword.setError(getString(R.string.error_strong_password));
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.loading);
        authViewModel.login(email, password);
    }

    private boolean isLoginLocked() {
        return SystemClock.elapsedRealtime() < lockoutUntil;
    }

    private void showLockoutMessage() {
        long remainingMs = lockoutUntil - SystemClock.elapsedRealtime();
        if (remainingMs < 0) remainingMs = 0;
        long remainingMinutes = (remainingMs + 60_000L - 1L) / 60_000L;
        String message = getString(R.string.login_locked, remainingMinutes);
        com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content), message);
    }

    private void setupNotificationPermissionLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, show welcome notification
                        if (loggedInUser != null) {
                            // Schedule health tips for patients
                            if (Constants.ROLE_PATIENT.equals(loggedInUser.getRole())) {
                                healthTipsHelper.scheduleDailyHealthTips();
                            }
                        }
                        // Navigate to dashboard
                        navigateToDashboard();
                    } else {
                        // Permission denied, show explanation but continue to dashboard
                        showNotificationPermissionDialog();
                        // Still navigate to dashboard
                        navigateToDashboard();
                    }
                }
        );
    }

    private void handleNotificationAndNavigation(UserEntity user) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                if (Constants.ROLE_PATIENT.equals(user.getRole())) {
                    healthTipsHelper.scheduleDailyHealthTips();
                }
                showSuccessAndNavigate(null); // Passing null to use the existing navigateToDashboard logic
            }
        } else {
            if (Constants.ROLE_PATIENT.equals(user.getRole())) {
                healthTipsHelper.scheduleDailyHealthTips();
            }
            showSuccessAndNavigate(null);
        }
    }

    private void showSuccessAndNavigate(Class<?> targetActivity) {
        // Commented out success dialog - goes directly to dashboard after loading
        // CustomDialog dialog = new CustomDialog(this)
        //         .setDialogType(CustomDialog.DialogType.SUCCESS)
        //         .setTitle("Login Successful")
        //         .setMessage("Welcome back! You have successfully logged in.")
        //         .hideNegativeButton()
        //         .hidePositiveButton()
        //         .show();

        // Go directly to dashboard
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            navigateToDashboard();
        }, 500);
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText(R.string.sign_in);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
        finish();
    }

    private void showNotificationPermissionDialog() {
        CustomDialog.showInfo(
            this,
            "Notification Permission Required",
            "HASET needs notification permission to send you important appointment reminders and welcome messages. Please enable notifications in settings.",
            "Go to Settings",
            v -> {
                // Open app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            },
            "Later",
            null
        );
    }

    // Network monitoring methods are handled by BaseActivity
    // or overridden if specific behavior is needed.

    @Override
    public void onRetryConnection() {
        // This is called when the user clicks retry in the bottom sheet
        if (NetworkUtils.isNetworkAvailable(this)) {
            dismissNoInternetBottomSheet();
            // Automatically attempt login if coming from a retry and network is available
            loginUser();
        } else {
            com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content),
                getString(R.string.still_no_internet));
        }
    }

    @Override
    public void onNetworkAvailable() {
        // This is called when the network becomes available (from broadcast receiver)
        dismissNoInternetBottomSheet();
    }

    @Override
    public void onNetworkUnavailable() {
        // This is called when the network becomes unavailable (from broadcast receiver)
        showNoInternetBottomSheet();
    }

    @Override
    public void onBottomSheetDismissed() {
        // This is called when the user explicitly dismisses the bottom sheet
        // If network is still unavailable, close the activity.
        if (!NetworkUtils.isNetworkAvailable(this)) {
            finish(); // Close LoginActivity
        }
        // If network is available, user can proceed normally, no action needed here.
    }

    private void showExitDialog() {
        CustomDialog exitDialog = new CustomDialog(this)
                .setDialogType(CustomDialog.DialogType.WARNING)
                .setTitle(getString(R.string.exit_app))
                .setMessage(getString(R.string.exit_app_confirm))
                .setPositiveButtonColor(R.color.colorError);
        
        exitDialog.setPositiveButton("Exit", v -> {
            exitDialog.dismiss();
            finish();
        });
        
        exitDialog.setNegativeButton("Stay", v -> exitDialog.dismiss());
        
        exitDialog.show();
    }
}
