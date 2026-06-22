package com.haset.hasetapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.WithdrawalRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawalHistoryAdapter extends RecyclerView.Adapter<WithdrawalHistoryAdapter.ViewHolder> {

    private List<WithdrawalRequest> requests = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public void setRequests(List<WithdrawalRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_withdrawal_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WithdrawalRequest request = requests.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAmount, tvStatus, tvMethod, tvDate, tvReason;
        private final LinearLayout llRejectionArea;

        public ViewHolder(@NonNull View view) {
            super(view);
            tvAmount = view.findViewById(R.id.tvWithdrawAmount);
            tvStatus = view.findViewById(R.id.tvStatusBadge);
            tvMethod = view.findViewById(R.id.tvWithdrawMethod);
            tvDate = view.findViewById(R.id.tvWithdrawDate);
            tvReason = view.findViewById(R.id.tvRejectionReason);
            llRejectionArea = view.findViewById(R.id.llRejectionArea);
        }

        public void bind(WithdrawalRequest request) {
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f TZS", request.getAmount()));
            tvDate.setText("Requested on: " + dateFormat.format(new Date(request.getRequestedAt())));
            tvMethod.setText(request.getMethod().equalsIgnoreCase("mobile") ? "Mobile Money" : "Bank Transfer");
            
            String status = request.getStatus();
            tvStatus.setText(status.toUpperCase());
            
            Context context = itemView.getContext();
            
            switch (status.toLowerCase()) {
                case WithdrawalRequest.STATUS_PENDING:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orange_primary));
                    llRejectionArea.setVisibility(View.GONE);
                    break;
                case WithdrawalRequest.STATUS_APPROVED:
                case WithdrawalRequest.STATUS_COMPLETED:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
                    llRejectionArea.setVisibility(View.GONE);
                    break;
                case WithdrawalRequest.STATUS_REJECTED:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red_light));
                    
                    if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
                        llRejectionArea.setVisibility(View.VISIBLE);
                        tvReason.setText(request.getRejectionReason());
                    } else {
                        llRejectionArea.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    }
}
