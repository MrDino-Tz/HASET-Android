package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "doctor_wallets")
public class DoctorWalletEntity {
    @PrimaryKey
    @NonNull
    private String doctorId;
    private String doctorName; // Cached doctor name for display
    private String regNo; // Doctor's registration number
    private double balance; // Current wallet balance in TZS
    private double totalEarnings; // Total earnings ever received
    private long lastUpdated;
    @androidx.room.Ignore private boolean mobileMoneyAvailable;
    @androidx.room.Ignore private boolean bankAvailable;
    @androidx.room.Ignore private String mobileMoneyLabel;
    @androidx.room.Ignore private String bankLabel;

    public DoctorWalletEntity() {
    }

    @androidx.room.Ignore
    public DoctorWalletEntity(@NonNull String doctorId, double balance) {
        this.doctorId = doctorId;
        this.balance = balance;
        this.totalEarnings = balance;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    @NonNull
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(@NonNull String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public boolean isMobileMoneyAvailable() { return mobileMoneyAvailable; }
    public void setMobileMoneyAvailable(boolean value) { mobileMoneyAvailable = value; }
    public boolean isBankAvailable() { return bankAvailable; }
    public void setBankAvailable(boolean value) { bankAvailable = value; }
    public String getMobileMoneyLabel() { return mobileMoneyLabel; }
    public void setMobileMoneyLabel(String value) { mobileMoneyLabel = value; }
    public String getBankLabel() { return bankLabel; }
    public void setBankLabel(String value) { bankLabel = value; }
}
