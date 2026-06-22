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
import com.haset.hasetapp.models.Appointment;

import java.util.ArrayList;
import java.util.List;

public class AppointmentNotificationAdapter extends RecyclerView.Adapter<AppointmentNotificationAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private Context context;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Appointment appointment);
    }

    public AppointmentNotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.appointments = new ArrayList<>();
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        
        holder.tvNotificationTitle.setText("Appointment " + appointment.getStatus());
        holder.tvNotificationMessage.setText("You have an appointment with " + appointment.getDoctorName() + 
                " on " + appointment.getDate() + " at " + appointment.getTime());
        holder.tvNotificationTime.setText(getRelativeTime(appointment.getCreatedAt()));
        
        // Set status badge
        holder.tvNotificationStatus.setVisibility(View.VISIBLE);
        if ("pending".equals(appointment.getStatus())) {
            holder.tvNotificationStatus.setText(holder.itemView.getContext().getString(R.string.status_pending));
            holder.tvNotificationStatus.setTextColor(context.getColor(R.color.warning_color));
            holder.tvNotificationStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if ("approved".equals(appointment.getStatus())) {
            holder.tvNotificationStatus.setText(holder.itemView.getContext().getString(R.string.status_approved));
            holder.tvNotificationStatus.setTextColor(context.getColor(R.color.success_color));
            holder.tvNotificationStatus.setBackgroundResource(R.drawable.bg_status_approved);
        } else if ("cancelled".equals(appointment.getStatus())) {
            holder.tvNotificationStatus.setText(holder.itemView.getContext().getString(R.string.status_cancelled));
            holder.tvNotificationStatus.setTextColor(context.getColor(R.color.colorError));
            holder.tvNotificationStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
        } else {
            holder.tvNotificationStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + " min ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";
        if (diff < 604800000) return (diff / 86400000) + "d ago";
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
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
