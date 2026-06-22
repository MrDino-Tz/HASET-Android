package com.haset.hasetapp.models;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName; // Add receiverName field
    private String message;
    private String attachmentUrl; // New: URL for attachment
    private String attachmentFileName; // New: File name for attachment
    private String attachmentSize; // New: Size of attachment (formatted)
    private String attachmentDuration; // Duration for audio messages
    private String messageType; // New: Type of message (text, image, document)
    private long timestamp;
    private boolean isRead;
    private String messageStatus; // "sending", "sent", "read"
    private long deliveredTimestamp;
    private long readTimestamp;

    // Reply fields
    private String replyToMessageId;
    private String replyToText;
    private String replyToSenderName;

    // Prescription and System fields
    private String prescriptionId;
    private java.util.Map<String, Object> metadata;

    public String getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(String attachmentSize) { this.attachmentSize = attachmentSize; }
    
    public String getAttachmentDuration() { return attachmentDuration; }
    public void setAttachmentDuration(String attachmentDuration) { this.attachmentDuration = attachmentDuration; }

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.messageType = "text"; // Default message type
        this.messageStatus = "sending"; // Default status (will be updated to "sent" on success)
        this.deliveredTimestamp = 0;
        this.readTimestamp = 0;
    }

    public ChatMessage(String senderId, String receiverId, String message) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
    }

    // Add new constructor for text messages
    public ChatMessage(String messageId, String senderId, String senderName, String receiverId, String receiverName, String message, long timestamp, boolean isRead) {
        this(messageId, senderId, senderName, receiverId, receiverName, message, null, null, timestamp, isRead, "text");
    }

    // New constructor for all fields, including attachment and messageType
    public ChatMessage(String messageId, String senderId, String senderName, String receiverId, String receiverName, String message, String attachmentUrl, String attachmentFileName, long timestamp, boolean isRead, String messageType) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.message = message;
        this.attachmentUrl = attachmentUrl;
        this.attachmentFileName = attachmentFileName;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.messageType = messageType;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    // Helper methods for Firebase mapping properties that start with "is"
    public boolean getIsRead() { return isRead; }
    public void setIsRead(boolean isRead) { this.isRead = isRead; }

    // Getters and Setters for new fields
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentFileName() { return attachmentFileName; }
    public void setAttachmentFileName(String attachmentFileName) { this.attachmentFileName = attachmentFileName; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMessageStatus() { return messageStatus; }
    public void setMessageStatus(String messageStatus) { this.messageStatus = messageStatus; }

    public long getDeliveredTimestamp() { return deliveredTimestamp; }
    public void setDeliveredTimestamp(long deliveredTimestamp) { this.deliveredTimestamp = deliveredTimestamp; }

    public long getReadTimestamp() { return readTimestamp; }
    public void setReadTimestamp(long readTimestamp) { this.readTimestamp = readTimestamp; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToText() { return replyToText; }
    public void setReplyToText(String replyToText) { this.replyToText = replyToText; }

    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
}
