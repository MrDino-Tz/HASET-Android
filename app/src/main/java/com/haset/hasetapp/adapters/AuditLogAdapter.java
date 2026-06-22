package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.AuditLogEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.AuditLogViewHolder> {
    private List<AuditLogEntity> auditLogs;
    private OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(AuditLogEntity log);
    }

    public AuditLogAdapter(OnLogClickListener listener) {
        this.auditLogs = new ArrayList<>();
        this.listener = listener;
    }

    public void setAuditLogs(List<AuditLogEntity> auditLogs) {
        this.auditLogs = auditLogs != null ? auditLogs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<AuditLogEntity> getAuditLogs() {
        return auditLogs;
    }

    @NonNull
    @Override
    public AuditLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audit_log_list, parent, false);
        return new AuditLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditLogViewHolder holder, int position) {
        AuditLogEntity log = auditLogs.get(position);
        holder.bind(log);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogClick(log);
            }
        });
    }

    @Override
    public int getItemCount() {
        return auditLogs.size();
    }

    static class AuditLogViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIcon;
        private TextView tvAction, tvUser, tvDescription, tvTimestamp, tvEntityType;

        public AuditLogViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvAction = itemView.findViewById(R.id.tvAction);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvEntityType = itemView.findViewById(R.id.tvEntityType);
        }

        public void bind(AuditLogEntity log) {
            tvAction.setText(log.getAction());
            tvUser.setText(log.getUserName() + " (" + log.getUserRole() + ")");
            tvDescription.setText(log.getDescription());
            
            // Format timestamp
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvTimestamp.setText(timeFormat.format(new Date(log.getTimestamp())));
            
            // Set icon based on action
            int iconRes = getIconForAction(log.getAction());
            ivIcon.setImageResource(iconRes);
            
            // Set entity type if available
            if (log.getEntityType() != null && !log.getEntityType().isEmpty()) {
                tvEntityType.setText(log.getEntityType());
                tvEntityType.setVisibility(View.VISIBLE);
            } else {
                tvEntityType.setVisibility(View.GONE);
            }
        }

        private int getIconForAction(String action) {
            if (action == null) return R.drawable.ic_info;
            switch (action.toUpperCase()) {
                case "LOGIN":
                    return R.drawable.ic_login;
                case "LOGOUT":
                    return R.drawable.ic_logout;
                case "CREATE":
                case "CREATED":
                    return R.drawable.ic_add;
                case "UPDATE":
                case "UPDATED":
                    return R.drawable.ic_edit;
                case "DELETE":
                case "DELETED":
                    return R.drawable.ic_delete;
                case "APPROVE":
                case "APPROVED":
                    return R.drawable.ic_check_circle;
                case "DECLINE":
                case "DECLINED":
                    return R.drawable.ic_cancel;
                case "BOOK":
                case "BOOKED":
                    return R.drawable.ic_calendar;
                case "CHAT":
                    return R.drawable.ic_chat;
                case "APPOINTMENT":
                    return R.drawable.ic_calendar;
                default:
                    return R.drawable.ic_info;
            }
        }
    }
}
