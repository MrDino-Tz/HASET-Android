# HASET App — WhatsApp-style Local Message Cache (Plan)

Plan only. No code has been implemented yet. Created for review before execution.

## Goal

Give the chat a WhatsApp-like experience: messages render instantly, previous chats
are readable fully offline, and attachment files persist on the device — all without
blocking the current Firebase Realtime Database as source of truth.

## Background / Current State

- Chat messages are stored in Firebase RTDB under `messages/{chatRoomId}` where
  `chatRoomId` = `uidA_uidB` sorted (`ChatRepository.generateChatRoomId`, `ChatRepository.java:338`).
- `MessageEntity.java` exists but is a plain Java class — **not** a Room entity.
- There is **no `MessageDao`** and messages are **not** part of `AppDatabase`.
- `ChatRepository.loadMessages()` (`ChatRepository.java:78`) attaches a
  `ChildEventListener` on `messages/{chatRoomId}` ordered by `timestamp` and streams
  all messages into a `MutableLiveData`.
- `ChatViewModel` (`ChatViewModel.java`) is a thin wrapper over `ChatRepository`; its
  public API must remain unchanged.
- Room is version **19** (`AppDatabase.java:55`), Room 2.6.1, `fallbackToDestructiveMigration` removed.
- The app already has a `getFilesDir()` folder-writing pattern in `DatabaseBackupHelper`.

## Storage Decision (flagged)

Target SDK 36 (Android 16). A visible public root folder (`/HASET/...`) requires the
"All files access" permission, which fails Play Store review for a health app.

**Chosen approach (hybrid):**
- Working cache lives in the app-specific folder:
  `Android/data/com.haset.hasetapp/files/Pictures/HASET/chat_media/` (no permissions).
- Users can explicitly export media to visible `Pictures/HASET` via MediaStore
  (same pattern already used for QR saving, `ProfileFragment.java:701-710`).

---

# Phase 1 — Room-backed Message Database Cache

Goal: instant display, offline history, incremental sync. Firebase stays the source
of truth; Room mirrors it.

## 1.1 Promote `MessageEntity` to a Room entity

File: `app/src/main/java/com/haset/hasetapp/database/entities/MessageEntity.java`

- Add `@Entity(tableName = "messages")`.
- Primary key: `messageId`.
- Add `chatRoomId` column (the `uidA_uidB` key).
- Columns mirroring every `ChatMessage` field (`ChatMessage.java:4-28`):
  - senderId, senderName, receiverId, receiverName
  - message
  - attachmentUrl, attachmentFileName, attachmentSize, attachmentDuration
  - messageType
  - timestamp
  - isRead
  - messageStatus
  - deliveredTimestamp, readTimestamp
  - replyToMessageId, replyToText, replyToSenderName
  - prescriptionId
- Add `attachmentLocalPath` (reserved for Phase 2 media folder reference).
- Add `@Entity`-compatible getters/setters (existing POJO getters/setters are reusable).

## 1.2 New `MessageDao`

File: `app/src/main/java/com/haset/hasetapp/database/dao/MessageDao.java`

- `@Query("SELECT * FROM messages WHERE chatRoomId = :room ORDER BY timestamp ASC, messageId ASC")`
  → `LiveData<List<MessageEntity>> getMessagesForRoom(String room)`
  (order mirrors `MESSAGE_ORDER` tie-break, `ChatRepository.java:30`).
- `@Insert(onConflict = OnConflictStrategy.REPLACE)` → `upsert(MessageEntity)` (dedupe by messageId).
- `@Query("SELECT * FROM messages WHERE chatRoomId = :room ORDER BY timestamp DESC, messageId DESC LIMIT 1")`
  → `getLastMessage(String room)`.
- `@Query("DELETE FROM messages WHERE chatRoomId = :room")` → `deleteForRoom(String room)`.
- `@Query("DELETE FROM messages WHERE messageId = :id")` → `deleteMessage(String id)`.

## 1.3 Register in `AppDatabase`

File: `app/src/main/java/com/haset/hasetapp/database/AppDatabase.java`

