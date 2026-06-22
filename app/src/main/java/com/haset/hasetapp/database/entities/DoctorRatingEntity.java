package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.io.Serializable;

@Entity(tableName = "doctor_ratings")
public class DoctorRatingEntity implements Serializable {
    @PrimaryKey
    @NonNull
    private String ratingId;
    @NonNull
    private String doctorId;
    @NonNull
    private String patientId;
    private String patientName;
    private float rating; // 1.0 to 5.0
    private String comment; // Optional review comment
    private String appointmentId; // Optional: link to appointment
    private long createdAt;

    public DoctorRatingEntity() {
    }

    @androidx.room.Ignore
    public DoctorRatingEntity(@NonNull String ratingId, String doctorId, String patientId, 
                             String patientName, float rating, String comment, String appointmentId) {
        this.ratingId = ratingId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.rating = rating;
        this.comment = comment;
        this.appointmentId = appointmentId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    @NonNull
    public String getRatingId() {
        return ratingId;
    }

    public void setRatingId(@NonNull String ratingId) {
        this.ratingId = ratingId;
    }

    @NonNull
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(@NonNull String doctorId) {
        this.doctorId = doctorId;
    }

    @NonNull
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(@NonNull String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

