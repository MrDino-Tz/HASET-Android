package com.haset.hasetapp.models;

public class PaymentRequest {
    private String user_id;
    private String doctor_id;
    private String consultation_id;
    private long amount;
    private String payment_method;
    private String provider;
    private String payment_account;

    public PaymentRequest(String userId, String doctorId, String consultationId, double amount,
                         String provider, String paymentAccount) {
        this.user_id = userId;
        this.doctor_id = doctorId;
        this.consultation_id = consultationId;
        this.amount = Math.round(amount);
        this.payment_method = "mobile_money";
        this.provider = provider;
        this.payment_account = paymentAccount;
    }

    // Getters
    public String getUserId() { return user_id; }
    public String getDoctorId() { return doctor_id; }
    public String getConsultationId() { return consultation_id; }
    public long getAmount() { return amount; }
    public String getPaymentMethod() { return payment_method; }
    public String getProvider() { return provider; }
    public String getPaymentAccount() { return payment_account; }

    // Setters
    public void setUserId(String userId) { this.user_id = userId; }
    public void setDoctorId(String doctorId) { this.doctor_id = doctorId; }
    public void setConsultationId(String consultationId) { this.consultation_id = consultationId; }
    public void setAmount(double amount) { this.amount = Math.round(amount); }
    public void setPaymentMethod(String paymentMethod) { this.payment_method = paymentMethod; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setPaymentAccount(String paymentAccount) { this.payment_account = paymentAccount; }
}
