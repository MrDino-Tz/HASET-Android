package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.ValidationUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ResetPasswordBottomSheet — completes the password reset fully inside the app.
 *
 * Flow (no Firebase web page involved):
 *  1. User taps the reset link in their email → deep link opens ForgotPasswordActivity
 *     with the oobCode, OR the user copies/pastes the link into this sheet.
 *  2. oobCode is extracted, verified via verifyPasswordResetCode() (shows the
 *     account email), then confirmPasswordReset(code, newPassword) applies it.
 */
public class ResetPasswordBottomSheet extends BottomSheetDialogFragment {

    public interface OnPasswordResetListener {
        void onPasswordResetSuccess();
    }

    private static final String ARG_LINK = "arg_link";
    private static final Pattern OOB_CODE_PATTERN = Pattern.compile("[?&]oobCode=([A-Za-z0-9_-]+)");

    private OnPasswordResetListener listener;

    private TextInputEditText etPasteLink;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputLayout tilPasteLink;
    private TextView tvVerifiedEmail;
    private MaterialButton btnResetPassword;
    private ProgressBar progressReset;

    public static ResetPasswordBottomSheet newInstance(@Nullable String pastedLinkOrCode) {
        ResetPasswordBottomSheet sheet = new ResetPasswordBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LINK, pastedLinkOrCode);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnPasswordResetListener(OnPasswordResetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilPasteLink = view.findViewById(R.id.tilPasteLink);
        etPasteLink = view.findViewById(R.id.etPasteLink);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        tvVerifiedEmail = view.findViewById(R.id.tvVerifiedEmail);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        progressReset = view.findViewById(R.id.progressReset);

        String prefilled = getArguments() != null ? getArguments().getString(ARG_LINK) : null;
        if (!TextUtils.isEmpty(prefilled)) {
            // Opened via deep link: code already captured, hide the paste field
            tilPasteLink.setVisibility(View.GONE);
            etPasteLink.setText(prefilled);
        }

        btnResetPassword.setOnClickListener(v -> attemptReset());
    }

    private void attemptReset() {
        String rawInput = etPasteLink.getText() != null
                ? etPasteLink.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null
                ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null
                ? etConfirmPassword.getText().toString().trim() : "";

        String code = extractOobCode(rawInput);
        if (TextUtils.isEmpty(code)) {
            if (tilPasteLink.getVisibility() == View.VISIBLE) {
                tilPasteLink.setError(getString(R.string.invalid_reset_code));
                return;
            }
            Toast.makeText(getContext(), R.string.invalid_reset_code, Toast.LENGTH_LONG).show();
            return;
        }

        if (!ValidationUtils.isStrongPassword(newPassword)) {
            etNewPassword.setError(getString(R.string.error_strong_password));
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            return;
        }

        setLoading(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Step 1: verify code (also confirms which account it belongs to)
        auth.verifyPasswordResetCode(code)
                .addOnSuccessListener(email -> {
                    tvVerifiedEmail.setText(getString(R.string.reset_code_verified_as, email));
                    tvVerifiedEmail.setVisibility(View.VISIBLE);
                    // Step 2: apply the new password
                    auth.confirmPasswordReset(code, newPassword)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                Toast.makeText(getContext(),
                                        R.string.password_reset_success, Toast.LENGTH_LONG).show();
                                if (listener != null) {
                                    listener.onPasswordResetSuccess();
                                }
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showFailure(e);
                });
    }

    private void showFailure(Exception e) {
        int messageRes;
        if (e instanceof FirebaseAuthWeakPasswordException) {
            messageRes = R.string.error_weak_password;
        } else if (e.getMessage() != null
                && (e.getMessage().toLowerCase().contains("password-does-not-meet-requirements")
                    || e.getMessage().toLowerCase().contains("missing password requirements"))) {
            messageRes = R.string.error_weak_password;
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            messageRes = R.string.invalid_reset_code;
        } else {
            messageRes = R.string.invalid_reset_code; // covers expired/used codes too
        }
        Toast.makeText(getContext(), getString(messageRes), Toast.LENGTH_LONG).show();
    }

    /**
     * Accepts either the full Firebase action link
     * (...__/auth/action?mode=resetPassword&oobCode=XYZ...) or a bare oobCode.
     */
    static String extractOobCode(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        Matcher matcher = OOB_CODE_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Bare code fallback: Firebase oobCodes are long url-safe tokens
        if (input.matches("[A-Za-z0-9_-]{20,}")) {
            return input;
        }
        return null;
    }

    private void setLoading(boolean loading) {
        btnResetPassword.setEnabled(!loading);
        progressReset.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
