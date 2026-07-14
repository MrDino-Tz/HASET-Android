package com.haset.hasetapp.models;

public class PaymentRequest {
    private String user_id;
    private String doctor_id;
    private double amount;
    private String provider;
    private String payment_account;

    public PaymentRequest(String userId, String doctorId, double amount, 
                         String provider, String paymentAccount) {
        this.user_id = userId;
        this.doctor_id = doctorId;
        this.amount = amount;
        this.provider = provider;
        this.payment_account = paymentAccount;
    }

    // Getters
    public String getUserId() { return user_id; }
    public String getDoctorId() { return doctor_id; }
    public double getAmount() { return amount; }
    public String getProvider() { return provider; }
    public String getPaymentAccount() { return payment_account; }

    // Setters
    public void setUserId(String userId) { this.user_id = userId; }
    public void setDoctorId(String doctorId) { this.doctor_id = doctorId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setPaymentAccount(String paymentAccount) { this.payment_account = paymentAccount; }
}
