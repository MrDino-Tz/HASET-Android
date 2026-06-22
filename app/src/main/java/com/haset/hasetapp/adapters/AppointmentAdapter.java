package com.haset.hasetapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {
    private List<Appointment> appointments;
    private OnAppointmentActionListener listener;
    private boolean showActions;
    private String userRole; // Add user role to determine what to display

    public interface OnAppointmentActionListener {
        void onApprove(Appointment appointment);
        void onDecline(Appointment appointment);
        void onAppointmentClick(Appointment appointment);
        void onCancel(Appointment appointment);
        void onReschedule(Appointment appointment);
        void onRateDoctor(Appointment appointment);
        void onStartSession(Appointment appointment);
    }

    public AppointmentAdapter(OnAppointmentActionListener listener, boolean showActions, String userRole) {
        this.appointments = new ArrayList<>();
        this.listener = listener;
        this.showActions = showActions;
        this.userRole = userRole;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvSpecialty, tvDate, tvTime, tvStatus;
        private LinearLayout layoutActions, layoutPatientActions;
        private MaterialButton btnApprove, btnDecline, btnCancel, btnReschedule, btnRateDoctor, btnStartSession;
        private CircleImageView ivProfile;
        private com.facebook.shimmer.ShimmerFrameLayout shimmerProfile;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            layoutPatientActions = itemView.findViewById(R.id.layoutPatientActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReschedule = itemView.findViewById(R.id.btnReschedule);
            btnStartSession = itemView.findViewById(R.id.btnStartSession);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            shimmerProfile = itemView.findViewById(R.id.shimmerProfile);
        }

        public void bind(Appointment appointment) {
            if (appointment == null) return;
            
            // Display information based on user role
            if (Constants.ROLE_DOCTOR.equals(userRole)) {
                tvName.setText(appointment.getPatientName() != null ? 
                        appointment.getPatientName() : "Unknown Patient");
                tvSpecialty.setText(appointment.getDoctorSpecialty() != null ? 
                        appointment.getDoctorSpecialty() : "General Practice");
                ProfilePhotoHelper.loadProfilePhoto(itemView.getContext(), appointment.getPatientId(), ivProfile, shimmerProfile);
            } else {
                tvName.setText(appointment.getDoctorName() != null ? 
                        "Dr. " + appointment.getDoctorName() : "Unknown Doctor");
                tvSpecialty.setText(appointment.getDoctorSpecialty() != null ? 
                        appointment.getDoctorSpecialty() : "General Practice");
                ProfilePhotoHelper.loadProfilePhoto(itemView.getContext(), appointment.getDoctorId(), ivProfile, shimmerProfile);
            }
            
            tvDate.setText(appointment.getDate() != null ? appointment.getDate() : "");
            tvTime.setText(appointment.getTime() != null ? appointment.getTime() : "");
            tvStatus.setText(appointment.getStatus() != null ? appointment.getStatus().toUpperCase() : "UNKNOWN");

            // Set status color
            int statusColor = getStatusColor(appointment.getStatus());
            tvStatus.setBackgroundColor(statusColor);

            // Show/hide action buttons
            if (showActions && appointment.getStatus().equals(Constants.STATUS_PENDING) && !appointment.isPast()) {
                layoutActions.setVisibility(View.VISIBLE);
                layoutPatientActions.setVisibility(View.GONE);
            } else if (!showActions && (appointment.getStatus().equals(Constants.STATUS_APPROVED) || appointment.getStatus().equals(Constants.STATUS_PENDING))) {
                layoutActions.setVisibility(View.GONE);
                layoutPatientActions.setVisibility(View.VISIBLE);
                
                if (Constants.STATUS_APPROVED.equals(appointment.getStatus()) && 
                   (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(appointment.getAppointmentType()) || 
                    "Video Call".equals(appointment.getAppointmentType()))) {
                    btnStartSession.setVisibility(View.VISIBLE);
                    btnStartSession.setText(Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(appointment.getAppointmentType()) ? "Chat Now" : "Join Call");
                } else {
                    btnStartSession.setVisibility(View.GONE);
                }
            } else if (!showActions && Constants.STATUS_COMPLETED.equals(appointment.getStatus())) {
                layoutActions.setVisibility(View.GONE);
                layoutPatientActions.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.GONE);
                layoutPatientActions.setVisibility(View.GONE);
            }

            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(appointment);
                }
            });

            btnDecline.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDecline(appointment);
                }
            });

            btnCancel.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancel(appointment);
                }
            });

            btnReschedule.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReschedule(appointment);
                }
            });

//            btnRateDoctor.setOnClickListener(v -> {
//                if (listener != null) {
//                    listener.onRateDoctor(appointment);
//                }
//            });

            btnStartSession.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartSession(appointment);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppointmentClick(appointment);
                }
            });
        }

        private int getStatusColor(String status) {
            switch (status) {
                case Constants.STATUS_PENDING:
                    return Color.parseColor("#FFA500");
                case Constants.STATUS_APPROVED:
                    return Color.parseColor("#008800");
                case Constants.STATUS_DECLINED:
                    return Color.parseColor("#DD0000");
                case Constants.STATUS_COMPLETED:
                    return Color.parseColor("#0088FF");
                case Constants.STATUS_CANCELLED:
                    return Color.parseColor("#888888");
                default:
                    return Color.parseColor("#CCCCCC");
            }
        }
    }
}
