# Chat System Analysis

## Overview
The HASET App implements a real-time chat system using Firebase Realtime Database for messaging and Firebase Storage for file attachments. The system supports text messages, documents, audio, and video files.

---

## 1. Architecture & Components

### 1.1 Core Components
- **ChatActivity**: Main chat interface for sending/receiving messages
- **ChatListFragment**: Displays list of conversations (currently placeholder)
- **InboxActivity**: Shows all user conversations with inbox/archive tabs
- **ChatAdapter**: Handles message display in RecyclerView
- **ConversationAdapter**: Handles conversation list display
- **ChatMessage**: Data model for individual messages
- **Conversation**: Data model for conversation metadata

### 1.2 Supporting Components
- **FirebaseHelper**: Firebase database reference management
- **NotificationBadgeHelper**: Manages unread message counts
- **FileUploadHelper**: Handles file uploads to Firebase Storage
- **FileAttachmentBottomSheet**: UI for selecting file types
- **ChatMoreOptionsBottomSheet**: Menu for chat actions

---

## 2. Firebase Database Structure

### 2.1 Database Paths (Constants.java)
```java
MESSAGES_PATH = "messages"
USER_CONVERSATIONS_PATH = "user_conversations"
```

### 2.2 Messages Structure
```
messages/
  └── {chatRoomId}/           // e.g., "userId1_userId2"
      └── {messageId}/        // Auto-generated push key
          ├── messageId
          ├── senderId
          ├── senderName
          ├── receiverId
          ├── receiverName
          ├── message
          ├── attachmentUrl      // For file attachments
          ├── attachmentFileName // For file attachments
          ├── messageType        // "text", "document", "audio", "video"
          ├── timestamp
          └── isRead
```

### 2.3 Conversations Structure
```
user_conversations/
  └── {userId}/
      └── {otherUserId}/       // Other participant's ID
          ├── otherUserId
          ├── otherUserName
          ├── lastMessage
          └── lastMessageTimestamp
```

### 2.4 Chat Room ID Generation
**Location**: `ChatActivity.generateChatRoomId()`
```java
private String generateChatRoomId(String userId1, String userId2) {
    return userId1.compareTo(userId2) < 0 ? 
            userId1 + "_" + userId2 : userId2 + "_" + userId1;
}
```
**Logic**: Ensures consistent room ID regardless of user order (alphabetical sorting)

---

## 3. Message Flow

### 3.1 Sending a Message
**Location**: `ChatActivity.sendMessage()`

**Process**:
1. Validate message text (non-empty)
2. Generate unique message ID using `push().getKey()`
3. Create `ChatMessage` object with:
   - `senderId`: Current user ID
   - `receiverId`: Chat partner ID
   - `senderName`: From PreferenceManager
   - `receiverName`: From intent extra
   - `message`: Text content
   - `timestamp`: System.currentTimeMillis()
4. Save to Firebase: `messages/{chatRoomId}/{messageId}`
5. Update conversations for both users
6. Clear input field on success

**Code Flow**:
```java
firebaseHelper.getMessagesRef()
    .child(chatRoomId)
    .child(messageId)
    .setValue(message)
    .addOnSuccessListener(...)
```

### 3.2 Receiving Messages
**Location**: `ChatActivity.loadMessages()`

**Process**:
1. Attach `ChildEventListener` to `messages/{chatRoomId}`
2. On `onChildAdded`: Parse message and add to adapter
3. Auto-scroll to latest message
4. Real-time updates via Firebase listeners

**Code Flow**:
```java
firebaseHelper.getMessagesRef()
    .child(chatRoomId)
    .addChildEventListener(new ChildEventListener() {
        onChildAdded(...) // New message received
    })
```

### 3.3 Conversation Updates
**Location**: `ChatActivity.updateConversation()`

**Process**:
1. Update both users' conversation records
2. Store: `otherUserId`, `otherUserName`, `lastMessage`, `lastMessageTimestamp`
3. Path: `user_conversations/{userId}/{otherUserId}`

**Note**: Currently doesn't track unread count in conversation (commented TODO)

---

## 4. Message Types & Attachments

