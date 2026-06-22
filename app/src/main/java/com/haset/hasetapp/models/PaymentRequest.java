package com.haset.hasetapp.models;

public class PaymentRequest {
    private String user_id;
    private String doctor_id;
    private double amount;
    private String provider;
    private String payment_account;
    private String webhook_url;
    private String buyer_email;
    private String buyer_name;
    private String buyer_phone;
    private String order_id;

    public PaymentRequest(String userId, String doctorId, double amount, 
                         String provider, String paymentAccount) {
        this.user_id = userId;
        this.doctor_id = doctorId;
        this.amount = amount;
        this.provider = provider;
        this.payment_account = paymentAccount;
        this.webhook_url = com.haset.hasetapp.utils.Constants.PAYMENT_WEBHOOK_URL;
    }

    // Getters
    public String getUserId() { return user_id; }
    public String getDoctorId() { return doctor_id; }
    public double getAmount() { return amount; }
    public String getProvider() { return provider; }
    public String getPaymentAccount() { return payment_account; }
    public String getWebhookUrl() { return webhook_url; }
    public String getBuyerEmail() { return buyer_email; }
    public String getBuyerName() { return buyer_name; }
    public String getBuyerPhone() { return buyer_phone; }
    public String getOrderId() { return order_id; }

    // Setters
    public void setUserId(String userId) { this.user_id = userId; }
    public void setDoctorId(String doctorId) { this.doctor_id = doctorId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setPaymentAccount(String paymentAccount) { this.payment_account = paymentAccount; }
    public void setWebhookUrl(String webhookUrl) { this.webhook_url = webhookUrl; }
    public void setBuyerEmail(String buyerEmail) { this.buyer_email = buyerEmail; }
    public void setBuyerName(String buyerName) { this.buyer_name = buyerName; }
    public void setBuyerPhone(String buyerPhone) { this.buyer_phone = buyerPhone; }
    public void setOrderId(String orderId) { this.order_id = orderId; }
}
