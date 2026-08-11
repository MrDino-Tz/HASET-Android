package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "withdrawal_requests")
public class WithdrawalRequest {
    
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_COMPLETED = "completed";
    
    public static final String METHOD_MOBILE_MONEY = "mobile";
    public static final String METHOD_BANK = "bank";
    
    @PrimaryKey
    @NonNull
    private String requestId;
    private String doctorId;
    private String doctorName;
    private double amount;
    @Ignore
    private double feeAmount;
    private String method; // mobile or bank
    private String accountNumber; // mobile number or bank account
    private String accountName; // bank account name (optional)
    private String bankName; // bank name (optional)
    private String status; // pending, approved, rejected, completed
    private long requestedAt;
    private long processedAt;
    private String processedBy; // admin user id who processed
    private String rejectionReason;
    private String notes;
    
    public WithdrawalRequest() {
    }
    
    @Ignore
    public WithdrawalRequest(@NonNull String requestId, String doctorId, String doctorName, 
                            double amount, String method, String accountNumber) {
        this.requestId = requestId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.amount = amount;
        this.method = method;
        this.accountNumber = accountNumber;
        this.status = STATUS_PENDING;
        this.requestedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(@NonNull String requestId) {
        this.requestId = requestId;
    }
    
    public String getDoctorId() {
        return doctorId;
    }
    
    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }
    
    public String getDoctorName() {
        return doctorName;
    }
    
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getFeeAmount() { return feeAmount; }
    public void setFeeAmount(double feeAmount) { this.feeAmount = feeAmount; }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public long getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(long processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getProcessedBy() {
        return processedBy;
    }
    
    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @com.google.firebase.database.Exclude
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
    
    @com.google.firebase.database.Exclude
    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }
    
    @com.google.firebase.database.Exclude
    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }
}
