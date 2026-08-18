package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.haset.hasetapp.firebase.FirebaseHelper;
import com.haset.hasetapp.models.ChatMessage;
import com.haset.hasetapp.models.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.haset.hasetapp.utils.NotificationBadgeHelper;
import android.content.Context;
import android.util.Log;

public class ChatRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public static final Comparator<ChatMessage> MESSAGE_ORDER = (m1, m2) -> {
        // Primary sort: Timestamp (ascending - oldest first)
        long t1 = m1.getTimestamp();
        long t2 = m2.getTimestamp();
        if (t1 != t2) {
            return Long.compare(t1, t2);
        }
        
        // Secondary sort: Message ID as tie-breaker (ascending)
        // Firebase push IDs are lexicographically chronological, making this a very stable tie-breaker.
        String id1 = m1.getMessageId();
        String id2 = m2.getMessageId();
        if (id1 == null && id2 == null) return 0;
        if (id1 == null) return -1;
        if (id2 == null) return 1;
        return id1.compareTo(id2);
    };

    public LiveData<List<Conversation>> getConversations(String userId) {
        MutableLiveData<List<Conversation>> conversationsLiveData = new MutableLiveData<>();
        
        firebaseHelper.getUserConversationsRef().child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Conversation> conversations = new ArrayList<>();
                for (DataSnapshot conversationSnapshot : snapshot.getChildren()) {
                    Conversation conversation = conversationSnapshot.getValue(Conversation.class);
                    if (conversation != null) {
                        String otherUserId = conversationSnapshot.getKey();
                        conversation.setOtherUserId(otherUserId);
                        // Room ID logic is usually duplicated, could be helperized
                        conversation.setConversationId(generateChatRoomId(userId, otherUserId));
                        conversations.add(conversation);
                    }
                }
                Collections.sort(conversations, (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                conversationsLiveData.postValue(conversations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Resolve the LiveData instead of leaving the caller's shimmer
                // loading state hanging (denied reads, offline, etc.).
                Log.w("ChatRepository", "Failed to load conversations for " + userId + ": "
                        + error.getMessage());
                conversationsLiveData.postValue(new ArrayList<>());
            }
        });

        return conversationsLiveData;
    }

    public LiveData<List<ChatMessage>> loadMessages(String chatRoomId) {
        return loadMessages(chatRoomId, FirebaseAuth.getInstance().getUid(), null);
    }

    public LiveData<List<ChatMessage>> loadMessages(String chatRoomId, String currentUserId, String otherUserId) {
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
        Map<String, ChatMessage> messagesById = new HashMap<>();
        List<ChatMessage> messages = new ArrayList<>();

        if (currentUserId == null) {
            messagesLiveData.postValue(Collections.emptyList());
            return messagesLiveData;
        }

        Set<String> roomIds = getCandidateRoomIds(chatRoomId, currentUserId, otherUserId);

        ChildEventListener participantMessageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                upsertMessage(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                upsertMessage(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String messageId = snapshot.getKey();
                if (messageId != null) {
                    String sourceRoomId = snapshot.getRef().getParent() != null
                            ? snapshot.getRef().getParent().getKey() : chatRoomId;
                    DatabaseReference roomRef = firebaseHelper.getMessagesRef().child(sourceRoomId);
                    roomRef.child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot current) {
                            // Only remove after confirming the database node is gone.
                            if (!current.exists()) {
                                messagesById.remove(sourceRoomId + "/" + messageId);
                                messages.clear();
                                messages.addAll(messagesById.values());
                                Collections.sort(messages, MESSAGE_ORDER);
                                messagesLiveData.postValue(new ArrayList<>(messages));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w("ChatRepository", "Unable to verify message removal: " + messageId,
                                    error.toException());
                        }
                    });
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}

            private void upsertMessage(@NonNull DataSnapshot snapshot) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message == null) return;

                message.setMessageId(snapshot.getKey());
                String id = message.getMessageId();
                if (id == null) return;

                String sourceRoomId = snapshot.getRef().getParent() != null
                        ? snapshot.getRef().getParent().getKey() : chatRoomId;
                message.setSourceRoomId(sourceRoomId);
                String mapKey = sourceRoomId + "/" + id;

                messagesById.put(mapKey, message);
                messages.clear();
                messages.addAll(messagesById.values());
                Collections.sort(messages, MESSAGE_ORDER);
                messagesLiveData.postValue(new ArrayList<>(messages));

                // A recipient seeing the message in realtime is the delivery
                // acknowledgement. Do this before the chat screen marks it read.
                if (currentUserId.equals(message.getReceiverId())
                        && !message.isRead()
                        && "sent".equalsIgnoreCase(message.getMessageStatus())) {
                    markMessageAsDelivered(sourceRoomId, id);
                }
            }
        };

        for (String roomId : roomIds) {
            DatabaseReference roomRef = firebaseHelper.getMessagesRef().child(roomId);
            roomRef.orderByChild("senderId").equalTo(currentUserId)
                    .addChildEventListener(participantMessageListener);
            roomRef.orderByChild("receiverId").equalTo(currentUserId)
                    .addChildEventListener(participantMessageListener);
        }

        return messagesLiveData;
    }

    private Set<String> getCandidateRoomIds(String chatRoomId, String currentUserId, String otherUserId) {
        Set<String> roomIds = new LinkedHashSet<>();
        if (chatRoomId != null && !chatRoomId.trim().isEmpty()) {
            roomIds.add(chatRoomId);
        }
        if (currentUserId != null && otherUserId != null
                && !currentUserId.trim().isEmpty()
                && !otherUserId.trim().isEmpty()) {
            roomIds.add(generateChatRoomId(currentUserId, otherUserId));
            roomIds.add(currentUserId + "_" + otherUserId);
            roomIds.add(otherUserId + "_" + currentUserId);
        }
        return roomIds;
    }

    public String sendMessage(String chatRoomId, ChatMessage message, String senderId, String receiverId, String senderName, String receiverName) {
        String authenticatedSenderId = FirebaseAuth.getInstance().getUid();
        if (authenticatedSenderId == null || authenticatedSenderId.trim().isEmpty()) {
            Log.e("ChatRepository", "Message write blocked: no authenticated Firebase user");
            return null;
        }
        if (receiverId == null || receiverId.trim().isEmpty()
                || authenticatedSenderId.equals(receiverId)) {
            Log.e("ChatRepository", "Message write blocked: invalid receiver");
            return null;
        }

        String effectiveChatRoomId = generateChatRoomId(authenticatedSenderId, receiverId);
        message.setSenderId(authenticatedSenderId);
        message.setReceiverId(receiverId);

        DatabaseReference chatRef = firebaseHelper.getMessagesRef().child(effectiveChatRoomId).push();
        String messageId = chatRef.getKey();
        message.setMessageId(messageId);
        
        // Text messages should be durable in their first write.  A second status
        // write can be rejected by the immutable-message rules and cause the
        // optimistic local event to roll back and disappear.
        if ("text".equalsIgnoreCase(message.getMessageType())
                && (message.getMessageStatus() == null || "sending".equalsIgnoreCase(message.getMessageStatus()))) {
            message.setMessageStatus("sent");
        }

        Map<String, Object> firebaseMessage = toFirebaseMessageMap(message);
        chatRef.setValue(firebaseMessage).addOnSuccessListener(aVoid -> {
            updateConversation(authenticatedSenderId, receiverId, receiverName,
                    message.getMessage(), message.getTimestamp(), authenticatedSenderId);
            updateConversation(receiverId, authenticatedSenderId, senderName,
                    message.getMessage(), message.getTimestamp(), authenticatedSenderId);
            // A Cloud Function creates the receiver-owned durable notification.
            // The client cannot securely write into another user's node.
        }).addOnFailureListener(error ->
                Log.e("ChatRepository", "Message write failed for room=" + effectiveChatRoomId
                        + ", senderMatchesAuth=" + authenticatedSenderId.equals(message.getSenderId())
                        + ", fields=" + firebaseMessage.keySet()
                        + ", id=" + messageId + ": " + error.getMessage(), error));
        return messageId;
    }

    /**
     * Build the exact schema accepted by database.rules.json. Serializing the
     * model directly is unsafe because JavaBean boolean accessors can expose
     * additional property names (for example read/isRead), which are correctly
     * rejected by the rules' $other guard.
     */
    private Map<String, Object> toFirebaseMessageMap(ChatMessage message) {
        Map<String, Object> value = new HashMap<>();
        value.put("messageId", message.getMessageId());
        value.put("senderId", message.getSenderId());
        value.put("senderName", message.getSenderName() != null ? message.getSenderName() : "");
        value.put("receiverId", message.getReceiverId());
        value.put("receiverName", message.getReceiverName() != null ? message.getReceiverName() : "");
        value.put("message", message.getMessage() != null ? message.getMessage() : "");
        value.put("messageType", message.getMessageType() != null ? message.getMessageType() : "text");
        value.put("messageStatus", message.getMessageStatus() != null ? message.getMessageStatus() : "sent");
        value.put("timestamp", message.getTimestamp());
        value.put("isRead", message.isRead());
        value.put("deliveredTimestamp", message.getDeliveredTimestamp());
        value.put("readTimestamp", message.getReadTimestamp());

        putOptional(value, "attachmentUrl", message.getAttachmentUrl());
        putOptional(value, "attachmentFileName", message.getAttachmentFileName());
        putOptional(value, "attachmentSize", message.getAttachmentSize());
        putOptional(value, "attachmentDuration", message.getAttachmentDuration());
        putOptional(value, "replyToMessageId", message.getReplyToMessageId());
        putOptional(value, "replyToText", message.getReplyToText());
        putOptional(value, "replyToSenderName", message.getReplyToSenderName());
        putOptional(value, "prescriptionId", message.getPrescriptionId());
        if (message.getMetadata() != null) value.put("metadata", message.getMetadata());
        return value;
    }

    private void putOptional(Map<String, Object> value, String key, String fieldValue) {
        if (fieldValue != null) value.put(key, fieldValue);
    }

    public void updateMessageAttachment(String chatRoomId, String messageId, String attachmentUrl, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("attachmentUrl", attachmentUrl);
        updates.put("messageStatus", status);
        firebaseHelper.getMessagesRef().child(chatRoomId).child(messageId).updateChildren(updates);
    }

    public void deleteMessage(String chatRoomId, ChatMessage message, String currentUserId, String otherUserId) {
        if (chatRoomId == null || message == null || message.getMessageId() == null) return;

        String sourceRoomId = message.getSourceRoomId() != null ? message.getSourceRoomId() : chatRoomId;
        firebaseHelper.getMessagesRef().child(sourceRoomId).child(message.getMessageId()).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Find the new last message after deletion
                updateLastMessageAfterDeletion(sourceRoomId, currentUserId, otherUserId);
                Log.d("ChatRepository", "Message deleted successfully");
            });
    }

    public void deleteMessages(String chatRoomId, List<ChatMessage> selectedMessages,
                               String currentUserId, String otherUserId) {
        if (chatRoomId == null || selectedMessages == null || selectedMessages.isEmpty()) return;

        Map<String, Map<String, Object>> deletionsByRoom = new HashMap<>();
        for (ChatMessage message : selectedMessages) {
            if (message != null && message.getMessageId() != null
                    && currentUserId.equals(message.getSenderId())) {
                String sourceRoomId = message.getSourceRoomId() != null ? message.getSourceRoomId() : chatRoomId;
                Map<String, Object> roomDeletions = deletionsByRoom.get(sourceRoomId);
                if (roomDeletions == null) {
                    roomDeletions = new HashMap<>();
                    deletionsByRoom.put(sourceRoomId, roomDeletions);
                }
                roomDeletions.put(message.getMessageId(), null);
            }
        }
        if (deletionsByRoom.isEmpty()) return;

        for (Map.Entry<String, Map<String, Object>> entry : deletionsByRoom.entrySet()) {
            String sourceRoomId = entry.getKey();
            Map<String, Object> deletions = entry.getValue();
            firebaseHelper.getMessagesRef().child(sourceRoomId).updateChildren(deletions)
                    .addOnSuccessListener(ignored -> {
                        updateLastMessageAfterDeletion(sourceRoomId, currentUserId, otherUserId);
                        Log.d("ChatRepository", "Deleted " + deletions.size() + " selected messages");
                    })
                    .addOnFailureListener(error -> Log.e("ChatRepository",
                            "Failed to delete selected messages", error));
        }
    }
    
    private void updateLastMessageAfterDeletion(String chatRoomId, String currentUserId, String otherUserId) {
        // Room-level timestamp queries are intentionally blocked by the rules.
        // Merge the two participant-scoped queries that are permitted, then
        // calculate the remaining last message locally.
        Map<String, ChatMessage> remaining = new HashMap<>();
        int[] pendingQueries = {2};
        DatabaseReference roomRef = firebaseHelper.getMessagesRef().child(chatRoomId);

        ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                        ChatMessage message = msgSnapshot.getValue(ChatMessage.class);
                        if (message != null && msgSnapshot.getKey() != null) {
                            message.setMessageId(msgSnapshot.getKey());
                            remaining.put(msgSnapshot.getKey(), message);
                        }
                    }
                    if (--pendingQueries[0] == 0) {
                        finishLastMessageUpdate(remaining, currentUserId, otherUserId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatRepository", "Error updating last message after deletion", error.toException());
                    if (--pendingQueries[0] == 0) {
                        finishLastMessageUpdate(remaining, currentUserId, otherUserId);
                    }
                }
            };

        roomRef.orderByChild("senderId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(listener);
        roomRef.orderByChild("receiverId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(listener);
    }

    private void finishLastMessageUpdate(Map<String, ChatMessage> remaining,
                                         String currentUserId, String otherUserId) {
        ChatMessage lastMessage = null;
        for (ChatMessage candidate : remaining.values()) {
            if (lastMessage == null || MESSAGE_ORDER.compare(candidate, lastMessage) > 0) {
                lastMessage = candidate;
            }
        }
        if (lastMessage != null) {
            updateConversationLastMessage(currentUserId, otherUserId, lastMessage);
            updateConversationLastMessage(otherUserId, currentUserId, lastMessage);
        } else {
            clearConversationLastMessage(currentUserId, otherUserId);
            clearConversationLastMessage(otherUserId, currentUserId);
        }
    }
    
    private void updateConversationLastMessage(String userId, String otherUserId, ChatMessage message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getMessage());
        updates.put("lastMessageTimestamp", message.getTimestamp());
        updates.put("lastMessageSenderId", message.getSenderId());
        
        firebaseHelper.getUserConversationsRef().child(userId).child(otherUserId).updateChildren(updates);
    }
    
    private void clearConversationLastMessage(String userId, String otherUserId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", "");
        updates.put("lastMessageTimestamp", 0L);
        updates.put("lastMessageSenderId", "");
        
        firebaseHelper.getUserConversationsRef().child(userId).child(otherUserId).updateChildren(updates);
    }

    private void updateConversation(String userId, String otherUserId, String otherUserName, String lastMessage, long timestamp, String senderId) {
        Map<String, Object> update = new HashMap<>();
        update.put("otherUserId", otherUserId);
        update.put("otherUserName", otherUserName);
        update.put("lastMessage", lastMessage);
        update.put("lastMessageTimestamp", timestamp);
        update.put("lastMessageSenderId", senderId);
        update.put("isArchived", false);
        
        firebaseHelper.getUserConversationsRef().child(userId).child(otherUserId).updateChildren(update);
    }

    public void deleteConversation(String userId, String otherUserId) {
        firebaseHelper.getUserConversationsRef().child(userId).child(otherUserId).removeValue();
    }

    public void toggleArchiveConversation(String userId, String otherUserId, boolean isArchived) {
        firebaseHelper.getUserConversationsRef().child(userId).child(otherUserId).child("archived").setValue(isArchived);
    }

    public void markMessageAsRead(String chatRoomId, String messageId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("messageStatus", "read");
        updates.put("isRead", true);
        updates.put("readTimestamp", System.currentTimeMillis());

        firebaseHelper.getMessagesRef().child(chatRoomId).child(messageId).updateChildren(updates);
    }

    private void markMessageAsDelivered(String chatRoomId, String messageId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("messageStatus", "delivered");
        firebaseHelper.getMessagesRef().child(chatRoomId).child(messageId).updateChildren(updates)
                .addOnFailureListener(error -> Log.w("ChatRepository",
                        "Unable to mark message delivered: " + messageId, error));
    }

    public void markAllMessagesAsRead(String chatRoomId, String currentUserId) {
        markAllMessagesAsRead(chatRoomId, currentUserId, null);
    }

    public void markAllMessagesAsRead(String chatRoomId, String currentUserId, String otherUserId) {
        for (String roomId : getCandidateRoomIds(chatRoomId, currentUserId, otherUserId)) {
            firebaseHelper.getMessagesRef().child(roomId)
                .orderByChild("receiverId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                    if (message != null && message.getReceiverId().equals(currentUserId) && !message.isRead()) {
                        markMessageAsRead(roomId, messageSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        }
    }

    public void updateTypingStatus(String chatRoomId, String userId, boolean isTyping) {
        firebaseHelper.getDatabaseReference().child("typing").child(chatRoomId).child(userId).setValue(isTyping);
    }

    public LiveData<Boolean> getTypingStatus(String chatRoomId, String otherUserId) {
        MutableLiveData<Boolean> typingLiveData = new MutableLiveData<>();
        if (chatRoomId == null || otherUserId == null) {
            Log.w("ChatRepository", "getTypingStatus: chatRoomId or otherUserId is null");
            typingLiveData.postValue(false);
            return typingLiveData;
        }
        firebaseHelper.getDatabaseReference().child("typing").child(chatRoomId).child(otherUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        typingLiveData.postValue(snapshot.getValue(Boolean.class));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        return typingLiveData;
    }

    public void syncUnreadCounts(Context context, String userId, List<Conversation> conversations,
                                 Runnable onCountUpdated) {
        if (userId == null || conversations == null) return;
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(context);
        DatabaseReference messagesRef = firebaseHelper.getMessagesRef();
        
        for (Conversation conversation : conversations) {
            String chatRoomId = conversation.getConversationId();
            messagesRef.child(chatRoomId)
                    .orderByChild("receiverId").equalTo(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int unreadCount = 0;
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                        if (message != null && message.getReceiverId().equals(userId) && !message.isRead()) {
                            unreadCount++;
                        }
                    }
                    badgeHelper.setConversationUnreadCount(chatRoomId, unreadCount);
                    
                    // Recalculate total
                    recalculateTotalUnread(context, conversations);
                    if (onCountUpdated != null) onCountUpdated.run();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatRepository", "Failed to sync unread for room: " + chatRoomId, error.toException());
                }
            });
        }
    }

    private void recalculateTotalUnread(Context context, List<Conversation> conversations) {
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(context);
        int total = 0;
        for (Conversation conversation : conversations) {
            total += badgeHelper.getConversationUnreadCount(conversation.getConversationId());
        }
        badgeHelper.setTotalUnreadCount(total);
    }

    private String generateChatRoomId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }
}
