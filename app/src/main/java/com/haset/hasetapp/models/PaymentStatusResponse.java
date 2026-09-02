package com.haset.hasetapp.models;

import com.google.gson.Gson;

import java.util.Locale;
import java.util.Map;

public class PaymentStatusResponse {
    private String status;
    private String message;
    private Transaction transaction;
    private Object data;

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
            return isSettledPayment(status, payment_status);
        }

        public boolean isFailed() {
            return isFailedPayment(status, payment_status);
        }

        public boolean isProcessing() {
            if (isSuccess() || isFailed()) {
                return false;
            }
            if (payment_status != null && !payment_status.trim().isEmpty()) {
                return isPendingPayment(payment_status) || !isSettledPayment(null, payment_status);
            }
            return isPendingPayment(status);
        }
    }

    private static boolean isSettledPayment(String status, String paymentStatus) {
        if (paymentStatus != null && !paymentStatus.trim().isEmpty()) {
            if (isPendingPayment(paymentStatus) || isFailedPayment(null, paymentStatus)) {
                return false;
            }
            return isSettledValue(paymentStatus);
        }
        return isSettledValue(status);
    }

    private static boolean isFailedPayment(String status, String paymentStatus) {
        return isFailedValue(status) || isFailedValue(paymentStatus);
    }

    private static boolean isSettledValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return "success".equals(normalized)
                || "completed".equals(normalized)
                || "paid".equals(normalized);
    }

    private static boolean isPendingPayment(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return "processing".equals(normalized)
                || "pending".equals(normalized)
                || "initiated".equals(normalized)
                || "submitted".equals(normalized);
    }

    private static boolean isFailedValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return "failed".equals(normalized)
                || "cancelled".equals(normalized)
                || "canceled".equals(normalized)
                || "expired".equals(normalized)
                || "declined".equals(normalized)
                || "rejected".equals(normalized)
                || "voided".equals(normalized);
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Transaction getTransaction() {
        if (transaction != null) {
            return transaction;
        }
        if (data instanceof Map<?, ?>) {
            Map<?, ?> payload = (Map<?, ?>) data;
            Object nestedTransaction = payload.get("transaction");
            if (nestedTransaction != null) {
                try {
                    return new Gson().fromJson(new Gson().toJson(nestedTransaction), Transaction.class);
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            if (!payload.containsKey("id")
                    && !payload.containsKey("transaction_id")
                    && !payload.containsKey("payment_status")) {
                return null;
            }
            try {
                return new Gson().fromJson(new Gson().toJson(data), Transaction.class);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public void setData(Object data) { this.data = data; }
    
    public boolean isSuccess() {
        Transaction resolved = getTransaction();
        return resolved != null && resolved.isSuccess();
    }
}
