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
import com.haset.hasetapp.utils.CrashMonitor;
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
    private String pendingPaymentAppointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);
        overridePendingTransition(R.anim.anim_slide_up, 0);

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
        
        optionOnlineChat.setOnClickListener(v -> selectInstantAppointment(Constants.APPOINTMENT_TYPE_ONLINE_CHAT));

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
                if (processing) {
                    btnConfirmBooking.setText(R.string.loading);
                } else {
                    refreshConfirmButtonLabel();
                }
            }
        });

        viewModel.getBookingError().observe(this, error -> {
            if (error == null) return;
            String detail = com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(BookAppointmentActivity.this, error);
            Snackbar.make(rootView, "Failed to " + (isReschedule ? "reschedule" : "book") + " appointment: " + detail, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> bookAppointment())
                    .show();
            if (com.haset.hasetapp.utils.ErrorDisplay.isAuthError(error)) {
                com.haset.hasetapp.utils.ErrorDisplay.navigateToLogin(BookAppointmentActivity.this);
            } else {
                com.haset.hasetapp.utils.ErrorLogger.log(detail, error);
            }
            viewModel.clearBookingError();
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

        // Keep booking doctorId in sync with the loaded doctor record.
        if (doctorDetail.getDoctorId() != null && !doctorDetail.getDoctorId().trim().isEmpty()) {
            doctorId = doctorDetail.getDoctorId();
        } else if (doctorDetail.getUserId() != null && !doctorDetail.getUserId().trim().isEmpty()) {
            doctorId = doctorDetail.getUserId();
        }
        
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
        
        String photoUserId = doctorDetail.getUserId() != null ? doctorDetail.getUserId() : doctorDetail.getDoctorId();
        ProfilePhotoHelper.loadProfilePhoto(this, photoUserId, ivDoctorImage, shimmerDoctorImage);
        
        // Show verified badge if doctor is verified
        if (ivVerifiedBadge != null && doctorDetail.isVerified()) {
            ivVerifiedBadge.setVisibility(View.VISIBLE);
        } else if (ivVerifiedBadge != null) {
            ivVerifiedBadge.setVisibility(View.GONE);
        }

        updateInstantAppointmentAvailability();
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
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            Snackbar.make(rootView, "Please sign in again to book an appointment", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if ((doctorId == null || doctorId.trim().isEmpty()) && doctor != null) {
            doctorId = doctor.getDoctorId() != null ? doctor.getDoctorId() : doctor.getUserId();
        }

        if (selectedDate == null || selectedDate.isEmpty()) {
            Snackbar.make(rootView, "Please select a date", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if (selectedTime == null || selectedTime.isEmpty()) {
            Snackbar.make(rootView, "Please select a time", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if (doctor == null || doctorId == null || doctorId.trim().isEmpty()) {
            Snackbar.make(rootView, "Doctor not loaded. Please go back and try again.", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equalsIgnoreCase(appointmentType) && !isDoctorOnline()) {
            showDoctorOfflineMessage();
            return;
        }

        prepareAppointmentForPayment();
    }

    private void prepareAppointmentForPayment() {
        if (isLaunchingPayment) {
            Snackbar.make(rootView, "Please wait, preparing your booking…", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Check if this is a demo doctor - skip payment
        if (doctor != null && doctor.isDemo()) {
            isLaunchingPayment = true;
            // Show confirmation and proceed directly without payment
            new AlertDialog.Builder(this)
                .setTitle(R.string.demo_doctor)
                .setMessage(R.string.free_demo_consultation_confirm)
                .setPositiveButton(R.string.continue_action, (dialog, which) -> {
                    paidAt = System.currentTimeMillis();
                    paymentTransactionId = 0;
                    proceedWithBooking();
                    isLaunchingPayment = false;
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    isLaunchingPayment = false;
                })
                .setOnCancelListener(dialog -> isLaunchingPayment = false)
                .show();
            return;
        }

        isLaunchingPayment = true;
        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText(R.string.loading);
        // Unstick the button if Firebase never responds.
        safetyHandler.removeCallbacksAndMessages(null);
        safetyHandler.postDelayed(() -> {
            if (!isFinishing() && isLaunchingPayment && pendingPaymentAppointmentId == null) {
                isLaunchingPayment = false;
                btnConfirmBooking.setEnabled(true);
                refreshConfirmButtonLabel();
                Snackbar.make(rootView, "Booking timed out. Please try again.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.colorError))
                        .show();
            }
        }, 20000);

        if (pendingPaymentAppointmentId != null && !pendingPaymentAppointmentId.trim().isEmpty()) {
            launchPaymentActivity(pendingPaymentAppointmentId);
            return;
        }

        String appointmentId = FirebaseHelper.getAppointmentsRef().push().getKey();
        if (appointmentId == null) {
            resetBookingButtonState();
            Snackbar.make(rootView, "Unable to prepare payment session", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.colorError))
                    .show();
            return;
        }

        AppointmentEntity appointmentEntity = buildAppointmentEntity(appointmentId);
        // Hold before payment so doctors don't see unpaid bookings as approval requests.
        appointmentEntity.setStatus(Constants.STATUS_AWAITING_PAYMENT);
        appointmentEntity.setPaymentStatus(Constants.PAYMENT_STATUS_UNPAID);
        CrashMonitor.step("appointment", "BookAppointmentActivity",
                "creating awaiting_payment draft type=" + appointmentType + " doctor=" + doctorId);
        FirebaseHelper.createAppointment(appointmentEntity, new FirebaseHelper.OnCompleteListener<AppointmentEntity>() {
            @Override
            public void onSuccess(AppointmentEntity result) {
                pendingPaymentAppointmentId = result.getAppointmentId();
                launchPaymentActivity(pendingPaymentAppointmentId);
            }

            @Override
            public void onError(String error) {
                resetBookingButtonState();
                String message = error != null ? error : "Unable to prepare payment session";
                if (message.toLowerCase(Locale.US).contains("permission")) {
                    message = "Booking blocked (permission denied). Sign out/in and try again, or pick another doctor.";
                }
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.colorError))
                        .show();
            }
        });
    }

    private void resetBookingButtonState() {
        isLaunchingPayment = false;
        safetyHandler.removeCallbacksAndMessages(null);
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setEnabled(true);
            refreshConfirmButtonLabel();
        }
    }

    private void refreshConfirmButtonLabel() {
        if (btnConfirmBooking == null) return;
        if (isReschedule) {
            btnConfirmBooking.setText(R.string.reschedule_appointment);
        } else if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equalsIgnoreCase(appointmentType)) {
            btnConfirmBooking.setText(getString(R.string.book_appointment_type, appointmentType));
        } else {
            btnConfirmBooking.setText(R.string.book_appointment);
        }
    }

    private void launchPaymentActivity(String appointmentId) {
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("doctor", doctor);
        double fee = doctor.getConsultationFee() > 0 ? doctor.getConsultationFee() : 0.0;
        paymentIntent.putExtra("consultation_fee", fee);
        paymentIntent.putExtra("consultation_id", appointmentId);
        startActivityForResult(paymentIntent, 100);
        
        // Keep guard briefly so double-taps don't spawn multiple payment screens.
        safetyHandler.removeCallbacksAndMessages(null);
        safetyHandler.postDelayed(() -> {
            if (!isFinishing()) {
                isLaunchingPayment = false;
                if (btnConfirmBooking != null) {
                    btnConfirmBooking.setEnabled(true);
                    refreshConfirmButtonLabel();
                }
            }
        }, 1500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            resetBookingButtonState();
            if (resultCode == RESULT_OK) {
                paidAt = System.currentTimeMillis();
                paymentTransactionId = data != null ? data.getIntExtra("transaction_id", -1) : -1;
                proceedWithBooking();
            } else {
                cleanupPendingPaymentAppointment();
            }
        }
    }

    private void cleanupPendingPaymentAppointment() {
        if (pendingPaymentAppointmentId == null || pendingPaymentAppointmentId.trim().isEmpty()) {
            return;
        }
        String appointmentId = pendingPaymentAppointmentId;
        pendingPaymentAppointmentId = null;
        String patientId = preferenceManager.getUserId();
        String resolvedDoctorId = doctorId;

        // Atomic multi-path cleanup (rules allow patient delete of awaiting_payment).
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("appointments/" + appointmentId, null);
        if (patientId != null && !patientId.trim().isEmpty()) {
            updates.put("patient_appointments/" + patientId + "/" + appointmentId, null);
        }
        if (resolvedDoctorId != null && !resolvedDoctorId.trim().isEmpty()) {
            updates.put("doctor_appointments/" + resolvedDoctorId + "/" + appointmentId, null);
        }
        FirebaseHelper.getFirebaseDatabase().getReference().updateChildren(updates)
                .addOnFailureListener(e -> {
                    // Fallback: mark cancelled so it never shows as pending approval.
                    java.util.Map<String, Object> cancel = new java.util.HashMap<>();
                    cancel.put("status", Constants.STATUS_CANCELLED);
                    cancel.put("paymentStatus", Constants.PAYMENT_STATUS_UNPAID);
                    cancel.put("updatedAt", System.currentTimeMillis());
                    FirebaseHelper.getAppointmentsRef().child(appointmentId).updateChildren(cancel);
                });
    }

    private void proceedWithBooking() {
        CrashMonitor.step("appointment", "BookAppointmentActivity", "proceeding with booked appointment");
        AppointmentEntity appointmentEntity = buildAppointmentEntity(pendingPaymentAppointmentId);
        // Only after successful payment does this become a real pending approval request.
        appointmentEntity.setStatus(Constants.STATUS_PENDING);
        if (paidAt > 0 || paymentTransactionId >= 0) {
            long paymentTime = paidAt > 0 ? paidAt : System.currentTimeMillis();
            appointmentEntity.setPaymentStatus(Constants.PAYMENT_STATUS_PAID);
            appointmentEntity.setPaidAt(paymentTime);
            appointmentEntity.setPaymentTransactionId(String.valueOf(paymentTransactionId));
            appointmentEntity.setChatStartsAt(0L);
            appointmentEntity.setChatExpiresAt(0L);
            appointmentEntity.setChatActive(false);
        } else if (doctor != null && doctor.isDemo()) {
            appointmentEntity.setPaymentStatus(Constants.PAYMENT_STATUS_PAID);
        }

        viewModel.createAppointment(appointmentEntity);
    }

    private AppointmentEntity buildAppointmentEntity(String appointmentId) {
        // Rules require patientId === Firebase Auth UID.
        String patientId = null;
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            patientId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            patientId = preferenceManager.getUserId();
        }
        String patientName = preferenceManager.getUserName();

        AppointmentEntity appointmentEntity = new AppointmentEntity();
        if (appointmentId != null && !appointmentId.trim().isEmpty()) {
            appointmentEntity.setAppointmentId(appointmentId);
        }
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

        // Default for completed booking path; pre-payment path overrides to awaiting_payment.
        appointmentEntity.setStatus(Constants.STATUS_PENDING);
        return appointmentEntity;
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
                if (!isDoctorOnline()) {
                    contentToToggle.setVisibility(View.GONE);
                    iconToRotate.animate().rotation(0f).setDuration(300).start();
                    showDoctorOfflineMessage();
                    return;
                }
                selectInstantAppointment(Constants.APPOINTMENT_TYPE_ONLINE_CHAT);
            } else {
                appointmentType = "Visit";
                btnConfirmBooking.setText(R.string.schedule_appointment);
                etDate.requestFocus();
            }
        }
    }


    private void selectInstantAppointment(String type) {
        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equalsIgnoreCase(type) && !isDoctorOnline()) {
            showDoctorOfflineMessage();
            return;
        }

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
        
        if (Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equals(selectedType)) {
            optionOnlineChat.setBackgroundTintList(ColorStateList.valueOf(colorGreen));
            ((ImageView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(0)).setColorFilter(colorWhite);
            ((TextView) ((android.widget.LinearLayout) optionOnlineChat).getChildAt(1)).setTextColor(colorWhite);
        }
    }

    private void updateInstantAppointmentAvailability() {
        boolean online = isDoctorOnline();
        optionOnlineChat.setEnabled(online);
        optionOnlineChat.setAlpha(online ? 1.0f : 0.45f);
        cardInstantAppointment.setAlpha(online ? 1.0f : 0.65f);
        if (!online && Constants.APPOINTMENT_TYPE_ONLINE_CHAT.equalsIgnoreCase(appointmentType)) {
            appointmentType = "Visit";
            btnConfirmBooking.setText(R.string.schedule_appointment);
        }
    }

    private boolean isDoctorOnline() {
        if (doctor == null) return false;
        String onlineStatus = doctor.getOnlineStatus();
        return doctor.isOnline() && (onlineStatus == null || "online".equalsIgnoreCase(onlineStatus));
    }

    private void showDoctorOfflineMessage() {
        Snackbar.make(rootView, "Doctor is offline. Please schedule a visit instead.", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.colorError))
                .show();
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
