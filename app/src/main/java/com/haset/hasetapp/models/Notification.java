package com.haset.hasetapp.models;

import java.util.Map;

public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private long timestamp;
    private boolean isRead;
    private Map<String, Object> data;

    public Notification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    public Notification(String userId, String title, String message, String type, String referenceId) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.referenceId = referenceId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
