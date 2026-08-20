package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

@Entity(tableName = "doctors")
public class DoctorEntity implements Serializable {
    @PrimaryKey
    @NonNull
    private String doctorId;
    private String specialty;
    private double consultationFee;
    private String availableTimes; // JSON string or comma-separated: "09:00,10:00,11:00"
    private String location; // New field for doctor's location
    private boolean isApproved; // Admin approval status
    private long lastUpdated;
    private String profileImage; // New field for profile image path
    private String about; // Bio information
    private boolean isOnline; // Doctor online/offline status
    private String onlineStatus; // "online", "offline", "busy"
    private String regNo; // Medical Council Registration Number
    private long createdAt;
    private boolean isDemo; // Demo doctor flag (free consultation)

    public DoctorEntity() {
    }

    @androidx.room.Ignore
    public DoctorEntity(@NonNull String doctorId, String specialty, double consultationFee, String availableTimes, String location, String profileImage) {
        this.doctorId = doctorId;
        this.specialty = specialty;
        this.consultationFee = consultationFee;
        this.availableTimes = availableTimes;
        this.location = location;
        this.profileImage = profileImage; // Initialize profileImage
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    @NonNull
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(@NonNull String doctorId) {
        this.doctorId = doctorId;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(String availableTimes) {
        this.availableTimes = availableTimes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @com.google.firebase.database.PropertyName("approved")
    public boolean isApproved() {
        return isApproved;
    }

    @com.google.firebase.database.PropertyName("approved")
    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getProfileImage() { // New getter
        return profileImage;
    }

    public void setProfileImage(@Nullable String profileImage) { // New setter
        this.profileImage = profileImage;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    // New fields for doctor rating and experience
    private Float averageRating = 0.0f; // Default to 0.0f
    private int experience = 0; // Default to 0 years

    public Float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Float averageRating) {
        this.averageRating = averageRating;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    private int patientsTreated = 0;

    public int getPatientsTreated() {
        return patientsTreated;
    }

    public void setPatientsTreated(int patientsTreated) {
        this.patientsTreated = patientsTreated;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }
    
    public String getRegNo() {
        return regNo;
    }
    
    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isDemo() { return isDemo; }
    public void setDemo(boolean demo) { isDemo = demo; }
}

