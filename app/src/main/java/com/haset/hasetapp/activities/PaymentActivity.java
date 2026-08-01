package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.AuditLogger;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.PaymentViewModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {
    private TextView tvDoctorName, tvSpecialty, tvAmount, tvPaymentMethod, tvSelectedPaymentDetails;
    private ImageView ivDoctorPhoto, ivVerifiedBadge;
    private MaterialButton btnPayNow, btnCancel;
    private LinearProgressIndicator progressIndicator;
    private Doctor doctor;
    private double consultationFee;
    private String paymentMethod = ""; // Will be set when user selects
    private String paymentMethodCode = "";
    private String paymentProvider = ""; // Specific provider (Mpesa, CRDB, etc.)
    private String paymentAccount = ""; // Mobile money number or account number
    private PaymentViewModel viewModel;
    private boolean paymentInitiated = false; // Guard against duplicate initiations
    private long lastClickTime = 0; // For debouncing clicks
    private String serviceMessageId = null;
    private String chatRoomId = null;
    private String consultationId;
    private boolean cardCheckoutOpened = false;

    private static final String STATE_CONSULTATION_ID = "payment_consultation_id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Block screenshots for payment screen (sensitive - financial data)
        com.haset.hasetapp.utils.SensitiveActivityHelper.blockScreenshots(this);
        
        setContentView(R.layout.activity_payment);

        // Get data from intent
        doctor = (Doctor) getIntent().getSerializableExtra("doctor");
        consultationFee = getIntent().getDoubleExtra("consultation_fee", 0.0);
        serviceMessageId = getIntent().getStringExtra("service_message_id");
        chatRoomId = getIntent().getStringExtra("chat_room_id");
        consultationId = savedInstanceState != null
            ? savedInstanceState.getString(STATE_CONSULTATION_ID)
            : getIntent().getStringExtra("consultation_id");
        if (TextUtils.isEmpty(consultationId)) {
            consultationId = !TextUtils.isEmpty(serviceMessageId)
                ? "service-" + serviceMessageId
                : "consult-" + UUID.randomUUID().toString();
        }

        initViews();
        viewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        setupObservers();
        setupViews();
        setupClickListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CONSULTATION_ID, consultationId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling when app goes to background
        if (!cardCheckoutOpened && viewModel != null && viewModel.getProcessing().getValue() != null && viewModel.getProcessing().getValue()) {
            viewModel.cancelPayment();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop polling when activity is destroyed
        if (viewModel != null) {
            viewModel.cancelPayment();
        }
    }

    private void setupObservers() {
        viewModel.getProcessing().observe(this, processing -> {
            if (processing != null && processing) {
                progressIndicator.setVisibility(View.VISIBLE);
                btnPayNow.setEnabled(false);
                btnCancel.setEnabled(false);
            } else {
                // Not processing anymore
                if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                if (btnPayNow != null) btnPayNow.setEnabled(true);
                if (btnCancel != null) btnCancel.setEnabled(true);
            }
        });

        viewModel.getInitiated().observe(this, initiated -> {
            if (initiated != null && initiated) {
                progressIndicator.setVisibility(View.VISIBLE);
                progressIndicator.setIndeterminate(true);
                tvSelectedPaymentDetails.setVisibility(View.VISIBLE);
                tvSelectedPaymentDetails.setText(R.string.payment_initiated);
                tvSelectedPaymentDetails.setTextColor(getResources().getColor(R.color.green_primary));
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    R.string.check_phone_ussd, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(android.R.color.holo_green_dark))
                    .show();
            }
        });

        viewModel.getPaymentUrl().observe(this, paymentUrl -> {
            if (paymentUrl == null || paymentUrl.trim().isEmpty()) return;
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paymentUrl));
                cardCheckoutOpened = true;
                startActivity(browserIntent);
                tvSelectedPaymentDetails.setText(R.string.card_checkout_opened);
            } catch (Exception ignored) {
                tvSelectedPaymentDetails.setText(R.string.card_checkout_unavailable);
            }
        });

        viewModel.getSuccess().observe(this, success -> {
            if (success != null && success) {
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgress(100, true);
                tvSelectedPaymentDetails.setText(R.string.payment_confirmed);
                
                // Log payment success
                if (doctor != null) {
                    AuditLogger.getInstance(this).logAppointmentUpdated(
                        "", "PAYMENT_SUCCESS", 
                        "Payment of " + consultationFee + " TZS successful for Dr. " + doctor.getFullName()
                    );
                }
                
                showSuccessDialog();
            }
        });

        viewModel.getCanRetry().observe(this, canRetry -> {
            if (canRetry != null && canRetry) {
                btnPayNow.setVisibility(View.GONE);
                btnCancel.setText(R.string.check_status);
                btnCancel.setOnClickListener(v -> {
                    viewModel.retryCheckStatus();
                    btnCancel.setEnabled(false);
                });
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                paymentInitiated = false;
                progressIndicator.setVisibility(View.GONE);
                tvSelectedPaymentDetails.setVisibility(View.VISIBLE);
                tvSelectedPaymentDetails.setText("Error: " + error);
                tvSelectedPaymentDetails.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                
                boolean isTimeout = error.toLowerCase().contains("time") || 
                    error.toLowerCase().contains("longer than expected") ||
                    error.toLowerCase().contains("unsuccessful");
                
                if (isTimeout) {
                    btnPayNow.setVisibility(View.GONE);
                    btnCancel.setText(R.string.check_status);
                    btnCancel.setOnClickListener(v -> {
                        viewModel.retryCheckStatus();
                        btnCancel.setEnabled(false);
                    });
                    btnCancel.setEnabled(true);
                } else {
                    btnPayNow.setEnabled(true);
                    btnPayNow.setAlpha(1.0f);
                    btnPayNow.setVisibility(View.VISIBLE);
                    btnCancel.setEnabled(true);
                    btnCancel.setText(R.string.cancel);
                    btnCancel.setOnClickListener(v -> {
                        if (viewModel.getProcessing().getValue() != null && viewModel.getProcessing().getValue()) {
                            showAbortDialog();
                        } else {
                            finish();
                        }
                    });
                }
                
                showErrorDialog(error);
            }
        });
    }

    private void showErrorDialog(String errorMessage) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_error, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnRetry = dialogView.findViewById(R.id.btnRetry);
        MaterialButton btnConfirmPaid = dialogView.findViewById(R.id.btnConfirmPaid);
        MaterialButton btnSecondary = dialogView.findViewById(R.id.btnSecondary);

        // Map technical errors to user-friendly messages if needed
        String displayMessage = errorMessage;
        if (errorMessage.toLowerCase().contains("insufficient funds")) {
            displayMessage = getString(R.string.insufficient_funds_msg);
        } else if (errorMessage.toLowerCase().contains("timeout") || errorMessage.toLowerCase().contains("timed out")) {
            displayMessage = getString(R.string.timeout_msg);
        } else if (errorMessage.toLowerCase().contains("cancel")) {
            displayMessage = getString(R.string.transaction_cancelled_msg);
        }

        tvDialogMessage.setText(displayMessage);

        boolean isTimeout = errorMessage.toLowerCase().contains("time") ||
            errorMessage.toLowerCase().contains("longer than expected") ||
            errorMessage.toLowerCase().contains("unsuccessful");

        if (isTimeout) {
            btnRetry.setVisibility(View.GONE);
            // Payment success must only come from the authenticated backend status.
            btnConfirmPaid.setVisibility(View.GONE);
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(R.string.check_status);
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                viewModel.retryCheckStatus();
            });
        } else {
            btnRetry.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(v -> {
                dialog.dismiss();
                processPayment();
            });
            btnConfirmPaid.setVisibility(View.GONE);
            btnSecondary.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void showSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_success, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnDone = dialogView.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("service_message_id", serviceMessageId);
            resultIntent.putExtra("chat_room_id", chatRoomId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        dialog.show();
    }

    private void showAbortDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_error, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnStay = dialogView.findViewById(R.id.btnRetry);
        MaterialButton btnAbort = dialogView.findViewById(R.id.btnSecondary);

        tvTitle.setText(R.string.abort_session_title);
        tvTitle.setTextColor(getResources().getColor(R.color.warning_color));
        tvMessage.setText(R.string.abort_session_warning);
        
        btnStay.setText(R.string.wait_for_session);
        btnStay.setOnClickListener(v -> dialog.dismiss());
        
        btnAbort.setText(R.string.abort_anyway);
        btnAbort.setOnClickListener(v -> {
            btnAbort.setEnabled(false);
            viewModel.requestCancelPayment(new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<com.haset.hasetapp.models.PaymentStatusResponse>() {
                @Override public void onSuccess(com.haset.hasetapp.models.PaymentStatusResponse ignored) {
                    dialog.dismiss();
                    finish();
                }
                @Override public void onError(String error) {
                    btnAbort.setEnabled(true);
                    Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void initViews() {
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpecialty = findViewById(R.id.tvSpecialty);
        tvAmount = findViewById(R.id.tvAmount);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvSelectedPaymentDetails = findViewById(R.id.tvSelectedPaymentDetails);
        ivDoctorPhoto = findViewById(R.id.ivDoctorPhoto);
        ivVerifiedBadge = findViewById(R.id.ivVerifiedBadge);
        btnPayNow = findViewById(R.id.btnPayNow);
        btnCancel = findViewById(R.id.btnCancel);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (viewModel.getProcessing().getValue() != null && viewModel.getProcessing().getValue()) {
                showAbortDialog();
            } else {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (viewModel.getProcessing().getValue() != null && viewModel.getProcessing().getValue()) {
            showAbortDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void setupViews() {
        if (doctor != null) {
            tvDoctorName.setText(getString(R.string.dr_prefix, doctor.getFullName()));
            tvSpecialty.setText(doctor.getSpecialty());
            
            // Load doctor profile photo
            if (doctor.getProfileImage() != null && !doctor.getProfileImage().isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(doctor.getProfileImage())
                        .placeholder(R.drawable.profile_photo)
                        .error(R.drawable.profile_photo)
                        .circleCrop()
                        .into(ivDoctorPhoto);
            }
            
            // Show verified badge if doctor is verified
            android.util.Log.d("PaymentActivity", "Doctor verified status: " + doctor.isVerified());
            if (doctor.isVerified()) {
                ivVerifiedBadge.setVisibility(View.VISIBLE);
                android.util.Log.d("PaymentActivity", "Verified badge shown");
            } else {
                ivVerifiedBadge.setVisibility(View.GONE);
            }
        }
        
        // Format amount in Tanzania Shillings
        String formattedAmount = String.format(Locale.getDefault(), "%,.0f TZS", consultationFee);
        tvAmount.setText(formattedAmount);
        tvPaymentMethod.setText(R.string.select_method);
        
        // Disable pay button until payment method is selected
        btnPayNow.setEnabled(false);
        btnPayNow.setAlpha(0.5f);
    }

    private void setupClickListeners() {
        btnPayNow.setOnClickListener(v -> {
            android.util.Log.d("PaymentActivity", "=== PAY NOW BUTTON CLICKED ===");
            
            // Prevent duplicate clicks by disabling button immediately and debouncing
            long currentTime = System.currentTimeMillis();
            if (!btnPayNow.isEnabled() || (currentTime - lastClickTime < 2000)) {
                android.util.Log.w("PaymentActivity", "Button disabled or debounced, ignoring click");
                return;
            }
            lastClickTime = currentTime;
            
            if (paymentMethod.isEmpty() || ("mobile_money".equals(paymentMethodCode) && paymentProvider.isEmpty())) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    R.string.select_payment_method, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
                return;
            }
            if ("mobile_money".equals(paymentMethodCode) && paymentAccount.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    R.string.enter_wallet_number, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
                return;
            }
            
            // Disable button and show processing state
            android.util.Log.d("PaymentActivity", "Disabling button and calling processPayment()");
            btnPayNow.setEnabled(false);
            btnPayNow.setAlpha(0.5f);
            
            processPayment();
        });
        btnCancel.setOnClickListener(v -> {
            if (viewModel.getProcessing().getValue() != null && viewModel.getProcessing().getValue()) {
                showAbortDialog();
            } else {
                finish();
            }
        });
        
        // Payment method selection - show bottom sheet
        tvPaymentMethod.setOnClickListener(v -> showPaymentMethodBottomSheet());
    }

    private void showPaymentMethodBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_payment_method, null);
        
        androidx.cardview.widget.CardView llMobileMoney = view.findViewById(R.id.llMobileMoney);
        androidx.cardview.widget.CardView llCardPayment = view.findViewById(R.id.llCardPayment);
        
        llMobileMoney.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showMobileMoneyProvidersBottomSheet();
        });
        
        llCardPayment.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            paymentMethodCode = "card";
            paymentMethod = getString(R.string.payment_card_payment);
            paymentProvider = "";
            paymentAccount = "";
            updatePaymentMethodDisplay(paymentMethod, "", getString(R.string.hosted_card_checkout));
            enablePayButton();
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showMobileMoneyProvidersBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_mobile_money_providers, null);
        
        androidx.cardview.widget.CardView llMpesa = view.findViewById(R.id.llMpesa);
        androidx.cardview.widget.CardView llMixxByYas = view.findViewById(R.id.llMixxByYas);
        androidx.cardview.widget.CardView llHalopesa = view.findViewById(R.id.llHalopesa);
        androidx.cardview.widget.CardView llAirtelMoney = view.findViewById(R.id.llAirtelMoney);
        androidx.cardview.widget.CardView llTPesa = view.findViewById(R.id.llTPesa);

        // Map for provider images
        Map<String, Integer> providerImages = new HashMap<>();
        providerImages.put("Mpesa", R.drawable.a_m_pesa_logo);
        providerImages.put("Mixx By Yas", R.drawable.a_mixx_by_yas);
        providerImages.put("Halopesa", R.drawable.a_halopesa_1);
        providerImages.put("Airtel Money", R.drawable.a_airtel_money);
        providerImages.put("T-Pesa", R.drawable.a_ttcl_pesa);
        
        View.OnClickListener providerClickListener = v -> {
            String provider = "";
            int imageResId = 0;
            if (v.getId() == R.id.llMpesa) {
                provider = "Vodacom";  // API expects: Vodacom
                imageResId = R.drawable.a_m_pesa_logo;
            } else if (v.getId() == R.id.llMixxByYas) {
                provider = "Mixx By Yas";
                imageResId = R.drawable.a_mixx_by_yas;
            } else if (v.getId() == R.id.llHalopesa) {
                provider = "Halotel";  // API expects: Halotel
                imageResId = R.drawable.a_halopesa_1;
            } else if (v.getId() == R.id.llAirtelMoney) {
                provider = "Airtel";  // API expects: Airtel
                imageResId = R.drawable.a_airtel_money;
            } else if (v.getId() == R.id.llTPesa) {
                provider = "Tigo";  // API expects: Tigo
                imageResId = R.drawable.a_ttcl_pesa;
            }
            
            paymentMethod = getString(R.string.payment_mobile_money);
            paymentMethodCode = "mobile_money";
            paymentProvider = provider;
            bottomSheetDialog.dismiss();
            showMobileNumberInputBottomSheet(provider, imageResId);
        };
        
        llMpesa.setOnClickListener(providerClickListener);
        llMixxByYas.setOnClickListener(providerClickListener);
        llHalopesa.setOnClickListener(providerClickListener);
        llAirtelMoney.setOnClickListener(providerClickListener);
        llTPesa.setOnClickListener(providerClickListener);
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showCardPaymentProvidersBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_card_payment_providers, null);
        
        androidx.cardview.widget.CardView llAkiba = view.findViewById(R.id.llAkiba);
        androidx.cardview.widget.CardView llCrdb = view.findViewById(R.id.llCrdb);
        androidx.cardview.widget.CardView llTcb = view.findViewById(R.id.llTcb);
        androidx.cardview.widget.CardView llNmb = view.findViewById(R.id.llNmb);
        
        View.OnClickListener providerClickListener = v -> {
            String provider = "";
            if (v.getId() == R.id.llAkiba) {
                provider = "AKIBA";
            } else if (v.getId() == R.id.llCrdb) {
                provider = "CRDB";
            } else if (v.getId() == R.id.llTcb) {
                provider = "TCB";
            } else if (v.getId() == R.id.llNmb) {
                provider = "NMB";
            }
            
            paymentMethod = getString(R.string.payment_card_payment);
            paymentMethodCode = "card";
            paymentProvider = "";
            bottomSheetDialog.dismiss();
            updatePaymentMethodDisplay(paymentMethod, provider, getString(R.string.hosted_card_checkout));
            enablePayButton();
        };
        
        llAkiba.setOnClickListener(providerClickListener);
        llCrdb.setOnClickListener(providerClickListener);
        llTcb.setOnClickListener(providerClickListener);
        llNmb.setOnClickListener(providerClickListener);
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showMobileNumberInputBottomSheet(String providerName, int providerImageResId) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_mobile_number_input, null);
        
        TextView tvProviderName = view.findViewById(R.id.tvProviderName);
        ImageView ivProviderLogo = view.findViewById(R.id.ivProviderLogo);
        TextInputEditText etMobileNumber = view.findViewById(R.id.etMobileNumber);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        
        tvProviderName.setText(providerName);
        ivProviderLogo.setImageResource(providerImageResId);
        
        // Add text watcher to format mobile number with spaces
        etMobileNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) {
                    return;
                }
                
                isFormatting = true;
                
                // Remove all spaces
                String digits = s.toString().replaceAll("\\s", "");
                
                // Format with spaces: XXX XXX XXX (for 9 digits)
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 3 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }
                
                // Update the text
                String formattedText = formatted.toString();
                if (!s.toString().equals(formattedText)) {
                    s.replace(0, s.length(), formattedText);
                }
                
                isFormatting = false;
            }
        });
        
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            // Get the number without spaces
            String mobileNumber = etMobileNumber.getText() != null ? 
                etMobileNumber.getText().toString().replaceAll("\\s", "").trim() : "";
            
            if (TextUtils.isEmpty(mobileNumber)) {
                Toast.makeText(this, R.string.enter_mobile_number, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (mobileNumber.length() < 9) {
                Toast.makeText(this, R.string.enter_valid_mobile, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // The payment API accepts either 0XXXXXXXXX or +255XXXXXXXXX.
            if (mobileNumber.startsWith("+255")) {
                paymentAccount = mobileNumber;
            } else if (mobileNumber.startsWith("255")) {
                paymentAccount = "+" + mobileNumber;
            } else if (mobileNumber.startsWith("0")) {
                paymentAccount = "+255" + mobileNumber.substring(1);
            } else {
                paymentAccount = "+255" + mobileNumber;
            }
            
            // Format for display: +255 XXX XXX XXX
            String displayNumber = formatMobileNumberForDisplay(paymentAccount);
            updatePaymentMethodDisplay(paymentMethod, paymentProvider, displayNumber);
            enablePayButton();
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showAccountNumberInputBottomSheet(String providerName) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_account_number_input, null);
        
        TextView tvProviderName = view.findViewById(R.id.tvProviderName);
        TextInputEditText etAccountNumber = view.findViewById(R.id.etAccountNumber);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        
        tvProviderName.setText(providerName);
        
        // Add text watcher to format account number with spaces (groups of 4)
        etAccountNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private static final int MAX_DIGITS = 20; // Maximum 20 digits for bank account number
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) {
                    return;
                }
                
                isFormatting = true;
                
                // Get current cursor position
                int cursorPos = etAccountNumber.getSelectionStart();
                
                // Remove all spaces and non-digit characters
                String originalText = s.toString();
                String digits = originalText.replaceAll("[^0-9]", "");
                
                // Count digits before cursor
                int digitsBeforeCursor = 0;
                for (int i = 0; i < cursorPos && i < originalText.length(); i++) {
                    if (Character.isDigit(originalText.charAt(i))) {
                        digitsBeforeCursor++;
                    }
                }
                
                // Limit to MAX_DIGITS
                if (digits.length() > MAX_DIGITS) {
                    digits = digits.substring(0, MAX_DIGITS);
                }
                
                // Format with spaces: XXXX XXXX XXXX XXXX (groups of 4)
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }
                
                // Update the text
                String formattedText = formatted.toString();
                if (!originalText.equals(formattedText)) {
                    s.replace(0, s.length(), formattedText);
                    
                    // Calculate new cursor position
                    int newCursorPos = 0;
                    int digitCount = 0;
                    for (int i = 0; i < formattedText.length() && digitCount < digitsBeforeCursor; i++) {
                        if (Character.isDigit(formattedText.charAt(i))) {
                            digitCount++;
                        }
                        newCursorPos = i + 1;
                    }
                    
                    // Set cursor position
                    if (newCursorPos <= formattedText.length()) {
                        etAccountNumber.setSelection(newCursorPos);
                    } else {
                        etAccountNumber.setSelection(formattedText.length());
                    }
                }
                
                isFormatting = false;
            }
        });
        
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            // Get the number without spaces
            String accountNumber = etAccountNumber.getText() != null ? 
                etAccountNumber.getText().toString().replaceAll("\\s", "").trim() : "";
            
            if (TextUtils.isEmpty(accountNumber)) {
                Toast.makeText(this, R.string.enter_account_number, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (accountNumber.length() < 8) {
                Toast.makeText(this, R.string.enter_valid_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (accountNumber.length() > 20) {
                Toast.makeText(this, R.string.account_number_limit, Toast.LENGTH_SHORT).show();
                return;
            }
            
            paymentAccount = accountNumber;
            // Format for display: XXXX XXXX XXXX
            String displayNumber = formatAccountNumberForDisplay(paymentAccount);
            updatePaymentMethodDisplay(paymentMethod, paymentProvider, displayNumber);
            enablePayButton();
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void updatePaymentMethodDisplay(String method, String provider, String wallet) {
        // Update the payment method button text
        tvPaymentMethod.setText(method);
        
        // Show the selected payment details below
        String details = provider + " • " + wallet;
        tvSelectedPaymentDetails.setText(details);
        tvSelectedPaymentDetails.setVisibility(View.VISIBLE);
    }

    private String formatMobileNumberForDisplay(String number) {
        // Remove +255 if present to format the remaining digits
        String digits = number.replace("+255", "").replaceAll("\\s", "");
        
        // Format: XXX XXX XXX
        StringBuilder formatted = new StringBuilder("+255");
        for (int i = 0; i < digits.length(); i++) {
            if (i % 3 == 0 && i > 0) {
                formatted.append(" ");
            }
            formatted.append(digits.charAt(i));
        }
        
        return formatted.toString();
    }

    private String formatAccountNumberForDisplay(String number) {
        // Remove all spaces
        String digits = number.replaceAll("\\s", "");
        
        // Format: XXXX XXXX XXXX XXXX (groups of 4)
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(digits.charAt(i));
        }
        
        return formatted.toString();
    }

    private void enablePayButton() {
        btnPayNow.setEnabled(true);
        btnPayNow.setAlpha(1.0f);
    }

    private void processPayment() {
        android.util.Log.d("PaymentActivity", "=== processPayment() CALLED ===");
        
        // Absolute guard against duplicate payment initiation
        if (paymentInitiated) {
            android.util.Log.w("PaymentActivity", "Payment already initiated in this session, blocking duplicate call");
            return;
        }
        
        // Validate payment amount using secure constants
        if (consultationFee < com.haset.hasetapp.utils.Constants.MIN_PAYMENT_AMOUNT) {
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                R.string.min_payment_msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
            return;
        }
        
        // Validate maximum amount to prevent fraud
        if (consultationFee > com.haset.hasetapp.utils.Constants.MAX_PAYMENT_AMOUNT) {
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                "Amount exceeds maximum limit", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
            return;
        }

        if (doctor != null) {
            String doctorId = doctor.getDoctorId() != null ? doctor.getDoctorId() : doctor.getUserId();
            String userId = getCurrentUserId();
            
            if (doctorId != null) {
                // Mark payment as initiated
                paymentInitiated = true;
                android.util.Log.d("PaymentActivity", "Calling viewModel.processPayment() - Amount: " + consultationFee + ", Provider: " + paymentProvider);
                
                // Call backend API with the documented mobile payment payload
                com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String buyerEmail = firebaseUser != null ? firebaseUser.getEmail() : null;
                String buyerName = firebaseUser != null ? firebaseUser.getDisplayName() : null;
                String buyerPhone = firebaseUser != null ? firebaseUser.getPhoneNumber() : null;
                viewModel.processPayment(
                    userId,
                    doctorId,
                    consultationId,
                    consultationFee,
                    paymentMethodCode,
                    paymentProvider,  // Already set when user selects provider
                    paymentAccount,
                    buyerEmail,
                    buyerName,
                    buyerPhone
                );
            } else {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                    R.string.doctor_id_missing, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
                progressIndicator.setVisibility(View.GONE);
                btnPayNow.setEnabled(true);
                btnCancel.setEnabled(true);
            }
        } else {
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                R.string.doctor_info_missing, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
            progressIndicator.setVisibility(View.GONE);
            btnPayNow.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }
    
    private String getCurrentUserId() {
        return com.google.firebase.auth.FirebaseAuth.getInstance()
               .getCurrentUser() != null ? 
               com.google.firebase.auth.FirebaseAuth.getInstance()
               .getCurrentUser().getUid() : null;
    }
}
