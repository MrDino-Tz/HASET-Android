package com.haset.hasetapp.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.AppointmentReminderHelper;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import com.haset.hasetapp.utils.DoctorNotificationManager;
import com.haset.hasetapp.utils.CustomDialog;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AppointmentBookingViewModel;
import androidx.transition.TransitionManager;
import androidx.transition.AutoTransition;
import android.content.res.ColorStateList;

public class BookAppointmentActivity extends BaseActivity {
    private MaterialToolbar toolbar;
    private CircleImageView ivDoctorImage;
    private ImageView ivVerifiedBadge;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerDoctorImage;
    private TextView tvDoctorName, tvSpecialty, tvConsultationFee;
    private TextInputEditText etDate, etReason, etTime;
    private MaterialButton btnConfirmBooking;
    private View cardInstantAppointment, cardScheduleAppointment;
    private View optionOnlineChat;
    private View headerInstantAppointment, headerScheduleAppointment;
    private View contentInstantAppointment, contentScheduleAppointment;
    private ImageView expandIconInstant, expandIconSchedule;
    
    private String selectedDate;
    private String selectedTime;
    private String doctorId;
    private Doctor doctor;
    private String appointmentType = "Visit"; // Default to Visit
    
    private PreferenceManager preferenceManager;
    private AppointmentReminderHelper reminderHelper;
    private DoctorNotificationManager doctorNotificationManager;
    private boolean isReschedule = false;
    private String originalAppointmentId;
    private View rootView;
    private AppointmentBookingViewModel viewModel;
    private boolean isLaunchingPayment = false; // Guard against multiple activity launches
    private final android.os.Handler safetyHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long paidAt = 0L;
    private int paymentTransactionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        initViews();
        preferenceManager = new PreferenceManager(this);
        reminderHelper = new AppointmentReminderHelper(this);
        doctorNotificationManager = new DoctorNotificationManager(this);
        rootView = findViewById(android.R.id.content);

        doctorId = getIntent().getStringExtra(Constants.EXTRA_DOCTOR_ID);
        isReschedule = getIntent().getBooleanExtra("is_reschedule", false);
        originalAppointmentId = getIntent().getStringExtra("original_appointment_id");
        
        if (isReschedule) {
            btnConfirmBooking.setText(R.string.reschedule_appointment);
        }

        // Try to get doctor object from intent first for instant loading
        Doctor intentDoctor = (Doctor) getIntent().getSerializableExtra("doctor");
        if (intentDoctor != null) {
            this.doctor = intentDoctor;
            populateDoctorViews(intentDoctor);
        }
        
        viewModel = new ViewModelProvider(this).get(AppointmentBookingViewModel.class);
        setupObservers();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnConfirmBooking.setOnClickListener(v -> bookAppointment());
        
        headerInstantAppointment.setOnClickListener(v -> toggleCardExpansion("instant"));
        headerScheduleAppointment.setOnClickListener(v -> toggleCardExpansion("schedule"));
        
