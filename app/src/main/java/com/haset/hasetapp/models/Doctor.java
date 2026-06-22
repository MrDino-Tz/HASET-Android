package com.haset.hasetapp.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Doctor implements Serializable {
    private String doctorId;
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String specialty;
    private String about;
    private int experience; // years
    private float rating;
    private String profileImage;
    private List<String> availableDays; // ["Monday", "Tuesday", ...]
    private List<String> availableTimes; // ["09:00", "10:00", ...]
    private boolean isAvailable;
    
    // Enhanced fields for advanced search
    private String gender; // "male", "female", "other"
    private double consultationFee; // consultation fee amount
    private String location; // clinic address/location
    private List<String> languages; // languages spoken
    private List<String> insuranceProviders; // accepted insurance
    private String education; // medical education background
    private List<String> certifications; // professional certifications
    private double latitude; // for location-based search
    private double longitude; // for location-based search
    private int patientsTreated; // total patients treated
    private float responseTime; // average response time in hours
    private boolean verified; // verification status
    private boolean isOnline; // doctor online/offline status
    private String onlineStatus; // "online", "offline", "busy"
    private String regNo; // Medical Council Registration Number
    private long createdAt; // Creation timestamp for "New" label logic
    private boolean isDemo; // Demo doctor flag (free consultation)

    public Doctor() {
        this.availableDays = new ArrayList<>();
        this.availableTimes = new ArrayList<>();
        this.isAvailable = true;
        this.isOnline = false; // Default to offline
        this.onlineStatus = "offline";
        this.rating = 4.5f;
        this.isDemo = false;
    }

    public Doctor(String doctorId, String userId, String fullName, String specialty) {
        this();
        this.doctorId = doctorId;
        this.userId = userId;
        this.fullName = fullName;
        this.specialty = specialty;
    }

    // Getters and Setters
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public List<String> getAvailableDays() { return availableDays; }
    public void setAvailableDays(List<String> availableDays) { this.availableDays = availableDays; }

    public List<String> getAvailableTimes() { return availableTimes; }
    public void setAvailableTimes(List<String> availableTimes) { this.availableTimes = availableTimes; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    
    // Enhanced getters and setters
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    
    public List<String> getInsuranceProviders() { return insuranceProviders; }
    public void setInsuranceProviders(List<String> insuranceProviders) { this.insuranceProviders = insuranceProviders; }
    
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    
    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public int getPatientsTreated() { return patientsTreated; }
    public void setPatientsTreated(int patientsTreated) { this.patientsTreated = patientsTreated; }
    
    public float getResponseTime() { return responseTime; }
    public void setResponseTime(float responseTime) { this.responseTime = responseTime; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public String getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(String onlineStatus) { this.onlineStatus = onlineStatus; }
    
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public boolean isDemo() { return isDemo; }
    public void setDemo(boolean demo) { isDemo = demo; }
}
