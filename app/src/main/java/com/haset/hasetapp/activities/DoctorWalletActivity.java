package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.DoctorHomeViewModel;
import com.haset.hasetapp.adapters.WithdrawalHistoryAdapter;
import com.haset.hasetapp.database.entities.WithdrawalRequest;
import java.util.List;
import java.util.Locale;

public class DoctorWalletActivity extends BaseActivity {
    private TextView tvBalance, tvTotalEarnings;
    private RecyclerView rvTransactions;
    private PreferenceManager preferenceManager;
    private DoctorWalletEntity currentWallet;
    private MaterialButton btnWithdraw;
    private DoctorHomeViewModel viewModel;
    private BottomSheetDialog withdrawDialog;
    private ImageView ivToggleBalance;
    private View llBalanceContainer;
    private boolean isBalanceVisible = false;
    private double balanceAmount = 0;
    private WithdrawalHistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_wallet);

        preferenceManager = new PreferenceManager(this);

        initViews();
        viewModel = new ViewModelProvider(this).get(DoctorHomeViewModel.class);
        setupObservers();
        loadWalletData();
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
                    loadWalletData(); // Reload wallet data
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
                historyAdapter.setRequests(requests);
            }
        });
    }

    private com.facebook.shimmer.ShimmerFrameLayout shimmerBalance;

    private void initViews() {
        tvBalance = findViewById(R.id.tvBalance);
        llBalanceContainer = findViewById(R.id.llBalanceContainer);
        shimmerBalance = findViewById(R.id.shimmerBalance);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        rvTransactions = findViewById(R.id.rvTransactions);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        ivToggleBalance = findViewById(R.id.ivToggleBalance);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnWithdraw.setOnClickListener(v -> showWithdrawBottomSheet());
        
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
            currentWallet = wallet;
            if (wallet != null) {
                balanceAmount = wallet.getBalance();
                updateBalanceDisplay();
                
                // Format and display total earnings
                String formattedEarnings = String.format(Locale.getDefault(), getString(R.string.currency_format), wallet.getTotalEarnings());
                tvTotalEarnings.setText(formattedEarnings);
                
                // Enable/disable withdraw button based on balance
                btnWithdraw.setEnabled(wallet.getBalance() > 0);
                btnWithdraw.setAlpha(wallet.getBalance() > 0 ? 1.0f : 0.5f);
                
                // Stop shimmer and show content
                if (shimmerBalance != null) {
                    shimmerBalance.stopShimmer();
                    shimmerBalance.setVisibility(View.GONE);
                }
                if (llBalanceContainer != null) llBalanceContainer.setVisibility(View.VISIBLE);
            } else {
                balanceAmount = 0;
                updateBalanceDisplay();
                tvTotalEarnings.setText("0 TZS");
                btnWithdraw.setEnabled(false);
                btnWithdraw.setAlpha(0.5f);
                
                // Stop shimmer and show default content
                if (shimmerBalance != null) {
                    shimmerBalance.stopShimmer();
                    shimmerBalance.setVisibility(View.GONE);
                }
                if (llBalanceContainer != null) llBalanceContainer.setVisibility(View.VISIBLE);
            }
        });
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
        // Bank transfer options commented out - using mobile money only
        // LinearLayout llBankTransfer = view.findViewById(R.id.llBankTransfer);
        com.google.android.material.card.MaterialCardView cvMobileMoney = view.findViewById(R.id.llMobileMoney);
        ImageView ivMobileSelected = view.findViewById(R.id.ivMobileSelected);
        LinearLayout llAccountDetails = view.findViewById(R.id.llAccountDetails);
        TextInputLayout tilMobileNumber = view.findViewById(R.id.tilMobileNumber);
        TextInputEditText etMobileNumber = view.findViewById(R.id.etMobileNumber);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirmWithdraw = view.findViewById(R.id.btnConfirmWithdraw);

        // Set available balance
        String availableBalance = String.format(Locale.getDefault(), getString(R.string.available_balance_format), currentWallet.getBalance());
        tvAvailableBalance.setText(availableBalance);

        // Withdrawal method selection
        String[] selectedMethod = {""}; // Use array to allow modification in inner class

        // Bank transfer option commented out - using mobile money only
        // llBankTransfer.setOnClickListener(v -> {
        //     selectedMethod[0] = "bank";
        //     ivBankSelected.setVisibility(View.VISIBLE);
        //     ivMobileSelected.setVisibility(View.GONE);
        //     llAccountDetails.setVisibility(View.VISIBLE);
        //     tilBankAccount.setVisibility(View.VISIBLE);
        //     tilBankName.setVisibility(View.VISIBLE);
        //     tilMobileNumber.setVisibility(View.GONE);
        //     
        //     // Clear mobile number input
        //     if (etMobileNumber.getText() != null) {
        //         etMobileNumber.getText().clear();
        //     }
        // });

        cvMobileMoney.setOnClickListener(v -> {
            selectedMethod[0] = "mobile";
            ivMobileSelected.setVisibility(View.VISIBLE);
            llAccountDetails.setVisibility(View.VISIBLE);
            tilMobileNumber.setVisibility(View.VISIBLE);
            
            // Highlight selected card
            cvMobileMoney.setCardBackgroundColor(getResources().getColor(R.color.green_light_very));
            cvMobileMoney.setStrokeColor(getResources().getColor(R.color.green_primary));
            cvMobileMoney.setStrokeWidth(4);
        });
        
        // Default select mobile money if it's the only option
        cvMobileMoney.performClick();
        
        // Bank account text watcher commented out - using mobile money only
        // etBankAccount.addTextChangedListener(new TextWatcher() {
        //     private boolean isFormatting = false;
        //     
        //     @Override
        //     public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        //     
        //     @Override
        //     public void onTextChanged(CharSequence s, int start, int before, int count) {}
        //     
        //     @Override
        //     public void afterTextChanged(Editable s) {
        //         if (isFormatting) return;
        //         isFormatting = true;
        //         
        //         String digits = s.toString().replaceAll("[^0-9]", "");
        //         if (digits.length() > 20) digits = digits.substring(0, 20);
        //         
        //         StringBuilder formatted = new StringBuilder();
        //         for (int i = 0; i < digits.length(); i++) {
        //             if (i > 0 && i % 4 == 0) formatted.append(" ");
        //             formatted.append(digits.charAt(i));
        //         }
        //         
        //         String formattedText = formatted.toString();
        //         if (!s.toString().equals(formattedText)) {
        //             s.replace(0, s.length(), formattedText);
        //         }
        //         isFormatting = false;
        //     }
        // });
        
        // Add text watcher for mobile number formatting (groups of 3)
        etMobileNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                
                String digits = s.toString().replaceAll("\\s", "");
                if (digits.length() > 9) digits = digits.substring(0, 9);
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 3 == 0) formatted.append(" ");
                    formatted.append(digits.charAt(i));
                }
                
                String formattedText = formatted.toString();
                if (!s.toString().equals(formattedText)) {
                    s.replace(0, s.length(), formattedText);
                }
                isFormatting = false;
            }
        });

        // Format amount input
        etWithdrawAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Enable confirm button only if amount is valid
                String amountStr = s.toString().trim();
                if (!TextUtils.isEmpty(amountStr)) {
                    try {
                        double amount = Double.parseDouble(amountStr);
                        btnConfirmWithdraw.setEnabled(amount > 0 && amount <= currentWallet.getBalance());
                    } catch (NumberFormatException e) {
                        btnConfirmWithdraw.setEnabled(false);
                    }
                } else {
                    btnConfirmWithdraw.setEnabled(false);
                }
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

            if (amount <= 0) {
                Toast.makeText(this, R.string.amount_greater_than_zero, Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount > currentWallet.getBalance()) {
                Toast.makeText(this, R.string.insufficient_balance, Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(selectedMethod[0])) {
                Toast.makeText(this, R.string.select_withdrawal_method, Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate account details based on method
            // Bank validation commented out - using mobile money only
            // if (selectedMethod[0].equals("bank")) {
            //     String bankAccount = etBankAccount.getText() != null ? etBankAccount.getText().toString().trim() : "";
            //     String bankName = etBankName.getText() != null ? etBankName.getText().toString().trim() : "";
            //     
            //     if (TextUtils.isEmpty(bankAccount) || bankAccount.replaceAll("\\s", "").length() < 8) {
            //         Toast.makeText(this, "Please enter a valid account number", Toast.LENGTH_SHORT).show();
            //         return;
            //     }
            //     
            //     if (TextUtils.isEmpty(bankName)) {
            //         Toast.makeText(this, "Please enter bank name", Toast.LENGTH_SHORT).show();
            //         return;
            //     }
            // } else 
            if (selectedMethod[0].equals("mobile")) {
                String mobileNumber = etMobileNumber.getText() != null ? 
                    etMobileNumber.getText().toString().replaceAll("\\s", "").trim() : "";
                
                if (TextUtils.isEmpty(mobileNumber) || mobileNumber.length() < 9) {
                    Toast.makeText(this, R.string.enter_valid_mobile, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Process withdrawal
            processWithdrawal(amount, selectedMethod[0], btnConfirmWithdraw);
        });

        withdrawDialog.setContentView(view);
        if (withdrawDialog.getWindow() != null) {
            withdrawDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        withdrawDialog.show();
    }

    private void processWithdrawal(double amount, String method, MaterialButton confirmBtn) {
        String doctorId = preferenceManager.getUserId();
        String doctorName = preferenceManager.getUserName();
        confirmBtn.setEnabled(false);
        confirmBtn.setText(R.string.processing);
        
        // Get account details
        View view = withdrawDialog.getWindow().getDecorView();
        TextInputEditText etMobileNumber = view.findViewById(R.id.etMobileNumber);
        String accountNumber = etMobileNumber.getText() != null ? 
            etMobileNumber.getText().toString().replaceAll("\\s", "").trim() : "";
        
        // Request withdrawal instead of instant deduction
        viewModel.requestWithdrawal(doctorId, doctorName, amount, method, accountNumber, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload wallet data when activity resumes
        loadWalletData();
    }
}

