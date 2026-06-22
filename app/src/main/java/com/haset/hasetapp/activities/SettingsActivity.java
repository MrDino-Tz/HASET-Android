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
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.BottomSheetHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ThemeHelper;
import com.haset.hasetapp.viewmodels.ProfileViewModel;

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
    private TextView tvLanguageValue;
    private TextView tvThemeValue;
    private PreferenceManager preferenceManager;
    private ProfileViewModel viewModel;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferenceManager = new PreferenceManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initializeViews();
        setupNotificationSwitch();
        setupLocationSwitch();
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
        tvLanguageValue = findViewById(R.id.tvLanguageValue);
        tvThemeValue = findViewById(R.id.tvThemeValue);
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
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
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
        com.haset.hasetapp.utils.CustomDialog.showLoading(this, "Submitting...");
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
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
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
                    com.haset.hasetapp.utils.LocaleHelper.setLocale(this, languageCode);
                    if (preferenceManager != null) {
                        preferenceManager.setLanguage(languageCode);
                    }
                    com.haset.hasetapp.utils.CustomDialog.hideLoading();
                    overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
                    recreate();
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

            if (newPass.length() < 6) {
                android.widget.Toast.makeText(this, R.string.error_password, android.widget.Toast.LENGTH_SHORT).show();
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
                    if (error != null) {
                        com.haset.hasetapp.utils.CustomDialog.hideLoading();
                        android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        bottomSheetDialog.show();
    }
}
