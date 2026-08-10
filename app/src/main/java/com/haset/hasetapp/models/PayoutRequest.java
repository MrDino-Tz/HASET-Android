package com.haset.hasetapp.models;

public class PayoutRequest {
    private String request_id;
    private String doctor_id;
    private double amount;
    private String phone_number;
    private String provider;
    private String admin_id;

    public PayoutRequest(String requestId, String doctorId, double amount, String phoneNumber, String provider, String adminId) {
        this.request_id = requestId;
        this.doctor_id = doctorId;
        this.amount = amount;
        this.phone_number = phoneNumber;
        this.provider = provider;
        this.admin_id = adminId;
    }

    // Getters
    public String getRequestId() { return request_id; }
    public String getDoctorId() { return doctor_id; }
    public double getAmount() { return amount; }
    public String getPhoneNumber() { return phone_number; }
    public String getProvider() { return provider; }
    public String getAdminId() { return admin_id; }

    // Setters
    public void setRequestId(String requestId) { this.request_id = requestId; }
    public void setDoctorId(String doctorId) { this.doctor_id = doctorId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setPhoneNumber(String phoneNumber) { this.phone_number = phoneNumber; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setAdminId(String adminId) { this.admin_id = adminId; }
}
