package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AuthViewModel;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.ValidationUtils;
import android.content.Intent;
import android.net.Uri;

public class ForgotPasswordActivity extends BaseActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSend;
    private TextView tvBackToLogin;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        overridePendingTransition(R.anim.anim_slide_up, 0);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        initViews();
        setupClickListeners();
        setupObservers();
        handleResetLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleResetLink(intent);
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSend = findViewById(R.id.btnSend);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupObservers() {
        authViewModel.getAuthState().observe(this, state -> {
            switch (state.status) {
                case LOADING:
                    btnSend.setEnabled(false);
                    btnSend.setText(R.string.loading);
                    break;
                case SUCCESS:
                    btnSend.setEnabled(true);
                    btnSend.setText(R.string.send_reset_link);
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                        state.message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(android.R.color.holo_green_dark))
                        .show();
                    
                    // Delay finish slightly so user can read message
                    etEmail.postDelayed(() -> {
                        finish();
                    }, 1500);
                    break;
                case ERROR:
                    btnSend.setEnabled(true);
                    btnSend.setText(R.string.send_reset_link);
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content),
                        state.message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(getResources().getColor(R.color.colorError))
                        .show();
                    break;
            }
        });
    }

    private void setupClickListeners() {
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });

        // Send reset link button
        btnSend.setOnClickListener(v -> {
            String identifier = etEmail.getText().toString().trim();
            String email = Constants.resolveLoginEmail(identifier);
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.setError(getString(R.string.error_email));
                return;
            }

            // Password reset mail is sent by the Hostinger SMTP backend.
            authViewModel.resetPassword(email);
        });

        // Back to login link
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void handleResetLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        Uri data = intent.getData();
        if (!"resetPassword".equals(data.getQueryParameter("mode"))) {
            return;
        }

        ResetPasswordBottomSheet sheet = ResetPasswordBottomSheet.newInstance(data.toString());
        sheet.setOnPasswordResetListener(() -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        sheet.show(getSupportFragmentManager(), "ResetPasswordBottomSheet");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }

}
