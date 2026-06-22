package com.haset.hasetapp.models;

import java.io.Serializable;
import java.util.UUID;

public class Service implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String serviceId;
    private String serviceName;
    private double appointmentFee;  // Full fee set by doctor
    private int patientPercentage; // Percentage patient should pay (0-100)
    private double patientPayAmount; // Calculated amount
    private String doctorId;
    private String patientId;
    private String appointmentId;
    private boolean isPaid;
    private long createdAt;
    private String paymentStatus; // "pending", "paid"
    
    public Service() {
        this.serviceId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.isPaid = false;
        this.paymentStatus = "pending";
    }
    
    // Calculate patient pay amount
    public void calculatePatientAmount() {
        this.patientPayAmount = (appointmentFee * patientPercentage) / 100.0;
    }
    
    // Getters and Setters
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public double getAppointmentFee() { return appointmentFee; }
    public void setAppointmentFee(double appointmentFee) { 
        this.appointmentFee = appointmentFee;
        calculatePatientAmount();
    }
    
    public int getPatientPercentage() { return patientPercentage; }
    public void setPatientPercentage(int patientPercentage) { 
        this.patientPercentage = patientPercentage;
        calculatePatientAmount();
    }
    
    public double getPatientPayAmount() { return patientPayAmount; }
    public void setPatientPayAmount(double patientPayAmount) { this.patientPayAmount = patientPayAmount; }
    
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
