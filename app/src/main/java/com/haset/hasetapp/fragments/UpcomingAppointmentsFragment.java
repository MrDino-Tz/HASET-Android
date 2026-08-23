package com.haset.hasetapp.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.BookAppointmentActivity;
import com.haset.hasetapp.activities.ChatActivity;
import com.haset.hasetapp.adapters.AppointmentAdapter;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.haset.hasetapp.utils.FirebaseHelper;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AppointmentsViewModel;

import java.util.ArrayList;
import java.util.List;

public class UpcomingAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {
    private RecyclerView rvAppointments;
    private AppointmentAdapter appointmentAdapter;
    private PreferenceManager preferenceManager;
    private View rootView;
    private LinearLayout shimmerContainer;
    private View emptyStateCard;
    private TextView tvEmptyStateTitle;
    private TextView tvEmptyStateSubtitle;
    private ImageView ivEmptyStateIcon;
    private AppointmentsViewModel viewModel;
    private com.haset.hasetapp.utils.AppointmentReminderHelper reminderHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        rvAppointments = view.findViewById(R.id.rvAppointments);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        tvEmptyStateTitle = emptyStateCard.findViewById(R.id.tvEmptyStateTitle);
        tvEmptyStateSubtitle = emptyStateCard.findViewById(R.id.tvEmptyStateSubtitle);
        ivEmptyStateIcon = emptyStateCard.findViewById(R.id.ivEmptyStateIcon);

