package com.haset.hasetapp.models;

import java.util.Locale;

public class Appointment {
    private static final long AUTO_COMPLETE_AFTER_MILLIS = 24 * 60 * 60 * 1000L;

    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String date;
    private String time;
    private String status; // "pending", "approved", "declined", "completed"
    private String reason;
    private String appointmentType; // "Visit" or "Online Chat"
    private long createdAt;
    private long updatedAt;
    private double amount;

    public Appointment() {
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Appointment(String appointmentId, String patientId, String doctorId, String date, String time) {
        this();
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
        this.appointmentType = "Visit"; // Default value
    }
    
    // New constructor to convert from AppointmentEntity
    public Appointment(com.haset.hasetapp.database.entities.AppointmentEntity entity) {
        this.appointmentId = entity.getAppointmentId();
        this.patientId = entity.getPatientId();
        this.patientName = entity.getPatientName();
        this.doctorId = entity.getDoctorId();
        this.doctorName = entity.getDoctorName();
        // Doctor specialty is not directly available from AppointmentEntity.
        // It might need to be fetched separately from Doctor data if required for display.
        this.doctorSpecialty = null;
        this.date = entity.getDate();
        this.time = entity.getTime();
        this.status = entity.getStatus();
        this.reason = entity.getReason();
        this.appointmentType = entity.getAppointmentType();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = System.currentTimeMillis(); // Assuming update time should be current
        this.amount = entity.getAmount();
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    // Utility methods for tab filtering
    public boolean isUpcoming() {
        if (status == null) return false;
        if ("pending".equalsIgnoreCase(this.status)) return true;
        
        long now = System.currentTimeMillis();
        long apptTime = parseToMillis(this.date, this.time);
        
        // If approved, keep in upcoming if it's in the future or was scheduled within the last 2 hours
        return "approved".equalsIgnoreCase(this.status) && apptTime >= (now - 2 * 3600 * 1000);
    }

    public boolean isPast() {
        if (status == null) return false;
        if ("cancelled".equalsIgnoreCase(this.status) || "pending".equalsIgnoreCase(this.status)) return false;
        if ("completed".equalsIgnoreCase(this.status) || "declined".equalsIgnoreCase(this.status)) return true;
        
        long now = System.currentTimeMillis();
        long apptTime = parseToMillis(this.date, this.time);
        
        // If approved but more than 2 hours past, move to history
        return "approved".equalsIgnoreCase(this.status) && apptTime < (now - 2 * 3600 * 1000);
    }

    public boolean isCancelled() {
        return status != null && "cancelled".equalsIgnoreCase(this.status);
    }

    public boolean shouldAutoComplete() {
        if (!"approved".equalsIgnoreCase(this.status)) return false;

        long apptTime = parseToMillis(this.date, this.time);
        return System.currentTimeMillis() >= apptTime + AUTO_COMPLETE_AFTER_MILLIS;
    }

    // Helper to parse date+time to millis (supports multiple formats)
    private long parseToMillis(String date, String time) {
        if (date == null || time == null) return System.currentTimeMillis();
        try {
            java.text.SimpleDateFormat sdf0 = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return sdf0.parse(date + " " + time).getTime();
        } catch (java.text.ParseException e0) {
            try {
                java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
                return sdf1.parse(date + " " + time).getTime();
            } catch (java.text.ParseException e1) {
                try {
                    java.text.SimpleDateFormat sdf3 = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    return sdf3.parse(date + " " + time).getTime();
                } catch (java.text.ParseException e3) {
                    try {
                        java.text.SimpleDateFormat sdf4 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        return sdf4.parse(date + " " + time).getTime();
                    } catch (java.text.ParseException e4) {
                        return System.currentTimeMillis();
                    }
                }
            }
        }
    }
}