### 4.1 Supported Message Types
- **Text**: Default message type
- **Document**: PDF, DOC, DOCX, TXT, XLS, XLSX
- **Audio**: MP3, WAV, AAC, M4A, OGG
- **Video**: MP4, AVI, MOV, MKV, WMV, 3GP

### 4.2 File Upload Process
**Location**: `FileUploadHelper.uploadFile()`

**Process**:
1. Generate unique filename: `{fileType}_{timestamp}_{originalName}`
2. Upload to Firebase Storage: `chat_attachments/{uniqueFileName}`
3. Track upload progress
4. Get download URL on completion
5. Create ChatMessage with attachment details

**Storage Path**: `chat_attachments/` in Firebase Storage

**Current Status**: File selection implemented, upload logic exists but not fully integrated in `ChatActivity.sendFileMessage()`

### 4.3 Message Type Detection
**Location**: `ChatAdapter.getItemViewType()`

**Logic**:
- Checks `message.getMessageType()`
- Returns appropriate view type: `VIEW_TYPE_TEXT`, `VIEW_TYPE_DOCUMENT`, `VIEW_TYPE_AUDIO`, `VIEW_TYPE_VIDEO`
- Defaults to `VIEW_TYPE_TEXT` if null or unknown

---

## 5. UI Components & Adapters

### 5.1 ChatAdapter
**Features**:
- Multiple view types for different message formats
- Date separators (shows date when day changes)
- Timestamp display (inline for short messages, below for long messages)
- Message alignment (right for sent, left for received)
- Long message detection (>50 characters)

**View Types**:
- `item_chat_message.xml`: Text messages
- `item_chat_document.xml`: Document attachments
- `item_chat_audio.xml`: Audio files
- `item_chat_video.xml`: Video files

### 5.2 ConversationAdapter
**Features**:
- Displays conversation list
- Shows profile photo, name, last message, timestamp
- Unread count badge (currently using test data)
- Click listener to open ChatActivity

**Current Issue**: Unread count uses test data (`position + 1`) instead of actual Firebase data

---

## 6. Notification Badge System

### 6.1 NotificationBadgeHelper
**Location**: `NotificationBadgeHelper.java`

**Features**:
- Manages unread counts per conversation
- Tracks total unread messages
- SharedPreferences-based storage
- Badge display utilities

**Storage Keys**:
- `total_unread_messages`: Total count
- `unread_{conversationId}`: Per-conversation count

**Methods**:
- `markConversationAsRead()`: Clears badge when chat opened
- `incrementConversationUnread()`: Increments on new message
- `updateMessageBadge()`: Updates UI badge view
- `updateConversationBadge()`: Updates conversation list badge

### 6.2 Badge Clearing
**Location**: `ChatActivity.clearNotificationBadge()`

**Process**: Called when ChatActivity opens, marks conversation as read

---

## 7. Chat List & Inbox

### 7.1 InboxActivity
**Features**:
- Tab layout (Inbox/Archive)
- Loads conversations from `user_conversations/{userId}`
- Sorted by `lastMessageTimestamp` (newest first)
- Click to open ChatActivity

**Current Status**:
- Inbox tab: Loads all conversations
- Archive tab: Shows empty list (not implemented)

### 7.2 ChatListFragment
**Current Status**: Placeholder only, shows "Coming soon" toast

---

## 8. Configuration & Constants

### 8.1 Request Codes (Constants.java)
```java
REQUEST_CODE_DOCUMENT = 1001
REQUEST_CODE_AUDIO = 1002
REQUEST_CODE_VIDEO = 1003
```

### 8.2 Intent Extras
```java
EXTRA_CHAT_USER_ID = "chatUserId"
EXTRA_CHAT_USER_NAME = "chatUserName"
```

### 8.3 Firebase Helper Methods
```java
getMessagesRef()              // Returns messages/ reference
getUserConversationsRef()     // Returns user_conversations/ reference
```

---

## 9. Current Issues & TODOs

### 9.1 Incomplete Features
1. **File Upload Integration**: `sendFileMessage()` in ChatActivity only shows toast, doesn't actually upload
2. **Search Messages**: Placeholder only
3. **Delete Messages**: Placeholder only
4. **Clear Chat**: Placeholder only
5. **Export Chat**: Placeholder only
6. **Mute Notifications**: Placeholder only
7. **View Contact**: Placeholder only
8. **Report**: Placeholder only
9. **ChatListFragment**: Not implemented
10. **Archive Conversations**: Not implemented

