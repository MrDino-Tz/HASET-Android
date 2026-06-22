package com.haset.hasetapp.models;

public class CallSignal {
    private String senderId;
    private String senderName;
    private String receiverId;
    private String type; // invite, accept, decline, end
    private String roomUrl;
    private long timestamp;

    public CallSignal() {
    }

    public CallSignal(String senderId, String senderName, String receiverId, String type, String roomUrl) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.type = type;
        this.roomUrl = roomUrl;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
