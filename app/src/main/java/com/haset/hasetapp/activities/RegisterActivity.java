package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ValidationUtils;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.viewmodels.AuthViewModel;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.models.AppConfig;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.GoogleAuthHelper;

public class RegisterActivity extends BaseActivity implements com.haset.hasetapp.fragments.WelcomeBottomSheet.WelcomeCallback {
    private static final String TAG = "HASETDoctorFlow";
    private TextInputEditText etFullName, etEmail, etPassword, etRegNo, etNin;
    private com.google.android.material.textfield.TextInputLayout tilFullName, tilEmail, tilPassword, tilRegNo, tilNin;
    private LinearLayout layoutDoctorDocuments;
    private MaterialButton btnUploadNin, btnUploadMct;
    private TextView tvNinUploadStatus, tvMctUploadStatus;
    private Uri ninDocumentUri;
    private Uri mctCertificateUri;
    private String pendingNinDocumentUrl;
    private String pendingMctCertificateUrl;
    private com.google.android.material.progressindicator.LinearProgressIndicator passwordStrengthBar;
    private TextView tvPasswordStrength;
    private MaterialButton btnRegister;
    private MaterialCardView btnGoogleLogin;
    private TextView tvLogin, tvRole;
    private String userRole;
    private PreferenceManager preferenceManager;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    /** True while completing a doctor registration that was authenticated via Google. */
    private boolean registeredViaGoogle;
    /** True when this screen just provisioned a brand-new Google patient. */
    private boolean provisionedViaGoogle;

