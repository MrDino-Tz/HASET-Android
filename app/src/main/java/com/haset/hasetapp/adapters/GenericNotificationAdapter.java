package com.haset.hasetapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.NotificationEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GenericNotificationAdapter extends RecyclerView.Adapter<GenericNotificationAdapter.ViewHolder> {

    private List<NotificationEntity> notifications = new ArrayList<>();
    private Context context;

    public void setNotifications(List<NotificationEntity> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationEntity notification = notifications.get(position);
        
        holder.tvNotificationTitle.setText(notification.getTitle());
        holder.tvNotificationMessage.setText(notification.getMessage());
        holder.tvNotificationTime.setText(getRelativeTime(notification.getTimestamp()));
        
        holder.tvNotificationStatus.setVisibility(View.GONE);
        
        if ("withdrawal".equals(notification.getType())) {
            holder.ivNotificationIcon.setImageResource(R.drawable.ic_wallet);
            holder.ivNotificationIcon.setBackgroundResource(R.drawable.bg_icon_circle);
        } else if ("payment".equals(notification.getType())) {
            holder.ivNotificationIcon.setImageResource(R.drawable.ic_payment);
            holder.ivNotificationIcon.setBackgroundResource(R.drawable.bg_icon_circle);
        } else {
            holder.ivNotificationIcon.setImageResource(R.drawable.ic_notification);
            holder.ivNotificationIcon.setBackgroundResource(R.drawable.bg_icon_circle);
        }
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + " min ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";
        if (diff < 604800000) return (diff / 86400000) + "d ago";
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivNotificationIcon;
        TextView tvNotificationTitle;
        TextView tvNotificationMessage;
        TextView tvNotificationTime;
        TextView tvNotificationStatus;

        ViewHolder(View itemView) {
            super(itemView);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            tvNotificationStatus = itemView.findViewById(R.id.tvNotificationStatus);
        }
    }
}
