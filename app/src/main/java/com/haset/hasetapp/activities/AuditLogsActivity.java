package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.AuditLogAdapter;
import com.haset.hasetapp.utils.ShimmerHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AuditLogsViewModel;
import com.haset.hasetapp.database.entities.AuditLogEntity;

import java.util.List;

public class AuditLogsActivity extends AppCompatActivity {
    private RecyclerView rvAuditLogs;
    private AuditLogAdapter auditLogAdapter;
    private LinearLayout shimmerContainer;
    private ImageView btnBack;
    private ImageView btnRefresh;
    private AuditLogsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_logs);

        initViews();
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(this).get(AuditLogsViewModel.class);
        setupObservers();
    }
    
    private void setupObservers() {
        showShimmerLoading();
        viewModel.getAuditLogs().observe(this, entities -> {
            hideShimmerLoading();
            if (entities != null && !entities.isEmpty()) {
                // Sort by timestamp descending
                java.util.Collections.sort(entities, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                
                auditLogAdapter.setAuditLogs(entities);
            } else {
                Snackbar.make(rvAuditLogs, R.string.no_audit_logs_found, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Logs are observed reactively
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        rvAuditLogs = findViewById(R.id.rvAuditLogs);
        shimmerContainer = findViewById(R.id.shimmerContainer);

        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> viewModel.refreshLogs());
    }

    private void setupRecyclerView() {
        auditLogAdapter = new AuditLogAdapter(this::showLogDetailBottomSheet);
        rvAuditLogs.setLayoutManager(new LinearLayoutManager(this));
        rvAuditLogs.setAdapter(auditLogAdapter);
    }

    private void showLogDetailBottomSheet(AuditLogEntity entity) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_audit_log_detail, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView tvLogId = bottomSheetView.findViewById(R.id.tvDetailLogId);
        TextView tvUser = bottomSheetView.findViewById(R.id.tvDetailUser);
        TextView tvAction = bottomSheetView.findViewById(R.id.tvDetailAction);
        TextView tvDescription = bottomSheetView.findViewById(R.id.tvDetailDescription);
        TextView tvTimestamp = bottomSheetView.findViewById(R.id.tvDetailTimestamp);
        TextView tvEntity = bottomSheetView.findViewById(R.id.tvDetailEntity);
        TextView tvDevice = bottomSheetView.findViewById(R.id.tvDetailDevice);
        LinearLayout layoutEntityInfo = bottomSheetView.findViewById(R.id.layoutEntityInfo);

        tvLogId.setText(entity.getLogId());
        tvUser.setText(entity.getUserName() + " (" + entity.getUserRole() + ")");
        tvAction.setText(entity.getAction());
        tvDescription.setText(entity.getDescription());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault());
        tvTimestamp.setText(sdf.format(new Date(entity.getTimestamp())));
        
        if (entity.getEntityType() != null && !entity.getEntityType().isEmpty()) {
            layoutEntityInfo.setVisibility(View.VISIBLE);
            tvEntity.setText(entity.getEntityType() + ": " + (entity.getEntityId() != null ? entity.getEntityId() : getString(R.string.na)));
        } else {
            layoutEntityInfo.setVisibility(View.GONE);
        }

        tvDevice.setText(entity.getDeviceInfo() != null ? entity.getDeviceInfo() : getString(R.string.na));

        bottomSheetDialog.show();
    }

    private void loadAuditLogs() {
        // Handled by setupObservers
    }

    private void showShimmerLoading() {
        shimmerContainer.setVisibility(View.VISIBLE);
        rvAuditLogs.setVisibility(View.GONE);
        ShimmerHelper.showListShimmer(this, shimmerContainer, 5, R.layout.shimmer_audit_log_list);
    }

    private void hideShimmerLoading() {
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
        rvAuditLogs.setVisibility(View.VISIBLE);
    }
}