        preferenceManager = new PreferenceManager(requireContext());
        reminderHelper = new com.haset.hasetapp.utils.AppointmentReminderHelper(requireContext());
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentsViewModel.class);
        setupObservers();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload appointments when fragment becomes visible
        loadAppointments();
    }

    private void setupRecyclerView() {
        String userRole = preferenceManager.getUserRole();
        boolean showActions = Constants.ROLE_DOCTOR.equalsIgnoreCase(userRole);
        appointmentAdapter = new AppointmentAdapter(this, showActions, userRole);
        rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAppointments.setAdapter(appointmentAdapter);
    }

    private void setupObservers() {
        showShimmerLoading();
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();

        viewModel.setUserInfo(userId, role);
        boolean isDoctor = Constants.ROLE_DOCTOR.equalsIgnoreCase(role);
        androidx.lifecycle.LiveData<List<Appointment>> appointmentsSource = isDoctor
                ? viewModel.getPendingAppointments()
                : viewModel.getUpcomingAppointments();
        appointmentsSource.observe(getViewLifecycleOwner(), appointments -> {
            if (isAdded() && rootView != null) {
                hideShimmerLoading();
                if (appointmentAdapter != null) {
                    appointmentAdapter.setAppointments(appointments);
                }

                if (appointments == null || appointments.isEmpty()) {
                    showEmptyState(getString(R.string.no_upcoming_appointments_title),
                        getString(R.string.no_upcoming_appointments_desc),
                        R.drawable.ic_no_data);
                } else {
                    hideEmptyState();
                }
            }
        });
    }

    public void loadAppointments() {
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void showShimmerLoading() {
        shimmerContainer.setVisibility(View.VISIBLE);
        rvAppointments.setVisibility(View.GONE);
        emptyStateCard.setVisibility(View.GONE); // Hide empty state when loading
        ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 4, R.layout.shimmer_layout_appointment_card);
    }
    private void hideShimmerLoading() {
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
        // rvAppointments.setVisibility(View.VISIBLE); // Visibility handled by empty state logic
    }

    private void showEmptyState(String title, String subtitle, int iconRes) {
        emptyStateCard.setVisibility(View.VISIBLE);
        rvAppointments.setVisibility(View.GONE);

        tvEmptyStateTitle.setText(title);
        tvEmptyStateSubtitle.setText(subtitle);
        ivEmptyStateIcon.setImageResource(iconRes);
    }

    private void hideEmptyState() {
        emptyStateCard.setVisibility(View.GONE);
        rvAppointments.setVisibility(View.VISIBLE);
    }

    public List<Appointment> getCurrentAppointments() {
        return appointmentAdapter != null ? appointmentAdapter.getAppointments() : new ArrayList<>();
    }

    @Override
    public void onApprove(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            showSnackbar(getString(R.string.invalid_appointment));
            return;
        }

        viewModel.updateStatus(appointment, Constants.STATUS_APPROVED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    showApprovalDialog(appointment);
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(getView(), error);
                }
            }
        });
    }

    private void showApprovalDialog(Appointment appointment) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chat_start);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvPatientInfo = dialog.findViewById(R.id.tvPatientInfo);
        TextView tvAppointmentDetails = dialog.findViewById(R.id.tvAppointmentDetails);
        TextView tvCountdown = dialog.findViewById(R.id.tvCountdown);
        MaterialButton btnStartChat = dialog.findViewById(R.id.btnStartChat);
        TextView tvSessionExpired = dialog.findViewById(R.id.tvSessionExpired);

        tvPatientInfo.setText(getString(R.string.appointment_with_patient, appointment.getPatientName()));
        tvAppointmentDetails.setText(String.format("%s at %s",
                appointment.getDate() != null ? appointment.getDate() : "",
                appointment.getTime() != null ? appointment.getTime() : ""));

        long approvedAt = System.currentTimeMillis();

        btnStartChat.setOnClickListener(v -> {
            dialog.dismiss();
            startChatWithPatient(appointment, approvedAt);
        });

        // 60-second countdown
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] secondsLeft = {60};
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (secondsLeft[0] <= 0) {
                    tvCountdown.setText("00:00");
                    btnStartChat.setVisibility(View.GONE);
                    tvSessionExpired.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> { if (dialog.isShowing()) dialog.dismiss(); }, 3000);
                    return;
                }
                int min = secondsLeft[0] / 60;
                int sec = secondsLeft[0] % 60;
                tvCountdown.setText(String.format("%02d:%02d", min, sec));
                secondsLeft[0]--;
                handler.postDelayed(this, 1000);
            }
        });

        dialog.show();
    }

    private void startChatWithPatient(Appointment appointment, long approvedAt) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(Constants.EXTRA_CHAT_USER_ID, appointment.getPatientId());
        intent.putExtra(Constants.EXTRA_CHAT_USER_NAME, appointment.getPatientName());
        intent.putExtra(Constants.EXTRA_APPOINTMENT_ID, appointment.getAppointmentId());
        intent.putExtra(Constants.EXTRA_IS_FROM_APPOINTMENT, true);
        intent.putExtra(Constants.EXTRA_APPOINTMENT_APPROVED_AT, approvedAt);
        startActivity(intent);
    }

    @Override
    public void onDecline(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            showSnackbar(getString(R.string.invalid_appointment));
            return;
        }

        viewModel.updateStatus(appointment, Constants.STATUS_DECLINED, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    showSnackbar(getString(R.string.appointment_declined_success));
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(getView(), error);
                }
            }
        });
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Handle appointment click - could show details dialog
    }

    @Override
    public void onCancel(Appointment appointment) {
        if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            if (appointment == null || appointment.getAppointmentId() == null) {
                showSnackbar(getString(R.string.invalid_appointment));
                return;
            }

            viewModel.updateStatus(appointment, Constants.STATUS_CANCELLED, new FirebaseHelper.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (isAdded()) {
                        showSnackbar(getString(R.string.appointment_cancelled_success));
                    }
                }

                @Override
            public void onError(String error) {
                if (isAdded()) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(getView(), error);
                }
            }
            });
        }
    }

    @Override
    public void onReschedule(Appointment appointment) {
        showRescheduleDialog(appointment);
    }

    private void showRescheduleDialog(Appointment appointment) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reschedule, null);
        
        TextView tvAppointmentInfo = dialogView.findViewById(R.id.tvAppointmentInfo);
        MaterialButton btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        String currentUserId = preferenceManager.getUserId();
        String otherName = currentUserId.equals(appointment.getPatientId()) ? 
                "Dr. " + appointment.getDoctorName() : appointment.getPatientName();
        tvAppointmentInfo.setText("Rescheduling appointment with " + otherName + "\n" +
                "Current: " + appointment.getDate() + " at " + appointment.getTime());
        
        // Start from the existing appointment values. This lets the patient
        // change only the time while keeping today's date.
        final String[] selectedDate = {appointment.getDate()};
        final String[] selectedTime = {appointment.getTime()};
        btnSelectDate.setText(selectedDate[0]);
        btnSelectTime.setText(selectedTime[0]);
        checkRescheduleReady(btnConfirm, selectedDate[0], selectedTime[0]);
        
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        bottomSheet.setContentView(dialogView);
        
        btnSelectDate.setOnClickListener(v -> {
            com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker = 
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            
            datePicker.addOnPositiveButtonClickListener(selection -> {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
                sdf.setLenient(false);
                selectedDate[0] = sdf.format(new java.util.Date(selection));
                btnSelectDate.setText(selectedDate[0]);
                btnSelectDate.setIconTintResource(android.R.color.transparent);
                checkRescheduleReady(btnConfirm, selectedDate[0], selectedTime[0]);
            });
            
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
        
        btnSelectTime.setOnClickListener(v -> {
            com.google.android.material.timepicker.MaterialTimePicker timePicker = 
                    new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTitleText("Select Time")
                    .setHour(getInitialTimePart(appointment.getTime(), true))
                    .setMinute(getInitialTimePart(appointment.getTime(), false))
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                    .build();
            
            timePicker.addOnPositiveButtonClickListener(selection -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                // 24-hour format to stay consistent with BookAppointmentActivity
                selectedTime[0] = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
                btnSelectTime.setText(selectedTime[0]);
                btnSelectTime.setIconTintResource(android.R.color.transparent);
                checkRescheduleReady(btnConfirm, selectedDate[0], selectedTime[0]);
            });
            
            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });
        
        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            rescheduleAppointment(appointment, selectedDate[0], selectedTime[0], currentUserId);
            bottomSheet.dismiss();
        });
        
        bottomSheet.show();
    }
    
    private void checkRescheduleReady(MaterialButton btnConfirm, String date, String time) {
        btnConfirm.setEnabled(date != null && time != null);
    }
    
    private void rescheduleAppointment(Appointment appointment, String newDate, String newTime, String currentUserId) {
        String appointmentId = appointment.getAppointmentId();
        if (appointmentId == null) return;
        if (newDate == null || newTime == null) {
            showSnackbar("Please select a new date and time");
            return;
        }

        long newDateTimeMillis = parseDateTimeToMillis(newDate, newTime);
        if (newDateTimeMillis <= System.currentTimeMillis()) {
            showSnackbar("Please select a future date and time");
            return;
        }
        
        com.haset.hasetapp.utils.CustomDialog.showLoading(requireContext(), "Rescheduling...");
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("date", newDate);
        updates.put("time", newTime);
        updates.put("status", Constants.STATUS_PENDING);
        updates.put("lastUpdated", System.currentTimeMillis());
        updates.put("rescheduledBy", currentUserId);
        
        com.haset.hasetapp.utils.FirebaseHelper.getAppointmentsRef()
                .child(appointmentId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    com.haset.hasetapp.utils.CustomDialog.hideLoading();
                    showSnackbar("Appointment rescheduled successfully");

                    // Cancel old reminders and schedule new ones for the new date/time
                    reminderHelper.cancelRemindersByAppointmentId(appointmentId);
                    appointment.setDate(newDate);
                    appointment.setTime(newTime);
                    reminderHelper.scheduleReminders(appointment);

                    // Refresh list so the new date/time is shown immediately
                    if (viewModel != null) {
                        viewModel.refresh();
                    }

                    // Send notification to other party
                    sendRescheduleNotification(appointment, newDate, newTime, currentUserId);
                })
                .addOnFailureListener(e -> {
                    com.haset.hasetapp.utils.CustomDialog.hideLoading();
                    showSnackbar("Failed to reschedule: " + e.getMessage());
                });
    }

    private long parseDateTimeToMillis(String date, String time) {
        String[] formats = {"dd MMM yyyy HH:mm", "dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm"};
        for (String format : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.getDefault());
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(date + " " + time);
                if (parsed != null) return parsed.getTime();
            } catch (java.text.ParseException ignored) {
                // Try the next supported appointment date format.
            }
        }
        return 0;
    }

    private int getInitialTimePart(String time, boolean hour) {
        if (time == null) return 0;
        try {
            String[] parts = time.trim().split(":");
            return Integer.parseInt(parts[hour ? 0 : 1]);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
    
    private void sendRescheduleNotification(Appointment appointment, String newDate, String newTime, String currentUserId) {
        String recipientId;
        String senderName = preferenceManager.getUserName();
        
        if (currentUserId.equals(appointment.getPatientId())) {
            recipientId = appointment.getDoctorId();
        } else {
            recipientId = appointment.getPatientId();
        }
        
        if (recipientId == null) return;
        
        String message = senderName + " has rescheduled appointment to " + newDate + " at " + newTime;
        
        com.google.firebase.database.DatabaseReference notifRef = com.haset.hasetapp.utils.FirebaseHelper.getNotificationsRef(recipientId);
        notifRef.push().setValue(new com.haset.hasetapp.models.Notification(
                recipientId,
                "Appointment Rescheduled",
                message,
                "appointment_rescheduled",
                appointment.getAppointmentId()
        ));
    }

    private void showSnackbar(String message) {
        if (isAdded() && getView() != null) {
            com.google.android.material.snackbar.Snackbar.make(getView(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStartSession(Appointment appointment) {
        if (appointment == null) return;
        
        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(appointment.getAppointmentType())) {
            Intent chatIntent = new Intent(requireContext(), com.haset.hasetapp.activities.ChatActivity.class);
            // If the current user is a patient, the other user is the doctor (doctorId)
            // If the current user is a doctor, the other user is the patient (patientId)
            String currentUserId = preferenceManager.getUserId();
            String otherUserId = currentUserId.equals(appointment.getPatientId()) ? 
                    appointment.getDoctorId() : appointment.getPatientId();
            String otherUserName = currentUserId.equals(appointment.getPatientId()) ? 
                    appointment.getDoctorName() : appointment.getPatientName();
                    
            chatIntent.putExtra(Constants.EXTRA_CHAT_USER_ID, otherUserId);
            chatIntent.putExtra(Constants.EXTRA_CHAT_USER_NAME, otherUserName);
            startActivity(chatIntent);
        }
    }

    @Override
    public void onRateDoctor(Appointment appointment) {
        // Rating only available for completed appointments
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clear adapter
        if (rvAppointments != null) {
            rvAppointments.setAdapter(null);
        }
        appointmentAdapter = null;
        
        // Null out view references
        rvAppointments = null;
        shimmerContainer = null;
        emptyStateCard = null;
        tvEmptyStateTitle = null;
        tvEmptyStateSubtitle = null;
        ivEmptyStateIcon = null;
        rootView = null;
    }
}
