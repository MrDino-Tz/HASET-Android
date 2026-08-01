package com.haset.hasetapp.models;

public class PaymentResponse {
    private String status;
    private String message;
    private int transaction_id;
    private String order_reference;
    private String payment_status;
    private String payment_channel;
    private String reference;
    private String payment_url;
    private Object data;

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getTransactionId() { return transaction_id; }
    public String getOrderReference() { return order_reference; }
    public String getPaymentStatus() { return payment_status; }
    public String getPaymentChannel() { return payment_channel; }
    public String getReference() { return reference; }
    public String getPaymentUrl() { return payment_url; }
    public Object getData() { return data; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setTransactionId(int transactionId) { this.transaction_id = transactionId; }
    public void setOrderReference(String orderReference) { this.order_reference = orderReference; }
    public void setPaymentStatus(String paymentStatus) { this.payment_status = paymentStatus; }
    public void setPaymentChannel(String paymentChannel) { this.payment_channel = paymentChannel; }
    public void setReference(String reference) { this.reference = reference; }
    public void setPaymentUrl(String paymentUrl) { this.payment_url = paymentUrl; }
    public void setData(Object data) { this.data = data; }
    
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
