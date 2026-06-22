package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.database.entities.WithdrawalRequest;
import com.google.android.material.textfield.TextInputLayout;
import com.haset.hasetapp.models.PayoutRequest;
import com.haset.hasetapp.models.PaymentResponse;
import com.haset.hasetapp.repositories.PaymentRepository;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminWalletManagementActivity extends BaseActivity {

    private RecyclerView rvDoctorWallets, rvWithdrawalRequests;
    private LinearLayout llNoWallets, llNoRequests;
    private View shimmerWallets, shimmerRequests;
    private PreferenceManager preferenceManager;
    private List<DoctorWalletEntity> walletList = new ArrayList<>();
    private List<WithdrawalRequest> pendingRequests = new ArrayList<>();
    private DoctorWalletAdapter walletAdapter;
    private WithdrawalRequestAdapter requestAdapter;
    private PaymentRepository paymentRepository;
    private TextView tvTotalPending, tvTotalBalance, tvWalletCount, tvPendingCount;
    // private TextView tvCompanyBalance;
    private ImageView ivToggleBalance, ivTogglePending;
    // private ImageView ivToggleCompanyBalance;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private double totalWalletBalance = 0;
    private double totalPendingAmount = 0;
    // private double currentCompanyBalance = 0;
    private boolean isBalanceVisible = false;
    private boolean isPendingVisible = false;
    // private boolean isCompanyBalanceVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_wallet_management);

        preferenceManager = new PreferenceManager(this);
        paymentRepository = new PaymentRepository();

        initViews();
        loadData();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvDoctorWallets = findViewById(R.id.rvDoctorWallets);
        rvWithdrawalRequests = findViewById(R.id.rvWithdrawalRequests);
        llNoWallets = findViewById(R.id.llNoWallets);
        llNoRequests = findViewById(R.id.llNoRequests);
        shimmerWallets = findViewById(R.id.shimmerWallets);
        shimmerRequests = findViewById(R.id.shimmerRequests);
        tvTotalPending = findViewById(R.id.tvTotalPending);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvWalletCount = findViewById(R.id.tvWalletCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        // tvCompanyBalance = findViewById(R.id.tvCompanyBalance);
        ivToggleBalance = findViewById(R.id.ivToggleBalance);
        ivTogglePending = findViewById(R.id.ivTogglePending);
        // ivToggleCompanyBalance = findViewById(R.id.ivToggleCompanyBalance);

        ivToggleBalance.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            animateDisplay(tvTotalBalance, ivToggleBalance, isBalanceVisible, totalWalletBalance);
        });

        ivTogglePending.setOnClickListener(v -> {
            isPendingVisible = !isPendingVisible;
            animateDisplay(tvTotalPending, ivTogglePending, isPendingVisible, totalPendingAmount);
        });

        /*
        if (ivToggleCompanyBalance != null) {
            ivToggleCompanyBalance.setOnClickListener(v -> {
                isCompanyBalanceVisible = !isCompanyBalanceVisible;
                animateDisplay(tvCompanyBalance, ivToggleCompanyBalance, isCompanyBalanceVisible, currentCompanyBalance);
            });
        }
        */

        rvDoctorWallets.setLayoutManager(new LinearLayoutManager(this));
        rvWithdrawalRequests.setLayoutManager(new LinearLayoutManager(this));

        walletAdapter = new DoctorWalletAdapter(walletList);
        requestAdapter = new WithdrawalRequestAdapter(pendingRequests, this);

        rvDoctorWallets.setAdapter(walletAdapter);
        rvWithdrawalRequests.setAdapter(requestAdapter);

        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.green_primary));
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        if (!swipeRefresh.isRefreshing()) {
            if (shimmerWallets != null) shimmerWallets.setVisibility(View.VISIBLE);
            if (shimmerRequests != null) shimmerRequests.setVisibility(View.VISIBLE);
        }
        loadDoctorWallets();
        loadPendingWithdrawals();
        // loadCompanyBalance();
    }

    /*
    private void loadCompanyBalance() {
        paymentRepository.getGatewayBalance(new FirebaseHelper.OnCompleteListener<PaymentResponse>() {
            @Override
            public void onSuccess(PaymentResponse response) {
                if (response != null && response.getData() != null) {
                    // Zeno returns balance in its 'data' field
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getData();
                    if (data.containsKey("balance")) {
                        Object balanceObj = data.get("balance");
                        if (balanceObj instanceof Number) {
                            currentCompanyBalance = ((Number) balanceObj).doubleValue();
                        }
                    }
                }
                updateSummaryCards();
            }

            @Override
            public void onError(String error) {
                Log.e("AdminWallet", "Failed to load company balance: " + error);
            }
        });
    }
    */

    private void loadDoctorWallets() {
        FirebaseHelper.getDoctorWalletsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                walletList.clear();
                totalWalletBalance = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DoctorWalletEntity wallet = snapshot.getValue(DoctorWalletEntity.class);
                    if (wallet != null) {
                        walletList.add(wallet);
                        totalWalletBalance += wallet.getBalance();
                    }
                }

                if (tvWalletCount != null) {
                    tvWalletCount.setText(String.format(Locale.getDefault(), "%d Wallets", walletList.size()));
                }
                updateSummaryCards();

                // Fetch doctor names for each wallet
                for (int i = 0; i < walletList.size(); i++) {
                    final int index = i;
                    String doctorId = walletList.get(i).getDoctorId();
                    final int finalIndex = index;
                    FirebaseHelper.getUsersRef().child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("fullName").getValue(String.class);
                                if (name == null) name = snapshot.child("userName").getValue(String.class);
                                String regNo = snapshot.child("regNo").getValue(String.class);
                                
                                if (finalIndex < walletList.size()) {
                                    if (name != null) walletList.get(finalIndex).setDoctorName(name);
                                    if (regNo != null) walletList.get(finalIndex).setRegNo(regNo);
                                    walletAdapter.notifyItemChanged(finalIndex);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }

                if (shimmerWallets != null) {
                    shimmerWallets.setVisibility(View.GONE);
                }

                if (walletList.isEmpty()) {
                    llNoWallets.setVisibility(View.VISIBLE);
                    rvDoctorWallets.setVisibility(View.GONE);
                } else {
                    llNoWallets.setVisibility(View.GONE);
                    rvDoctorWallets.setVisibility(View.VISIBLE);
                    walletAdapter.notifyDataSetChanged();
                }

                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (shimmerWallets != null) {
                    shimmerWallets.setVisibility(View.GONE);
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                llNoWallets.setVisibility(View.VISIBLE);
                Toast.makeText(AdminWalletManagementActivity.this, R.string.error_loading_wallets, Toast.LENGTH_SHORT).show();
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
                    toggleIcon.setImageResource(R.drawable.ic_eye); // Or an "eye-slash" if you have one
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

    private void updateSummaryCards() {
        if (tvTotalBalance != null) {
            tvTotalBalance.setText(isBalanceVisible ? 
                String.format(Locale.getDefault(), getString(R.string.currency_format), totalWalletBalance) : "•••••••• TZS");
            ivToggleBalance.setAlpha(isBalanceVisible ? 1.0f : 0.4f);
        }
        
        if (tvTotalPending != null) {
            tvTotalPending.setText(isPendingVisible ? 
                String.format(Locale.getDefault(), getString(R.string.currency_format), totalPendingAmount) : "•••••••• TZS");
            ivTogglePending.setAlpha(isPendingVisible ? 1.0f : 0.4f);
        }

        /*
        if (tvCompanyBalance != null) {
            tvCompanyBalance.setText(isCompanyBalanceVisible ? 
                String.format(Locale.getDefault(), getString(R.string.currency_format), currentCompanyBalance) : "•••••••• TZS");
            if (ivToggleCompanyBalance != null) ivToggleCompanyBalance.setAlpha(isCompanyBalanceVisible ? 1.0f : 0.4f);
        }
        */
    }

    private void loadPendingWithdrawals() {
        FirebaseHelper.getPendingWithdrawalRequests(new FirebaseHelper.OnCompleteListener<List<WithdrawalRequest>>() {
            @Override
            public void onSuccess(List<WithdrawalRequest> requests) {
                if (shimmerRequests != null) {
                    shimmerRequests.setVisibility(View.GONE);
                }

                pendingRequests.clear();
                pendingRequests.addAll(requests);

                totalPendingAmount = 0;
                for (WithdrawalRequest request : requests) {
                    if (request != null) {
                        totalPendingAmount += request.getAmount();
                    }
                }

                if (tvPendingCount != null) {
                    tvPendingCount.setText(String.format(Locale.getDefault(), "%d Requests", pendingRequests.size()));
                }
                updateSummaryCards();

                if (pendingRequests.isEmpty()) {
                    llNoRequests.setVisibility(View.VISIBLE);
                    rvWithdrawalRequests.setVisibility(View.GONE);
                } else {
                    llNoRequests.setVisibility(View.GONE);
                    rvWithdrawalRequests.setVisibility(View.VISIBLE);
                    requestAdapter.notifyDataSetChanged();
                }

                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                if (shimmerRequests != null) {
                    shimmerRequests.setVisibility(View.GONE);
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                llNoRequests.setVisibility(View.VISIBLE);
                Toast.makeText(AdminWalletManagementActivity.this, R.string.error_loading_requests, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void approveWithdrawal(WithdrawalRequest request) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payout_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        TextView tvTitle = dialogView.findViewById(R.id.tvPayoutTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvPayoutMessage);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etAdminPassword);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelPayout);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmPayout);

        tvTitle.setText(R.string.approve);
        String amountStr = String.format(Locale.getDefault(), getString(R.string.currency_format), request.getAmount());
        tvMessage.setText(getString(R.string.approve_withdrawal_confirm, amountStr, request.getDoctorName()));
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (password.isEmpty()) {
                etPassword.setError(getString(R.string.password_required));
                return;
            }
            dialog.dismiss();
            processWithdrawal(request, "approved", null, password);
        });

        dialog.show();
    }

    public void rejectWithdrawal(WithdrawalRequest request) {
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reject_withdrawal, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        TextInputEditText etReason = view.findViewById(R.id.etRejectionReason);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnReject = view.findViewById(R.id.btnReject);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnReject.setOnClickListener(v -> {
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            if (reason.isEmpty()) {
                Toast.makeText(this, R.string.rejection_reason_required, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            processWithdrawal(request, "rejected", reason, null);
        });

        dialog.show();
    }

    private void processWithdrawal(WithdrawalRequest request, String status, String reason, String password) {
        String adminId = preferenceManager.getUserId();

        if ("approved".equals(status)) {
            // 1. First, call the Zeno Payout API
            PayoutRequest payoutRequest = new PayoutRequest(
                request.getRequestId(),
                request.getDoctorId(),
                request.getAmount(),
                request.getAccountNumber(),
                request.getMethod(),
                adminId,
                password
            );

            Toast.makeText(this, "Initiating payout via Zeno...", Toast.LENGTH_SHORT).show();

            paymentRepository.disburseFunds(payoutRequest, new FirebaseHelper.OnCompleteListener<PaymentResponse>() {
                @Override
                public void onSuccess(PaymentResponse response) {
                    // 2. If API payout succeeds, update accounting in Firebase
                    FirebaseHelper.deductFromDoctorWallet(request.getDoctorId(), request.getAmount(), 
                        new FirebaseHelper.OnCompleteListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            if (success) {
                                FirebaseHelper.approveWithdrawal(request.getRequestId(), request.getDoctorId(), request.getAmount(), adminId, 
                                    new FirebaseHelper.OnCompleteListener<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        Toast.makeText(AdminWalletManagementActivity.this, 
                                            "Payout Successful: " + response.getMessage(), Toast.LENGTH_LONG).show();
                                        loadData();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(AdminWalletManagementActivity.this, 
                                            "Funds sent but status update failed. Please check logs.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                Toast.makeText(AdminWalletManagementActivity.this, 
                                    "Funds sent but failed to deduct from wallet records.", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(AdminWalletManagementActivity.this, 
                                "Payout Success but Database error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AdminWalletManagementActivity.this, 
                        "Zeno Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            FirebaseHelper.rejectWithdrawal(request.getRequestId(), request.getDoctorId(), request.getAmount(), adminId, reason,
                new FirebaseHelper.OnCompleteListener<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    Toast.makeText(AdminWalletManagementActivity.this, 
                        R.string.withdrawal_rejected, Toast.LENGTH_SHORT).show();
                    loadData();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AdminWalletManagementActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private class DoctorWalletAdapter extends RecyclerView.Adapter<DoctorWalletAdapter.ViewHolder> {
        private List<DoctorWalletEntity> wallets;

        DoctorWalletAdapter(List<DoctorWalletEntity> wallets) {
            this.wallets = wallets;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DoctorWalletEntity wallet = wallets.get(position);
            String displayName = wallet.getDoctorName() != null ? wallet.getDoctorName() : getString(R.string.unknown);
            String displayId = wallet.getRegNo() != null ? wallet.getRegNo() : wallet.getDoctorId();
            
            holder.tvDoctorName.setText(displayName);
            holder.tvDoctorId.setText(displayId);
            holder.tvBalance.setText(String.format(Locale.getDefault(), getString(R.string.currency_format), wallet.getBalance()));
            holder.tvTotalEarnings.setText(String.format(Locale.getDefault(), "Total: " + getString(R.string.currency_format), wallet.getTotalEarnings()));
        }

        @Override
        public int getItemCount() {
            return wallets.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDoctorName, tvDoctorId, tvBalance, tvTotalEarnings;

            ViewHolder(View itemView) {
                super(itemView);
                tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
                tvDoctorId = itemView.findViewById(R.id.tvDoctorId);
                tvBalance = itemView.findViewById(R.id.tvBalance);
                tvTotalEarnings = itemView.findViewById(R.id.tvTotalEarnings);
            }
        }
    }

    private class WithdrawalRequestAdapter extends RecyclerView.Adapter<WithdrawalRequestAdapter.ViewHolder> {
        private List<WithdrawalRequest> requests;
        private AdminWalletManagementActivity activity;

        WithdrawalRequestAdapter(List<WithdrawalRequest> requests, AdminWalletManagementActivity activity) {
            this.requests = requests;
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal_request_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WithdrawalRequest request = requests.get(position);
            holder.tvDoctorName.setText(request.getDoctorName());
            holder.tvAmount.setText(String.format(Locale.getDefault(), getString(R.string.currency_format), request.getAmount()));
            holder.tvMethod.setText(getString(R.string.select_method) + ": " + (request.getMethod() != null ? request.getMethod() : getString(R.string.na)));
            holder.tvAccount.setText(getString(R.string.email) + ": " + (request.getAccountNumber() != null ? request.getAccountNumber() : getString(R.string.na)));
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText("Requested: " + sdf.format(new java.util.Date(request.getRequestedAt())));

            holder.btnApprove.setOnClickListener(v -> activity.approveWithdrawal(request));
            holder.btnReject.setOnClickListener(v -> activity.rejectWithdrawal(request));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDoctorName, tvAmount, tvMethod, tvAccount, tvDate;
            MaterialButton btnApprove, btnReject;

            ViewHolder(View itemView) {
                super(itemView);
                tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvMethod = itemView.findViewById(R.id.tvMethod);
                tvAccount = itemView.findViewById(R.id.tvAccount);
                tvDate = itemView.findViewById(R.id.tvDate);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}
