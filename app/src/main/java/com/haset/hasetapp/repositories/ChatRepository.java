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
import com.haset.hasetapp.firebase.FirebaseHelper;
import com.haset.hasetapp.models.ChatMessage;
import com.haset.hasetapp.models.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                // Handle error
            }
        });

        return conversationsLiveData;
    }

    public LiveData<List<ChatMessage>> loadMessages(String chatRoomId) {
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
        Map<String, ChatMessage> messagesById = new HashMap<>();
        List<ChatMessage> messages = new ArrayList<>();

        Query orderedMessages = firebaseHelper.getMessagesRef()
                .child(chatRoomId)
                .orderByChild("timestamp");

        orderedMessages.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    String id = message.getMessageId();
                    if (id == null) return;

                    messagesById.put(id, message);
                    messages.clear();
                    messages.addAll(messagesById.values());
                    Collections.sort(messages, MESSAGE_ORDER);
                    messagesLiveData.postValue(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage updatedMessage = snapshot.getValue(ChatMessage.class);
                if (updatedMessage != null) {
                    updatedMessage.setMessageId(snapshot.getKey());
                    String id = updatedMessage.getMessageId();
                    if (id == null) return;

                    messagesById.put(id, updatedMessage);
                    messages.clear();
                    messages.addAll(messagesById.values());
                    Collections.sort(messages, MESSAGE_ORDER);
                    messagesLiveData.postValue(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String messageId = snapshot.getKey();
                if (messageId != null) {
                    messagesById.remove(messageId);
                    messages.clear();
                    messages.addAll(messagesById.values());
                    Collections.sort(messages, MESSAGE_ORDER);
                    messagesLiveData.postValue(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return messagesLiveData;
    }

    public String sendMessage(String chatRoomId, ChatMessage message, String senderId, String receiverId, String senderName, String receiverName) {
        DatabaseReference chatRef = firebaseHelper.getMessagesRef().child(chatRoomId).push();
        String messageId = chatRef.getKey();
        message.setMessageId(messageId);
        
        // Don't overwrite it if it's already set to something like "uploading"
        if (message.getMessageStatus() == null) {
            message.setMessageStatus("sending");
        }

        chatRef.setValue(message).addOnSuccessListener(aVoid -> {
            if ("sending".equals(message.getMessageStatus())) {
                chatRef.child("messageStatus").setValue("sent");
            }
            updateConversation(senderId, receiverId, receiverName, message.getMessage(), message.getTimestamp(), senderId);
            updateConversation(receiverId, senderId, senderName, message.getMessage(), message.getTimestamp(), senderId);
        });
        return messageId;
    }

    public void updateMessageAttachment(String chatRoomId, String messageId, String attachmentUrl, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("attachmentUrl", attachmentUrl);
        updates.put("messageStatus", status);
        firebaseHelper.getMessagesRef().child(chatRoomId).child(messageId).updateChildren(updates);
    }

    public void deleteMessage(String chatRoomId, ChatMessage message, String currentUserId, String otherUserId) {
        if (chatRoomId == null || message == null || message.getMessageId() == null) return;
        
        firebaseHelper.getMessagesRef().child(chatRoomId).child(message.getMessageId()).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Find the new last message after deletion
                updateLastMessageAfterDeletion(chatRoomId, currentUserId, otherUserId);
                Log.d("ChatRepository", "Message deleted successfully");
            });
    }
    
    private void updateLastMessageAfterDeletion(String chatRoomId, String currentUserId, String otherUserId) {
        // Get all remaining messages and find the last one
        firebaseHelper.getMessagesRef().child(chatRoomId)
            .orderByChild("timestamp")
            .limitToLast(1)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            ChatMessage lastMsg = msgSnapshot.getValue(ChatMessage.class);
                            if (lastMsg != null) {
                                // Update conversation for both users
                                updateConversationLastMessage(currentUserId, otherUserId, lastMsg);
                                updateConversationLastMessage(otherUserId, currentUserId, lastMsg);
                            }
                        }
                    } else {
                        // No messages left, clear last message
                        clearConversationLastMessage(currentUserId, otherUserId);
                        clearConversationLastMessage(otherUserId, currentUserId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatRepository", "Error updating last message after deletion", error.toException());
                }
            });
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

    public void markAllMessagesAsRead(String chatRoomId, String currentUserId) {
        firebaseHelper.getMessagesRef().child(chatRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                    if (message != null && message.getReceiverId().equals(currentUserId) && !message.isRead()) {
                        markMessageAsRead(chatRoomId, messageSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    public void syncUnreadCounts(Context context, String userId, List<Conversation> conversations) {
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(context);
        DatabaseReference messagesRef = firebaseHelper.getMessagesRef();
        
        for (Conversation conversation : conversations) {
            String chatRoomId = conversation.getConversationId();
            messagesRef.child(chatRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
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
