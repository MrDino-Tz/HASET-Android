package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.haset.hasetapp.R;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.DoctorHomeViewModel;
import com.haset.hasetapp.adapters.WithdrawalHistoryAdapter;
import com.haset.hasetapp.database.entities.WithdrawalRequest;
import java.util.List;
import java.util.Locale;
import com.haset.hasetapp.ui.MfaCodeInputView;
import com.haset.hasetapp.utils.FirebaseHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorWalletActivity extends BaseActivity {
    private static final int MFA_ENROLLMENT_REQUEST = 1703;
    private static final double DEFAULT_WITHDRAWAL_FEE = 1500.0;
    private TextView tvBalance, tvTotalEarnings;
    private RecyclerView rvTransactions;
    private PreferenceManager preferenceManager;
    private DoctorWalletEntity currentWallet;
    private MaterialButton btnWithdraw, btnPayoutAccounts;
    private DoctorHomeViewModel viewModel;
    private BottomSheetDialog withdrawDialog;
    private ImageView ivToggleBalance;
    private View llBalanceContainer;
    private boolean isBalanceVisible = false;
    private double balanceAmount = 0;
    private double withdrawalFee = DEFAULT_WITHDRAWAL_FEE;
    private WithdrawalHistoryAdapter historyAdapter;
    private List<WithdrawalRequest> latestWithdrawalRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_wallet);

        preferenceManager = new PreferenceManager(this);

        initViews();
        viewModel = new ViewModelProvider(this).get(DoctorHomeViewModel.class);
        setupObservers();
        loadWalletData();
        loadWithdrawalFee();
    }

    private void setupObservers() {
        viewModel.getWithdrawSuccess().observe(this, success -> {
            if (success != null) {
                if (withdrawDialog != null && withdrawDialog.isShowing()) {
                    withdrawDialog.dismiss();
                }
                if (success) {
                    Toast.makeText(DoctorWalletActivity.this, 
                        R.string.withdrawal_request_submitted, 
                        Toast.LENGTH_LONG).show();
                    String doctorId = preferenceManager.getUserId();
                    viewModel.refreshWalletBalance(doctorId);
                    viewModel.refreshWithdrawalRequests(doctorId);
                } else {
                    Toast.makeText(DoctorWalletActivity.this, R.string.insufficient_balance, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                if (withdrawDialog != null && withdrawDialog.isShowing()) {
                    withdrawDialog.dismiss();
                }
                Toast.makeText(this, getString(R.string.operation_failed, error), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                btnWithdraw.setEnabled(!isLoading);
                if (isLoading) {
                    btnWithdraw.setText("Processing...");
                } else {
                    btnWithdraw.setText("Withdraw");
                }
            }
        });

        viewModel.getWithdrawalRequests(preferenceManager.getUserId()).observe(this, requests -> {
            if (requests != null && historyAdapter != null) {
                latestWithdrawalRequests = requests;
                applyWithdrawalDestinationFallback();
                historyAdapter.setRequests(requests);
            }
        });
    }

    private com.facebook.shimmer.ShimmerFrameLayout shimmerBalance;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerTotalEarnings;
    private View headerBar;

    private void initViews() {
        tvBalance = findViewById(R.id.tvBalance);
        llBalanceContainer = findViewById(R.id.llBalanceContainer);
        shimmerBalance = findViewById(R.id.shimmerBalance);
        shimmerTotalEarnings = findViewById(R.id.shimmerTotalEarnings);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        rvTransactions = findViewById(R.id.rvTransactions);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnPayoutAccounts = findViewById(R.id.btnPayoutAccounts);
        ivToggleBalance = findViewById(R.id.ivToggleBalance);
        
        // Frosted header: transparent at rest, fades in a scrim as content scrolls under it.
        headerBar = findViewById(R.id.headerBar);
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollView);
        if (headerBar != null && headerBar.getBackground() != null) {
            headerBar.getBackground().mutate().setAlpha(0);
        }
        scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float fraction = Math.min(1f, scrollY / 400f);
            if (headerBar != null && headerBar.getBackground() != null) {
                headerBar.getBackground().mutate().setAlpha((int) (fraction * 235));
            }
        });
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnWithdraw.setOnClickListener(v -> checkMfaThenShowWithdrawal());
        btnPayoutAccounts.setOnClickListener(v -> checkMfaThenShowPayoutAccounts());
        
        ivToggleBalance.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            animateDisplay(tvBalance, ivToggleBalance, isBalanceVisible, balanceAmount);
        });
        
        // Setup RecyclerView for transactions
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new WithdrawalHistoryAdapter();
        rvTransactions.setAdapter(historyAdapter);
    }

    private void loadWalletData() {
        String doctorId = preferenceManager.getUserId();
        if (doctorId == null || doctorId.isEmpty()) return;

        viewModel.getWalletBalance(doctorId).observe(this, wallet -> {
            DoctorWalletEntity previous = currentWallet;
            currentWallet = wallet;
            if (wallet != null && previous != null) {
                if (!wallet.isMobileMoneyAvailable() && previous.isMobileMoneyPending() && !TextUtils.isEmpty(previous.getMobileMoneyLabel())) {
                    wallet.setMobileMoneyPending(true);
                    wallet.setMobileMoneyLabel(previous.getMobileMoneyLabel());
                }
                if (!wallet.isBankAvailable() && previous.isBankPending() && !TextUtils.isEmpty(previous.getBankLabel())) {
                    wallet.setBankPending(true);
                    wallet.setBankLabel(previous.getBankLabel());
                }
            }
            if (wallet != null) {
                if (wallet.getWithdrawalFeeAmount() != null && wallet.getWithdrawalFeeAmount() >= 0) {
                    withdrawalFee = wallet.getWithdrawalFeeAmount();
                }
                balanceAmount = wallet.getBalance();
                updateBalanceDisplay();
                
                // Format and display total earnings
                String formattedEarnings = String.format(Locale.getDefault(), getString(R.string.currency_format), wallet.getTotalEarnings());
                tvTotalEarnings.setText(formattedEarnings);
                
                applyWithdrawalDestinationFallback();
                updateWithdrawButtonState();
                
                // Stop shimmer and show content
                if (shimmerBalance != null) {
                    shimmerBalance.stopShimmer();
                    shimmerBalance.setVisibility(View.GONE);
                }
                if (shimmerTotalEarnings != null) {
                    shimmerTotalEarnings.stopShimmer();
                    shimmerTotalEarnings.setVisibility(View.GONE);
                }
                if (tvTotalEarnings != null) tvTotalEarnings.setVisibility(View.VISIBLE);
                if (llBalanceContainer != null) llBalanceContainer.setVisibility(View.VISIBLE);
            } else {
                balanceAmount = 0;
                updateBalanceDisplay();
                tvTotalEarnings.setText("0 TZS");
                updateWithdrawButtonState();
                
                // Stop shimmer and show default content
                if (shimmerBalance != null) {
                    shimmerBalance.stopShimmer();
                    shimmerBalance.setVisibility(View.GONE);
                }
                if (shimmerTotalEarnings != null) {
                    shimmerTotalEarnings.stopShimmer();
                    shimmerTotalEarnings.setVisibility(View.GONE);
                }
                if (tvTotalEarnings != null) tvTotalEarnings.setVisibility(View.VISIBLE);
                if (llBalanceContainer != null) llBalanceContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadWithdrawalFee() {
        FirebaseHelper.getAppConfigRef().get()
                .addOnSuccessListener(snapshot -> {
                    Double parsed = firstAmount(
                            snapshot.child("withdrawalFee").getValue(),
                            snapshot.child("withdrawal_fee").getValue());
                    if (parsed != null && parsed >= 0) {
                        withdrawalFee = parsed;
                    } else {
                        loadWithdrawalFeeFromAppSettings();
                    }
                })
                .addOnFailureListener(error -> loadWithdrawalFeeFromAppSettings());
    }

    private void loadWithdrawalFeeFromAppSettings() {
        FirebaseHelper.getAppSettingsRef().get()
                .addOnSuccessListener(snapshot -> {
                    Double parsed = firstAmount(
                            snapshot.child("withdrawalFee").getValue(),
                            snapshot.child("withdrawal_fee").getValue());
                    withdrawalFee = parsed != null && parsed >= 0 ? parsed : DEFAULT_WITHDRAWAL_FEE;
                })
                .addOnFailureListener(error -> withdrawalFee = DEFAULT_WITHDRAWAL_FEE);
    }

    private void applyWithdrawalDestinationFallback() {
        if (currentWallet == null || latestWithdrawalRequests == null) return;
        if (currentWallet.isMobileMoneyAvailable() || currentWallet.isBankAvailable()) return;

        for (WithdrawalRequest request : latestWithdrawalRequests) {
            if (request == null) continue;
            String status = request.getStatus();
            boolean usableStatus = WithdrawalRequest.STATUS_APPROVED.equalsIgnoreCase(status)
                    || WithdrawalRequest.STATUS_COMPLETED.equalsIgnoreCase(status)
                    || WithdrawalRequest.STATUS_PENDING.equalsIgnoreCase(status);
            if (!usableStatus) continue;

            String account = request.getAccountNumber();
            if (TextUtils.isEmpty(account)) continue;

            boolean approved = WithdrawalRequest.STATUS_APPROVED.equalsIgnoreCase(status)
                    || WithdrawalRequest.STATUS_COMPLETED.equalsIgnoreCase(status);
            boolean pending = WithdrawalRequest.STATUS_PENDING.equalsIgnoreCase(status);

            if (WithdrawalRequest.METHOD_BANK.equals(request.getMethod())) {
                currentWallet.setBankAvailable(approved);
                currentWallet.setBankPending(pending);
                String bankName = TextUtils.isEmpty(request.getBankName()) ? "" : request.getBankName();
                currentWallet.setBankLabel((bankName + "  " + account).trim());
            } else {
                currentWallet.setMobileMoneyAvailable(approved);
                currentWallet.setMobileMoneyPending(pending);
                String provider = TextUtils.isEmpty(request.getBankName()) ? "Mobile Money" : request.getBankName();
                currentWallet.setMobileMoneyLabel((provider + "  " + account).trim());
            }
            return;
        }
    }
    private void animateDisplay(TextView textView, ImageView toggleIcon, boolean isVisible, double amount) {
        if (textView == null || toggleIcon == null) return;

        textView.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction(() -> {
                if (isVisible) {
                    textView.setText(String.format(Locale.getDefault(), getString(R.string.currency_format), amount));
                    toggleIcon.setAlpha(1.0f);
                } else {
                    textView.setText("•••••••• TZS");
                    toggleIcon.setAlpha(0.4f);
                }
                
                textView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start();
            })
            .start();
    }

    private void updateBalanceDisplay() {
        if (tvBalance == null) return;
        tvBalance.setText(isBalanceVisible ? 
            String.format(Locale.getDefault(), getString(R.string.currency_format), balanceAmount) : "•••••••• TZS");
        if (ivToggleBalance != null) ivToggleBalance.setAlpha(isBalanceVisible ? 1.0f : 0.4f);
    }

    private void showWithdrawBottomSheet() {
        if (currentWallet == null || currentWallet.getBalance() <= 0) {
            Toast.makeText(this, R.string.no_balance_withdrawal, Toast.LENGTH_SHORT).show();
            return;
        }

        if (withdrawDialog == null) {
            withdrawDialog = new BottomSheetDialog(this);
        }
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_withdraw, null);

        TextView tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        TextInputEditText etWithdrawAmount = view.findViewById(R.id.etAmount);
        TextView tvPayoutAmount = view.findViewById(R.id.tvPayoutAmount);
        TextView tvWithdrawalFee = view.findViewById(R.id.tvWithdrawalFee);
        TextView tvTotalDeduction = view.findViewById(R.id.tvTotalDeduction);
        com.google.android.material.card.MaterialCardView cvMobileMoney = view.findViewById(R.id.llMobileMoney);
        com.google.android.material.card.MaterialCardView cvBank = view.findViewById(R.id.llBank);
        TextView tvMobileDestination = view.findViewById(R.id.tvMobileDestination);
        TextView tvBankDestination = view.findViewById(R.id.tvBankDestination);
        ImageView ivMobileSelected = view.findViewById(R.id.ivMobileSelected);
        ImageView ivBankSelected = view.findViewById(R.id.ivBankSelected);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);
        MfaCodeInputView mfaCodeInput = view.findViewById(R.id.mfaCodeInput);

        double feeAmountForLimit = Math.max(0, withdrawalFee);
        double maxPayoutAmount = Math.max(0, currentWallet.getBalance() - feeAmountForLimit);

        // Set available balance
        String availableBalance = String.format(Locale.getDefault(), getString(R.string.available_balance_with_max_format),
                currentWallet.getBalance(), maxPayoutAmount);
        tvAvailableBalance.setText(availableBalance);
        updateWithdrawalBreakdown(0, tvPayoutAmount, tvWithdrawalFee, tvTotalDeduction);

        final String[] selectedMethod = {currentWallet.isMobileMoneyAvailable() ? "mobile_money" : (currentWallet.isBankAvailable() ? "bank" : "")};
        boolean hasApprovedDestination = !selectedMethod[0].isEmpty();
        tvMobileDestination.setText(destinationLabel(currentWallet.getMobileMoneyLabel(), currentWallet.isMobileMoneyPending()));
        tvBankDestination.setText(destinationLabel(currentWallet.getBankLabel(), currentWallet.isBankPending()));
        cvMobileMoney.setEnabled(currentWallet.isMobileMoneyAvailable());
        cvBank.setEnabled(currentWallet.isBankAvailable());
        etWithdrawAmount.setEnabled(hasApprovedDestination);
        etWithdrawAmount.setFocusable(hasApprovedDestination);
        etWithdrawAmount.setFocusableInTouchMode(hasApprovedDestination);
        btnConfirmWithdraw.setEnabled(hasApprovedDestination);
        Runnable renderSelection = () -> {
            boolean mobile = "mobile_money".equals(selectedMethod[0]);
            boolean bank = "bank".equals(selectedMethod[0]);
            int selectedBackground = androidx.core.content.ContextCompat.getColor(this, R.color.green_light_very);
            int neutralBackground = androidx.core.content.ContextCompat.getColor(this, R.color.white);
            int selectedStroke = androidx.core.content.ContextCompat.getColor(this, R.color.green_primary);
            int neutralStroke = androidx.core.content.ContextCompat.getColor(this, R.color.chat_bubble_border);
            int oneDp = Math.max(1, Math.round(getResources().getDisplayMetrics().density));
            cvMobileMoney.setCardBackgroundColor(mobile ? selectedBackground : neutralBackground);
            cvMobileMoney.setStrokeColor(mobile ? selectedStroke : neutralStroke);
            cvMobileMoney.setStrokeWidth(mobile ? oneDp * 2 : oneDp);
            cvBank.setCardBackgroundColor(bank ? selectedBackground : neutralBackground);
            cvBank.setStrokeColor(bank ? selectedStroke : neutralStroke);
            cvBank.setStrokeWidth(bank ? oneDp * 2 : oneDp);
            cvMobileMoney.setAlpha(currentWallet.isMobileMoneyAvailable() ? 1f : 0.5f);
            cvBank.setAlpha(currentWallet.isBankAvailable() ? 1f : 0.5f);
            ivMobileSelected.setVisibility(mobile ? View.VISIBLE : View.INVISIBLE);
            ivBankSelected.setVisibility(bank ? View.VISIBLE : View.INVISIBLE);
        };
        cvMobileMoney.setOnClickListener(v -> { if (currentWallet.isMobileMoneyAvailable()) { selectedMethod[0] = "mobile_money"; renderSelection.run(); } });
        cvBank.setOnClickListener(v -> { if (currentWallet.isBankAvailable()) { selectedMethod[0] = "bank"; renderSelection.run(); } });
        renderSelection.run();
        
        // Format amount input
        etWithdrawAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String amountStr = s.toString().trim();
                // Keep the action responsive for invalid amounts so the click handler
                // can explain the gateway minimum or insufficient balance.
                btnConfirmWithdraw.setEnabled(hasApprovedDestination && !TextUtils.isEmpty(amountStr));
                Double amount = parseAmount(amountStr);
                updateWithdrawalBreakdown(amount != null ? amount : 0, tvPayoutAmount, tvWithdrawalFee, tvTotalDeduction);
            }
        });

        btnCancel.setOnClickListener(v -> withdrawDialog.dismiss());

        btnConfirmWithdraw.setOnClickListener(v -> {
            String amountStr = etWithdrawAmount.getText() != null ? etWithdrawAmount.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(this, R.string.enter_withdrawal_amount, Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.enter_valid_amount_msg, Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount < 5000) {
                double missing = 5000 - currentWallet.getBalance();
                String message = currentWallet.getBalance() < 5000
                        ? getString(R.string.minimum_withdrawal_missing_amount, missing)
                        : getString(R.string.minimum_withdrawal_amount);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }

            double feeAmount = Math.max(0, withdrawalFee);
            double totalDeduction = amount + feeAmount;
            if (totalDeduction > currentWallet.getBalance()) {
                Toast.makeText(this,
                        String.format(Locale.getDefault(),
                                getString(R.string.withdrawal_amount_exceeds_available_after_fee),
                                Math.max(0, currentWallet.getBalance() - feeAmount), feeAmount),
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Process withdrawal
            if (!mfaCodeInput.isComplete()) { mfaCodeInput.setErrorState(true); Toast.makeText(this, "Enter the six-digit MFA code", Toast.LENGTH_SHORT).show(); return; }
            if (selectedMethod[0].isEmpty()) { Toast.makeText(this, R.string.no_verified_payout_destination, Toast.LENGTH_LONG).show(); return; }
            processWithdrawal(amount, feeAmount, selectedMethod[0], btnConfirmWithdraw, mfaCodeInput.getCode());
        });

        withdrawDialog.setContentView(view);
        if (withdrawDialog.getWindow() != null) {
            withdrawDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        withdrawDialog.show();
    }

    private void checkMfaThenShowWithdrawal() {
        if (currentWallet == null || currentWallet.getBalance() <= 0) {
            Toast.makeText(this, R.string.no_balance_withdrawal, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasApprovedPayoutDestination()) {
            Toast.makeText(this, R.string.no_verified_payout_destination, Toast.LENGTH_LONG).show();
            return;
        }
        com.google.firebase.auth.FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
            return;
        }
        btnWithdraw.setEnabled(false);
        user.getIdToken(true).addOnSuccessListener(token ->
            RetrofitClient.getInstance().getMobileMfaApiService()
                .status("Bearer " + token.getToken())
                .enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        btnWithdraw.setEnabled(true);
                        boolean enabled = response.isSuccessful() && response.body() != null
                                && response.body().has("two_factor_enabled")
                                && response.body().get("two_factor_enabled").getAsBoolean();
                        if (enabled) showWithdrawBottomSheet();
                        else showMfaRequiredDialog();
                    }

                    @Override public void onFailure(Call<JsonObject> call, Throwable throwable) {
                        btnWithdraw.setEnabled(true);
                        Toast.makeText(DoctorWalletActivity.this,
                                R.string.mfa_status_unavailable, Toast.LENGTH_LONG).show();
                    }
                })
        ).addOnFailureListener(error -> {
            btnWithdraw.setEnabled(true);
            Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
        });
    }

    private boolean hasApprovedPayoutDestination() {
        return currentWallet != null
                && (currentWallet.isMobileMoneyAvailable() || currentWallet.isBankAvailable());
    }

    private void updateWithdrawButtonState() {
        if (btnWithdraw == null) return;
        boolean hasBalance = currentWallet != null && currentWallet.getBalance() > 0;
        boolean hasApprovedDestination = hasApprovedPayoutDestination();
        boolean enabled = hasBalance;
        btnWithdraw.setEnabled(enabled);
        btnWithdraw.setAlpha(hasBalance && hasApprovedDestination ? 1.0f : 0.55f);
    }

    private void showMfaRequiredDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.mfa_required_for_withdrawal_title)
                .setMessage(R.string.mfa_required_for_withdrawal_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.enable_mfa, (dialog, which) ->
                        startActivityForResult(
                                new Intent(this, MfaEnrollmentActivity.class),
                                MFA_ENROLLMENT_REQUEST))
                .show();
    }

    private void checkMfaThenShowPayoutAccounts() {
        com.google.firebase.auth.FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) { Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show(); return; }
        btnPayoutAccounts.setEnabled(false);
        user.getIdToken(true).addOnSuccessListener(token -> RetrofitClient.getInstance().getMobileMfaApiService()
                .status("Bearer " + token.getToken()).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        btnPayoutAccounts.setEnabled(true);
                        boolean enabled = response.isSuccessful() && response.body() != null
                                && response.body().has("two_factor_enabled")
                                && response.body().get("two_factor_enabled").getAsBoolean();
                        if (enabled) showPayoutAccountDialog(); else showMfaRequiredDialog();
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable throwable) {
                        btnPayoutAccounts.setEnabled(true);
                        Toast.makeText(DoctorWalletActivity.this, R.string.mfa_status_unavailable, Toast.LENGTH_LONG).show();
                    }
                }))
                .addOnFailureListener(error -> {
                    btnPayoutAccounts.setEnabled(true);
                    Toast.makeText(this, R.string.authentication_expired, Toast.LENGTH_SHORT).show();
                });
    }

    private void showPayoutAccountDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payout_destination, null);
        RadioGroup typeGroup = view.findViewById(R.id.rgDestinationType);
        LinearLayout mobileFields = view.findViewById(R.id.mobileFields);
        LinearLayout bankFields = view.findViewById(R.id.bankFields);
        AutoCompleteTextView provider = view.findViewById(R.id.etPayoutProvider);
        TextInputEditText phone = view.findViewById(R.id.etPayoutPhone);
        AutoCompleteTextView bankCode = view.findViewById(R.id.etBankCode);
        TextInputEditText bankAccount = view.findViewById(R.id.etBankAccount);
        MfaCodeInputView mfaCode = view.findViewById(R.id.destinationMfaCodeInput);
        String[] providers = {"Vodacom M-Pesa", "Airtel Money", "Tigo Pesa", "Halotel", "Mixx by Yas"};
        provider.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providers));
        String[] banks = {"CRDB", "NMB", "TCB", "AKIBA Bank", "DTB", "KCB", "Azania Bank", "Mwanga Bank", "Selcom MF Bank"};
        bankCode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, banks));
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean bank = checkedId == R.id.rbBank;
            mobileFields.setVisibility(bank ? View.GONE : View.VISIBLE);
            bankFields.setVisibility(bank ? View.VISIBLE : View.GONE);
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Payout accounts")
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Submit for approval", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean bank = typeGroup.getCheckedRadioButtonId() == R.id.rbBank;
            String code = mfaCode.getCode();
            if (!mfaCode.isComplete()) {
                mfaCode.setErrorState(true);
                Toast.makeText(this, "Enter your six-digit MFA code", Toast.LENGTH_SHORT).show();
                return;
            }
            JsonObject body = new JsonObject();
            body.addProperty("destination_type", bank ? "bank" : "mobile_money");
            if (bank) {
                if (text(bankCode).isEmpty() || text(bankAccount).length() < 5) { bankAccount.setError("Enter a valid bank and account number"); return; }
                body.addProperty("bank_code", text(bankCode)); body.addProperty("bank_account", text(bankAccount));
            } else {
                if (text(provider).isEmpty() || !text(phone).matches("^(?:0\\d{9}|\\+255\\d{9})$")) { phone.setError("Use 07XXXXXXXX or +255XXXXXXXXX"); return; }
                body.addProperty("provider", text(provider)); body.addProperty("phone_number", text(phone));
            }
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            submitPayoutDestination(body, code, dialog);
        }));
        dialog.show();
        mfaCode.focusFirst();
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void submitPayoutDestination(JsonObject destination, String mfaCode, androidx.appcompat.app.AlertDialog dialog) {
        com.google.firebase.auth.FirebaseUser user = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (user == null) { dialog.dismiss(); return; }
        user.getIdToken(true).addOnSuccessListener(token -> {
            String bearer = "Bearer " + token.getToken();
            JsonObject codeBody = new JsonObject(); codeBody.addProperty("code", mfaCode);
            RetrofitClient.getInstance().getMobileMfaApiService().verify(bearer, codeBody).enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null || !response.body().has("mfa_action_token")) {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(DoctorWalletActivity.this, "Invalid or expired MFA code.", Toast.LENGTH_LONG).show(); return;
                    }
                    String actionToken = response.body().get("mfa_action_token").getAsString();
                    RetrofitClient.getInstance().getDoctorPayoutApiService().updatePayoutDestination(bearer, actionToken, destination).enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            if (r.isSuccessful()) {
                                mirrorPendingPayoutDestination(destination, user.getUid());
                                dialog.dismiss();
                                Toast.makeText(DoctorWalletActivity.this, "Payout account submitted. Waiting for admin approval before withdrawal.", Toast.LENGTH_LONG).show();
                                boolean bank = "bank".equals(destination.get("destination_type").getAsString());
                                if (currentWallet == null) {
                                    currentWallet = new DoctorWalletEntity(preferenceManager.getUserId(), 0);
                                }
                                if (bank) {
                                    currentWallet.setBankAvailable(false);
                                    currentWallet.setBankPending(true);
                                    currentWallet.setBankLabel((destination.get("bank_code").getAsString() + "  " + destination.get("bank_account").getAsString()).trim());
                                } else {
                                    currentWallet.setMobileMoneyAvailable(false);
                                    currentWallet.setMobileMoneyPending(true);
                                    currentWallet.setMobileMoneyLabel((destination.get("provider").getAsString() + "  " + destination.get("phone_number").getAsString()).trim());
                                }
                                viewModel.refreshWalletBalance(preferenceManager.getUserId());
                            } else Toast.makeText(DoctorWalletActivity.this, payoutErrorMessage(r), Toast.LENGTH_LONG).show();
                        }
                        @Override public void onFailure(Call<JsonObject> c, Throwable t) { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true); Toast.makeText(DoctorWalletActivity.this, "Network error while saving payout account.", Toast.LENGTH_LONG).show(); }
                    });
                }
                @Override public void onFailure(Call<JsonObject> call, Throwable t) { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true); Toast.makeText(DoctorWalletActivity.this, "Network error while verifying MFA.", Toast.LENGTH_LONG).show(); }
            });
        }).addOnFailureListener(error -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true));
    }

    private void mirrorPendingPayoutDestination(JsonObject destination, String doctorId) {
        FirebaseHelper.submitPayoutDestinationForApproval(
                doctorId,
                destination.has("destination_type") ? destination.get("destination_type").getAsString() : "",
                destination.has("provider") ? destination.get("provider").getAsString() : "",
                destination.has("bank_code") ? destination.get("bank_code").getAsString() : "",
                destination.has("phone_number") ? destination.get("phone_number").getAsString() : "",
                destination.has("bank_account") ? destination.get("bank_account").getAsString() : "",
                new FirebaseHelper.OnCompleteListener<Boolean>() {
                    @Override public void onSuccess(Boolean result) {
                        Log.d("HASET_WALLET", "Pending payout destination mirrored to Firebase after backend submission");
                    }

                    @Override public void onError(String error) {
                        Log.d("HASET_WALLET", "Failed to mirror pending payout destination to Firebase: " + error);
                    }
                });
    }

    private String payoutErrorMessage(Response<JsonObject> response) {
        try {
            if (response.errorBody() != null) {
                JsonObject body = com.google.gson.JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                if (body.has("message") && !body.get("message").isJsonNull()) return body.get("message").getAsString();
                if (body.has("errors") && body.get("errors").isJsonObject()) {
                    for (String key : body.getAsJsonObject("errors").keySet()) {
                        if (body.getAsJsonObject("errors").get(key).isJsonArray()
                                && body.getAsJsonObject("errors").getAsJsonArray(key).size() > 0) {
                            return body.getAsJsonObject("errors").getAsJsonArray(key).get(0).getAsString();
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return response.code() >= 500
                ? "The payout service is temporarily unavailable. Please try again."
                : "Could not save payout account. Check the details and try again.";
    }

    private String destinationLabel(String label, boolean pending) {
        if (label == null || label.trim().isEmpty()) {
            return pending ? "Pending admin approval" : getString(R.string.not_configured);
        }
        return pending ? "Pending admin approval: " + label.trim() : label.trim();
    }

    private void updateWithdrawalBreakdown(double payoutAmount, TextView tvPayoutAmount, TextView tvWithdrawalFee, TextView tvTotalDeduction) {
        double safePayout = Math.max(0, payoutAmount);
        double feeAmount = Math.max(0, withdrawalFee);
        double totalDeduction = safePayout + feeAmount;
        tvPayoutAmount.setText(formatTzs(safePayout));
        tvWithdrawalFee.setText(formatTzs(feeAmount));
        tvTotalDeduction.setText(formatTzs(totalDeduction));
    }

    private String formatTzs(double amount) {
        return String.format(Locale.getDefault(), "TZS %,.0f", amount);
    }

    private Double parseAmount(Object rawValue) {
        if (rawValue instanceof Number) {
            return ((Number) rawValue).doubleValue();
        }
        if (rawValue instanceof String) {
            try {
                return Double.parseDouble(((String) rawValue).trim().replace(",", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double firstAmount(Object... values) {
        for (Object value : values) {
            Double parsed = parseAmount(value);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private void processWithdrawal(double amount, double feeAmount, String payoutMethod, MaterialButton confirmBtn, String mfaCode) {
        confirmBtn.setEnabled(false);
        confirmBtn.setText(R.string.processing);

        // The backend uses the independently verified payout destination stored on the wallet.
        viewModel.requestWithdrawalSecure(amount, feeAmount, "Doctor payout request", payoutMethod, mfaCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MFA_ENROLLMENT_REQUEST && resultCode == RESULT_OK) {
            checkMfaThenShowWithdrawal();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String doctorId = preferenceManager.getUserId();
        viewModel.refreshWalletBalance(doctorId);
        viewModel.refreshWithdrawalRequests(doctorId);
    }
}
