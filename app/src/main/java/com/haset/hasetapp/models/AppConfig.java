package com.haset.hasetapp.models;

public class AppConfig {
    private boolean maintenanceMode;
    private int minVersionCode;
    private String updateUrl;
    private String maintenanceMessage;
    private double doctorRegistrationFee = 500.0;

    public AppConfig() {
        // Required for Firebase
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public int getMinVersionCode() {
        return minVersionCode;
    }

    public void setMinVersionCode(int minVersionCode) {
        this.minVersionCode = minVersionCode;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public void setMaintenanceMessage(String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    public double getDoctorRegistrationFee() {
        return doctorRegistrationFee;
    }

    public void setDoctorRegistrationFee(double doctorRegistrationFee) {
        this.doctorRegistrationFee = doctorRegistrationFee;
    }
}
