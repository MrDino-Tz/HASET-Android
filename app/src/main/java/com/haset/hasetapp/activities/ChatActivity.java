package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.ChatAdapter;
import com.haset.hasetapp.models.ChatMessage;
import com.haset.hasetapp.models.Service;
import com.google.gson.Gson;
import com.haset.hasetapp.utils.ChatVoicePlayer;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.NotificationBadgeHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import android.database.Cursor;
import android.provider.OpenableColumns;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.FrameLayout; // Import FrameLayout
import android.text.Editable;
import android.text.TextWatcher;
import de.hdodenhof.circleimageview.CircleImageView;
import com.haset.hasetapp.fragments.ChatMoreOptionsBottomSheet; // Import the new BottomSheetDialogFragment
import com.haset.hasetapp.fragments.ChatManagementOptionsBottomSheet; // Import ChatManagementOptionsBottomSheet
import com.haset.hasetapp.fragments.FileAttachmentBottomSheet; // Import FileAttachmentBottomSheet
import com.haset.hasetapp.utils.FileUploadHelper; // Import FileUploadHelper
import com.haset.hasetapp.utils.VoiceRecordingBottomSheet; // Import VoiceRecordingBottomSheet
import com.haset.hasetapp.utils.VoicePlayerManager; // Import VoicePlayerManager
import com.haset.hasetapp.utils.OptimizedVoiceRecorderHelper; // Import OptimizedVoiceRecorderHelper
import com.haset.hasetapp.utils.MemoryMonitor; // Import MemoryMonitor
import com.haset.hasetapp.utils.PrescriptionDetailBottomSheet; // Import PrescriptionDetailBottomSheet
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log; // Import Log class
import androidx.transition.TransitionManager; // Import TransitionManager
import androidx.transition.AutoTransition; // Import AutoTransition
import android.view.MotionEvent; // Import MotionEvent for long press
import android.widget.EditText;
import com.haset.hasetapp.fragments.DoctorDetailsBottomSheet;
import com.haset.hasetapp.models.Doctor;
import android.os.Handler; // Import Handler for recording timer
import androidx.core.app.ActivityCompat; // Import ActivityCompat
import android.Manifest; // Import Manifest
import android.content.pm.PackageManager; // Import PackageManager
import java.io.File; // Import File
import android.provider.MediaStore; // Import MediaStore
import androidx.core.content.FileProvider; // Import FileProvider
import android.content.ContentResolver; // Import ContentResolver
import java.io.InputStream; // Import InputStream
import java.io.FileOutputStream; // Import FileOutputStream
import java.io.IOException; // Import IOException
import androidx.activity.result.ActivityResultLauncher; // Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // Import ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.ChatViewModel;
import com.haset.hasetapp.utils.AddServiceBottomSheet;
import com.haset.hasetapp.utils.AuditLogger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatActivity extends BaseActivity implements ChatMoreOptionsBottomSheet.OnOptionSelectedListener { // Implement the listener
    private static final OkHttpClient DOCUMENT_HTTP_CLIENT = new OkHttpClient();

    private ImageView btnBack; // Changed from MaterialToolbar
    // Removed private MaterialToolbar toolbar;
    private TextView tvChatName;
    private TextView tvOnlineStatus;
    private CircleImageView ivChatProfile;
    private ImageView ivMoreOptions;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerChatProfile;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private ImageView ivAttach;
    private ImageView ivMic;
    private ImageView btnSend;
    private ImageView ivVideoCall;
    private LinearLayout llTypingIndicator;
    private TextView tvTypingText;
    
    // Search views
    private LinearLayout llSearchBar;
    private EditText etSearchMessages;
    private ImageView ivCloseSearch;
    
    // Reply views
    private LinearLayout llReplyContainer;
    private TextView tvReplyName;
    private TextView tvReplyText;
    private ImageView ivCancelReply;
    private ChatMessage replyingToMessage;
    
    private ChatAdapter chatAdapter;
    private androidx.appcompat.view.ActionMode messageSelectionActionMode;
    private String chatUserId;
    private String chatUserName;
    private String currentUserId;
    private String chatRoomId;
    private String fallbackLastMessage;
    private long fallbackLastMessageTimestamp;
    private String fallbackLastMessageSenderId;
    
    private PreferenceManager preferenceManager;
    private NotificationBadgeHelper notificationBadgeHelper;
    private ChatViewModel viewModel;
    private Doctor chatDoctor;
    
    // Typing indicator variables
    private android.os.Handler typingHandler;
    private Runnable typingRunnable;
    private static final long TYPING_TIMEOUT = 3000; // 3 seconds
    
    // Voice recording variables - Bottom Sheet UI
    private VoiceRecordingBottomSheet voiceRecordingBottomSheet;
    private OptimizedVoiceRecorderHelper voiceRecorderHelper;
    private boolean isRecordingVoice = false;
    
    // Voice players for inline playback
    private HashMap<String, ChatVoicePlayer> voicePlayers;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;
    private static final int REQUEST_CAMERA_PERMISSION = 2002;
    private static final int SERVICE_PAYMENT_REQUEST_CODE = 2003;
    
    // Chat duration tracking
    private com.haset.hasetapp.database.entities.AppointmentEntity currentAppointment;
    private long chatStartTime;
    private Handler chatDurationHandler;
    private Runnable chatDurationRunnable;
    private TextView tvChatDuration;
    private boolean isChatSessionActive = false;
    private static final long CHAT_SESSION_DURATION = 24 * 60 * 60 * 1000L; // 24 hours
    private static final long[] NOTIFICATION_TIMES = {30 * 60 * 1000L, 10 * 60 * 1000L, 5 * 60 * 1000L}; // 30min, 10min, 5min before end
    private long chatExpiresAt = 0L;
    private boolean canSendInChat = false;
    private String chatDisabledMessage;
    
    // Camera variables
    private Uri currentImageUri;
    private File currentImageFile;
    
    // Activity result launchers
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    
    // Voice recording dialog
    private android.app.AlertDialog recordingDialog;
    private com.haset.hasetapp.views.VoiceWaveView voiceWaveView;
    private TextView tvRecordingDuration;
    private Runnable amplitudeUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Block screenshots for chat (sensitive - private messages)
//        com.haset.hasetapp.utils.SensitiveActivityHelper.blockScreenshots(this);
        
        setContentView(R.layout.activity_chat);
        overridePendingTransition(R.anim.anim_slide_up, 0);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (messageSelectionActionMode != null) {
                    messageSelectionActionMode.finish();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        initViews();
        chatDisabledMessage = getString(R.string.chat_checking_access);
        setChatInputEnabled(false);
        preferenceManager = new PreferenceManager(this);
        notificationBadgeHelper = new NotificationBadgeHelper(this);
        
        // Initialize voice recording components
        voiceRecorderHelper = new OptimizedVoiceRecorderHelper(this);
        
        // Initialize voice recording bottom sheet
        voiceRecordingBottomSheet = new VoiceRecordingBottomSheet(this, new VoiceRecordingBottomSheet.VoiceRecordingCallback() {
            @Override
            public void onRecordingStarted() {
                isRecordingVoice = true;
                // Update mic button appearance
                if (ivMic != null) {
                    ivMic.setAlpha(0.6f);
                }
                MemoryMonitor.logMemoryUsage("ChatActivity_RecordingStarted");
            }

            @Override
            public void onRecordingStopped(String audioFilePath, long duration) {
                android.util.Log.d("ChatActivity", "Voice recording stopped; duration=" + duration);
                
                isRecordingVoice = false;
                // Reset mic button appearance
                if (ivMic != null) {
                    ivMic.setAlpha(1.0f);
                }
                
                // Get recorded audio file - use callback params or fallback to bottom sheet
                if (audioFilePath == null && voiceRecordingBottomSheet != null) {
                    audioFilePath = voiceRecordingBottomSheet.getLastRecordedFilePath();
                }
                
                if (audioFilePath != null) {
                    sendVoiceMessage(audioFilePath, duration);
                } else {
                    android.util.Log.e("ChatActivity", "No audio file path received from recording");
                }
                
                MemoryMonitor.logMemoryUsage("ChatActivity_RecordingCompleted");
            }

            @Override
            public void onRecordingCancelled() {
                isRecordingVoice = false;
                // Reset mic button appearance
                if (ivMic != null) {
                    ivMic.setAlpha(1.0f);
                }
                
                // Cancel recording in helper
                voiceRecorderHelper.cancelRecording();
                
                MemoryMonitor.logMemoryUsage("ChatActivity_RecordingCancelled");
            }

            @Override
            public void onRecordingError(String error) {
                isRecordingVoice = false;
                // Reset mic button appearance
                if (ivMic != null) {
                    ivMic.setAlpha(1.0f);
                }
                Log.e("ChatActivity", "Voice recording error: " + error);
                MemoryMonitor.logMemoryUsage("ChatActivity_RecordingError");
            }
        });

        chatUserId = getIntent().getStringExtra(Constants.EXTRA_CHAT_USER_ID);
        chatUserName = getIntent().getStringExtra(Constants.EXTRA_CHAT_USER_NAME);
        fallbackLastMessage = getIntent().getStringExtra(Constants.EXTRA_CHAT_LAST_MESSAGE);
        fallbackLastMessageTimestamp = getIntent().getLongExtra(Constants.EXTRA_CHAT_LAST_MESSAGE_TIMESTAMP, 0L);
        fallbackLastMessageSenderId = getIntent().getStringExtra(Constants.EXTRA_CHAT_LAST_MESSAGE_SENDER_ID);
        // Firebase rules authorize messages with auth.uid. Preferences can be
        // stale after account switching or reinstalling, so never use them as
        // the chat participant identity when an authenticated UID is present.
        currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            currentUserId = preferenceManager.getUserId();
        }

        // Fallback for legacy "otherUserId" key (from old startChatWithPatient)
        if (chatUserId == null) {
            chatUserId = getIntent().getStringExtra("otherUserId");
        }
        if (chatUserName == null) {
            chatUserName = getIntent().getStringExtra("otherUserName");
        }

        // Check 1-minute window if launched from appointment approval
        long approvedAt = getIntent().getLongExtra(Constants.EXTRA_APPOINTMENT_APPROVED_AT, 0L);
        boolean isFromAppointment = getIntent().getBooleanExtra(Constants.EXTRA_IS_FROM_APPOINTMENT, false)
                || approvedAt > 0;
        if (isFromAppointment && approvedAt > 0) {
            long elapsed = System.currentTimeMillis() - approvedAt;
            if (elapsed > 60000) {
                Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        tvChatName.setText(chatUserName);
        // Set up back button listener
        btnBack.setOnClickListener(v -> {
            if (messageSelectionActionMode != null) {
                messageSelectionActionMode.finish();
            } else {
                finish();
            }
        });
        // Removed toolbar.setNavigationOnClickListener(v -> finish());
        ProfilePhotoHelper.loadProfilePhoto(this, chatUserId, ivChatProfile, shimmerChatProfile);

        chatRoomId = generateChatRoomId(currentUserId, chatUserId);
        
        if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())
                || Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole())) {
            checkAppointmentStatus();
        }
        if (Constants.ROLE_PATIENT.equals(preferenceManager.getUserRole())) {
            fetchChatDoctorDetails();
        }
        
        setupRecyclerView();
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        setupObservers();

        // Mark all messages as read when chat is opened
        markAllMessagesAsRead();
        
        
        // setupTypingIndicator listener is set in onResume
        
        // Set up TextWatcher for etMessage
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty() && canSendInChat;
                
                // Update Send Button status
                btnSend.setEnabled(hasText);

                if (!hasText) {
                    // Stop typing indicator when text is cleared
                    stopTyping();
                } else {
                    // Indicate that user is typing
                    indicateTyping();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set OnClickListener for send button
        btnSend.setOnClickListener(v -> sendMessage());

        // Set OnClickListener for file attachment
        ivAttach.setOnClickListener(v -> {
            if (!ensureChatCanSend()) return;
            FileAttachmentBottomSheet attachmentBottomSheet = new FileAttachmentBottomSheet();
            
            // Show prescription and service options only for doctors
            Bundle args = new Bundle();
            args.putBoolean("showPrescription", Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole()));
            args.putBoolean("showService", Constants.ROLE_DOCTOR.equals(preferenceManager.getUserRole()));
            attachmentBottomSheet.setArguments(args);

            attachmentBottomSheet.setOnFileAttachmentSelectedListener(new FileAttachmentBottomSheet.OnFileAttachmentSelectedListener() {
                @Override
                public void onDocumentSelected() {
                    attachmentBottomSheet.openDocumentPicker(ChatActivity.this, Constants.REQUEST_CODE_DOCUMENT);
                }

                @Override
                public void onImageSelected() {
                    attachmentBottomSheet.openImagePicker(ChatActivity.this, Constants.REQUEST_CODE_IMAGE);
                }

                @Override
                public void onVideoSelected() {
                    attachmentBottomSheet.openVideoPicker(ChatActivity.this, Constants.REQUEST_CODE_VIDEO);
                }

                @Override
                public void onPrescriptionSelected() {
                    openPrescriptionSheet();
                }

                @Override
                public void onServiceSelected() {
                    openServiceSheet();
                }
            });
            attachmentBottomSheet.show(getSupportFragmentManager(), FileAttachmentBottomSheet.class.getSimpleName());
        });



        // Set OnClickListener for header more options icon
        if (ivMoreOptions != null) {
            ivMoreOptions.setOnClickListener(v -> {
                ChatMoreOptionsBottomSheet moreOptionsBottomSheet = new ChatMoreOptionsBottomSheet();
                moreOptionsBottomSheet.setOnOptionSelectedListener(this); // Set the listener
                moreOptionsBottomSheet.show(getSupportFragmentManager(), ChatMoreOptionsBottomSheet.TAG);
            });
        }
        
        // Add Video Call Coming Soon listener
        if (ivVideoCall != null) {
            ivVideoCall.setOnClickListener(v -> showComingSoonDialog(getString(R.string.video_call)));
        }
        
        // Setup mic button for voice recording (press and hold)
        setupVoiceRecording();

        // Search Bar Listeners
        ivCloseSearch.setOnClickListener(v -> {
            llSearchBar.setVisibility(View.GONE);
            etSearchMessages.setText("");
            if (chatAdapter != null) {
                chatAdapter.filter("");
            }
            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearchMessages.getWindowToken(), 0);
            }
        });

        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatAdapter != null) {
                    chatAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkAppointmentStatus() {
        if (chatUserId == null || currentUserId == null) return;

        com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase()
                .getReference("chat_sessions")
                .child(chatRoomId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (activateExistingChatSession(snapshot)) {
                            return;
                        }
                        queryAppointmentsForChatAccess();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Log.w("ChatActivity", "Unable to read chat session before appointment check",
                                error.toException());
                        queryAppointmentsForChatAccess();
                    }
                });
    }

    private boolean activateExistingChatSession(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
        if (!snapshot.exists()) return false;

        String patientId = snapshot.child("patientId").getValue(String.class);
        String doctorId = snapshot.child("doctorId").getValue(String.class);
        boolean participantsMatch = (currentUserId.equals(patientId) && chatUserId.equals(doctorId))
                || (currentUserId.equals(doctorId) && chatUserId.equals(patientId));
        if (!participantsMatch) return false;

        long startsAt = getLongChild(snapshot, "chatStartsAt", 0L);
        long expiresAt = getLongChild(snapshot, "chatExpiresAt", 0L);
        Boolean active = snapshot.child("isChatActive").getValue(Boolean.class);
        if (Boolean.TRUE.equals(active) && expiresAt > System.currentTimeMillis()) {
            chatStartTime = startsAt > 0 ? startsAt : Math.max(0L, expiresAt - CHAT_SESSION_DURATION);
            chatExpiresAt = expiresAt;
            String appointmentId = snapshot.child("appointmentId").getValue(String.class);
            currentAppointment = new com.haset.hasetapp.database.entities.AppointmentEntity();
            currentAppointment.setAppointmentId(appointmentId != null ? appointmentId : "");
            currentAppointment.setPatientId(patientId);
            currentAppointment.setDoctorId(doctorId);
            currentAppointment.setChatStartTime(chatStartTime);
            currentAppointment.setChatActive(true);
            isChatSessionActive = true;
            canSendInChat = true;
            chatDisabledMessage = null;
            setChatInputEnabled(true);
            startChatDurationTracker();
            return true;
        }
        return false;
    }

    private void queryAppointmentsForChatAccess() {
        String role = preferenceManager.getUserRole();
        String queryField = Constants.ROLE_DOCTOR.equals(role) ? "doctorId" : "patientId";
        com.haset.hasetapp.utils.FirebaseHelper.getAppointmentsRef()
            .orderByChild(queryField)
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    boolean hasChatAppointment = false;
                    boolean hasApproved = false;
                    boolean hasApprovedPaid = false;
                    boolean onlyExpiredPaidSession = false;
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        String patientId = child.child("patientId").getValue(String.class);
                        String doctorId = child.child("doctorId").getValue(String.class);
                        String status = child.child("status").getValue(String.class);
                        String paymentStatus = child.child("paymentStatus").getValue(String.class);
                        String appointmentType = child.child("appointmentType").getValue(String.class);
                        boolean otherUserMatches = Constants.ROLE_DOCTOR.equals(role)
                                ? chatUserId.equals(patientId)
                                : chatUserId.equals(doctorId);
                        boolean isOnlineChat = appointmentType == null
                                || "online chat".equalsIgnoreCase(appointmentType);
                        if (!otherUserMatches || !isOnlineChat) {
                            continue;
                        }

                        hasChatAppointment = true;
                        if (!Constants.STATUS_APPROVED.equalsIgnoreCase(status)) {
                            continue;
                        }

                        hasApproved = true;
                        if (!"paid".equalsIgnoreCase(paymentStatus)) {
                            continue;
                        }

                        hasApprovedPaid = true;
                        long existingStart = getLongChild(child, "chatStartsAt", getLongChild(child, "chatStartTime", 0L));
                        long existingExpires = getLongChild(child, "chatExpiresAt", 0L);
                        if (existingExpires > 0 && System.currentTimeMillis() > existingExpires) {
                            onlyExpiredPaidSession = true;
                            continue;
                        }

                        com.haset.hasetapp.database.entities.AppointmentEntity appointment =
                                child.getValue(com.haset.hasetapp.database.entities.AppointmentEntity.class);
                        if (appointment != null) {
                            appointment.setAppointmentId(child.getKey());
                            appointment.setChatStartTime(existingStart);
                            currentAppointment = appointment;
                            startChatSession(appointment);
                            break;
                        }
                    }

                    if (!hasApprovedPaid) {
                        if (!hasChatAppointment) {
                            disableChatSending(getString(R.string.chat_no_appointment_found));
                        } else if (!hasApproved) {
                            disableChatSending(getString(R.string.access_denied_appointment));
                        } else {
                            disableChatSending(getString(R.string.chat_payment_required));
                        }
                    } else if (!canSendInChat && onlyExpiredPaidSession) {
                        disableChatSending(getString(R.string.chat_session_expired_pay_again));
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Log.e("ChatActivity", "Error checking appointment status", error.toException());
                }
            });
    }

    private long getLongChild(com.google.firebase.database.DataSnapshot snapshot, String childName, long fallback) {
        Long value = snapshot.child(childName).getValue(Long.class);
        if (value != null) return value;
        Integer intValue = snapshot.child(childName).getValue(Integer.class);
        return intValue != null ? intValue.longValue() : fallback;
    }
    
    private void startChatSession(com.haset.hasetapp.database.entities.AppointmentEntity appointment) {
        long now = System.currentTimeMillis();
        chatStartTime = appointment.getChatStartTime() > 0 ? appointment.getChatStartTime() : now;
        chatExpiresAt = chatStartTime + CHAT_SESSION_DURATION;
        if (now > chatExpiresAt) {
            disableChatSending(getString(R.string.chat_session_expired_pay_again));
            return;
        }

        isChatSessionActive = true;
        canSendInChat = true;
        chatDisabledMessage = null;
        setChatInputEnabled(true);

        appointment.setChatStartTime(chatStartTime);
        appointment.setChatActive(true);

        java.util.Map<String, Object> appointmentUpdates = new java.util.HashMap<>();
        appointmentUpdates.put("chatStartsAt", chatStartTime);
        appointmentUpdates.put("chatStartTime", chatStartTime);
        appointmentUpdates.put("chatExpiresAt", chatExpiresAt);
        appointmentUpdates.put("isChatActive", true);
        appointmentUpdates.put("updatedAt", now);
        com.haset.hasetapp.utils.FirebaseHelper.getAppointmentsRef()
                .child(appointment.getAppointmentId())
                .updateChildren(appointmentUpdates);

        java.util.Map<String, Object> session = new java.util.HashMap<>();
        session.put("appointmentId", appointment.getAppointmentId());
        session.put("patientId", appointment.getPatientId());
        session.put("doctorId", appointment.getDoctorId());
        session.put("chatStartsAt", chatStartTime);
        session.put("chatExpiresAt", chatExpiresAt);
        session.put("isChatActive", true);
        session.put("createdAt", now);
        com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase()
                .getReference("chat_sessions")
                .child(chatRoomId)
                .setValue(session)
                .addOnFailureListener(error -> {
                    Log.e("ChatActivity", "Unable to persist chat session", error);
                    disableChatSending("Unable to verify chat session.");
                });

        startChatDurationTracker();
    }

    private boolean ensureChatCanSend() {
        if (chatExpiresAt > 0 && System.currentTimeMillis() > chatExpiresAt) {
            disableChatSending(getString(R.string.chat_session_expired_pay_again));
            return false;
        }
        if (!canSendInChat) {
            Toast.makeText(this,
                    chatDisabledMessage != null ? chatDisabledMessage : getString(R.string.chat_checking_access),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void disableChatSending(String message) {
        canSendInChat = false;
        chatDisabledMessage = message;
        setChatInputEnabled(false);
        com.haset.hasetapp.utils.ErrorDisplay.toast(this, message);
    }

    private void setChatInputEnabled(boolean enabled) {
        if (etMessage != null) {
            etMessage.setEnabled(enabled);
            etMessage.setHint(enabled ? "" : (chatDisabledMessage != null
                    ? chatDisabledMessage
                    : getString(R.string.chat_session_ended_hint)));
        }
        if (btnSend != null) btnSend.setEnabled(enabled && etMessage != null && !etMessage.getText().toString().trim().isEmpty());
        if (ivAttach != null) {
            ivAttach.setEnabled(enabled);
            ivAttach.setAlpha(enabled ? 1.0f : 0.45f);
        }
        if (ivMic != null) {
            ivMic.setEnabled(enabled);
            ivMic.setAlpha(enabled ? 1.0f : 0.45f);
        }
    }
    
    private void startChatDurationTracker() {
        chatDurationHandler = new Handler(Looper.getMainLooper());
        chatDurationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isChatSessionActive) return;
                
                long elapsed = System.currentTimeMillis() - chatStartTime;
                long remaining = CHAT_SESSION_DURATION - elapsed;
                
                if (remaining <= 0) {
                    endChatSession(true);
                    return;
                }
                
                // Check for notifications
                for (long notifyTime : NOTIFICATION_TIMES) {
                    if (remaining <= notifyTime && remaining > notifyTime - 60000) {
                        showTimeWarningNotification(notifyTime);
                        break;
                    }
                }
                
                // Update every minute
                chatDurationHandler.postDelayed(this, 60000);
            }
        };
        
        chatDurationHandler.post(chatDurationRunnable);
    }
    
    private void showTimeWarningNotification(long timeRemaining) {
        String timeText;
        if (timeRemaining >= 30 * 60 * 1000L) {
            timeText = "30 minutes";
        } else if (timeRemaining >= 10 * 60 * 1000L) {
            timeText = "10 minutes";
        } else {
            timeText = "5 minutes";
        }
        
        String message = "Your chat session will end in " + timeText + ". Please wrap up your conversation.";
        
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show();
    }
    
    private void endChatSession(boolean forceEnd) {
        if (!isChatSessionActive) return;
        
        isChatSessionActive = false;
        
        if (chatDurationHandler != null && chatDurationRunnable != null) {
            chatDurationHandler.removeCallbacks(chatDurationRunnable);
        }
        
        if (currentAppointment != null) {
            long chatEndTime = System.currentTimeMillis();
            long duration = chatEndTime - chatStartTime;
            
            currentAppointment.setChatEndTime(chatEndTime);
            currentAppointment.setChatDuration(duration);
            currentAppointment.setChatActive(false);
            if (forceEnd) {
                currentAppointment.setStatus(com.haset.hasetapp.utils.Constants.STATUS_COMPLETED);
            }
            
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("chatEndTime", chatEndTime);
            updates.put("chatDuration", duration);
            updates.put("isChatActive", false);
            updates.put("updatedAt", chatEndTime);
            if (forceEnd) {
                updates.put("status", com.haset.hasetapp.utils.Constants.STATUS_COMPLETED);
            }
            com.haset.hasetapp.utils.FirebaseHelper.getAppointmentsRef()
                    .child(currentAppointment.getAppointmentId())
                    .updateChildren(updates);
            java.util.Map<String, Object> sessionUpdates = new java.util.HashMap<>();
            sessionUpdates.put("isChatActive", false);
            sessionUpdates.put("endedAt", chatEndTime);
            com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase()
                    .getReference("chat_sessions")
                    .child(chatRoomId)
                    .updateChildren(sessionUpdates);
        }
        
        if (forceEnd) {
            disableChatSending("Your chat session has ended.");
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "Your chat session has ended. Thank you for using our service.",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show();
            
            if (chatDurationHandler != null) {
                chatDurationHandler.postDelayed(() -> {
                    if (!isFinishing()) finish();
                }, 3000);
            }
        }
    }
    
    // Implement methods from ChatMoreOptionsBottomSheet.OnOptionSelectedListener
    // Implement methods from ChatMoreOptionsBottomSheet.OnOptionSelectedListener
    @Override
    public void onSearchMessagesSelected() {
        if (llSearchBar != null) {
            llSearchBar.setVisibility(View.VISIBLE);
            if (etSearchMessages != null) {
                etSearchMessages.requestFocus();
                // Show keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearchMessages, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    }

    @Override
    public void onViewContactSelected() {
        if (chatUserId == null) return;
        
        // Show loading or verify doctor
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorById(chatUserId, new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Doctor>() {
            @Override
            public void onSuccess(Doctor doctor) {
                if (doctor != null) {
                    DoctorDetailsBottomSheet bottomSheet = DoctorDetailsBottomSheet.newInstance(doctor);
                    bottomSheet.show(getSupportFragmentManager(), DoctorDetailsBottomSheet.TAG);
                } else {
                     Toast.makeText(ChatActivity.this, R.string.error_loading_contact, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                // If fetching doctor fails (e.g. regular user), just show a simple toast or handle gracefully
                Toast.makeText(ChatActivity.this, R.string.contact_info_unavailable, Toast.LENGTH_SHORT).show();
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
            }
        });
    }

    private void openPrescriptionSheet() {
        com.haset.hasetapp.utils.AddPrescriptionBottomSheet prescriptionSheet = new com.haset.hasetapp.utils.AddPrescriptionBottomSheet();
        Bundle args = new Bundle();
        args.putString("patientId", chatUserId);
        args.putString("patientName", chatUserName);
        if (currentAppointment != null && currentAppointment.getAppointmentId() != null
                && !currentAppointment.getAppointmentId().trim().isEmpty()) {
            args.putString("appointmentId", currentAppointment.getAppointmentId());
        }
        prescriptionSheet.setArguments(args);
        prescriptionSheet.show(getSupportFragmentManager(), "AddPrescriptionBottomSheet");
    }

    private void openServiceSheet() {
        if (!ensureChatCanSend()) return;
        // Get doctor info
        String doctorId = preferenceManager.getUserId();
        String doctorName = preferenceManager.getUserName();
        
        AddServiceBottomSheet serviceSheet = AddServiceBottomSheet.newInstance(chatUserId, chatUserName, doctorId, doctorName);
        serviceSheet.setOnServiceCreatedListener(service -> {
            // Send service message to chat
            sendServiceMessage(service);
        });
        serviceSheet.show(getSupportFragmentManager(), "AddServiceBottomSheet");
    }

    private void sendServiceMessage(com.haset.hasetapp.models.Service service) {
        if (!ensureChatCanSend()) return;
        String serviceJson = new com.google.gson.Gson().toJson(service);
        
        ChatMessage message = new ChatMessage(currentUserId, chatUserId, serviceJson);
        message.setSenderName(preferenceManager.getUserName());
        message.setReceiverName(chatUserName);
        message.setMessageType("service");
        message.setMessageStatus("sent");
        message.setTimestamp(System.currentTimeMillis());
        
        String messageId = viewModel.sendMessage(chatRoomId, message, currentUserId, chatUserId,
            preferenceManager.getUserName(), chatUserName);
        if (messageId == null) {
            Toast.makeText(this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("serviceId", service.getServiceId());
        request.put("messageId", messageId);
        request.put("chatRoomId", chatRoomId);
        request.put("doctorId", currentUserId);
        request.put("patientId", chatUserId);
        request.put("serviceName", service.getServiceName());
        request.put("appointmentFee", service.getAppointmentFee());
        request.put("patientPercentage", service.getPatientPercentage());
        request.put("patientPayAmount", service.getPatientPayAmount());
        request.put("status", "pending");
        request.put("createdAt", com.google.firebase.database.ServerValue.TIMESTAMP);
        com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase()
                .getReference("service_payment_requests")
                .child(service.getServiceId())
                .setValue(request)
                .addOnSuccessListener(ignored -> Toast.makeText(
                        ChatActivity.this, R.string.service_payment_sent, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> Toast.makeText(
                        ChatActivity.this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void onServicePayClicked(String messageId, Service service) {
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("service_message_id", messageId);
        paymentIntent.putExtra("consultation_fee", service.getPatientPayAmount());
        paymentIntent.putExtra("chat_room_id", chatRoomId);
        
        // Pass the doctor object if available
        if (chatDoctor != null) {
            paymentIntent.putExtra("doctor", chatDoctor);
        } else {
            // Fallback: create a minimal doctor object from available info
            Doctor minimalDoctor = new Doctor();
            minimalDoctor.setUserId(chatUserId);
            minimalDoctor.setFullName(chatUserName);
            paymentIntent.putExtra("doctor", minimalDoctor);
        }
        
        startActivityForResult(paymentIntent, SERVICE_PAYMENT_REQUEST_CODE);
    }

    private void fetchChatDoctorDetails() {
        if (chatUserId == null) return;
        
        com.haset.hasetapp.utils.FirebaseHelper.getDoctorById(chatUserId, new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Doctor>() {
            @Override
            public void onSuccess(Doctor doctor) {
                chatDoctor = doctor;
            }

            @Override
            public void onError(String error) {
                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
            }
        });
    }

    private void updateServicePaymentStatus(String messageId, boolean paid, int transactionId) {
        com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase().getReference("messages")
            .child(chatRoomId)
            .child(messageId)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null && "service".equalsIgnoreCase(message.getMessageType())) {
                        try {
                            Gson gson = new Gson();
                            Service service = gson.fromJson(message.getMessage(), Service.class);
                            if (service != null) {
                                Map<String, Object> paidUpdate = new HashMap<>();
                                paidUpdate.put("status", paid ? "paid" : "pending");
                                paidUpdate.put("transactionId", String.valueOf(transactionId));
                                paidUpdate.put("paidAt", com.google.firebase.database.ServerValue.TIMESTAMP);
                                com.haset.hasetapp.utils.FirebaseHelper.getFirebaseDatabase()
                                        .getReference("service_payment_requests")
                                        .child(service.getServiceId())
                                        .updateChildren(paidUpdate)
                                        .addOnSuccessListener(ignored -> Toast.makeText(
                                                ChatActivity.this,
                                                R.string.payment_successful_chat,
                                                Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(error -> Log.e(
                                                "ChatActivity", "Unable to persist service payment status", error));
                            }
                        } catch (Exception e) {
                            Log.e("ChatActivity", "Error updating payment status", e);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Log.e("ChatActivity", "Error updating payment status", error.toException());
                }
            });
    }

    private void clearNotificationBadge() {
        // Mark the conversation as read to clear the badge
        // Use chatRoomId (conversationId) instead of chatUserId
        if (notificationBadgeHelper != null && chatRoomId != null) {
            notificationBadgeHelper.markConversationAsRead(chatRoomId);
            Log.d("ChatActivity", "Cleared notification badge for conversation: " + chatRoomId);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack); // Initialize btnBack
        // Removed toolbar = findViewById(R.id.toolbar);
        tvChatName = findViewById(R.id.tvChatName);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        ivChatProfile = findViewById(R.id.ivChatProfile);
        ivMoreOptions = findViewById(R.id.ivMoreOptions);
        shimmerChatProfile = findViewById(R.id.shimmerChatProfile);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        ivAttach = findViewById(R.id.ivAttach);
        ivMic = findViewById(R.id.ivMic);
        ivVideoCall = findViewById(R.id.ivVideoCall);
        
        btnSend = findViewById(R.id.btnSend);

        llTypingIndicator = findViewById(R.id.llTypingIndicator);
        tvTypingText = findViewById(R.id.tvTypingText);
        
        // Initialize typing handler - must be on main thread
        if (typingHandler == null) {
            typingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        
        llSearchBar = findViewById(R.id.llSearchBar);
        etSearchMessages = findViewById(R.id.etSearchMessages);
        ivCloseSearch = findViewById(R.id.ivCloseSearch);
        
        // Initialize reply views
        llReplyContainer = findViewById(R.id.llReplyContainer);
        tvReplyName = findViewById(R.id.tvReplyName);
        tvReplyText = findViewById(R.id.tvReplyText);
        ivCancelReply = findViewById(R.id.ivCancelReply);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(currentUserId);
        // Prefer the image passed by the launcher, otherwise resolve it from Firebase
        String launcherImage = getIntent().getStringExtra(Constants.EXTRA_CHAT_USER_IMAGE);
        if (launcherImage != null && !launcherImage.isEmpty()) {
            chatAdapter.setOtherUserProfileImageUrl(launcherImage);
        } else if (chatUserId != null) {
            com.haset.hasetapp.utils.ProfilePhotoHelper.fetchProfilePhotoUrl(this, chatUserId, url -> {
                if (url != null && chatAdapter != null) {
                    chatAdapter.setOtherUserProfileImageUrl(url);
                }
            });
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
        
        // Performance optimizations
        rvMessages.setHasFixedSize(true);
        rvMessages.setItemViewCacheSize(20);
        rvMessages.setDrawingCacheEnabled(true);
        rvMessages.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        chatAdapter.setOnMessageLongClickListener((message, view) -> {
            showChatContextMenu(message, view);
        });

        chatAdapter.setOnReplyClickListener(messageId -> {
            int position = chatAdapter.getPositionForMessage(messageId);
            if (position != -1) {
                rvMessages.smoothScrollToPosition(position);
            } else {
                Toast.makeText(this, R.string.original_message_not_found, Toast.LENGTH_SHORT).show();
            }
        });

        chatAdapter.setOnPrescriptionClickListener(prescriptionId -> {
            openPrescriptionDetails(prescriptionId);
        });

        chatAdapter.setOnServicePayClickListener((messageId, service) -> {
            onServicePayClicked(messageId, service);
        });

        chatAdapter.setOnE2eeHeaderClickListener(() -> {
            new com.haset.hasetapp.fragments.LearnMoreBottomSheet()
                .show(getSupportFragmentManager(), "LearnMoreBottomSheet");
        });

        chatAdapter.setOnMessageClickListener(message -> {
            if (chatAdapter.isSelectionMode()) {
                if (currentUserId.equals(message.getSenderId())) {
                    toggleMessageSelection(message);
                } else {
                    Toast.makeText(this, R.string.only_sent_messages_selectable, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            String type = message.getMessageType();
            String url = message.getAttachmentUrl();
            
            if ("prescription".equalsIgnoreCase(type)) {
                openPrescriptionDetails(message.getPrescriptionId());
                return;
            }

            if (url == null || url.isEmpty()) return;

            if ("image".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, FullScreenImageActivity.class);
                intent.putExtra("image_url", url);
                startActivity(intent);
                overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
            } else if ("video".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, VideoPlayerActivity.class);
                intent.putExtra("video_url", url);
                startActivity(intent);
                overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
            } else if ("document".equalsIgnoreCase(type)) {
                openDocumentMessage(message);
            } else if ("audio".equalsIgnoreCase(type)) {
                // Play audio inline and bind UI for wave visualization
                String audioUrl = message.getAttachmentUrl();
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    playAudioInline(audioUrl, message);
                } else {
                    Log.e("ChatActivity", "No audio URL for message: " + message.getMessageId());
                }
            }
        });

        // Scroll to bottom when keyboard appears
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMessages.postDelayed(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });
    }

    private void toggleMessageSelection(ChatMessage message) {
        chatAdapter.toggleSelection(message);
        if (chatAdapter.getSelectedCount() == 0) {
            if (messageSelectionActionMode != null) messageSelectionActionMode.finish();
            return;
        }

        if (messageSelectionActionMode == null) {
            messageSelectionActionMode = startSupportActionMode(messageSelectionCallback);
        }
        updateMessageSelectionTitle();
    }

    private void updateMessageSelectionTitle() {
        if (messageSelectionActionMode != null && chatAdapter != null) {
            messageSelectionActionMode.setTitle(
                    getString(R.string.selected_messages_count, chatAdapter.getSelectedCount()));
        }
    }

    private final androidx.appcompat.view.ActionMode.Callback messageSelectionCallback =
            new androidx.appcompat.view.ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, android.view.Menu menu) {
            android.view.MenuItem delete = menu.add(0, R.id.action_delete, 0, R.string.delete_messages);
            delete.setIcon(R.drawable.ic_delete);
            delete.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, android.view.Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, android.view.MenuItem item) {
            if (item.getItemId() != R.id.action_delete) return false;

            List<ChatMessage> selected = new java.util.ArrayList<>(chatAdapter.getSelectedMessages());
            int count = selected.size();
            new androidx.appcompat.app.AlertDialog.Builder(ChatActivity.this)
                    .setTitle(R.string.delete_messages)
                    .setMessage(getString(R.string.delete_selected_messages_confirm, count))
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        viewModel.deleteMessages(chatRoomId, selected, currentUserId, chatUserId);
                        Toast.makeText(ChatActivity.this,
                                getString(R.string.messages_deleted_count, count), Toast.LENGTH_SHORT).show();
                        mode.finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }

        @Override
        public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
            messageSelectionActionMode = null;
            if (chatAdapter != null) chatAdapter.clearSelection();
        }
    };

    private void setupObservers() {
        if (chatRoomId == null) return;
        
        viewModel.getMessages(chatRoomId, currentUserId, chatUserId).observe(this, messages -> {
            if (messages != null) {
                // Determine new messages and mark them read if necessary
                for (ChatMessage message : messages) {
                    if (message.getReceiverId().equals(currentUserId) && !message.isRead()) {
                        String sourceRoomId = message.getSourceRoomId() != null
                                ? message.getSourceRoomId() : chatRoomId;
                        viewModel.markAsRead(sourceRoomId, message.getMessageId());
                        
                        // Decrement unread count in SharedPreferences
                        if (notificationBadgeHelper != null) {
                            int currentCount = notificationBadgeHelper.getConversationUnreadCount(chatRoomId);
                            if (currentCount > 0) {
                                notificationBadgeHelper.setConversationUnreadCount(chatRoomId, currentCount - 1);
                                notificationBadgeHelper.decrementUnreadCount();
                            }
                        }
                    }
                }
                
                List<ChatMessage> displayMessages = messages.isEmpty()
                        ? buildFallbackConversationMessage()
                        : messages;
                chatAdapter.setMessages(displayMessages);
                
                // Scroll to bottom on new messages
                rvMessages.post(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            }
        });

        // Observe Typing Status
        viewModel.getTypingStatus(chatRoomId, chatUserId).observe(this, isTyping -> {
            if (isTyping != null && isTyping) {
                showTypingIndicator();
            } else {
                hideTypingIndicator();
            }
        });

        viewModel.getUploadSuccess().observe(this, result -> {
            if (result != null) {
                // Update the existing placeholder message instead of creating a new one
                viewModel.updateMessageAttachment(chatRoomId, result.messageId, result.downloadUrl, "sent");
            }
        });

        viewModel.getUploadStatus().observe(this, status -> {
            if (status != null && status.startsWith("Upload failed")) {
                // In a real app, you might want to mark the message as "Failed" in the UI
                // For now, we'll keep the toast but the progress bar will naturally stop
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openPrescriptionDetails(String prescriptionId) {
        if (prescriptionId == null || prescriptionId.isEmpty()) return;
        
        PrescriptionDetailBottomSheet bottomSheet = PrescriptionDetailBottomSheet.newInstance(prescriptionId);
        bottomSheet.show(getSupportFragmentManager(), "PrescriptionDetailBottomSheet");
    }

    private void finalizeFileMessage(String downloadUrl, String uploadedFileName, long fileSize, String messageType) {
        if (!ensureChatCanSend()) return;
        String previewText = messageType.substring(0, 1).toUpperCase() + messageType.substring(1);
        if (messageType.equals("audio")) previewText = getString(R.string.voice_note);
        
        ChatMessage message = new ChatMessage(currentUserId, chatUserId, previewText);
        message.setSenderName(preferenceManager.getUserName());
        message.setReceiverName(chatUserName);
        message.setMessageType(messageType);
        message.setAttachmentUrl(downloadUrl);
        message.setAttachmentFileName(uploadedFileName);
        if (fileSize > 0) {
            message.setAttachmentSize(com.haset.hasetapp.utils.FileUploadHelper.formatFileSize(fileSize));
        } else {
            message.setAttachmentSize("N/A");
        }
        message.setTimestamp(System.currentTimeMillis());
        
        viewModel.sendMessage(chatRoomId, message, currentUserId, chatUserId, 
            preferenceManager.getUserName(), chatUserName);
    }

    private void sendMessage() {
        Log.d("ChatActivity", "sendMessage called");
        if (!ensureChatCanSend()) return;
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        ChatMessage message = new ChatMessage(currentUserId, chatUserId, messageText);
        message.setSenderName(preferenceManager.getUserName());
        message.setReceiverName(chatUserName);
        message.setTimestamp(System.currentTimeMillis());
        
        // Attach reply information if replying
        if (replyingToMessage != null) {
            message.setReplyToMessageId(replyingToMessage.getMessageId());
            message.setReplyToText(getReplyPreviewText(replyingToMessage));
            message.setReplyToSenderName(replyingToMessage.getSenderName());
        }

        viewModel.sendMessage(chatRoomId, message, currentUserId, chatUserId, 
            preferenceManager.getUserName(), chatUserName);

        etMessage.setText("");
        
        // Clear reply preview after sending
        if (replyingToMessage != null) {
            cancelReply();
        }
        stopTyping();
    }

    private void showReplyPreview(ChatMessage message) {
        replyingToMessage = message;
        llReplyContainer.setVisibility(View.VISIBLE);
        
        // Set the sender name
        String senderName = message.getSenderId().equals(currentUserId) ? "You" : message.getSenderName();
        tvReplyName.setText(senderName);
        
        // Set the message text (or file type for attachments)
        tvReplyText.setText(getReplyPreviewText(message));
        
        // Set up cancel button
        ivCancelReply.setOnClickListener(v -> cancelReply());
        
        // Focus on input field
        etMessage.requestFocus();
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void cancelReply() {
        replyingToMessage = null;
        llReplyContainer.setVisibility(View.GONE);
    }

    private String getReplyPreviewText(ChatMessage message) {
        if (message == null) return "";
        String type = message.getMessageType();
        if (!"text".equals(type)) {
            String label = (type == null || type.isEmpty()) ? "Message"
                    : type.substring(0, 1).toUpperCase() + type.substring(1);
            String fileName = message.getAttachmentFileName();
            if (fileName != null && !fileName.isEmpty()) {
                label = label + " · " + fileName;
            }
            return label;
        }
        String text = message.getMessage();
        return (text == null || text.isEmpty()) ? "Message" : text;
    }

    private void updateConversation(String userId, String otherUserId, String otherUserName, String lastMessage, long timestamp, String senderId) {
    }

    private String generateChatRoomId(String userId1, String userId2) {
        return com.haset.hasetapp.utils.FirebaseHelper.generateChatRoomId(userId1, userId2);
    }

    /**
     * Mark a message as read when receiver views the chat
     */
    private void markMessageAsRead(String messageId) {
    }

    /**
     * Mark all unread messages in this chat as read when chat is opened
     */
    private void markAllMessagesAsRead() {
        if (chatRoomId == null || currentUserId == null) return;
        
        viewModel.markAllAsRead(chatRoomId, currentUserId, chatUserId);
        
        if (notificationBadgeHelper != null) {
            notificationBadgeHelper.markConversationAsRead(chatRoomId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == SERVICE_PAYMENT_REQUEST_CODE && data != null) {
                String messageId = data.getStringExtra("service_message_id");
                if (messageId != null) {
                    updateServicePaymentStatus(
                            messageId,
                            true,
                            data.getIntExtra("transaction_id", -1));
                }
            } else if (requestCode == Constants.REQUEST_CODE_CAMERA) {
                // Camera result - use the file we created
                if (currentImageUri != null && currentImageFile != null && currentImageFile.exists()) {
                    String fileName = currentImageFile.getName();
                    long fileSize = currentImageFile.length();
                    
                    // Create and send placeholder first
                    ChatMessage placeholder = createPlaceholder("image", fileName, fileSize);
                    String messageId = viewModel.sendMessage(chatRoomId, placeholder, currentUserId, chatUserId, preferenceManager.getUserName(), chatUserName);
                    
                    viewModel.uploadAttachment(this, currentImageUri, "image", fileName, fileSize, messageId);
                } else {
                    Toast.makeText(this, R.string.failed_capture_image, Toast.LENGTH_SHORT).show();
                }
            } else if (data != null) {
                Uri selectedFileUri = data.getData();
                
                if (selectedFileUri != null) {
                    String fileName = getFileName(selectedFileUri);
                    String messageType = "document";
                    
                    switch (requestCode) {
                        case Constants.REQUEST_CODE_DOCUMENT:
                            messageType = "document";
                            break;
                        case Constants.REQUEST_CODE_IMAGE:
                            messageType = "image";
                            break;
                        case Constants.REQUEST_CODE_AUDIO:
                            messageType = "audio";
                            break;
                        case Constants.REQUEST_CODE_VIDEO:
                            messageType = "video";
                            break;
                    }
                    
                    // Send the file as a message
                    sendFileMessage(selectedFileUri, fileName, messageType);
                }
            }
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Error getting filename from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "file";
    }
    
    private void showChatContextMenu(ChatMessage message, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        int menuRes = "text".equals(message.getMessageType()) ? R.menu.menu_chat_text : R.menu.menu_chat_file;
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());

        boolean isOwn = currentUserId.equals(message.getSenderId());
        popup.getMenu().findItem(R.id.action_delete).setVisible(isOwn);
        popup.getMenu().findItem(R.id.action_select).setVisible(isOwn);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_copy) {
                copyToClipboard(message.getMessage());
                return true;
            } else if (id == R.id.action_select) {
                toggleMessageSelection(message);
                return true;
            } else if (id == R.id.action_delete) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.delete_message)
                        .setMessage(R.string.delete_message_confirm)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            viewModel.deleteMessage(chatRoomId, message, currentUserId, chatUserId);
                            Toast.makeText(this, R.string.message_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            } else if (id == R.id.action_download) {
                // TODO: Implement download logic
                Toast.makeText(this, R.string.downloading_file, Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_share) {
                // TODO: Implement sharing logic
                Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_reply || id == R.id.action_forward) {
                if (id == R.id.action_reply) {
                    showReplyPreview(message);
                } else {
                    Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Chat Message", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private long getFileSize(Uri uri) {
        if (uri == null) return 0;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (index != -1) {
                        return cursor.getLong(index);
                    }
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Error getting file size", e);
            }
        } else if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                return file.length();
            }
        }
        return 0;
    }

    private void sendFileMessage(Uri fileUri, String fileName, String messageType) {
        if (!ensureChatCanSend()) return;
        if (fileUri == null) return;
        long fileSize = getFileSize(fileUri);
        
        // Create and send placeholder first
        ChatMessage placeholder = createPlaceholder(messageType, fileName, fileSize);
        String messageId = viewModel.sendMessage(chatRoomId, placeholder, currentUserId, chatUserId, preferenceManager.getUserName(), chatUserName);
        
        viewModel.uploadAttachment(this, fileUri, messageType, fileName, fileSize, messageId);
    }

    private ChatMessage createPlaceholder(String messageType, String fileName, long fileSize) {
        String previewText = messageType.substring(0, 1).toUpperCase() + messageType.substring(1);
        if (messageType.equals("audio")) previewText = getString(R.string.voice_note);
        
        ChatMessage message = new ChatMessage(currentUserId, chatUserId, previewText);
        message.setSenderName(preferenceManager.getUserName());
        message.setReceiverName(chatUserName);
        message.setMessageType(messageType);
        message.setAttachmentFileName(fileName);
        if (fileSize > 0) {
            message.setAttachmentSize(com.haset.hasetapp.utils.FileUploadHelper.formatFileSize(fileSize));
        } else {
            message.setAttachmentSize("N/A");
        }
        message.setMessageStatus("uploading");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    private void setupTypingIndicator() {
        // Handled by setupObservers() in MVVM
    }

    private List<ChatMessage> buildFallbackConversationMessage() {
        String text = fallbackLastMessage != null ? fallbackLastMessage.trim() : "";
        if (text.isEmpty() || isSystemConversationPreview(text)) {
            return java.util.Collections.emptyList();
        }

        String senderId = fallbackLastMessageSenderId != null && !fallbackLastMessageSenderId.trim().isEmpty()
                ? fallbackLastMessageSenderId
                : chatUserId;
        String receiverId = currentUserId != null && currentUserId.equals(senderId)
                ? chatUserId
                : currentUserId;
        ChatMessage message = new ChatMessage(senderId, receiverId, text);
        message.setMessageId("conversation-summary-" + Math.max(fallbackLastMessageTimestamp, 0L));
        message.setSenderName(currentUserId != null && currentUserId.equals(senderId)
                ? preferenceManager.getUserName()
                : chatUserName);
        message.setReceiverName(currentUserId != null && currentUserId.equals(senderId)
                ? chatUserName
                : preferenceManager.getUserName());
        message.setMessageType("text");
        message.setMessageStatus("sent");
        message.setTimestamp(fallbackLastMessageTimestamp > 0
                ? fallbackLastMessageTimestamp
                : System.currentTimeMillis());
        message.setRead(true);
        return java.util.Collections.singletonList(message);
    }

    private boolean isSystemConversationPreview(String text) {
        String normalized = text.trim().toLowerCase(java.util.Locale.US);
        return normalized.equals("chat appointment")
                || normalized.equals("chat appointment booked")
                || normalized.equals("appointment booked");
    }

    /**
     * Indicate that current user is typing
     */
    private void indicateTyping() {
        if (chatRoomId == null || currentUserId == null) return;
        
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        
        viewModel.setTyping(chatRoomId, currentUserId, true);
        
        typingRunnable = () -> stopTyping();
        typingHandler.postDelayed(typingRunnable, TYPING_TIMEOUT);
    }

    /**
     * Stop typing indicator
     */
    private void stopTyping() {
        if (chatRoomId == null || currentUserId == null) return;
        
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
            typingRunnable = null;
        }
        
        viewModel.setTyping(chatRoomId, currentUserId, false);
    }

    /**
     * Show typing indicator for other user
     */
    private void showTypingIndicator() {
        if (llTypingIndicator != null && tvTypingText != null) {
            tvTypingText.setText(getString(R.string.typing_indicator, chatUserName));
            llTypingIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide typing indicator
     */
    private void hideTypingIndicator() {
        if (llTypingIndicator != null) {
            llTypingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop typing when user leaves chat
        stopTyping();
        // Clear chatting flag
        com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).setCurrentlyChattingWith(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Fallback: Clear chatting flag
        com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).setCurrentlyChattingWith(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Notify MessageNotificationManager that we are chatting with this user
        if (chatUserId != null) {
            com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).setCurrentlyChattingWith(chatUserId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // End chat session if active
        endChatSession(false);
        // Stop typing
        stopTyping();
        // Clean up handler
        if (typingHandler != null) {
            typingHandler.removeCallbacksAndMessages(null);
        }
        
        // Clean up voice recording components
        if (voiceRecordingBottomSheet != null) {
            voiceRecordingBottomSheet.cleanup();
            voiceRecordingBottomSheet = null;
        }
        
        if (voiceRecorderHelper != null) {
            voiceRecorderHelper.cleanup();
            voiceRecorderHelper = null;
        }
        
        // Clean up voice players
        if (voicePlayers != null) {
            for (ChatVoicePlayer voicePlayer : voicePlayers.values()) {
                if (voicePlayer != null) {
                    voicePlayer.cleanup();
                }
            }
            voicePlayers.clear();
            voicePlayers = null;
        }
        
        if (chatDurationHandler != null) {
            chatDurationHandler.removeCallbacksAndMessages(null);
        }
        
        // Clear chatting flag
        com.haset.hasetapp.utils.MessageNotificationManager.getInstance(this).setCurrentlyChattingWith(null);
    }
    
    /**
     * Play audio inline using ChatVoicePlayer
     */
    private void playAudioInline(String audioUrl, ChatMessage message) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            Log.e("ChatActivity", "Invalid audio URL for inline playback");
            return;
        }
        
        try {
            // Create or get voice player for this message
            ChatVoicePlayer voicePlayer = getOrCreateVoicePlayer(message);
            
            // Download and play the audio file
            String localPath = downloadAudioForPlayback(audioUrl);
            if (localPath != null) {
                voicePlayer.playAudio(localPath);
            }
            
        } catch (Exception e) {
            Log.e("ChatActivity", "Error playing audio inline: " + e.getMessage(), e);
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get or create voice player for message
     */
    private ChatVoicePlayer getOrCreateVoicePlayer(ChatMessage message) {
        // Store voice players by message ID to manage multiple playing instances
        if (voicePlayers == null) {
            voicePlayers = new HashMap<>();
        }
        
        String messageId = message.getMessageId();
        ChatVoicePlayer voicePlayer = voicePlayers.get(messageId);
        
        if (voicePlayer == null) {
            voicePlayer = new ChatVoicePlayer(this);
            voicePlayers.put(messageId, voicePlayer);
        }
        
        return voicePlayer;
    }
    
    /**
     * Download audio file for local playback
     */
    private String downloadAudioForPlayback(String audioUrl) {
        // For now, return the URL as-is (in production, you'd download to cache)
        // This is a simplified implementation - in production you'd download the file
        return audioUrl;
    }
    
    /**
     * Setup voice recording with press and hold on mic button
     */
    private void setupVoiceRecording() {
        ivMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start recording on press
                    startVoiceRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Stop recording on release
                    stopVoiceRecording();
                    return true;
            }
            return false;
        });
    }
    
    /**
     * Start voice recording - Bottom Sheet UI
     */
    private void startVoiceRecording() {
        if (!ensureChatCanSend()) return;
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        
        // Show voice recording bottom sheet
        voiceRecordingBottomSheet.show();
    }

    
    /**
     * Stop voice recording - Bottom Sheet UI
     */
    private void stopVoiceRecording() {
        if (!isRecordingVoice || voiceRecordingBottomSheet == null) {
            return;
        }
        
        // Stop recording via bottom sheet
        voiceRecordingBottomSheet.stopRecording();
    }
    
    /**
     * Send voice message - Optimized version
     */
    private void sendVoiceMessage(String audioFilePath, long duration) {
        android.util.Log.d("ChatActivity", "sendVoiceMessage called with: " + audioFilePath);
        if (!ensureChatCanSend()) return;
        
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            Log.e("ChatActivity", "Invalid audio file path");
            return;
        }
        
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Log.e("ChatActivity", "Audio file does not exist: " + audioFilePath);
                return;
            }
            
            android.util.Log.d("ChatActivity", "Audio file exists, size: " + audioFile.length());
            
            // Check file size (max 10MB)
            long fileSize = audioFile.length();
            if (fileSize > 10 * 1024 * 1024) {
                Log.e("ChatActivity", "Audio file too large: " + (fileSize / 1024 / 1024) + "MB");
                audioFile.delete();
                return;
            }
            
            // Create filename
            String fileName = "voice_" + System.currentTimeMillis() + ".m4a";
            
            // Format duration
            String durationStr = formatDuration(duration);
            
            // Create and send placeholder first
            ChatMessage placeholder = createPlaceholder("audio", fileName, fileSize);
            placeholder.setAttachmentDuration(durationStr);
            
            String messageId = viewModel.sendMessage(chatRoomId, placeholder, currentUserId, chatUserId, preferenceManager.getUserName(), chatUserName);
            
            android.util.Log.d("ChatActivity", "Message created with ID: " + messageId);
            
            // Upload the file
            viewModel.uploadAttachment(this, Uri.fromFile(audioFile), "audio", fileName, fileSize, messageId);
            
            // Log voice message sent
            AuditLogger.getInstance(this).logAction(
                "VOICE_MESSAGE_SENT",
                "Voice message sent: " + fileName + ", Duration: " + durationStr + ", Size: " + (fileSize / 1024) + "KB",
                "CHAT",
                chatRoomId
            );
            
            // Log performance
            Log.d("ChatActivity", String.format("Voice message sent: %s, Size: %dKB, Duration: %dms", 
                fileName, fileSize / 1024, duration));
            
        } catch (Exception e) {
            Log.e("ChatActivity", "Error sending voice message: " + e.getMessage(), e);
            // Clean up file on error
            new File(audioFilePath).delete();
        }
    }
    
    /**
     * Format duration in milliseconds to mm:ss format
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Toast.makeText(this, R.string.microphone_permission_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Check camera permission and open camera
     */
    private void checkCameraPermissionAndOpenCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }
    
    private String getMimeType(String type, String url) {
        if ("image".equalsIgnoreCase(type)) return "image/*";
        if ("video".equalsIgnoreCase(type)) return "video/*";
        if ("audio".equalsIgnoreCase(type)) return "audio/*";
        
        // For documents, try to be more specific based on extension
        String path = Uri.parse(url).getPath();
        if (path == null) return "*/*";

        int extensionStart = path.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == path.length() - 1) return "*/*";

        String extension = path.substring(extensionStart + 1).toLowerCase(java.util.Locale.US);
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc":
            case "docx": return "application/msword";
            case "xls":
            case "xlsx": return "application/vnd.ms-excel";
            case "ppt":
            case "pptx": return "application/vnd.ms-powerpoint";
            case "txt": return "text/plain";
            default: return "*/*";
        }
    }

    private void openDocumentMessage(ChatMessage message) {
        String url = message.getAttachmentUrl();
        if (url == null || url.isEmpty()) return;

        String mimeType = getMimeType(message.getMessageType(), url);
        String fileName = message.getAttachmentFileName();
        Uri uri = Uri.parse(url);

        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            downloadAndOpenDocument(url, mimeType, fileName);
        } else {
            openDocumentUri(uri, mimeType);
        }
    }

    private void downloadAndOpenDocument(String url, String mimeType, String fileName) {
        Toast.makeText(this, "Opening file...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                try (Response response = DOCUMENT_HTTP_CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("HTTP " + response.code());
                    }

                    File cacheDir = new File(getCacheDir(), "chat_documents");
                    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                        throw new IOException("Unable to create cache directory");
                    }

                    File file = new File(cacheDir, sanitizeFileName(fileName, url, mimeType));
                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    Uri localUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                    runOnUiThread(() -> openDocumentUri(localUri, mimeType));
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Failed to download document before opening", e);
                runOnUiThread(() -> Toast.makeText(this, "File is not accessible. Please upload it again.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openDocumentUri(Uri uri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_app_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private String sanitizeFileName(String fileName, String url, String mimeType) {
        String safeName = fileName;
        if (safeName == null || safeName.trim().isEmpty()) {
            String path = Uri.parse(url).getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                safeName = cut >= 0 ? path.substring(cut + 1) : path;
            }
        }
        if (safeName == null || safeName.trim().isEmpty()) safeName = "document.pdf";
        safeName = safeName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.contains(".")) return safeName;
        return safeName + getExtensionForMimeType(mimeType);
    }

    private String getExtensionForMimeType(String mimeType) {
        if ("application/pdf".equalsIgnoreCase(mimeType)) return ".pdf";
        if ("text/plain".equalsIgnoreCase(mimeType)) return ".txt";
        if ("application/msword".equalsIgnoreCase(mimeType)) return ".doc";
        if ("application/vnd.ms-excel".equalsIgnoreCase(mimeType)) return ".xls";
        if ("application/vnd.ms-powerpoint".equalsIgnoreCase(mimeType)) return ".ppt";
        return ".bin";
    }
    
    /**
     * Open camera to take photo
     */
    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                File imageFile = createImageFile();
                if (imageFile != null) {
                    currentImageUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider",
                            imageFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                    cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(cameraIntent, Constants.REQUEST_CODE_CAMERA);
                } else {
                    Toast.makeText(this, R.string.failed_create_image_file, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ChatActivity", "Error opening camera: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Create image file for camera capture
     */
    private File createImageFile() {
        try {
            File imageDir = new File(getFilesDir(), "chat_images");
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }
            
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(imageDir, fileName);
            currentImageFile = imageFile;
            return imageFile;
        } catch (Exception e) {
            Log.e("ChatActivity", "Error creating image file: " + e.getMessage(), e);
            return null;
        }
    }
    
    
    private void showComingSoonDialog(String featureName) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_coming_soon);
        
        // Transparent background for the dialog window itself
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // Set width to 90% of screen
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        com.google.android.material.button.MaterialButton btnOk = dialog.findViewById(R.id.btnOk);

        if (tvTitle != null) tvTitle.setText(featureName + " Coming Soon!");
        
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
