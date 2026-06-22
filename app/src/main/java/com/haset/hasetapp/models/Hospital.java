package com.haset.hasetapp.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Hospital implements Serializable {
    private String hospitalId;
    private String name;
    private String type; // "General", "Specialized", "Clinic"
    private String description;
    
    // Location Data
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    
    // Contact Data
    private String email;
    private String phone;
    private String emergencyPhone;
    
    // Features & Capabilities
    private String imageUrl;
    private List<String> services; // ["Emergency", "Maternity", "Surgery", "X-Ray"]
    private List<String> supportedInsurance;
    
    // Status
    private boolean active;
    private long createdAt;

    public Hospital() {
        this.services = new ArrayList<>();
        this.supportedInsurance = new ArrayList<>();
        this.active = true;
    }

    public Hospital(String hospitalId, String name, String address) {
        this();
        this.hospitalId = hospitalId;
        this.name = name;
        this.address = address;
    }

    // Getters and Setters
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }

    public List<String> getSupportedInsurance() { return supportedInsurance; }
    public void setSupportedInsurance(List<String> supportedInsurance) { this.supportedInsurance = supportedInsurance; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
