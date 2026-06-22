package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentAppointmentAdapter extends RecyclerView.Adapter<RecentAppointmentAdapter.RecentAppointmentViewHolder> {
    private List<Appointment> appointments;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public RecentAppointmentAdapter(OnAppointmentClickListener listener) {
        this.appointments = new ArrayList<>();
        this.listener = listener;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments != null ? appointments : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentAppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_recent, parent, false);
        return new RecentAppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentAppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    class RecentAppointmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvDate, tvTime, tvStatus, tvReason;

        public RecentAppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReason = itemView.findViewById(R.id.tvReason);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAppointmentClick(appointments.get(position));
                }
            });
        }

        public void bind(Appointment appointment) {
            // Set patient name (for doctor view) or doctor name (for patient view)
            tvName.setText(appointment.getPatientName() != null ? 
                appointment.getPatientName() : "Patient");

            // Set status
            String status = appointment.getStatus();
            tvStatus.setText(status != null ? status : Constants.STATUS_PENDING);
            
            // Set status background color
            int statusColor;
            if (Constants.STATUS_PENDING.equals(status)) {
                statusColor = itemView.getContext().getColor(R.color.status_pending);
            } else if (Constants.STATUS_APPROVED.equals(status)) {
                statusColor = itemView.getContext().getColor(R.color.status_approved);
            } else if (Constants.STATUS_COMPLETED.equals(status)) {
                statusColor = itemView.getContext().getColor(R.color.status_completed);
            } else {
                statusColor = itemView.getContext().getColor(R.color.status_declined);
            }
            tvStatus.setBackgroundColor(statusColor);

            // Set date
            if (appointment.getDate() != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    Date date = inputFormat.parse(appointment.getDate());
                    if (date != null) {
                        tvDate.setText(outputFormat.format(date));
                    } else {
                        tvDate.setText(appointment.getDate());
                    }
                } catch (Exception e) {
                    tvDate.setText(appointment.getDate());
                }
            } else {
                tvDate.setText("Date not set");
            }

            // Set time
            tvTime.setText(appointment.getTime() != null ? appointment.getTime() : "Time not set");

            // Set reason if available
            if (appointment.getReason() != null && !appointment.getReason().isEmpty()) {
                tvReason.setText(appointment.getReason());
                tvReason.setVisibility(View.VISIBLE);
            } else {
                tvReason.setVisibility(View.GONE);
            }
        }
    }
}


