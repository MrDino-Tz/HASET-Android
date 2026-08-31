package com.haset.hasetapp.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.database.entities.DoctorRatingEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.database.entities.NotificationEntity;
import com.haset.hasetapp.models.AppConfig;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.ValueEventListener;

public class FirebaseHelper {

    private static FirebaseAuth mAuth;
    private static FirebaseDatabase mDatabase;
    private static FirebaseStorage mStorage;

    // Singleton instance not needed if methods are static, but for compatibility:
    private static FirebaseHelper instance;
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public static String getCurrentUserId() {
        return getFirebaseAuth().getCurrentUser() != null ? getFirebaseAuth().getCurrentUser().getUid() : null;
    }

    public static DatabaseReference getMessagesRef() {
        return getFirebaseDatabase().getReference(Constants.MESSAGES_PATH);
    }
    
    public static DatabaseReference getUserConversationsRef() {
        return getFirebaseDatabase().getReference(Constants.USER_CONVERSATIONS_PATH); // "user_conversations"
    }

    public static void sendMessage(com.haset.hasetapp.database.entities.MessageEntity message, OnCompleteListener<Void> listener) {
        DatabaseReference messagesRef = getMessagesRef();
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.setMessageId(messageId);
            messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            listener.onError("Failed to generate message ID");
        }
    }

    /**
     * Sends a text reply from a notification using the same Firebase schema as
     * ChatRepository. This path must work without any Activity or dashboard state.
     */
    public static void sendDirectReply(String receiverId, String senderName,
                                       String receiverName, String text,
                                       OnCompleteListener<Void> listener) {
        FirebaseUser authenticatedUser = getFirebaseAuth().getCurrentUser();
        if (authenticatedUser == null) {
            listener.onError("Authentication is not available");
            return;
        }
        String senderId = authenticatedUser.getUid();
        if (receiverId == null || receiverId.trim().isEmpty()
                || receiverId.equals(senderId) || text == null || text.trim().isEmpty()) {
            listener.onError("Invalid direct reply");
            return;
        }

        String chatRoomId = generateChatRoomId(senderId, receiverId);
        DatabaseReference messageRef = getMessagesRef().child(chatRoomId).push();
        String messageId = messageRef.getKey();
        if (messageId == null) {
            listener.onError("Failed to generate message ID");
            return;
        }

        long timestamp = System.currentTimeMillis();
        Map<String, Object> value = new HashMap<>();
        value.put("messageId", messageId);
        value.put("senderId", senderId);
        value.put("senderName", senderName == null ? "" : senderName);
        value.put("receiverId", receiverId);
        value.put("receiverName", receiverName == null ? "" : receiverName);
        value.put("message", text.trim());
        value.put("messageType", "text");
        value.put("messageStatus", "sent");
        value.put("timestamp", timestamp);
        value.put("isRead", false);
        value.put("deliveredTimestamp", 0L);
        value.put("readTimestamp", 0L);

        messageRef.setValue(value).addOnSuccessListener(ignored -> {
            Map<String, Object> senderConversation = new HashMap<>();
            senderConversation.put("otherUserId", receiverId);
            senderConversation.put("otherUserName", receiverName == null ? "" : receiverName);
            senderConversation.put("lastMessage", text.trim());
            senderConversation.put("lastMessageTimestamp", timestamp);
            senderConversation.put("lastMessageSenderId", senderId);
            senderConversation.put("isArchived", false);

            Map<String, Object> receiverConversation = new HashMap<>();
            receiverConversation.put("otherUserId", senderId);
            receiverConversation.put("otherUserName", senderName == null ? "" : senderName);
            receiverConversation.put("lastMessage", text.trim());
            receiverConversation.put("lastMessageTimestamp", timestamp);
            receiverConversation.put("lastMessageSenderId", senderId);
            receiverConversation.put("isArchived", false);

            getUserConversationsRef().child(senderId).child(receiverId)
                    .updateChildren(senderConversation);
            getUserConversationsRef().child(receiverId).child(senderId)
                    .updateChildren(receiverConversation);
            listener.onSuccess(null);
        }).addOnFailureListener(error -> listener.onError(error.getMessage()));
    }

    public static FirebaseAuth getFirebaseAuth() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        return mAuth;
    }

    public static FirebaseDatabase getFirebaseDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            // Keep recent conversations/messages available during brief
            // connectivity drops and allow listeners to catch up on resume.
            try {
                mDatabase.setPersistenceEnabled(true);
            } catch (DatabaseException ignored) {
                // Persistence may already be configured by the host process.
            }
        }
        return mDatabase;
    }

    public static FirebaseStorage getFirebaseStorage() {
        if (mStorage == null) {
            mStorage = FirebaseStorage.getInstance();
        }
        return mStorage;
    }

    // Password Management Methods
    public static void reauthenticateUser(String password, OnCompleteListener<Void> listener) {
        FirebaseUser user = getFirebaseAuth().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            listener.onError("User not authenticated or email not found.");
        }
    }

    public static void updatePassword(String newPassword, OnCompleteListener<Void> listener) {
        FirebaseUser user = getFirebaseAuth().getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            listener.onError("User not authenticated.");
        }
    }

    public static void sendPasswordResetEmail(String email, OnCompleteListener<Void> listener) {
        getFirebaseAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static DatabaseReference getDatabaseReference() {
        return getFirebaseDatabase().getReference();
    }

    // Optional: Methods to get specific database references if common
    public static DatabaseReference getUsersRef() {
        return getFirebaseDatabase().getReference("users");
    }

    // Optional: Methods to get specific storage references if common
    public static StorageReference getProfilePhotosStorageRef() {
        return getFirebaseStorage().getReference("profile_photos");
    }

    // Appointments methods
    public static DatabaseReference getAppointmentsRef() {
        return getFirebaseDatabase().getReference("appointments");
    }

    public static DatabaseReference getPatientAppointmentsRef(String patientId) {
        return getFirebaseDatabase().getReference("patient_appointments").child(patientId);
    }

    public static DatabaseReference getNotificationsRef(String userId) {
        return getFirebaseDatabase().getReference(Constants.NOTIFICATIONS_PATH).child(userId);
    }

    public static void addNotification(NotificationEntity notification, OnCompleteListener<Void> listener) {
        DatabaseReference ref = getNotificationsRef(notification.getUserId());
        String id = ref.push().getKey();
        if (id != null) {
            notification.setNotificationId(id);
            ref.child(id).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
        }
    }

    /**
     * Sends an in-app notification to an admin-selected audience. The admin
     * client only writes under each recipient's own notification node; the
     * database rules still verify that the caller is an admin.
     *
     * @param audience "all", "patients", "doctors", or "selected"
     * @param selectedUserIds used only when audience is "selected"
     */
    public static void sendAdminNotification(
            String title,
            String message,
            String audience,
            List<String> selectedUserIds,
            OnCompleteListener<Integer> listener) {
        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            if (listener != null) listener.onError("Title and message are required");
            return;
        }
        String target = audience == null ? "" : audience.trim().toLowerCase();
        if (!("all".equals(target) || "patients".equals(target)
                || "doctors".equals(target) || "selected".equals(target))) {
            if (listener != null) listener.onError("Invalid notification audience");
            return;
        }

        getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String role = userSnapshot.child("role").getValue(String.class);
                    boolean selected = "selected".equals(target)
                            && selectedUserIds != null && selectedUserIds.contains(userId);
                    boolean matches = "all".equals(target)
                            || ("patients".equals(target) && "patient".equals(role))
                            || ("doctors".equals(target) && "doctor".equals(role))
                            || selected;
                    if (!matches || userId == null || userId.trim().isEmpty()) continue;

                    String notificationId = getNotificationsRef(userId).push().getKey();
                    if (notificationId == null) continue;
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("notificationId", notificationId);
                    notification.put("userId", userId);
                    notification.put("title", title.trim());
                    notification.put("message", message.trim());
                    notification.put("type", "admin_broadcast");
                    notification.put("timestamp", System.currentTimeMillis());
                    notification.put("isRead", false);
                    updates.put("notifications/" + userId + "/" + notificationId, notification);
                }
                if (updates.isEmpty()) {
                    if (listener != null) listener.onSuccess(0);
                    return;
                }
                getFirebaseDatabase().getReference().updateChildren(updates)
                        .addOnSuccessListener(v -> { if (listener != null) listener.onSuccess(updates.size()); })
                        .addOnFailureListener(e -> { if (listener != null) listener.onError(e.getMessage()); });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onError(error.getMessage());
            }
        });
    }

    public static DatabaseReference getDoctorAppointmentsRef(String doctorId) {
        return getFirebaseDatabase().getReference("doctor_appointments").child(doctorId);
    }

    // Doctor specific references
    public static DatabaseReference getDoctorsNodeRef() {
        return getFirebaseDatabase().getReference("doctors");
    }
    
    // Doctor wallet references
    public static DatabaseReference getDoctorWalletRef() {
        return getFirebaseDatabase().getReference("doctor_wallets");
    }

    // Method to get a Doctor object by ID from Firebase
    public static void getDoctorById(String doctorId, OnCompleteListener<com.haset.hasetapp.models.Doctor> listener) {
        getUsersRef().child(doctorId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    com.haset.hasetapp.database.entities.UserEntity user = userSnapshot.getValue(com.haset.hasetapp.database.entities.UserEntity.class);
                    if (user != null && Constants.ROLE_DOCTOR.equals(user.getRole())) {
                        getDoctorsNodeRef().child(doctorId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot doctorEntitySnapshot) {
                                com.haset.hasetapp.database.entities.DoctorEntity doctorEntity = doctorEntitySnapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                                com.haset.hasetapp.models.Doctor doctor = new com.haset.hasetapp.models.Doctor();
                                doctor.setDoctorId(user.getUserId());
                                doctor.setUserId(user.getUserId());
                                doctor.setFullName(user.getFullName());
                                doctor.setEmail(user.getEmail());
                                
                                // Robust phone number handling
                                String userPhone = user.getPhone();
                                if (userPhone != null) {
                                    doctor.setPhone(userPhone);
                                } else {
                                    Object phoneValue = userSnapshot.child("phone").getValue();
                                    if (phoneValue != null) {
                                        doctor.setPhone(String.valueOf(phoneValue));
                                    } else {
                                        doctor.setPhone("");
                                    }
                                }

                                // Handle profile image with proper null safety
                                String userProfileImage = user.getProfileImage();
                                doctor.setProfileImage(userProfileImage != null ? userProfileImage : "");

                                if (doctorEntity != null) {
                                    // Extract professional details with fallbacks to User node (denormalized data)
                                    String specialty = doctorEntity.getSpecialty();
                                    if (specialty == null || specialty.isEmpty()) {
                                        specialty = userSnapshot.child("specialty").getValue(String.class);
                                    }
                                    doctor.setSpecialty(specialty != null ? specialty : "Medical Doctor");

                                    double fee = doctorEntity.getConsultationFee();
                                    if (fee <= 0) {
                                        Object userFee = userSnapshot.child("consultationFee").getValue();
                                        if (userFee instanceof Number) fee = ((Number) userFee).doubleValue();
                                    }
                                    doctor.setConsultationFee(fee);

                                    // Handle available times parsing
                                    if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
                                        String timesStr = doctorEntity.getAvailableTimes();
                                        List<String> timeList = new ArrayList<>();
                                        if (timesStr.contains("-")) {
                                            String[] range = timesStr.split("-");
                                            if (range.length == 2) {
                                                try {
                                                    int fromHour = Integer.parseInt(range[0].trim().split(":")[0]);
                                                    int fromMinute = Integer.parseInt(range[0].trim().split(":")[1]);
                                                    int toHour = Integer.parseInt(range[1].trim().split(":")[0]);
                                                    int toMinute = Integer.parseInt(range[1].trim().split(":")[1]);

                                                    java.util.Calendar cal = java.util.Calendar.getInstance();
                                                    cal.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
                                                    cal.set(java.util.Calendar.MINUTE, fromMinute);

                                                    java.util.Calendar endCal = java.util.Calendar.getInstance();
                                                    endCal.set(java.util.Calendar.HOUR_OF_DAY, toHour);
                                                    endCal.set(java.util.Calendar.MINUTE, toMinute);

                                                    while (!cal.after(endCal)) {
                                                        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                                                        int minute = cal.get(java.util.Calendar.MINUTE);
                                                        timeList.add(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
                                                        cal.add(java.util.Calendar.MINUTE, 30);
                                                    }
                                                } catch (Exception e) {
                                                    timeList.add(range[0].trim());
                                                    timeList.add(range[1].trim());
                                                }
                                            }
                                        } else {
                                            String[] times = timesStr.split(",");
                                            for (String time : times) {
                                                timeList.add(time.trim());
                                            }
                                        }
                                        doctor.setAvailableTimes(timeList);
                                    }
                                    
                                    doctor.setRating(doctorEntity.getAverageRating() > 0 ? doctorEntity.getAverageRating().floatValue() : 4.5f);
                                    doctor.setExperience(doctorEntity.getExperience() > 0 ? doctorEntity.getExperience() : 5);
                                    doctor.setAbout(doctorEntity.getAbout() != null ? doctorEntity.getAbout() : "");
                                    
                                    String location = doctorEntity.getLocation();
                                    if (location == null || location.isEmpty()) {
                                        location = userSnapshot.child("location").getValue(String.class);
                                    }
                                    doctor.setLocation(location != null ? location : "");
                                    
                                    doctor.setVerified(doctorEntity.isApproved());
                                    doctor.setPatientsTreated(doctorEntity.getPatientsTreated());
                                    doctor.setDemo(doctorEntity.isDemo());
                                } else {
                                    // Set some defaults if doctorEntity is missing
                                    doctor.setSpecialty("Medical Doctor");
                                    doctor.setVerified(false);
                                    doctor.setDemo(false);
                                }
                                
                                // Always return the doctor object if the user exists and is a doctor
                                listener.onSuccess(doctor);
                            }

                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                                listener.onError(databaseError.getMessage());
                            }
                        });
                    } else {
                        listener.onError("User is not a doctor or user data incomplete.");
                    }
                } else {
                    listener.onError("User not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    private static void getAppointmentsByIndex(String userId, String role,
                                               OnCompleteListener<List<AppointmentEntity>> listener) {
        DatabaseReference index = Constants.ROLE_DOCTOR.equalsIgnoreCase(role)
                ? getDoctorAppointmentsRef(userId) : getPatientAppointmentsRef(userId);
        index.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentEntity> result = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) if (child.getKey() != null) ids.add(child.getKey());
                if (ids.isEmpty()) { listener.onSuccess(result); return; }
                final int[] remaining = {ids.size()};
                for (String id : ids) {
                    getAppointmentsRef().child(id).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot item) {
                            AppointmentEntity value = item.getValue(AppointmentEntity.class);
                            if (value != null) result.add(value);
                            if (--remaining[0] == 0) listener.onSuccess(result);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { listener.onError(error.getMessage()); }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { listener.onError(error.getMessage()); }
        });
    }

    // Callback Interface
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // Appointments methods using OnCompleteListener
    public static void createAppointment(AppointmentEntity appointment, OnCompleteListener<AppointmentEntity> listener) {
        DatabaseReference appointmentsRef = getAppointmentsRef();
        String appointmentId = appointment.getAppointmentId();
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            appointmentId = appointmentsRef.push().getKey(); // Generate unique ID
        }
        if (appointmentId != null) {
            final String resolvedAppointmentId = appointmentId;
            appointment.setAppointmentId(resolvedAppointmentId);
            // Set creation timestamp if not already set
            if (appointment.getCreatedAt() == 0) {
                appointment.setCreatedAt(System.currentTimeMillis());
            }

            appointmentsRef.child(resolvedAppointmentId).setValue(appointment)
                    .addOnSuccessListener(aVoid -> {
                        // Update patient_appointments node
                        getPatientAppointmentsRef(appointment.getPatientId()).child(resolvedAppointmentId).setValue(true);
                        // Update doctor_appointments node
                        getDoctorAppointmentsRef(appointment.getDoctorId()).child(resolvedAppointmentId).setValue(true);
                        listener.onSuccess(appointment);
                    })
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            listener.onError("Failed to generate appointment ID.");
        }
    }

    public static void getAppointmentsByUser(String userId, String role, OnCompleteListener<List<AppointmentEntity>> listener) {
        // Read the indexed main collection in one request instead of fetching
        // the index and then issuing one network request per appointment.
        String participantField = Constants.ROLE_DOCTOR.equalsIgnoreCase(role) ? "doctorId" : "patientId";
        getAppointmentsRef().orderByChild(participantField).equalTo(userId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<AppointmentEntity> appointments = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    AppointmentEntity appointment = snapshot.getValue(AppointmentEntity.class);
                    if (appointment != null) appointments.add(appointment);
                }
                if (appointments.isEmpty()) {
                    getAppointmentsByIndex(userId, role, listener);
                } else {
                    listener.onSuccess(appointments);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                getAppointmentsByIndex(userId, role, listener);
            }
        });
    }

    public static void getDoctorAppointmentsByStatus(String doctorId, String status, OnCompleteListener<List<AppointmentEntity>> listener) {
        getDoctorAppointmentsRef(doctorId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<AppointmentEntity> filteredAppointments = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    int count = 0;
                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String appointmentId = snapshot.getKey();
                        if (appointmentId != null) {
                            getAppointmentsRef().child(appointmentId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot appointmentSnapshot) {
                                    AppointmentEntity appointment = appointmentSnapshot.getValue(AppointmentEntity.class);
                                    if (appointment != null && appointment.getStatus().equalsIgnoreCase(status)) {
                                        filteredAppointments.add(appointment);
                                    }
                                    if (++snapshotCount == dataSnapshot.getChildrenCount()) {
                                        listener.onSuccess(filteredAppointments);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                                    listener.onError(databaseError.getMessage());
                                }
                                private int snapshotCount = 0;
                            });
                        } else {
                            if (++count == dataSnapshot.getChildrenCount()) {
                                listener.onSuccess(filteredAppointments);
                            }
                        }
                    }
                } else {
                    listener.onSuccess(filteredAppointments); // No appointments found
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static void updateAppointmentStatus(String appointmentId, String status, OnCompleteListener<Boolean> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        getAppointmentsRef().child(appointmentId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess(true))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void getAllAppointments(OnCompleteListener<List<AppointmentEntity>> listener) {
        getAppointmentsRef().addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<AppointmentEntity> appointments = new ArrayList<>();
                try {
                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            // Get basic appointment data from Firebase
                            String appointmentId = snapshot.getKey();
                            String patientId = snapshot.child("patientId").getValue(String.class);
                            String doctorId = snapshot.child("doctorId").getValue(String.class);
                            String patientName = snapshot.child("patientName").getValue(String.class);
                            String doctorName = snapshot.child("doctorName").getValue(String.class);
                            String date = snapshot.child("date").getValue(String.class);
                            String time = snapshot.child("time").getValue(String.class);
                            String reason = snapshot.child("reason").getValue(String.class);
                            String status = snapshot.child("status").getValue(String.class);
                            String appointmentType = snapshot.child("appointmentType").getValue(String.class);
                            
                            // Create AppointmentEntity with required fields
                            if (appointmentId != null && patientId != null && doctorId != null) {
                                AppointmentEntity appointment = new AppointmentEntity();
                                appointment.setAppointmentId(appointmentId);
                                appointment.setPatientId(patientId);
                                appointment.setDoctorId(doctorId);
                                appointment.setPatientName(patientName != null ? patientName : "");
                                appointment.setDoctorName(doctorName != null ? doctorName : "");
                                appointment.setDate(date != null ? date : "");
                                appointment.setTime(time != null ? time : "");
                                appointment.setReason(reason != null ? reason : "");
                                appointment.setStatus(status != null ? status : "pending");
                                appointment.setAppointmentType(appointmentType != null ? appointmentType : "Visit");
                                appointment.setCreatedAt(snapshot.child("createdAt").getValue(Long.class) != null ? 
                                    snapshot.child("createdAt").getValue(Long.class) : System.currentTimeMillis());
                                
                                appointments.add(appointment);
                            }
                        } catch (Exception e) {
                            Log.e("FirebaseHelper", "Error parsing appointment: " + snapshot.getKey(), e);
                            // Continue with next appointment instead of crashing
                        }
                    }
                    listener.onSuccess(appointments);
                } catch (Exception e) {
                    Log.e("FirebaseHelper", "Error in getAllAppointments", e);
                    listener.onError("Error processing appointment data: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                Log.e("FirebaseHelper", "Firebase error in getAllAppointments: " + databaseError.getMessage());
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static void clearAppointmentsForUser(String userId, String role, OnCompleteListener<Void> listener) {
        // This operation can be complex due to denormalization. For simplicity, we'll remove from main appointments and then the user's specific node.
        // A more robust solution might involve a Cloud Function triggered on appointment deletion.

        DatabaseReference userAppointmentsRef;
        if (Constants.ROLE_DOCTOR.equals(role)) {
            userAppointmentsRef = getDoctorAppointmentsRef(userId);
        } else {
            userAppointmentsRef = getPatientAppointmentsRef(userId);
        }

        userAppointmentsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    final int[] completionCount = {0};
                    final int totalAppointments = (int) dataSnapshot.getChildrenCount();

                    if (totalAppointments == 0) {
                        listener.onSuccess(null); // No appointments to clear
                        return;
                    }

                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String appointmentId = snapshot.getKey();
                        if (appointmentId != null) {
                            getAppointmentsRef().child(appointmentId).removeValue()
                                    .addOnCompleteListener(task -> {
                                        completionCount[0]++;
                                        if (completionCount[0] == totalAppointments) {
                                            // All main appointments removed, now clear the user's index node
                                            userAppointmentsRef.removeValue()
                                                    .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                                                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
                                        }
                                    });
                        } else {
                            completionCount[0]++;
                            if (completionCount[0] == totalAppointments) {
                                userAppointmentsRef.removeValue()
                                        .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                                        .addOnFailureListener(e -> listener.onError(e.getMessage()));
                            }
                        }
                    }
                } else {
                    listener.onSuccess(null); // No appointments to clear
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static void updateAppointment(AppointmentEntity appointment, OnCompleteListener<Void> listener) {
        if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
            if (listener != null) listener.onError("Appointment ID is required for update.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        putIfNotNull(updates, "patientId", appointment.getPatientId());
        putIfNotNull(updates, "doctorId", appointment.getDoctorId());
        putIfNotNull(updates, "patientName", appointment.getPatientName());
        putIfNotNull(updates, "doctorName", appointment.getDoctorName());
        putIfNotNull(updates, "date", appointment.getDate());
        putIfNotNull(updates, "time", appointment.getTime());
        putIfNotNull(updates, "reason", appointment.getReason());
        putIfNotNull(updates, "status", appointment.getStatus());
        putIfNotNull(updates, "appointmentType", appointment.getAppointmentType());
        putIfNotNull(updates, "createdAt", appointment.getCreatedAt());
        putIfNotNull(updates, "amount", appointment.getAmount());
        putIfNotNull(updates, "updatedAt", System.currentTimeMillis());
        if (appointment.getChatStartTime() > 0) {
            putIfNotNull(updates, "chatStartTime", appointment.getChatStartTime());
            putIfNotNull(updates, "chatEndTime", appointment.getChatEndTime());
            putIfNotNull(updates, "chatDuration", appointment.getChatDuration());
            putIfNotNull(updates, "isChatActive", appointment.isChatActive());
        }
        getAppointmentsRef().child(appointment.getAppointmentId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    // Doctor Wallet Operations
    public static DatabaseReference getDoctorWalletsRef() {
        return getFirebaseDatabase().getReference("doctor_wallets");
    }

    public static DatabaseReference getPayoutDestinationRequestsRef() {
        return getFirebaseDatabase().getReference("payout_destination_requests");
    }

    /**
     * Persist a submitted payout destination as a pending record that admins
     * review. Mirrors the destination into the doctor's wallet node so
     * the admin wallets page can show it. Only masked account values are stored.
     */
    public static void submitPayoutDestinationForApproval(String doctorId, String destinationType,
            String provider, String bankCode, String phoneNumber, String bankAccount,
            OnCompleteListener<Boolean> listener) {
        if (doctorId == null || doctorId.isEmpty()) {
            if (listener != null) listener.onError("Missing doctor id.");
            return;
        }
        boolean bank = "bank".equalsIgnoreCase(destinationType);
        String maskedAccount = bank ? maskAccount(bankAccount) : maskAccount(phoneNumber);

        Map<String, Object> request = new HashMap<>();
        request.put("doctor_id", doctorId);
        request.put("destination_type", bank ? "bank" : "mobile_money");
        request.put("provider", bank ? bankCode : provider);
        if (bank && bankCode != null) request.put("bank_code", bankCode);
        request.put("masked_account", maskedAccount);
        request.put("status", "pending");
        request.put("submitted_by", doctorId);
        request.put("created_at", System.currentTimeMillis());
        request.put("can_review", true);

        Map<String, Object> walletDestination = new HashMap<>();
        walletDestination.put("status", "pending");
        walletDestination.put("available", false);
        walletDestination.put("masked_account", maskedAccount);
        walletDestination.put("created_at", System.currentTimeMillis());
        if (bank) {
            walletDestination.put("bank_code", bankCode);
            walletDestination.put("provider", bankCode);
        } else {
            walletDestination.put("provider", provider);
        }

        DatabaseReference requestRef = getPayoutDestinationRequestsRef().child(doctorId);
        DatabaseReference walletTypeRef = getDoctorWalletsRef().child(doctorId)
                .child("payout_destinations").child(bank ? "bank" : "mobile_money");

        requestRef.setValue(request, (error, ref) -> {
            if (error != null) {
                Log.e("FirebaseHelper", "Failed to write pending payout destination: " + error.getMessage());
                if (listener != null) listener.onError(error.getMessage());
                return;
            }
            walletTypeRef.setValue(walletDestination, (walletError, walletRef) -> {
                if (walletError != null) {
                    Log.e("FirebaseHelper", "Failed to write wallet payout_destinations: " + walletError.getMessage());
                    if (listener != null) listener.onError(walletError.getMessage());
                    return;
                }
                if (listener != null) listener.onSuccess(true);
            });
        });
    }

    private static String maskAccount(String value) {
        if (value == null) return "";
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() <= 6) return "****";
        return digits.substring(0, 2) + "****" + digits.substring(digits.length() - 4);
    }

    // Withdrawal Request Operations
    public static DatabaseReference getWithdrawalRequestsRef() {
        return getFirebaseDatabase().getReference(Constants.WITHDRAWAL_REQUESTS_PATH);
    }

    public static void createWithdrawalRequest(com.haset.hasetapp.database.entities.WithdrawalRequest request, OnCompleteListener<Boolean> listener) {
        // Payout requests must be created by the payment backend so identity,
        // MFA, limits, reservation, and audit rules are enforced server-side.
        // Never write a withdrawal directly from the client to Realtime Database.
        if (listener != null) {
            listener.onError("Payout service is unavailable until the secure backend payout endpoint is configured.");
        }
    }

    public static void getAllWithdrawalRequests(OnCompleteListener<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> listener) {
        getWithdrawalRequestsRef().orderByChild("requestedAt").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.database.entities.WithdrawalRequest> requests = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    com.haset.hasetapp.database.entities.WithdrawalRequest request = snapshot.getValue(com.haset.hasetapp.database.entities.WithdrawalRequest.class);
                    if (request != null) {
                        requests.add(request);
                    }
                }
                // Sort by requestedAt descending
                Collections.sort(requests, (a, b) -> Long.compare(b.getRequestedAt(), a.getRequestedAt()));
                listener.onSuccess(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static void getPendingWithdrawalRequests(OnCompleteListener<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> listener) {
        getWithdrawalRequestsRef().orderByChild("status").equalTo("pending").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.database.entities.WithdrawalRequest> requests = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    com.haset.hasetapp.database.entities.WithdrawalRequest request = snapshot.getValue(com.haset.hasetapp.database.entities.WithdrawalRequest.class);
                    if (request != null) {
                        requests.add(request);
                    }
                }
                Collections.sort(requests, (a, b) -> Long.compare(a.getRequestedAt(), b.getRequestedAt()));
                listener.onSuccess(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static void getWithdrawalRequestsByDoctor(String doctorId, OnCompleteListener<List<com.haset.hasetapp.database.entities.WithdrawalRequest>> listener) {
        getWithdrawalRequestsRef().orderByChild("doctorId").equalTo(doctorId).addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.database.entities.WithdrawalRequest> requests = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    com.haset.hasetapp.database.entities.WithdrawalRequest request = snapshot.getValue(com.haset.hasetapp.database.entities.WithdrawalRequest.class);
                    if (request != null) {
                        requests.add(request);
                    }
                }
                Collections.sort(requests, (a, b) -> Long.compare(b.getRequestedAt(), a.getRequestedAt()));
                listener.onSuccess(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    // Method to get a list of Doctor objects for patients (approved doctors).
    // Primary source: the public /doctors node (works without /users read permission).
    // Names/email/phone are enriched from /users via a best-effort query.
    public static void getDoctorsForPatients(OnCompleteListener<List<com.haset.hasetapp.models.Doctor>> listener) {
        loadDoctorsFromDoctorsNode(listener);
    }

    /**
     * Attaches a real-time listener on the /doctors node so the patient-facing
     * doctors list (including consultation fee) stays in sync whenever a doctor
     * updates their record. Returns the listener so callers can detach it.
     */
    public static com.google.firebase.database.ValueEventListener observeDoctorsForPatients(OnCompleteListener<List<com.haset.hasetapp.models.Doctor>> listener) {
        com.google.firebase.database.ValueEventListener valueEventListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.models.Doctor> doctors = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot doctorSnapshot : dataSnapshot.getChildren()) {
                        com.haset.hasetapp.database.entities.DoctorEntity doctorEntity = doctorSnapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                        if (doctorEntity != null && doctorEntity.isApproved()) {
                            doctors.add(buildDoctorFromEntity(doctorSnapshot.getKey(), doctorEntity));
                        }
                    }
                }
                if (doctors.isEmpty()) {
                    loadDoctorsFromUsersForPatients(listener);
                } else {
                    mergeDoctorNamesFromUsers(doctors, listener);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                loadDoctorsFromUsersForPatients(listener);
            }
        };
        getDoctorsNodeRef().addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    private static void loadDoctorsFromDoctorsNode(OnCompleteListener<List<com.haset.hasetapp.models.Doctor>> listener) {
        getDoctorsNodeRef().addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.models.Doctor> doctors = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot doctorSnapshot : dataSnapshot.getChildren()) {
                        com.haset.hasetapp.database.entities.DoctorEntity doctorEntity = doctorSnapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                        if (doctorEntity != null && doctorEntity.isApproved()) {
                            doctors.add(buildDoctorFromEntity(doctorSnapshot.getKey(), doctorEntity));
                        }
                    }
                }
                if (doctors.isEmpty()) {
                    loadDoctorsFromUsersForPatients(listener);
                } else {
                    mergeDoctorNamesFromUsers(doctors, listener);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                loadDoctorsFromUsersForPatients(listener);
            }
        });
    }

    private static com.haset.hasetapp.models.Doctor buildDoctorFromEntity(String doctorId, com.haset.hasetapp.database.entities.DoctorEntity doctorEntity) {
        com.haset.hasetapp.models.Doctor doctor = new com.haset.hasetapp.models.Doctor();
        doctor.setDoctorId(doctorId);
        doctor.setUserId(doctorId);
        doctor.setProfileImage(doctorEntity.getProfileImage());

        String specialty = doctorEntity.getSpecialty();
        if (specialty == null || specialty.isEmpty()) {
            specialty = "Medical Doctor";
        }
        doctor.setSpecialty(specialty);

        if (doctorEntity.getConsultationFee() > 0) {
            doctor.setConsultationFee(doctorEntity.getConsultationFee());
        }

        if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
            String timesStr = doctorEntity.getAvailableTimes();
            List<String> timeList = new ArrayList<>();
            if (timesStr.contains("-")) {
                String[] range = timesStr.split("-");
                if (range.length == 2) {
                    try {
                        int fromHour = Integer.parseInt(range[0].trim().split(":")[0]);
                        int fromMinute = Integer.parseInt(range[0].trim().split(":")[1]);
                        int toHour = Integer.parseInt(range[1].trim().split(":")[0]);
                        int toMinute = Integer.parseInt(range[1].trim().split(":")[1]);

                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
                        cal.set(java.util.Calendar.MINUTE, fromMinute);

                        java.util.Calendar endCal = java.util.Calendar.getInstance();
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, toHour);
                        endCal.set(java.util.Calendar.MINUTE, toMinute);

                        while (!cal.after(endCal)) {
                            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                            int minute = cal.get(java.util.Calendar.MINUTE);
                            timeList.add(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
                            cal.add(java.util.Calendar.MINUTE, 30);
                        }
                    } catch (Exception e) {
                        timeList.add(range[0].trim());
                        timeList.add(range[1].trim());
                    }
                }
            } else {
                String[] times = timesStr.split(",");
                for (String time : times) {
                    timeList.add(time.trim());
                }
            }
            doctor.setAvailableTimes(timeList);
        }

        doctor.setRating(doctorEntity.getAverageRating() > 0 ? doctorEntity.getAverageRating().floatValue() : 4.5f);
        doctor.setExperience(doctorEntity.getExperience() > 0 ? doctorEntity.getExperience() : 5);
        doctor.setAbout(doctorEntity.getAbout() != null ? doctorEntity.getAbout() : "");
        String location = doctorEntity.getLocation();
        doctor.setLocation(location != null ? location : "");
        doctor.setVerified(doctorEntity.isApproved());
        doctor.setOnline(doctorEntity.isOnline());
        doctor.setOnlineStatus(doctorEntity.getOnlineStatus() != null ? doctorEntity.getOnlineStatus() : "offline");
        doctor.setPatientsTreated(doctorEntity.getPatientsTreated());
        doctor.setCreatedAt(doctorEntity.getCreatedAt());
        doctor.setDemo(doctorEntity.isDemo());
        return doctor;
    }

    public static void mergeDoctorNamesFromUsers(List<com.haset.hasetapp.models.Doctor> doctors, OnCompleteListener<List<com.haset.hasetapp.models.Doctor>> listener) {
        if (doctors == null || doctors.isEmpty()) {
            listener.onSuccess(doctors);
            return;
        }
        // Read each doctor's /users/{uid} record individually. A single
        // orderByChild("role") query on /users requires list read permission,
        // which patients/doctors don't have. The rules allow reading a
        // specific user's record when that user is a doctor.
        final int total = doctors.size();
        final int[] processed = {0};
        final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);
        for (com.haset.hasetapp.models.Doctor doctor : doctors) {
            final com.haset.hasetapp.models.Doctor targetDoctor = doctor;
            getUsersRef().child(doctor.getDoctorId()).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot userSnapshot) {
                    if (userSnapshot.exists()) {
                        String fullName = userSnapshot.child("fullName").getValue(String.class);
                        if (fullName != null && !fullName.isEmpty()) {
                            targetDoctor.setFullName(fullName);
                        }
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            targetDoctor.setEmail(email);
                        }
                        Object phoneValue = userSnapshot.child("phone").getValue();
                        if (phoneValue != null) {
                            targetDoctor.setPhone(String.valueOf(phoneValue));
                        }
                        String profileImage = userSnapshot.child("profileImage").getValue(String.class);
                        if (profileImage != null) {
                            targetDoctor.setProfileImage(profileImage);
                        }
                    }
                    finish();
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                    finish();
                }

                private void finish() {
                    processed[0]++;
                    if (processed[0] == total && done.compareAndSet(false, true)) {
                        listener.onSuccess(doctors);
                    }
                }
            });
        }
    }

    private static void loadDoctorsFromUsersForPatients(OnCompleteListener<List<com.haset.hasetapp.models.Doctor>> listener) {
        getUsersRef().orderByChild("role").equalTo(Constants.ROLE_DOCTOR).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.models.Doctor> doctors = new ArrayList<>();
                final int totalDoctors = (int) dataSnapshot.getChildrenCount();
                final int[] doctorsProcessed = {0};

                if (totalDoctors == 0) {
                    listener.onSuccess(doctors);
                    return;
                }

                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    com.haset.hasetapp.database.entities.UserEntity user = userSnapshot.getValue(com.haset.hasetapp.database.entities.UserEntity.class);
                    if (user != null) {
                        getDoctorsNodeRef().child(user.getUserId()).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot doctorEntitySnapshot) {
                                com.haset.hasetapp.database.entities.DoctorEntity doctorEntity = doctorEntitySnapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                                if (doctorEntity != null && doctorEntity.isApproved()) {
                                    com.haset.hasetapp.models.Doctor doctor = new com.haset.hasetapp.models.Doctor();
                                    doctor.setDoctorId(user.getUserId());
                                    doctor.setUserId(user.getUserId());
                                    doctor.setFullName(user.getFullName());
                                    
                                    // Handle email and phone with proper type conversion
                                    doctor.setEmail(user.getEmail() != null ? user.getEmail() : "");
                                    
                                    Object phoneValue = userSnapshot.child("phone").getValue();
                                    doctor.setPhone(phoneValue != null ? String.valueOf(phoneValue) : "");
                                    
                                    // Handle profile image with proper null safety
                                    doctor.setProfileImage(user.getProfileImage() != null ? user.getProfileImage() : "");

                                    // Extract professional details with fallbacks to User node (denormalized data)
                                    String specialty = doctorEntity.getSpecialty();
                                    if (specialty == null || specialty.isEmpty()) {
                                        specialty = userSnapshot.child("specialty").getValue(String.class);
                                    }
                                    doctor.setSpecialty(specialty != null ? specialty : "Medical Doctor");

                                    double fee = doctorEntity.getConsultationFee();
                                    if (fee <= 0) {
                                        Object userFee = userSnapshot.child("consultationFee").getValue();
                                        if (userFee instanceof Number) fee = ((Number) userFee).doubleValue();
                                    }
                                    doctor.setConsultationFee(fee);

                                    // Handle available times parsing
                                    if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
                                        String timesStr = doctorEntity.getAvailableTimes();
                                        List<String> timeList = new ArrayList<>();
                                        if (timesStr.contains("-")) {
                                            String[] range = timesStr.split("-");
                                            if (range.length == 2) {
                                                try {
                                                    int fromHour = Integer.parseInt(range[0].trim().split(":")[0]);
                                                    int fromMinute = Integer.parseInt(range[0].trim().split(":")[1]);
                                                    int toHour = Integer.parseInt(range[1].trim().split(":")[0]);
                                                    int toMinute = Integer.parseInt(range[1].trim().split(":")[1]);

                                                    java.util.Calendar cal = java.util.Calendar.getInstance();
                                                    cal.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
                                                    cal.set(java.util.Calendar.MINUTE, fromMinute);

                                                    java.util.Calendar endCal = java.util.Calendar.getInstance();
                                                    endCal.set(java.util.Calendar.HOUR_OF_DAY, toHour);
                                                    endCal.set(java.util.Calendar.MINUTE, toMinute);

                                                    while (!cal.after(endCal)) {
                                                        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                                                        int minute = cal.get(java.util.Calendar.MINUTE);
                                                        timeList.add(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
                                                        cal.add(java.util.Calendar.MINUTE, 30);
                                                    }
                                                } catch (Exception e) {
                                                    timeList.add(range[0].trim());
                                                    timeList.add(range[1].trim());
                                                }
                                            }
                                        } else {
                                            String[] times = timesStr.split(",");
                                            for (String time : times) {
                                                timeList.add(time.trim());
                                            }
                                        }
                                        doctor.setAvailableTimes(timeList);
                                    }
                                    
                                    doctor.setRating(doctorEntity.getAverageRating() > 0 ? doctorEntity.getAverageRating().floatValue() : 4.5f);
                                    doctor.setExperience(doctorEntity.getExperience() > 0 ? doctorEntity.getExperience() : 5);
                                    doctor.setAbout(doctorEntity.getAbout() != null ? doctorEntity.getAbout() : "");
                                    
                                    String location = doctorEntity.getLocation();
                                    if (location == null || location.isEmpty()) {
                                        location = userSnapshot.child("location").getValue(String.class);
                                    }
                                    doctor.setLocation(location != null ? location : "");
                                    
                                    doctor.setVerified(doctorEntity.isApproved());
                                    
                                    // Set online status
                                    doctor.setOnline(doctorEntity.isOnline());
                                    doctor.setOnlineStatus(doctorEntity.getOnlineStatus() != null ? doctorEntity.getOnlineStatus() : "offline");
                                    
                                    // Handle patients treated from snapshot
                                    Object treated = doctorEntitySnapshot.child("patientsTreated").getValue();
                                    if (treated instanceof Number) {
                                        doctor.setPatientsTreated(((Number) treated).intValue());
                                    }
                                    
                                    doctor.setCreatedAt(user.getCreatedAt());
                                    
                                    // Set demo doctor flag
                                    doctor.setDemo(doctorEntity.isDemo());
                                    
                                    doctors.add(doctor);
                                }
                                doctorsProcessed[0]++;
                                if (doctorsProcessed[0] == totalDoctors) {
                                    listener.onSuccess(doctors);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                                doctorsProcessed[0]++;
                                if (doctorsProcessed[0] == totalDoctors) {
                                    listener.onSuccess(doctors);
                                }
                                listener.onError(databaseError.getMessage());
                            }
                        });
                    } else {
                        doctorsProcessed[0]++;
                        if (doctorsProcessed[0] == totalDoctors) {
                            listener.onSuccess(doctors);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    // User methods
    public static void getAllUsers(OnCompleteListener<List<UserEntity>> listener) {
        getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<UserEntity> users = new ArrayList<>();
                try {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            // Get basic user data from Firebase
                            String userId = snapshot.getKey();
                            String email = snapshot.child("email").getValue(String.class);
                            String fullName = snapshot.child("fullName").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);
                            String role = snapshot.child("role").getValue(String.class);
                            String profileImage = snapshot.child("profileImage").getValue(String.class);
                            
                            // Create UserEntity with required fields
                            if (userId != null && email != null && fullName != null && role != null) {
                                UserEntity user = new UserEntity();
                                user.setUserId(userId);
                                user.setEmail(email);
                                user.setFullName(fullName);
                                user.setPhone(phone != null ? phone : "");
                                user.setRole(role);
                                user.setProfileImage(profileImage != null ? profileImage : "");
                                user.setCreatedAt(snapshot.child("createdAt").getValue(Long.class) != null ? 
                                    snapshot.child("createdAt").getValue(Long.class) : System.currentTimeMillis());
                                
                                users.add(user);
                            }
                        } catch (Exception e) {
                            Log.e("FirebaseHelper", "Error parsing user: " + snapshot.getKey(), e);
                            // Continue with next user instead of crashing
                        }
                    }
                    listener.onSuccess(users);
                } catch (Exception e) {
                    Log.e("FirebaseHelper", "Error in getAllUsers", e);
                    listener.onError("Error processing user data: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseHelper", "Firebase error in getAllUsers: " + databaseError.getMessage());
                listener.onError(databaseError.getMessage());
            }
        });
    }

    // Audit Log methods
    public static DatabaseReference getAuditLogsRef() {
        return getFirebaseDatabase().getReference("audit_logs");
    }

    public static void getAllAuditLogs(OnCompleteListener<List<com.haset.hasetapp.database.entities.AuditLogEntity>> listener) {
        getAuditLogsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<com.haset.hasetapp.database.entities.AuditLogEntity> auditLogs = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    com.haset.hasetapp.database.entities.AuditLogEntity auditLog = snapshot.getValue(com.haset.hasetapp.database.entities.AuditLogEntity.class);
                    if (auditLog != null) {
                        auditLogs.add(auditLog);
                    }
                }
                listener.onSuccess(auditLogs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    /**
     * Deletes a user account from Firebase Authentication, Realtime Database, and Storage.
     * This should be called when a user initiates account deletion.
     * @param userId The ID of the user to delete.
     * @param listener Callback for success or failure.
     */
    public static void deleteUserAccount(String userId, OnCompleteListener<Void> listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onError("User ID is null or empty.");
            return;
        }

        DatabaseReference db = getFirebaseDatabase().getReference();
        
        // 1. Delete profile image from Storage
        StorageReference photoRef = getProfilePhotosStorageRef().child(userId + ".jpg");
        photoRef.delete().addOnCompleteListener(task -> {
            
            // 2. Perform batch deletion using Map for atomicity where possible
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            
            // Nodes to delete directly
            updates.put("users/" + userId, null);
            updates.put("doctors/" + userId, null);
            updates.put("doctor_wallets/" + userId, null);
            updates.put("patient_appointments/" + userId, null);
            updates.put("doctor_appointments/" + userId, null);
            updates.put("user_conversations/" + userId, null);
            
            db.updateChildren(updates).addOnCompleteListener(dbTask -> {
                
                // 3. Delete appointments and posts (requires query-then-delete)
                getAppointmentsRef().orderByChild("patientId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) s.getRef().removeValue();
                        
                        getAppointmentsRef().orderByChild("doctorId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                for (DataSnapshot s : snapshot2.getChildren()) s.getRef().removeValue();
                                
                                getFirebaseDatabase().getReference("article_posts").orderByChild("authorId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot3) {
                                        for (DataSnapshot s : snapshot3.getChildren()) s.getRef().removeValue();
                                        
                                        // 4. Final step: Auth Deletion
                                        FirebaseUser user = getFirebaseAuth().getCurrentUser();
                                        if (user != null && user.getUid().equals(userId)) {
                                            user.delete().addOnCompleteListener(authTask -> {
                                                if (authTask.isSuccessful()) {
                                                    listener.onSuccess(null);
                                                } else {
                                                    Exception exception = authTask.getException();
                                                    if (exception instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                                                        listener.onError("Session expired. Please log out and log in again to delete your account.");
                                                    } else {
                                                        // Data is wiped, auth might still exist but we report success
                                                        listener.onSuccess(null);
                                                    }
                                                }
                                            });
                                        } else {
                                            listener.onSuccess(null);
                                        }
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError e) { listener.onSuccess(null); }
                                });
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) { listener.onSuccess(null); }
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { listener.onSuccess(null); }
                });
            });
        });
    }

    // Rating methods
    public static DatabaseReference getDoctorRatingsRef() {
        return getFirebaseDatabase().getReference(Constants.DOCTOR_RATINGS_PATH);
    }

    public static void submitDoctorRating(DoctorRatingEntity rating, OnCompleteListener<Void> listener) {
        if (rating == null || rating.getDoctorId() == null) {
            listener.onError("Invalid rating data");
            return;
        }

        // 1. Save the rating
        getDoctorRatingsRef().child(rating.getRatingId()).setValue(rating)
                .addOnSuccessListener(aVoid -> {
                    // 2. Update doctor's average rating and count (patientsTreated) via transaction
                    DatabaseReference doctorRef = getDoctorsNodeRef().child(rating.getDoctorId());
                    doctorRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                        @NonNull
                        @Override
                        public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {
                            DoctorEntity doctor = mutableData.getValue(DoctorEntity.class);
                            if (doctor == null) {
                                return com.google.firebase.database.Transaction.success(mutableData);
                            }

                            // Calculate new average
                            // NewAvg = ((OldAvg * OldCount) + NewRating) / (OldCount + 1)
                            
                            float currentAvg = doctor.getAverageRating() != null ? doctor.getAverageRating() : 0f;
                            int currentCount = doctor.getPatientsTreated(); // Using patientsTreated as rating count

                            float newAvg = ((currentAvg * currentCount) + rating.getRating()) / (currentCount + 1);
                            
                            doctor.setAverageRating(newAvg);
                            doctor.setPatientsTreated(currentCount + 1);
                            
                            mutableData.setValue(doctor);
                            return com.google.firebase.database.Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                            if (databaseError != null) {
                                // Rating saved but aggregation failed
                                Log.e("FirebaseHelper", "Failed to update doctor aggregation: " + databaseError.getMessage());
                            }
                            listener.onSuccess(null);
                        }
                    });
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void getRatingByAppointment(String appointmentId, OnCompleteListener<DoctorRatingEntity> listener) {
        getDoctorRatingsRef().orderByChild("appointmentId").equalTo(appointmentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        DoctorRatingEntity rating = snapshot.getValue(DoctorRatingEntity.class);
                        listener.onSuccess(rating);
                        return; // return first match
                    }
                }
                listener.onSuccess(null); // Not found
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }
    
    public static void getDoctorDetails(String userId, OnCompleteListener<com.haset.hasetapp.database.entities.DoctorEntity> listener) {
        getDoctorsNodeRef().child(userId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    com.haset.hasetapp.database.entities.DoctorEntity doctor = snapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                    listener.onSuccess(doctor);
                } else {
                    listener.onSuccess(null);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public static void getDoctorApprovalStatus(String doctorId, OnCompleteListener<Boolean> listener) {
        getDoctorsNodeRef().child(doctorId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    com.haset.hasetapp.database.entities.DoctorEntity doctorEntity = snapshot.getValue(com.haset.hasetapp.database.entities.DoctorEntity.class);
                    boolean isApproved = doctorEntity != null && doctorEntity.isApproved();
                    listener.onSuccess(isApproved);
                } else {
                    listener.onSuccess(false); // Not approved if no record exists
                }
            }
            
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    // Doctor wallet methods
    public static void getDoctorWallet(String doctorId, OnCompleteListener<com.haset.hasetapp.database.entities.DoctorWalletEntity> listener) {
        getDoctorWalletsRef().child(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                com.haset.hasetapp.database.entities.DoctorWalletEntity wallet = null;
                try {
                    if (snapshot.exists()) {
                        Double balance = snapshot.child("balance").getValue(Double.class);
                        Double totalEarnings = snapshot.child("totalEarnings").getValue(Double.class);
                        Long lastUpdated = snapshot.child("lastUpdated").getValue(Long.class);
                        
                        if (doctorId != null) {
                            wallet = new com.haset.hasetapp.database.entities.DoctorWalletEntity();
                            wallet.setDoctorId(doctorId);
                            wallet.setBalance(balance != null ? balance : 0.0);
                            wallet.setTotalEarnings(totalEarnings != null ? totalEarnings : 0.0);
                            wallet.setLastUpdated(lastUpdated != null ? lastUpdated : System.currentTimeMillis());
                        }
                    }
                } catch (Exception e) {
                    Log.e("FirebaseHelper", "Error parsing wallet data", e);
                }
                
                listener.onSuccess(wallet);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
    
    // Doctor edit methods
    public static void saveOrUpdateDoctor(com.haset.hasetapp.database.entities.DoctorEntity doctorEntity, OnCompleteListener<Boolean> listener) {
        String doctorId = doctorEntity.getDoctorId();
        if (doctorId == null || doctorId.trim().isEmpty()) {
            listener.onError("Doctor ID is missing");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("doctorId", doctorId);
        updates.put("specialty", doctorEntity.getSpecialty());
        updates.put("consultationFee", doctorEntity.getConsultationFee());
        updates.put("availableTimes", doctorEntity.getAvailableTimes());
        updates.put("location", doctorEntity.getLocation());
        updates.put("regNo", doctorEntity.getRegNo());
        updates.put("about", doctorEntity.getAbout());
        updates.put("profileImage", doctorEntity.getProfileImage());
        updates.put("online", doctorEntity.isOnline());
        updates.put("onlineStatus", doctorEntity.getOnlineStatus());
        updates.put("lastUpdated", doctorEntity.getLastUpdated());

        // Update only mutable profile/presence fields. Replacing the entire
        // record would overwrite immutable approved/verified values and be
        // rejected by the database rules.
        getDoctorsNodeRef().child(doctorId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    getUsersRef().child(doctorId).updateChildren(updates)
                            .addOnSuccessListener(v -> listener.onSuccess(true))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    // Auth methods
    public static void updatePassword(String oldPassword, String newPassword, OnCompleteListener<Void> listener) {
        com.google.firebase.auth.FirebaseUser user = getFirebaseAuth().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.getEmail(), oldPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    listener.onSuccess(null);
                                } else {
                                    listener.onError(task1.getException() != null ? task1.getException().getMessage() : "Failed to update password");
                                }
                            });
                        } else {
                            listener.onError(task.getException() != null ? task.getException().getMessage() : "Authentication failed. Check your current password.");
                        }
                    });
        } else {
            listener.onError("User not authenticated");
        }
    }

    public static void getRatingsByDoctor(String doctorId, OnCompleteListener<List<DoctorRatingEntity>> listener) {
        getDoctorRatingsRef().orderByChild("doctorId").equalTo(doctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<DoctorRatingEntity> ratings = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        DoctorRatingEntity rating = snapshot.getValue(DoctorRatingEntity.class);
                        if (rating != null) {
                            ratings.add(rating);
                        }
                    }
                }
                // Sort by date (newest first)
                java.util.Collections.sort(ratings, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                listener.onSuccess(ratings);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    public static String generateChatRoomId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) return "temp_room";
        return userId1.compareTo(userId2) < 0 ? 
                userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    public static void sendPrescriptionMessage(com.haset.hasetapp.models.ChatMessage message,
                                               OnCompleteListener<Void> listener) {
        FirebaseUser authenticatedUser = getFirebaseAuth().getCurrentUser();
        if (authenticatedUser == null) {
            listener.onError("Authentication expired. Please sign in again.");
            return;
        }
        String senderId = authenticatedUser.getUid();
        String receiverId = message.getReceiverId();
        String prescriptionId = message.getPrescriptionId();
        if (receiverId == null || receiverId.trim().isEmpty()
                || receiverId.equals(senderId)
                || prescriptionId == null || prescriptionId.trim().isEmpty()) {
            listener.onError("Prescription chat details are incomplete");
            return;
        }

        String effectiveRoomId = generateChatRoomId(senderId, receiverId);
        DatabaseReference messageRef = getMessagesRef().child(effectiveRoomId).push();
        String messageId = messageRef.getKey();
        if (messageId == null) {
            listener.onError("Failed to generate message ID");
            return;
        }

        long timestamp = message.getTimestamp() > 0
                ? message.getTimestamp() : System.currentTimeMillis();
        Map<String, Object> value = new HashMap<>();
        value.put("messageId", messageId);
        value.put("senderId", senderId);
        value.put("senderName", message.getSenderName() == null ? "" : message.getSenderName());
        value.put("receiverId", receiverId);
        value.put("receiverName", message.getReceiverName() == null ? "" : message.getReceiverName());
        value.put("message", message.getMessage() == null ? "New Prescription Issued" : message.getMessage());
        value.put("messageType", "prescription");
        value.put("messageStatus", "sent");
        value.put("timestamp", timestamp);
        value.put("isRead", false);
        value.put("deliveredTimestamp", 0L);
        value.put("readTimestamp", 0L);
        value.put("prescriptionId", prescriptionId);

        messageRef.setValue(value).addOnSuccessListener(ignored -> {
            Map<String, Object> senderConversation = new HashMap<>();
            senderConversation.put("otherUserId", receiverId);
            senderConversation.put("otherUserName", message.getReceiverName() == null ? "" : message.getReceiverName());
            senderConversation.put("lastMessage", "Prescription Issued");
            senderConversation.put("lastMessageTimestamp", timestamp);
            senderConversation.put("lastMessageSenderId", senderId);
            senderConversation.put("isArchived", false);

            Map<String, Object> receiverConversation = new HashMap<>();
            receiverConversation.put("otherUserId", senderId);
            receiverConversation.put("otherUserName", message.getSenderName() == null ? "" : message.getSenderName());
            receiverConversation.put("lastMessage", "Prescription Issued");
            receiverConversation.put("lastMessageTimestamp", timestamp);
            receiverConversation.put("lastMessageSenderId", senderId);
            receiverConversation.put("isArchived", false);

            getUserConversationsRef().child(senderId).child(receiverId)
                    .updateChildren(senderConversation);
            getUserConversationsRef().child(receiverId).child(senderId)
                    .updateChildren(receiverConversation);
            listener.onSuccess(null);
        }).addOnFailureListener(error -> listener.onError(error.getMessage()));
    }
    public static DatabaseReference getAppConfigRef() {
        return getFirebaseDatabase().getReference(Constants.APP_CONFIG_PATH);
    }

    public static DatabaseReference getAppSettingsRef() {
        return getFirebaseDatabase().getReference("app_settings");
    }

    public static void getAppConfig(OnCompleteListener<AppConfig> listener) {
        // DatabaseReference#get() requests the current server snapshot first.
        // A one-shot ValueEventListener may immediately consume stale data from
        // Firebase's disk cache and detach before a changed admin fee arrives.
        getAppConfigRef().get()
                .addOnSuccessListener(snapshot ->
                        listener.onSuccess(snapshot.getValue(AppConfig.class)))
                .addOnFailureListener(error ->
                        listener.onError(error.getMessage() != null
                                ? error.getMessage()
                                : "Unable to load app configuration"));
    }

    public static void isDoctorRegistrationPending(String userId, OnCompleteListener<Boolean> listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onSuccess(false);
            return;
        }
        getUsersRef().child(userId).child("role").get()
                .addOnSuccessListener(roleSnap -> {
                    if (!Constants.ROLE_DOCTOR.equals(roleSnap.getValue(String.class))) {
                        listener.onSuccess(false);
                        return;
                    }
                    getDoctorsNodeRef().child(userId).child("registrationPaymentStatus").get()
                            .addOnSuccessListener(snapshot -> {
                                String status = snapshot.getValue(String.class);
                                listener.onSuccess("pending".equalsIgnoreCase(status));
                            })
                            .addOnFailureListener(error -> listener.onError(
                                    error.getMessage() != null
                                            ? error.getMessage()
                                            : "Unable to check registration payment"));
                })
                .addOnFailureListener(error -> listener.onError(
                        error.getMessage() != null
                                ? error.getMessage()
                                : "Unable to check registration payment"));
    }
}
