package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.models.ChatMessage;
import com.haset.hasetapp.repositories.ChatRepository;

import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;
    private LiveData<List<ChatMessage>> messages;
    private final MutableLiveData<String> chatRoomId = new MutableLiveData<>();
    private final MutableLiveData<Double> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<String> uploadStatus = new MutableLiveData<>();
    private final MutableLiveData<AttachmentResult> uploadSuccess = new MutableLiveData<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository();
    }

    public void setChatRoomId(String id) {
        chatRoomId.setValue(id);
    }

    public LiveData<List<ChatMessage>> getMessages(String id) {
        if (messages == null) {
            messages = repository.loadMessages(id);
        }
        return messages;
    }

    public LiveData<List<ChatMessage>> getMessages(String id, String currentUserId, String otherUserId) {
        if (messages == null) {
            messages = repository.loadMessages(id, currentUserId, otherUserId);
        }
        return messages;
    }

    public String sendMessage(String id, ChatMessage message, String senderId, String receiverId, String senderName, String receiverName) {
        return repository.sendMessage(id, message, senderId, receiverId, senderName, receiverName);
    }

    public void updateMessageAttachment(String id, String messageId, String attachmentUrl, String status) {
        repository.updateMessageAttachment(id, messageId, attachmentUrl, status);
    }

    public void deleteMessage(String id, ChatMessage message, String currentUserId, String otherUserId) {
        repository.deleteMessage(id, message, currentUserId, otherUserId);
    }

    public void deleteMessages(String id, List<ChatMessage> messages,
                               String currentUserId, String otherUserId) {
        repository.deleteMessages(id, messages, currentUserId, otherUserId);
    }

    public void markAsRead(String id, String messageId) {
        repository.markMessageAsRead(id, messageId);
    }

    public void markAllAsRead(String id, String currentUserId) {
        repository.markAllMessagesAsRead(id, currentUserId);
    }

    public void markAllAsRead(String id, String currentUserId, String otherUserId) {
        repository.markAllMessagesAsRead(id, currentUserId, otherUserId);
    }

    public void setTyping(String id, String userId, boolean isTyping) {
        repository.updateTypingStatus(id, userId, isTyping);
    }

    public LiveData<Boolean> getTypingStatus(String id, String otherUserId) {
        return repository.getTypingStatus(id, otherUserId);
    }

    public LiveData<Double> getUploadProgress() { return uploadProgress; }
    public LiveData<String> getUploadStatus() { return uploadStatus; }
    public LiveData<AttachmentResult> getUploadSuccess() { return uploadSuccess; }

    public void uploadAttachment(android.content.Context context, android.net.Uri uri, String type, String fileName, long fileSize, String messageId) {
        uploadStatus.setValue("Uploading...");
        com.haset.hasetapp.utils.CloudinaryUploadHelper.uploadFile(context, uri, type, fileName, "chat_attachments",
            new com.haset.hasetapp.utils.CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {
                    uploadStatus.postValue("Starting upload...");
                }

                @Override
                public void onUploadProgress(double progress) {
                    uploadProgress.postValue(progress);
                }

                @Override
                public void onUploadSuccess(String downloadUrl, String uploadedFileName) {
                    uploadStatus.postValue("Upload successful");
                    // Pass the messageId back in the success result
                    uploadSuccess.postValue(new AttachmentResult(downloadUrl, fileName, fileSize, type, messageId));
                }

                @Override
                public void onUploadError(String error) {
                    uploadStatus.postValue("Upload failed: " + error);
                }
            });
    }

    public static class AttachmentResult {
        public final String downloadUrl;
        public final String fileName;
        public final long fileSize;
        public final String type;
        public final String messageId;

        public AttachmentResult(String downloadUrl, String fileName, long fileSize, String type, String messageId) {
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.type = type;
            this.messageId = messageId;
        }
    }
}
