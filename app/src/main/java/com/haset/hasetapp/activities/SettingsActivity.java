package com.haset.hasetapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.haset.hasetapp.R;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.ui.MfaCodeInputView;
import com.haset.hasetapp.utils.BottomSheetHelper;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.utils.ValidationUtils;
import com.haset.hasetapp.viewmodels.ProfileViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends BaseActivity {

    private ImageView btnBack;
    private LinearLayout btnChangePassword;
    private LinearLayout btnSupport;
    private LinearLayout tvLanguage;
    private LinearLayout tvTheme;
    private LinearLayout layoutLocationPermission;
    private View dividerLocation;
    private MaterialSwitch switchNotification;
    private MaterialSwitch switchLocation;
    private MaterialSwitch switchMfa;
    private TextView tvLanguageValue;
    private TextView tvThemeValue;
    private TextView tvMfaDescription;
    private PreferenceManager preferenceManager;
    private ProfileViewModel viewModel;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int MFA_ENROLLMENT_REQUEST = 1702;
    private boolean updatingMfaSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferenceManager = new PreferenceManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initializeViews();
        setupNotificationSwitch();
        setupLocationSwitch();
        setupMfaSwitch();
        checkUserRole();
        updateLanguageText();
        updateThemeText();
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSupport = findViewById(R.id.btnSupport);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvTheme = findViewById(R.id.tvTheme);
        layoutLocationPermission = findViewById(R.id.layoutLocationPermission);
        dividerLocation = findViewById(R.id.dividerLocation);
        switchNotification = findViewById(R.id.switchNotification);
        switchLocation = findViewById(R.id.switchLocation);
        switchMfa = findViewById(R.id.switchMfa);
        tvLanguageValue = findViewById(R.id.tvLanguageValue);
        tvThemeValue = findViewById(R.id.tvThemeValue);
        tvMfaDescription = findViewById(R.id.tvMfaDescription);
        TextView tvVersion = findViewById(R.id.tvVersion);
        if (tvVersion != null) {
            tvVersion.setText(R.string.app_version_100dtc);
        }
    }

    private void checkUserRole() {
        String role = preferenceManager.getUserRole();
        if ("doctor".equals(role)) {
            if (layoutLocationPermission != null) {
                layoutLocationPermission.setVisibility(View.VISIBLE);
            }
            if (dividerLocation != null) {
                dividerLocation.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupNotificationSwitch() {
        if (switchNotification != null) {
            boolean notificationsEnabled = preferenceManager.isNotificationEnabled();
            switchNotification.setChecked(notificationsEnabled);

            switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferenceManager.setNotificationEnabled(isChecked);
                if (isChecked) {
                    requestNotificationPermission();
                }
            });
        }
    }

    private void setupLocationSwitch() {
        if (switchLocation != null) {
            boolean locationEnabled = preferenceManager.isLocationEnabled();
            switchLocation.setChecked(locationEnabled);

            switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    requestLocationPermission();
                } else {
                    preferenceManager.setLocationEnabled(false);
                }
            });
        }
    }

    private void setupMfaSwitch() {
        if (switchMfa == null) return;
        switchMfa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingMfaSwitch) return;
            if (isChecked) {
                startActivityForResult(new Intent(this, MfaEnrollmentActivity.class), MFA_ENROLLMENT_REQUEST);
            } else {
                showDisableMfaDialog();
            }
        });
        loadMfaStatus();
    }

    private void loadMfaStatus() {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) {
            setMfaUi(false, false);
            return;
        }
        switchMfa.setEnabled(false);
        user.getIdToken(true).addOnSuccessListener(token ->
                RetrofitClient.getInstance().getMobileMfaApiService().status("Bearer " + token.getToken())
                        .enqueue(new Callback<JsonObject>() {
                            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if (!response.isSuccessful() || response.body() == null) {
                                    setMfaUi(false, false);
                                    Toast.makeText(SettingsActivity.this, R.string.mfa_status_unavailable, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                boolean enabled = response.body().has("two_factor_enabled")
                                        && response.body().get("two_factor_enabled").getAsBoolean();
                                setMfaUi(enabled, true);
                            }

                            @Override public void onFailure(Call<JsonObject> call, Throwable throwable) {
                                setMfaUi(false, false);
                                Toast.makeText(SettingsActivity.this, R.string.mfa_status_unavailable, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .addOnFailureListener(error -> {
                    setMfaUi(false, false);
                    Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
                });
    }

    private void setMfaUi(boolean enabled, boolean interactive) {
        updatingMfaSwitch = true;
        switchMfa.setChecked(enabled);
        switchMfa.setEnabled(interactive);
        updatingMfaSwitch = false;
        if (tvMfaDescription != null) {
            tvMfaDescription.setText(enabled ? R.string.mfa_enabled_desc : R.string.mfa_disabled_desc);
        }
    }

    private void showDisableMfaDialog() {
        MfaCodeInputView input = new MfaCodeInputView(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.mfa_disable_title)
                .setMessage(R.string.mfa_disable_message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, (dismissed, which) -> setMfaUi(true, true))
                .setNeutralButton(R.string.use_recovery_code, null)
                .setPositiveButton(R.string.mfa_disable_action, null)
                .create();
        dialog.setOnCancelListener(ignored -> setMfaUi(true, true));
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (!input.isComplete()) {
                    input.setErrorState(true);
                    return;
                }
                String code = input.getCode();
                input.clearCode();
                dialog.dismiss();
                disableMfa(code);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                dialog.dismiss();
                showRecoveryDisableDialog();
            });
        });
        dialog.show();
        input.focusFirst();
    }

    private void showRecoveryDisableDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.recovery_code_hint);
        input.setSingleLine(true);
        input.setAllCaps(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10)});

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.recovery_code_title)
                .setMessage(R.string.recovery_code_message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, (dismissed, which) -> setMfaUi(true, true))
                .setPositiveButton(R.string.mfa_disable_action, null)
                .create();
        dialog.setOnCancelListener(ignored -> setMfaUi(true, true));
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String code = input.getText().toString().trim().toUpperCase(java.util.Locale.US);
            if (!code.matches("[A-F0-9]{10}")) {
                input.setError(getString(R.string.invalid_recovery_code));
                return;
            }
            dialog.dismiss();
            disableMfa(code);
        }));
        dialog.show();
        input.requestFocus();
    }

    private void disableMfa(String code) {
        FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) {
            setMfaUi(true, true);
            Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
            return;
        }
        switchMfa.setEnabled(false);
        CustomDialog.showLoading(this, getString(R.string.mfa_disable_action));
        user.getIdToken(true).addOnSuccessListener(token -> {
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            RetrofitClient.getInstance().getMobileMfaApiService().disable("Bearer " + token.getToken(), body)
                    .enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            CustomDialog.hideLoading();
                            if (response.isSuccessful()) {
                                setMfaUi(false, true);
                                Toast.makeText(SettingsActivity.this, R.string.mfa_disabled_success, Toast.LENGTH_SHORT).show();
                            } else {
                                setMfaUi(true, true);
                                Toast.makeText(SettingsActivity.this, R.string.invalid_or_expired_mfa_code, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override public void onFailure(Call<JsonObject> call, Throwable throwable) {
                            CustomDialog.hideLoading();
                            setMfaUi(true, true);
                            Toast.makeText(SettingsActivity.this, R.string.unable_to_disable_mfa, Toast.LENGTH_LONG).show();
                        }
                    });
        }).addOnFailureListener(error -> {
            CustomDialog.hideLoading();
            setMfaUi(true, true);
            Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MFA_ENROLLMENT_REQUEST) {
            if (resultCode == RESULT_OK) setMfaUi(true, true);
            else loadMfaStatus();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        1001);
            }
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 
                    LOCATION_PERMISSION_REQUEST);
        } else {
            preferenceManager.setLocationEnabled(true);
            if (switchLocation != null) {
                switchLocation.setChecked(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                preferenceManager.setLocationEnabled(true);
                if (switchLocation != null) {
                    switchLocation.setChecked(true);
                }
            } else {
                preferenceManager.setLocationEnabled(false);
                if (switchLocation != null) {
                    switchLocation.setChecked(false);
                }
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordBottomSheet());
        }

        if (tvLanguage != null) {
            tvLanguage.setOnClickListener(v -> showLanguageBottomSheet());
        }

        if (btnSupport != null) {
            btnSupport.setOnClickListener(v -> showSupportDialog());
        }

        if (tvTheme != null) {
            tvTheme.setOnClickListener(v -> showThemeDialog());
        }
    }

    private void showSupportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_support_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnWhatsApp = dialogView.findViewById(R.id.btnWhatsApp);
        MaterialButton btnCall = dialogView.findViewById(R.id.btnCall);
        MaterialButton btnBugReport = dialogView.findViewById(R.id.btnBugReport);
        MaterialButton btnTestCrash = dialogView.findViewById(R.id.btnTestCrash);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        btnWhatsApp.setOnClickListener(v -> {
            dialog.dismiss();
            openWhatsApp();
        });

        btnCall.setOnClickListener(v -> {
            dialog.dismiss();
            makeCall();
        });

        btnBugReport.setOnClickListener(v -> {
            dialog.dismiss();
            showBugReportDialog();
        });

        // TEMP: Test Crash — for Crashlytics setup verification. Remove after testing.
        if (btnTestCrash != null) {
            btnTestCrash.setOnClickListener(v -> {
                dialog.dismiss();
                throw new RuntimeException("Test Crash");
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openWhatsApp() {
        String phone = "255754501671"; // HASET Support Number
        String message = getString(R.string.whatsapp_support_msg) + " (UserID: " + preferenceManager.getUserId() + ")";
        String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + Uri.encode(message);
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void makeCall() {
        String phone = "+255754501671"; // HASET Support Number
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void showBugReportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bug_report, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        com.google.android.material.textfield.TextInputEditText etBugReport = dialogView.findViewById(R.id.etBugReport);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        btnSubmit.setOnClickListener(v -> {
            String report = etBugReport.getText() != null ? etBugReport.getText().toString().trim() : "";
            if (!report.isEmpty()) {
                submitBugReport(report);
                dialog.dismiss();
            } else {
                etBugReport.setError(getString(R.string.error_required));
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void submitBugReport(String report) {
        // Logic to save bug report to Firebase
        com.haset.hasetapp.utils.CustomDialog.showLoading(this, getString(R.string.submitting));
        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("support_tickets").push();
        
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", preferenceManager.getUserId());
        data.put("userName", preferenceManager.getUserName());
        data.put("report", report);
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", "open");

        ref.setValue(data).addOnCompleteListener(task -> {
            com.haset.hasetapp.utils.CustomDialog.hideLoading();
            if (task.isSuccessful()) {
                Toast.makeText(this, R.string.bug_report_submitted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.failed_to_submit_report, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLanguageText() {
        if (tvLanguageValue != null) {
            String currentCode = com.haset.hasetapp.utils.LocaleHelper.getLanguage(this);
            if ("sw".equals(currentCode)) {
                tvLanguageValue.setText(R.string.swahili_variant);
            } else {
                tvLanguageValue.setText(R.string.english_variant);
            }
        }
    }

    private void updateThemeText() {
        if (tvThemeValue != null) {
            int currentTheme = preferenceManager.getTheme();
            String themeName;
            switch (currentTheme) {
                case PreferenceManager.THEME_LIGHT:
                    themeName = getString(R.string.theme_light);
                    break;
                case PreferenceManager.THEME_DARK:
                    themeName = getString(R.string.theme_dark);
                    break;
                case PreferenceManager.THEME_SYSTEM:
                default:
                    themeName = getString(R.string.theme_system);
                    break;
            }
            tvThemeValue.setText(themeName);
        }
    }

    private void showLanguageBottomSheet() {
        String currentCode = com.haset.hasetapp.utils.LocaleHelper.getLanguage(this);

        BottomSheetHelper.showLanguageBottomSheet(
            this,
            currentCode,
            languageCode -> {
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
            }
        );
    }

    private void showThemeDialog() {
        if (preferenceManager == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_theme_selector, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        LinearLayout layoutLight = dialogView.findViewById(R.id.layoutLight);
        LinearLayout layoutDark = dialogView.findViewById(R.id.layoutDark);
        LinearLayout layoutSystem = dialogView.findViewById(R.id.layoutSystem);
        ImageView ivLightCheck = dialogView.findViewById(R.id.ivLightCheck);
        ImageView ivDarkCheck = dialogView.findViewById(R.id.ivDarkCheck);
        ImageView ivSystemCheck = dialogView.findViewById(R.id.ivSystemCheck);

        int currentTheme = preferenceManager.getTheme();
        updateThemeCheckmarks(ivLightCheck, ivDarkCheck, ivSystemCheck, currentTheme);

        layoutLight.setOnClickListener(v -> {
            preferenceManager.setTheme(PreferenceManager.THEME_LIGHT);
            ThemeHelper.applyTheme(this, PreferenceManager.THEME_LIGHT);
            updateThemeText();
            updateThemeCheckmarks(ivLightCheck, ivDarkCheck, ivSystemCheck, PreferenceManager.THEME_LIGHT);
            dialog.dismiss();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
            recreate();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
        });

        layoutDark.setOnClickListener(v -> {
            preferenceManager.setTheme(PreferenceManager.THEME_DARK);
            ThemeHelper.applyTheme(this, PreferenceManager.THEME_DARK);
            updateThemeText();
            updateThemeCheckmarks(ivLightCheck, ivDarkCheck, ivSystemCheck, PreferenceManager.THEME_DARK);
            dialog.dismiss();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
            recreate();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
        });

        layoutSystem.setOnClickListener(v -> {
            preferenceManager.setTheme(PreferenceManager.THEME_SYSTEM);
            ThemeHelper.applyTheme(this, PreferenceManager.THEME_SYSTEM);
            updateThemeText();
            updateThemeCheckmarks(ivLightCheck, ivDarkCheck, ivSystemCheck, PreferenceManager.THEME_SYSTEM);
            dialog.dismiss();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
            recreate();
            overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
        });

        dialog.show();
    }

    private void updateThemeCheckmarks(ImageView ivLight, ImageView ivDark, ImageView ivSystem, int currentTheme) {
        if (ivLight != null) ivLight.setVisibility(View.GONE);
        if (ivDark != null) ivDark.setVisibility(View.GONE);
        if (ivSystem != null) ivSystem.setVisibility(View.GONE);

        switch (currentTheme) {
            case PreferenceManager.THEME_LIGHT:
                if (ivLight != null) ivLight.setVisibility(View.VISIBLE);
                break;
            case PreferenceManager.THEME_DARK:
                if (ivDark != null) ivDark.setVisibility(View.VISIBLE);
                break;
            case PreferenceManager.THEME_SYSTEM:
            default:
                if (ivSystem != null) ivSystem.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showChangePasswordBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_change_password, null);
        bottomSheetDialog.setContentView(view);

        com.google.android.material.textfield.TextInputEditText etOld = view.findViewById(R.id.etOldPassword);
        com.google.android.material.textfield.TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        com.google.android.material.textfield.TextInputEditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        com.google.android.material.button.MaterialButton btnUpdate = view.findViewById(R.id.btnUpdatePassword);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String oldPass = etOld.getText().toString().trim();
            String newPass = etNew.getText().toString().trim();
            String confirmPass = etConfirm.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                android.widget.Toast.makeText(this, R.string.error_fields, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                android.widget.Toast.makeText(this, R.string.error_password_match, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isStrongPassword(newPass)) {
                android.widget.Toast.makeText(this, R.string.error_strong_password, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            com.haset.hasetapp.utils.CustomDialog.showLoading(this, getString(R.string.updating));
            String userId = preferenceManager.getUserId();
            if (userId != null) {
                viewModel.changePassword(oldPass, newPass);
                viewModel.getPasswordChangeSuccess().observe(this, success -> {
                    if (success != null && success) {
                        com.haset.hasetapp.utils.CustomDialog.hideLoading();
                        android.widget.Toast.makeText(this, R.string.password_updated, android.widget.Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                    }
                });
                viewModel.getError().observe(this, error -> {
                    if (error == null) return;
                    com.haset.hasetapp.utils.CustomDialog.hideLoading();
                    if (com.haset.hasetapp.utils.ErrorDisplay.isAuthError(error)) {
                        com.haset.hasetapp.utils.ErrorDisplay.navigateToLogin(SettingsActivity.this);
                        return;
                    }
                    com.haset.hasetapp.utils.ErrorDisplay.toast(SettingsActivity.this, error);
                    viewModel.clearError();
                });
            }
        });

        bottomSheetDialog.show();
    }
}
