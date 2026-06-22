package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AuthViewModel;
import com.haset.hasetapp.utils.ValidationUtils;
import android.widget.Toast;
import android.content.Intent;

public class ForgotPasswordActivity extends BaseActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSend;
    private TextView tvBackToLogin;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        initViews();
        setupClickListeners();
        setupObservers();
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
            String email = etEmail.getText().toString().trim();
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.setError(getString(R.string.error_email));
                return;
            }

            authViewModel.resetPassword(email);
        });

        // Back to login link
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }
}