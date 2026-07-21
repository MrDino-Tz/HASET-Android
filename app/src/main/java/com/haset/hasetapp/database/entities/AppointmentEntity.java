package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "appointments")
public class AppointmentEntity {
    @PrimaryKey
    @NonNull
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String patientName;
    private String doctorName;
    private String date;
    private String time;
    private String reason;
    private String status; // "pending", "approved", "declined", "completed"
    private String appointmentType; // New field for appointment type: "Visit" or "Online Chat"
    private long createdAt;
    private long chatStartTime; // When the chat session started
    private long chatEndTime; // When the chat session ended
    private long chatDuration; // Duration in milliseconds
    private boolean isChatActive; // Whether chat session is currently active
    private static final long DEFAULT_CHAT_DURATION = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds

    public AppointmentEntity() {
    }

    @androidx.room.Ignore
    public AppointmentEntity(@NonNull String appointmentId, String patientId, String doctorId,
                            String patientName, String doctorName, String date, String time,
                            String reason, String status, String appointmentType) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.status = status;
        this.appointmentType = appointmentType; // Initialize appointmentType
        this.createdAt = System.currentTimeMillis();
    }

    // New constructor to convert from Appointment model for updating status
    @androidx.room.Ignore
    public AppointmentEntity(com.haset.hasetapp.models.Appointment appointment, String status) {
        this.appointmentId = appointment.getAppointmentId();
        this.patientId = appointment.getPatientId();
        this.doctorId = appointment.getDoctorId();
        this.patientName = appointment.getPatientName();
        this.doctorName = appointment.getDoctorName();
        this.date = appointment.getDate();
        this.time = appointment.getTime();
        this.reason = appointment.getReason();
        this.status = status; // Use the provided status
        this.appointmentType = appointment.getAppointmentType();
        this.createdAt = appointment.getCreatedAt(); // Explicitly set createdAt from appointment model
        // updatedAt is handled by FirebaseHelper or implicitly by Firebase itself when saving
    }

    // Getters and Setters
    @NonNull
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(@NonNull String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getChatStartTime() { return chatStartTime; }
    public void setChatStartTime(long chatStartTime) { this.chatStartTime = chatStartTime; }

    public long getChatEndTime() { return chatEndTime; }
    public void setChatEndTime(long chatEndTime) { this.chatEndTime = chatEndTime; }

    public long getChatDuration() { return chatDuration; }
    public void setChatDuration(long chatDuration) { this.chatDuration = chatDuration; }

    public boolean isChatActive() { return isChatActive; }
    public void setChatActive(boolean chatActive) { isChatActive = chatActive; }

    public long getDefaultChatDuration() { return DEFAULT_CHAT_DURATION; }
}
