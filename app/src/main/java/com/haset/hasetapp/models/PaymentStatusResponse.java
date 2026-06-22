package com.haset.hasetapp.models;

public class PaymentStatusResponse {
    private String status;
    private String message;
    private Transaction transaction;

    public static class Transaction {
        private int id;
        private String status;
        private String payment_status;
        private double amount;
        private String currency;
        private String provider;
        private String created_at;
        private String updated_at;
        private String external_reference;

        // Getters
        public int getId() { return id; }
        public String getStatus() { return status; }
        public String getPaymentStatus() { return payment_status; }
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getProvider() { return provider; }
        public String getCreatedAt() { return created_at; }
        public String getUpdatedAt() { return updated_at; }
        public String getExternalReference() { return external_reference; }

        // Setters
        public void setId(int id) { this.id = id; }
        public void setStatus(String status) { this.status = status; }
        public void setPaymentStatus(String paymentStatus) { this.payment_status = paymentStatus; }
        public void setAmount(double amount) { this.amount = amount; }
        public void setCurrency(String currency) { this.currency = currency; }
        public void setProvider(String provider) { this.provider = provider; }
        public void setCreatedAt(String createdAt) { this.created_at = createdAt; }
        public void setUpdatedAt(String updatedAt) { this.updated_at = updatedAt; }
        public void setExternalReference(String externalReference) { this.external_reference = externalReference; }
        
        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }
        
        public boolean isFailed() {
            return "failed".equalsIgnoreCase(status) || 
                   "cancelled".equalsIgnoreCase(status) ||
                   "expired".equalsIgnoreCase(status) ||
                   "declined".equalsIgnoreCase(status);
        }
        
        public boolean isProcessing() {
            return "processing".equalsIgnoreCase(status) || 
                   "pending".equalsIgnoreCase(status);
        }
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Transaction getTransaction() { return transaction; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