### 9.2 Data Issues
1. **Unread Count**: ConversationAdapter uses test data instead of Firebase
2. **Conversation Unread Count**: Not stored in Firebase conversation object
3. **Message Read Status**: `isRead` field exists but not updated when messages are read

### 9.3 Missing Features
1. **Typing Indicators**: Not implemented
2. **Message Status Indicators**: Sent/Delivered/Read not shown
3. **Message Reactions**: Not implemented
4. **Message Forwarding**: Not implemented
5. **Message Search**: Not implemented
6. **Message Pagination**: All messages loaded at once (could be slow for long chats)

---

## 10. Security Considerations

### 10.1 Firebase Rules (from SETUP_GUIDE.md)
```json
{
  "messages": {
    "$chatRoomId": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

**Current Rules**: Allow any authenticated user to read/write any chat room
**Recommendation**: Implement user-specific access control

### 10.2 Suggested Security Rules
```json
{
  "messages": {
    "$chatRoomId": {
      ".read": "auth != null && ($chatRoomId.contains(auth.uid))",
      ".write": "auth != null && ($chatRoomId.contains(auth.uid))"
    }
  },
  "user_conversations": {
    "$userId": {
      ".read": "$userId === auth.uid",
      ".write": "$userId === auth.uid"
    }
  }
}
```

---

## 11. Performance Considerations

### 11.1 Current Implementation
- **Real-time Listeners**: Uses `ChildEventListener` for live updates
- **No Pagination**: All messages loaded at once
- **No Caching**: No local storage of messages

### 11.2 Recommendations
1. Implement message pagination (load last 50, then load more on scroll)
2. Cache messages locally (Room database)
3. Implement offline support
4. Optimize image loading (use Glide/Picasso with caching)
5. Limit conversation list size (pagination)

---

## 12. Data Flow Diagram

```
User sends message
    ↓
ChatActivity.sendMessage()
    ↓
Create ChatMessage object
    ↓
Save to Firebase: messages/{chatRoomId}/{messageId}
    ↓
Update conversations: user_conversations/{userId}/{otherUserId}
    ↓
Firebase triggers ChildEventListener.onChildAdded()
    ↓
ChatAdapter.addMessage()
    ↓
UI updates with new message
```

---

## 13. Key Files Reference

| File | Purpose |
|------|---------|
| `ChatActivity.java` | Main chat interface |
| `ChatAdapter.java` | Message display adapter |
| `ChatMessage.java` | Message data model |
| `Conversation.java` | Conversation data model |
| `InboxActivity.java` | Conversation list |
| `ConversationAdapter.java` | Conversation list adapter |
| `FirebaseHelper.java` | Firebase database references |
| `FileUploadHelper.java` | File upload to Firebase Storage |
| `NotificationBadgeHelper.java` | Unread count management |
| `Constants.java` | Configuration constants |

---

## 14. Recommendations

### 14.1 Immediate Improvements
1. Complete file upload integration in `ChatActivity.sendFileMessage()`
2. Implement actual unread count tracking from Firebase
3. Update `isRead` status when messages are viewed
4. Implement message search functionality

### 14.2 Medium-term Enhancements
1. Add typing indicators
2. Implement message status (sent/delivered/read)
3. Add message pagination
4. Implement archive functionality
5. Add local caching with Room database

### 14.3 Long-term Features
1. End-to-end encryption for sensitive medical chats
2. Message reactions
3. Voice messages
4. Video calls integration
5. Group chats for medical teams

---

## 15. Testing Checklist

- [ ] Send text message
- [ ] Receive text message in real-time
- [ ] Send document attachment
- [ ] Send audio attachment
- [ ] Send video attachment
- [ ] Conversation list updates correctly
- [ ] Unread badge updates correctly
- [ ] Badge clears when chat opened
- [ ] Chat room ID generation works correctly
- [ ] Messages persist after app restart
- [ ] Profile photos load correctly
- [ ] Date separators show correctly
- [ ] Long messages display timestamp below
- [ ] Short messages display timestamp inline

---

**Last Updated**: Based on current codebase analysis
**Version**: 1.0