        optionOnlineChat.setOnClickListener(v -> selectInstantAppointment("Online Chat"));
    }

    private void setupObservers() {
        if (doctorId != null) {
            viewModel.getDoctorDetails(doctorId).observe(this, doctorDetail -> {
                if (doctorDetail != null) {
                    this.doctor = doctorDetail;
                    populateDoctorViews(doctorDetail);
                }
            });
        }

        viewModel.getBookingProcessing().observe(this, processing -> {
            if (processing != null) {
                btnConfirmBooking.setEnabled(!processing);
                btnConfirmBooking.setText(processing ? R.string.loading : (isReschedule ? R.string.reschedule_appointment : R.string.book_appointment));
            }
        });

        viewModel.getBookingError().observe(this, error -> {
            if (error != null) {
                Snackbar.make(rootView, "Failed to " + (isReschedule ? "reschedule" : "book") + " appointment: " + error, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> bookAppointment())
                        .show();
            }
        });

        viewModel.getBookingSuccess().observe(this, resultEntity -> {
            if (resultEntity != null) {
                com.haset.hasetapp.models.Appointment resultAppointment = new com.haset.hasetapp.models.Appointment(resultEntity);
                handleBookingSuccess(resultAppointment);
            }
        });
    }

    private void handleBookingSuccess(com.haset.hasetapp.models.Appointment resultAppointment) {
        if (isReschedule) {
            AuditLogger.getInstance(BookAppointmentActivity.this).logAppointmentUpdated(
                resultAppointment.getAppointmentId(), "RESCHEDULE",
                "Rescheduled appointment with " + resultAppointment.getDoctorName()
            );
            if (originalAppointmentId != null) {
                reminderHelper.cancelRemindersByAppointmentId(originalAppointmentId);
            }
        } else {
            AuditLogger.getInstance(BookAppointmentActivity.this).logAppointmentCreated(
                resultAppointment.getAppointmentId(), resultAppointment.getDoctorName()
            );
        }

        reminderHelper.scheduleReminders(resultAppointment);

        // Show success and finish
        String successMessage = "Your appointment has been booked successfully. Please wait for the doctor to approve it.";
        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(appointmentType)) {
            successMessage = "Your chat appointment is booked. You will be notified once the doctor is ready to chat.";
        }

        CustomDialog.showSuccess(this, "Booking Successful", 
            isReschedule ? "Appointment rescheduled successfully" : successMessage, 
            "Go to Appointments", v -> {
                finish();
            });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivDoctorImage = findViewById(R.id.ivDoctorImage);
        ivVerifiedBadge = findViewById(R.id.ivVerifiedBadge);
        shimmerDoctorImage = findViewById(R.id.shimmerDoctorImage);
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpecialty = findViewById(R.id.tvSpecialty);
        tvConsultationFee = findViewById(R.id.tvConsultationFee);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etReason = findViewById(R.id.etReason);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        cardInstantAppointment = findViewById(R.id.cardInstantAppointment);
        cardScheduleAppointment = findViewById(R.id.cardScheduleAppointment);
        optionOnlineChat = findViewById(R.id.optionOnlineChat);
        headerInstantAppointment = findViewById(R.id.headerInstantAppointment);
        headerScheduleAppointment = findViewById(R.id.headerScheduleAppointment);
        contentInstantAppointment = findViewById(R.id.contentInstantAppointment);
        contentScheduleAppointment = findViewById(R.id.contentScheduleAppointment);
        expandIconInstant = findViewById(R.id.expandIconInstant);
        expandIconSchedule = findViewById(R.id.expandIconSchedule);
    }

    private void populateDoctorViews(Doctor doctorDetail) {
        if (doctorDetail == null) return;
        
        tvDoctorName.setText(getString(R.string.dr_prefix, doctorDetail.getFullName()));
        tvSpecialty.setText(doctorDetail.getSpecialty() != null ? doctorDetail.getSpecialty() : "General Physician");
        
        double fee = doctorDetail.getConsultationFee() > 0 ? doctorDetail.getConsultationFee() : 0.0;
        String feeText;
        if (doctorDetail.isDemo()) {
            feeText = "FREE";
        } else {
            feeText = String.format(Locale.getDefault(), "%,.0f TZS", fee);
        }
        tvConsultationFee.setText(feeText);
        
        ProfilePhotoHelper.loadProfilePhoto(this, doctorDetail.getUserId(), ivDoctorImage, shimmerDoctorImage);
        
        // Show verified badge if doctor is verified
        if (ivVerifiedBadge != null && doctorDetail.isVerified()) {
            ivVerifiedBadge.setVisibility(View.VISIBLE);
        } else if (ivVerifiedBadge != null) {
            ivVerifiedBadge.setVisibility(View.GONE);
        }
    }

    private void loadDoctorDetails() {
        // Handled by setupObservers and intent check
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());
                    etDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Calendar picked = java.util.Calendar.getInstance();
            picked.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
            picked.set(java.util.Calendar.MINUTE, minute1);
            String formatted = sdf.format(picked.getTime());
            selectedTime = formatted;
            etTime.setText(formatted);
        }, hour, minute, true).show();
    }

    private void bookAppointment() {
        if (selectedDate == null || selectedDate.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(rootView, "Please select a date", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if (selectedTime == null || selectedTime.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(rootView, "Please select a time", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if (doctor == null) {
            com.google.android.material.snackbar.Snackbar.make(rootView, "Doctor not loaded", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        launchPaymentActivity();
    }

    private void launchPaymentActivity() {
        if (isLaunchingPayment) return;
        
        // Check if this is a demo doctor - skip payment
        if (doctor != null && doctor.isDemo()) {
            isLaunchingPayment = true;
            // Show confirmation and proceed directly without payment
            new AlertDialog.Builder(this)
                .setTitle("Demo Doctor")
                .setMessage("This is a free demo consultation. No payment required. Do you want to continue?")
                .setPositiveButton("Continue", (dialog, which) -> {
                    paidAt = System.currentTimeMillis();
                    paymentTransactionId = 0;
                    proceedWithBooking();
                    isLaunchingPayment = false;
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    isLaunchingPayment = false;
                })
                .setOnCancelListener(dialog -> isLaunchingPayment = false)
                .show();
            return;
        }
        
        isLaunchingPayment = true;
        
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("doctor", doctor);
        double fee = doctor.getConsultationFee() > 0 ? doctor.getConsultationFee() : 0.0;
        paymentIntent.putExtra("consultation_fee", fee);
        startActivityForResult(paymentIntent, 100);
        
        // Safety timeout to reset the flag if the activity somehow fails to start or we don't get a result
        // Safety timeout to reset the flag — uses named handler so it can be cancelled in onDestroy
        safetyHandler.postDelayed(() -> {
            if (!isFinishing()) isLaunchingPayment = false;
        }, 2000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            isLaunchingPayment = false; // Reset guard
            if (resultCode == RESULT_OK) {
                paidAt = System.currentTimeMillis();
                paymentTransactionId = data != null ? data.getIntExtra("transaction_id", -1) : -1;
                proceedWithBooking();
            }
        }
    }

    private void proceedWithBooking() {
        String patientId = preferenceManager.getUserId();
        String patientName = preferenceManager.getUserName();

        AppointmentEntity appointmentEntity = new AppointmentEntity();
        appointmentEntity.setPatientId(patientId);
        appointmentEntity.setDoctorId(doctorId);
        appointmentEntity.setPatientName(patientName);
        appointmentEntity.setDoctorName(doctor.getFullName());
        appointmentEntity.setDate(selectedDate);
        appointmentEntity.setTime(selectedTime);
        appointmentEntity.setReason(etReason.getText().toString().trim());
        appointmentEntity.setAppointmentType(appointmentType);
        // Persist the consultation fee so admin revenue reports can read it
        appointmentEntity.setAmount(doctor != null && doctor.getConsultationFee() > 0 ? doctor.getConsultationFee() : 0.0);
        if (paidAt > 0 || paymentTransactionId >= 0) {
            long paymentTime = paidAt > 0 ? paidAt : System.currentTimeMillis();
            appointmentEntity.setPaymentStatus("paid");
            appointmentEntity.setPaidAt(paymentTime);
            appointmentEntity.setPaymentTransactionId(String.valueOf(paymentTransactionId));
            appointmentEntity.setChatStartsAt(0L);
            appointmentEntity.setChatExpiresAt(0L);
            appointmentEntity.setChatActive(false);
        }
        
        // All appointments start as pending and require doctor approval
        appointmentEntity.setStatus(Constants.STATUS_PENDING);

        viewModel.createAppointment(appointmentEntity);
    }

    private void toggleCardExpansion(String cardType) {
        boolean isInstant = "instant".equals(cardType);
        View contentToToggle = isInstant ? contentInstantAppointment : contentScheduleAppointment;
        ImageView iconToRotate = isInstant ? expandIconInstant : expandIconSchedule;
        View otherContent = isInstant ? contentScheduleAppointment : contentInstantAppointment;
        ImageView otherIcon = isInstant ? expandIconSchedule : expandIconInstant;
        
        ViewGroup parent = (ViewGroup) cardInstantAppointment.getParent();
        TransitionManager.beginDelayedTransition(parent, new AutoTransition().setDuration(300));

        if (contentToToggle.getVisibility() == View.VISIBLE) {
            // Collapse current
            contentToToggle.setVisibility(View.GONE);
            iconToRotate.animate().rotation(0f).setDuration(300).start();
        } else {
            // Expand current, collapse other
            if (otherContent.getVisibility() == View.VISIBLE) {
                otherContent.setVisibility(View.GONE);
                otherIcon.animate().rotation(0f).setDuration(300).start();
            }
            
            contentToToggle.setVisibility(View.VISIBLE);
            iconToRotate.animate().rotation(180f).setDuration(300).start();
            
            if (isInstant) {
                selectInstantAppointment("Online Chat");
            } else {
                appointmentType = "Visit";
                btnConfirmBooking.setText(R.string.schedule_appointment);
                etDate.requestFocus();
            }
        }
    }


    private void selectInstantAppointment(String type) {
        appointmentType = type;
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        
        String currentDate = dateFormat.format(calendar.getTime());
        String currentTime = timeFormat.format(calendar.getTime());
        
        etDate.setText(currentDate);
        etTime.setText(currentTime);
        selectedDate = currentDate;
        selectedTime = currentTime;
        
        btnConfirmBooking.setText(getString(R.string.book_appointment_type, type));
        updateOptionSelection(type);
    }

    private void updateOptionSelection(String selectedType) {
        int colorGreen = getResources().getColor(R.color.green_primary);
        int colorTextPrimary = getResources().getColor(R.color.text_primary);
        int colorWhite = getResources().getColor(android.R.color.white);
        int colorBackground = getResources().getColor(R.color.background_light);

        // Reset both
        optionOnlineChat.setBackgroundTintList(ColorStateList.valueOf(colorBackground));
        ((ImageView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(0)).setColorFilter(colorGreen);
        ((TextView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(1)).setTextColor(colorTextPrimary);
        
        if ("Online Chat".equals(selectedType)) {
            optionOnlineChat.setBackgroundTintList(ColorStateList.valueOf(colorGreen));
            ((ImageView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(0)).setColorFilter(colorWhite);
            ((TextView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(1)).setTextColor(colorWhite);
        }
    }

    private void showComingSoonDialog(String featureName) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_coming_soon);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(featureName + " Coming Soon!");

        View btnOk = dialog.findViewById(R.id.btnOk);
        if (btnOk != null) btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        safetyHandler.removeCallbacksAndMessages(null);
    }
}