    private AuthViewModel authViewModel;
    private ActivityResultLauncher<Intent> doctorPaymentLauncher;
    private final ActivityResultLauncher<String[]> ninPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                if (!ValidationUtils.isPdfDocument(getContentResolver(), uri)) {
                    ninDocumentUri = null;
                    com.haset.hasetapp.utils.SnackbarHelper.error(
                            findViewById(android.R.id.content), getString(R.string.error_document_pdf_only));
                    return;
                }
                persistReadPermission(uri);
                ninDocumentUri = uri;
                if (tvNinUploadStatus != null) {
                    tvNinUploadStatus.setText(R.string.nin_document_selected);
                    tvNinUploadStatus.setTextColor(getResources().getColor(R.color.green_primary));
                }
            });
    private final ActivityResultLauncher<String[]> mctPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                if (!ValidationUtils.isPdfDocument(getContentResolver(), uri)) {
                    mctCertificateUri = null;
                    com.haset.hasetapp.utils.SnackbarHelper.error(
                            findViewById(android.R.id.content), getString(R.string.error_document_pdf_only));
                    return;
                }
                persistReadPermission(uri);
                mctCertificateUri = uri;
                if (tvMctUploadStatus != null) {
                    tvMctUploadStatus.setText(R.string.mct_certificate_selected);
                    tvMctUploadStatus.setTextColor(getResources().getColor(R.color.green_primary));
                }
            });
    private UserEntity pendingDoctorUser;
    private String pendingDoctorEmail;
    private String pendingDoctorPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved theme
        // Theme is initialized globally in HASETApplication
        
        setContentView(R.layout.activity_register);
        overridePendingTransition(R.anim.anim_slide_up, 0);

        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            userRole = Constants.ROLE_PATIENT;
        }

        initViews();
        setupKeyboardScroll();
        preferenceManager = new PreferenceManager(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();

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

        // Configure Google Sign-In
        mGoogleSignInClient = GoogleAuthHelper.getClient(this);

        // Initialize Google Sign-In Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data == null) {
                        // User backed out of the Google account chooser.
                        CustomDialog.hideLoading();
                        return;
                    }
                    // Google returns RESULT_CANCELED *with* the error status inside the
                    // intent when sign-in fails after picking an account, so the data
                    // must always be inspected — not only on RESULT_OK.
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            authViewModel.loginWithGoogle(account.getIdToken());
                        } else {
                            CustomDialog.hideLoading();
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                                "Google Sign-In returned no account. Please try again.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                                .setBackgroundTint(getResources().getColor(R.color.colorError))
                                .show();
                        }
                    } catch (ApiException e) {
                        Log.w("RegisterActivity", "Google sign in failed: statusCode=" + e.getStatusCode(), e);
                        CustomDialog.hideLoading();
                        String message = com.haset.hasetapp.utils.GoogleSignInErrors.message(e.getStatusCode());
                        if (message != null) {
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                                message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.colorError))
                                .show();
                        }
                    }
                }
        );

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
        tilFullName = findViewById(R.id.tilFullName);
        etEmail = findViewById(R.id.etEmail);
        tilEmail = findViewById(R.id.tilEmail);
        etPassword = findViewById(R.id.etPassword);
        tilPassword = findViewById(R.id.tilPassword);
        etRegNo = findViewById(R.id.etRegNo);
        tilRegNo = findViewById(R.id.tilRegNo);
        etNin = findViewById(R.id.etNin);
        tilNin = findViewById(R.id.tilNin);
        layoutDoctorDocuments = findViewById(R.id.layoutDoctorDocuments);
        btnUploadNin = findViewById(R.id.btnUploadNin);
        btnUploadMct = findViewById(R.id.btnUploadMct);
        tvNinUploadStatus = findViewById(R.id.tvNinUploadStatus);
        tvMctUploadStatus = findViewById(R.id.tvMctUploadStatus);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        tvRole = findViewById(R.id.tvRole);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            if (tilRegNo != null) tilRegNo.setVisibility(android.view.View.VISIBLE);
            if (tilNin != null) tilNin.setVisibility(android.view.View.VISIBLE);
            if (layoutDoctorDocuments != null) layoutDoctorDocuments.setVisibility(android.view.View.VISIBLE);
        }
        
        setupClickListeners();
        setupPasswordStrengthWatcher();
        setupFieldErrorClearing();
    }

    private void setupKeyboardScroll() {
        final android.view.View content = findViewById(android.R.id.content);
        if (content == null) return;
        final int[] lastVisibleHeight = {content.getHeight()};
        content.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int visible = content.getHeight();
                // Detect keyboard appearing (visible area shrinks significantly)
                if (lastVisibleHeight[0] - visible > 200) {
                    android.view.View focused = getCurrentFocus();
                    if (focused != null) {
                        focused.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                focused.requestFocus();
                                if (focused.getParent() != null) {
                                    android.view.View scrollTarget = focused;
                                    android.view.ViewParent parent = focused.getParent();
                                    while (parent instanceof android.view.View) {
                                        if (parent instanceof android.widget.ScrollView) {
                                            ((android.widget.ScrollView) parent).smoothScrollTo(0, scrollTarget.getTop());
                                            break;
                                        }
                                        scrollTarget = (android.view.View) parent;
                                        parent = parent.getParent();
                                    }
                                }
                            }
                        }, 120);
                    }
                }
                lastVisibleHeight[0] = visible;
            }
        });
    }

    private void setupPasswordStrengthWatcher() {
        if (etPassword == null) return;
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (tilPassword != null && tilPassword.getError() != null) {
                    tilPassword.setError(null);
                }
                updatePasswordStrength(s != null ? s.toString() : "");
            }
        });
    }

    private void setupFieldErrorClearing() {
        clearErrorWhileTyping(etFullName, tilFullName);
        clearErrorWhileTyping(etEmail, tilEmail);
    }

    private void clearErrorWhileTyping(android.widget.EditText editText,
                                       com.google.android.material.textfield.TextInputLayout layout) {
        if (editText == null || layout == null) return;
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (layout.getError() != null) layout.setError(null);
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (passwordStrengthBar == null || tvPasswordStrength == null) return;

        int minChars = 12;
        int len = password.length();
        boolean hasLength = len >= minChars;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");

        int met = 0;
        if (hasLength) met++;
        if (hasUpper) met++;
        if (hasLower) met++;
        if (hasDigit) met++;

        int max = 4;
        if (password.isEmpty()) {
            met = 0;
        }

        int progress = Math.min(met, max);
        passwordStrengthBar.setMax(max);
        passwordStrengthBar.setProgress(progress);

        String countText = getString(R.string.password_strength_chars, Math.min(len, minChars), minChars);

        int color;
        String label;
        if (password.isEmpty()) {
            color = android.graphics.Color.rgb(158, 158, 158);
            label = getString(R.string.password_strength_label) + " (" + countText + ")";
        } else if (met == 1) {
            color = getResources().getColor(R.color.red_primary);
            label = getString(R.string.password_strength_label) + ": " + getString(R.string.password_strength_weak)
                    + " (" + countText + ")";
        } else if (met == 2) {
            color = getResources().getColor(R.color.warning_color);
            label = getString(R.string.password_strength_label) + ": " + getString(R.string.password_strength_fair)
                    + " (" + countText + ")";
        } else if (met == 3) {
            color = getResources().getColor(R.color.status_approved);
            label = getString(R.string.password_strength_label) + ": " + getString(R.string.password_strength_good)
                    + " (" + countText + ")";
        } else {
            color = getResources().getColor(R.color.green_primary);
            label = getString(R.string.password_strength_label) + ": " + getString(R.string.password_strength_strong)
                    + " (" + countText + ")";
        }

        passwordStrengthBar.setIndicatorColor(color);
        tvPasswordStrength.setText(label);
        tvPasswordStrength.setTextColor(color);
    }
    
    private void setupClickListeners() {
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                if (!com.haset.hasetapp.utils.NetworkUtils.isNetworkAvailable(this)) {
                    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content),
                            getString(R.string.no_internet_desc));
                    return;
                }
                CustomDialog.showLoading(this, getString(R.string.loading));
                GoogleAuthHelper.signOutThen(this, () -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            });
        }
        
        findViewById(R.id.tvPrivacyPolicy).setOnClickListener(v -> 
            openWebPage(Constants.PRIVACY_POLICY_URL));
            
        findViewById(R.id.tvTermsOfService).setOnClickListener(v -> 
            openWebPage(Constants.TERMS_CONDITIONS_URL));

        if (btnUploadNin != null) {
            btnUploadNin.setOnClickListener(v ->
                    ninPicker.launch(new String[]{"application/pdf"}));
        }
        if (btnUploadMct != null) {
            btnUploadMct.setOnClickListener(v ->
                    mctPicker.launch(new String[]{"application/pdf"}));
        }
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
                    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content), registerDetail);
                    pendingDoctorUser = null;
                    pendingDoctorEmail = null;
                    pendingDoctorPassword = null;
                    resetRegisterButton();
                    break;
                case SUCCESS:
                    CustomDialog.hideLoading();
                    if (Constants.ROLE_DOCTOR.equals(userRole)) {
                        String doctorTitle = registeredViaGoogle
                                ? getString(R.string.registration_successful)
                                : getString(R.string.verification_email_sent);
                        String doctorMessage = registeredViaGoogle
                                ? getString(R.string.google_register_payment_next)
                                : getString(R.string.verify_email_before_login_payment);
                        com.haset.hasetapp.fragments.WelcomeBottomSheet welcome =
                                com.haset.hasetapp.fragments.WelcomeBottomSheet.newInstance(
                                        doctorTitle,
                                        doctorMessage);
                        welcome.show(getSupportFragmentManager(), com.haset.hasetapp.fragments.WelcomeBottomSheet.TAG);
                    } else {
                        com.haset.hasetapp.fragments.WelcomeBottomSheet welcome =
                                new com.haset.hasetapp.fragments.WelcomeBottomSheet();
                        welcome.show(getSupportFragmentManager(), com.haset.hasetapp.fragments.WelcomeBottomSheet.TAG);
                    }
                    break;
                case AUTHENTICATED:
                    CustomDialog.hideLoading();
                    if (provisionedViaGoogle) {
                        // Brand-new Google patient we just created on this screen.
                        provisionedViaGoogle = false;
                        UserEntity user = (UserEntity) state.data;
                        onAuthSuccess(user);
                    } else {
                        // Google sign-in resolved to an existing account: send them
                        // to login where verification/payment gates are enforced.
                        com.haset.hasetapp.utils.SnackbarHelper.info(findViewById(android.R.id.content),
                                getString(R.string.account_exists_sign_in));
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
                            finish();
                        }, 1500);
                    }
                    break;
                case UNREGISTERED:
                    CustomDialog.hideLoading();
                    String uid = (String) state.data;
                    onGoogleUserUnregistered(uid);
                    break;
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
        com.haset.hasetapp.utils.SnackbarHelper.success(findViewById(android.R.id.content),
            getString(R.string.registration_successful));

        showSuccessAndNavigate(DashboardActivity.class, "Registration Successful", getString(R.string.verify_email_after_registration));
    }

    /**
     * A Google account signed in but has no profile in the app yet.
     * Doctors must stay on this form and supply reg. number, NIN and both
     * PDFs before the profile is created; patients are auto-provisioned.
     */
    private void onGoogleUserUnregistered(String uid) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            registeredViaGoogle = true;
            holdForDoctorDetails(fUser);
        } else {
            createNewGoogleUser(uid);
        }
    }

    private void holdForDoctorDetails(FirebaseUser fUser) {
        if (etFullName != null && etFullName.getText() != null
                && etFullName.getText().toString().trim().isEmpty()
                && fUser.getDisplayName() != null) {
            etFullName.setText(fUser.getDisplayName());
        }
        if (etEmail != null) {
            etEmail.setText(fUser.getEmail());
            etEmail.setEnabled(false);
            etEmail.setFocusable(false);
            etEmail.setFocusableInTouchMode(false);
        }
        if (tilPassword != null) tilPassword.setVisibility(android.view.View.GONE);
        if (passwordStrengthBar != null) passwordStrengthBar.setVisibility(android.view.View.GONE);
        if (tvPasswordStrength != null) tvPasswordStrength.setVisibility(android.view.View.GONE);
        com.haset.hasetapp.utils.SnackbarHelper.success(findViewById(android.R.id.content),
                getString(R.string.google_signed_in_complete_details));

        // Bring the reg. number / NIN / PDF upload fields into view so the
        // doctor immediately sees what is still required.
        final android.view.View doctorStart = findViewById(R.id.tilRegNo);
        if (doctorStart != null && doctorStart.getParent() instanceof android.view.ViewGroup
                && doctorStart.getParent().getParent() instanceof android.widget.ScrollView) {
            final android.widget.ScrollView scrollView =
                    (android.widget.ScrollView) doctorStart.getParent().getParent();
            doctorStart.post(() -> scrollView.smoothScrollTo(0, Math.max(0, doctorStart.getTop() - 16)));
        }
    }

    private void createNewGoogleUser(String uid) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        UserEntity newUser = new UserEntity();
        newUser.setUserId(uid);
        newUser.setEmail(fUser.getEmail());
        newUser.setFullName(fUser.getDisplayName() != null ? fUser.getDisplayName() : "");
        newUser.setRole(Constants.ROLE_PATIENT); // Google sign-up defaults to patient
        newUser.setCreatedAt(System.currentTimeMillis());

        provisionedViaGoogle = true;
        authViewModel.saveUserAndLogin(newUser);
    }

    private void registerUser() {
        final String fullName = etFullName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = registeredViaGoogle
                ? ""
                : etPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            tilFullName.setError(getString(R.string.error_full_name_required));
            etFullName.requestFocus();
            return;
        }
        if (!ValidationUtils.isValidName(fullName)) {
            tilFullName.setError(getString(R.string.error_name_invalid));
            etFullName.requestFocus();
            return;
        }
        tilFullName.setError(null);

        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email));
            etEmail.requestFocus();
            return;
        }
        tilEmail.setError(null);

        if (!registeredViaGoogle && !ValidationUtils.isStrongPassword(password)) {
            tilPassword.setError(getPasswordRequirementError(password));
            return;
        }

        String regNo = "";
        String nin = "";
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            if (etRegNo != null) {
                regNo = etRegNo.getText().toString().trim();
                if (android.text.TextUtils.isEmpty(regNo)) {
                    etRegNo.setError(getString(R.string.mct_reg_required));
                    return;
                }
            }
            nin = etNin != null && etNin.getText() != null ? etNin.getText().toString().trim() : "";
            if (nin.isEmpty()) {
                if (etNin != null) etNin.setError(getString(R.string.nin_required));
                return;
            }
            if (!ValidationUtils.isValidNin(nin)) {
                if (etNin != null) etNin.setError(getString(R.string.error_valid_nin));
                return;
            }
            if (ninDocumentUri == null) {
                com.haset.hasetapp.utils.SnackbarHelper.error(
                        findViewById(android.R.id.content), getString(R.string.error_nin_document_required));
                return;
            }
            if (!ValidationUtils.isPdfDocument(getContentResolver(), ninDocumentUri)) {
                com.haset.hasetapp.utils.SnackbarHelper.error(
                        findViewById(android.R.id.content), getString(R.string.error_document_pdf_only));
                return;
            }
            if (mctCertificateUri == null) {
                com.haset.hasetapp.utils.SnackbarHelper.error(
                        findViewById(android.R.id.content), getString(R.string.error_mct_certificate_required));
                return;
            }
            if (!ValidationUtils.isPdfDocument(getContentResolver(), mctCertificateUri)) {
                com.haset.hasetapp.utils.SnackbarHelper.error(
                        findViewById(android.R.id.content), getString(R.string.error_document_pdf_only));
                return;
            }
        }

        if (!com.haset.hasetapp.utils.NetworkUtils.isNetworkAvailable(this)) {
            com.haset.hasetapp.utils.SnackbarHelper.error(
                    findViewById(android.R.id.content), getString(R.string.error_network));
            return;
        }

        btnRegister.setEnabled(false);

        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setPhone("");
        newUser.setRole(userRole);
        if (registeredViaGoogle) {
            FirebaseUser googleUser = FirebaseAuth.getInstance().getCurrentUser();
            if (googleUser != null) {
                newUser.setUserId(googleUser.getUid());
            }
        }
        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            newUser.setRegNo(regNo);
            newUser.setNin(nin);
        }
        newUser.setCreatedAt(System.currentTimeMillis());

        if (Constants.ROLE_DOCTOR.equals(userRole)) {
            uploadDoctorDocumentsThenRegister(email, password, newUser);
        } else if (registeredViaGoogle) {
            authViewModel.finishGoogleRegistration(newUser);
        } else {
            authViewModel.register(email, password, newUser);
        }
    }

    private String getPasswordRequirementError(String password) {
        if (password == null || password.length() < 12) {
            return getString(R.string.error_password_min_12);
        }
        if (!password.matches(".*[A-Z].*")) {
            return getString(R.string.error_password_uppercase);
        }
        if (!password.matches(".*[a-z].*")) {
            return getString(R.string.error_password_lowercase);
        }
        if (!password.matches(".*[0-9].*")) {
            return getString(R.string.error_password_number);
        }
        return getString(R.string.error_strong_password);
    }

    private void uploadDoctorDocumentsThenRegister(String email, String password, UserEntity newUser) {
        CustomDialog.showLoading(this, getString(R.string.uploading_documents));
        String ninType = uploadTypeFor(ninDocumentUri);
        CloudinaryUploadHelper.uploadFile(this, ninDocumentUri, ninType, "nin_document",
                "doctor_verification", new CloudinaryUploadHelper.OnFileUploadListener() {
                    @Override
                    public void onUploadStart() {}

                    @Override
                    public void onUploadProgress(double progress) {}

                    @Override
                    public void onUploadSuccess(String downloadUrl, String fileName) {
                        pendingNinDocumentUrl = downloadUrl;
                        uploadMctCertificateThenRegister(email, password, newUser);
                    }

                    @Override
                    public void onUploadError(String error) {
                        CustomDialog.hideLoading();
                        resetRegisterButton();
                        com.haset.hasetapp.utils.SnackbarHelper.error(
                                findViewById(android.R.id.content),
                                error != null
                                        ? com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(RegisterActivity.this, error)
                                        : getString(R.string.error_nin_document_required));
                    }
                });
    }

    private void uploadMctCertificateThenRegister(String email, String password, UserEntity newUser) {
        String mctType = uploadTypeFor(mctCertificateUri);
        CloudinaryUploadHelper.uploadFile(this, mctCertificateUri, mctType, "mct_certificate",
                "doctor_verification", new CloudinaryUploadHelper.OnFileUploadListener() {
                    @Override
                    public void onUploadStart() {}

                    @Override
                    public void onUploadProgress(double progress) {}

                    @Override
                    public void onUploadSuccess(String downloadUrl, String fileName) {
                        pendingMctCertificateUrl = downloadUrl;
                        newUser.setNinDocumentUrl(pendingNinDocumentUrl);
                        newUser.setMctCertificateUrl(pendingMctCertificateUrl);
                        if (registeredViaGoogle) {
                            // Auth already exists (Google): just save the doctor profile.
                            authViewModel.finishGoogleRegistration(newUser);
                        } else {
                            authViewModel.register(email, password, newUser);
                        }
                    }

                    @Override
                    public void onUploadError(String error) {
                        CustomDialog.hideLoading();
                        resetRegisterButton();
                        com.haset.hasetapp.utils.SnackbarHelper.error(
                                findViewById(android.R.id.content),
                                error != null
                                        ? com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(RegisterActivity.this, error)
                                        : getString(R.string.error_mct_certificate_required));
                    }
                });
    }

    private String uploadTypeFor(Uri uri) {
        return "document";
    }

    private void persistReadPermission(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
    }

    private void fetchDoctorRegistrationFeeAndShowPaymentDialog(String email, String password, UserEntity newUser) {
        android.util.Log.d(TAG, "Fetching doctor registration fee for " + email);
        FirebaseHelper.getAppConfig(new FirebaseHelper.OnCompleteListener<AppConfig>() {
            @Override
            public void onSuccess(AppConfig config) {
                // Zero is a valid admin-configured fee (free registration).
                // Only fall back when app_config itself could not be loaded.
                double fee = config != null
                    ? Math.max(0.0, config.getDoctorRegistrationFee())
                    : 500.0;
                android.util.Log.d(TAG, "Doctor registration fee loaded: " + fee);
                if (fee == 0.0) {
                    FirebaseAuth.getInstance().signOut();
                    showSuccessAndNavigate(LoginActivity.class,
                        getString(R.string.registration_successful),
                        getString(R.string.verify_email_after_registration));
                    return;
                }
                showDoctorRegistrationPaymentDialog(email, password, newUser, fee);
            }

            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                showDoctorRegistrationPaymentDialog(email, password, newUser, 500.0);
                showDoctorRegistrationPaymentDialog(email, password, newUser, 500.0);
            }
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
                    // The account and verification email were created before payment.
                    FirebaseAuth.getInstance().signOut();
                    CustomDialog.hideLoading();
                    com.haset.hasetapp.utils.SnackbarHelper.success(
                        findViewById(android.R.id.content), getString(R.string.registration_successful));
                    showSuccessAndNavigate(LoginActivity.class,
                        getString(R.string.registration_successful),
                        getString(R.string.verify_email_after_registration));
                } else {
                    CustomDialog.hideLoading();
                    com.haset.hasetapp.utils.SnackbarHelper.error(findViewById(android.R.id.content),
                        getString(R.string.the_payment_request_was_rejected_or_did_));
                    resetRegisterButton();
                }

                pendingDoctorUser = null;
                pendingDoctorEmail = null;
                pendingDoctorPassword = null;
            }
        );
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

    @Override
    public void onWelcomeDone() {
        // The welcome sheet has been shown and dismissed: send the user to login
        // so they can sign in after verifying their email.
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.auth_fade_enter, R.anim.auth_fade_exit);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