- Bump `version = 19` → `20`.
- Add `MessageEntity.class` to `@Database(entities = { ... })`.
- Add `abstract MessageDao messageDao();`.
- Add `MIGRATION_19_20`:
  - `CREATE TABLE IF NOT EXISTS messages (...)` with the columns from 1.1.
  - Follow the existing migration style (transaction + try/catch, see `MIGRATION_1_2`, `AppDatabase.java:74-117`).
  - Additive only — no data loss. Keep existing migrations in the chain (`AppDatabase.java:2730`).
- Add schema export note: `exportSchema = false` stays.

## 1.4 Rewrite `ChatRepository.loadMessages()` to Room-first

File: `app/src/main/java/com/haset/hasetapp/repositories/ChatRepository.java`

- Inject `AppDatabase` + a background `Executor` (pattern already present, `AppDatabase.java:29-30`).
- Flow:
  1. Fire Room query on the background executor → `postValue` immediately
     (instant render / offline).
  2. Keep the existing Firebase `ChildEventListener`.
  3. On `onChildAdded` / `onChildChanged` → upsert into Room → re-emit from Room.
  4. On `onChildRemoved` → delete from Room → re-emit.
  5. On `onCancelled` → keep whatever Room holds (offline-friendly).
- Room DB access must be thread-safe (single instance, executor).

## 1.5 Mirror all write paths in Room

File: `app/src/main/java/com/haset/hasetapp/repositories/ChatRepository.java`

Firebase remains authoritative; every write also updates Room:

- `sendMessage` (`:142`) → after `chatRef.setValue`, upsert local copy.
- `updateMessageAttachment` (`:162`) → update Firebase + upsert Room.
- `deleteMessage` (`:169`) → Firebase remove + Room delete.
- `markMessageAsRead` (`:249`) → Firebase update + Room update.
- `markAllMessagesAsRead` (`:258`) → batch Firebase + Room.

`ChatViewModel` (`ChatViewModel.java:31-64`) requires **no API changes**.

## 1.6 Phase 1 Out of Scope

- Offline **send** queueing with retry worker (separate feature).
- Conversation-list Room caching (already covered by Firebase offline cache).
- Pagination / `limitToLast` (Room holds full history; acceptable at current scale).

---

# Phase 2 — Attachment Media Folder (WhatsApp-like)

Goal: attachment files persist on device; previews load from disk (faster + offline);
users can export media to a visible location.

## 2.1 `ChatMediaStorage` helper

File: `app/src/main/java/com/haset/hasetapp/utils/ChatMediaStorage.java`

- Resolve root:
  `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)/HASET/chat_media/`
  (app-specific → no permissions needed, Android 11–16 safe).
- Methods:
  - `File getMediaDir()`
  - `String saveMedia(Uri uri, String fileName)` → local path
  - `File getFile(String fileName)`
  - `boolean deleteMedia(String fileName)`
- Same folder-writing approach as `DatabaseBackupHelper` (`getFilesDir()` pattern).

## 2.2 Download attachments on arrival

- When a message with `attachmentUrl` and no `attachmentLocalPath` arrives
  (via the Phase 1 Room flow / child listener), download to the media folder
  on the background executor.
- Store `attachmentLocalPath` in the Room row.
- Previews load from disk when available (faster, offline-capable).

## 2.3 "Save to Pictures/HASET" export

- Reuse MediaStore insert pattern (`ProfileFragment.java:701-710`):
  `RELATIVE_PATH = Pictures/HASET`, so users can copy cached media into the
  visible gallery when they explicitly choose to.

## 2.4 Phase 2 Out of Scope

- Automatic media expiry / cleanup worker (defer until delete-conversation UX is in place).
- Media resizing/compression pipeline.

---

# Verification Plan

- `./gradlew assembleDebug` compiles.
- Open a chat → messages render instantly from Room.
- Airplane mode → reopen chat → history still renders.
- Send / delete / mark-as-read → Firebase and Room stay in sync.
- Kill app → reopen → history persists.
- Room migration 19 → 20 runs without data loss on an existing install.

# Execution Order

1. Phase 1.1 → 1.3 (entity, DAO, migration) and compile.
2. Phase 1.4 → 1.5 (repository integration) and compile + manual chat test.
3. Phase 2 (media folder) — separate session, after other priorities.
