package com.haset.hasetapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.ChatMessage;
import com.haset.hasetapp.models.Service;
import com.haset.hasetapp.utils.ChatVoicePlayer;
import com.haset.hasetapp.utils.DateTimeUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TEXT_SENT = 0;
    private static final int TYPE_TEXT_RECEIVED = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_VIDEO = 3;
    private static final int TYPE_DOCUMENT = 4;
    private static final int TYPE_AUDIO = 5;
    private static final int TYPE_PRESCRIPTION = 6;
    private static final int TYPE_SERVICE = 7;
    private static final int TYPE_HEADER = 8;

    private final List<ChatMessage> messages;
    private final List<ChatMessage> messagesFull;
    private final String currentUserId;
    private String otherUserProfileImageUrl;
    private OnMessageLongClickListener longClickListener;
    private final Set<String> selectedMessageIds = new LinkedHashSet<>();

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, View view);
    }

    public interface OnMessageClickListener {
        void onMessageClick(ChatMessage message);
    }

    public interface OnReplyClickListener {
        void onReplyClick(String messageId);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    private OnMessageClickListener clickListener;

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.clickListener = listener;
    }

    private OnReplyClickListener replyClickListener;

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public ChatAdapter(String currentUserId) {
        this.messages = new ArrayList<>();
        this.messagesFull = new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    public void setOtherUserProfileImageUrl(String url) {
        this.otherUserProfileImageUrl = url;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> sorted = new ArrayList<>(newMessages);
        Collections.sort(sorted, com.haset.hasetapp.repositories.ChatRepository.MESSAGE_ORDER);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new MessageDiffCallback(this.messages, sorted)
        );

        this.messages.clear();
        this.messages.addAll(sorted);
        this.messagesFull.clear();
        this.messagesFull.addAll(sorted);
        Set<String> existingIds = new HashSet<>();
        for (ChatMessage message : sorted) {
            if (message.getMessageId() != null) existingIds.add(message.getMessageId());
        }
        selectedMessageIds.retainAll(existingIds);
        
        diffResult.dispatchUpdatesTo(new androidx.recyclerview.widget.ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position + 1, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position + 1, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition + 1, toPosition + 1);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                notifyItemRangeChanged(position + 1, count, payload);
            }
        });
    }

    public void filter(String query) {
        messages.clear();
        if (query.isEmpty()) {
            messages.addAll(messagesFull);
        } else {
            query = query.toLowerCase();
            for (ChatMessage item : messagesFull) {
                boolean matches = false;
                if (item.getMessage() != null && item.getMessage().toLowerCase().contains(query)) {
                    matches = true;
                } else if (item.getAttachmentFileName() != null && item.getAttachmentFileName().toLowerCase().contains(query)) {
                    matches = true;
                }
                if (matches) {
                    messages.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getPositionForMessage(String messageId) {
        if (messageId == null) return -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getMessageId())) {
                return i + 1; // +1 for Header
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;

        // Adjust position because of header
        // If messages list is empty, we only show header (count 1), but accessing index -1 would crash if we went here.
        // But getItemCount handles size.
        if (position - 1 < messages.size()) {
            ChatMessage message = messages.get(position - 1);
            String type = message.getMessageType();

            if ("image".equalsIgnoreCase(type)) return TYPE_IMAGE;
            if ("video".equalsIgnoreCase(type)) return TYPE_VIDEO;
            if ("document".equalsIgnoreCase(type)) return TYPE_DOCUMENT;
            if ("audio".equalsIgnoreCase(type)) return TYPE_AUDIO;
            if ("prescription".equalsIgnoreCase(type)) return TYPE_PRESCRIPTION;
            if ("service".equalsIgnoreCase(type)) return TYPE_SERVICE;
            
            return message.getSenderId().equals(currentUserId) ? TYPE_TEXT_SENT : TYPE_TEXT_RECEIVED;
        }
        return TYPE_TEXT_RECEIVED; // Fallback
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_chat_e2ee_header, parent, false));
            case TYPE_TEXT_SENT:
                return new TextViewHolder(inflater.inflate(R.layout.item_chat_message_sent, parent, false));
            case TYPE_TEXT_RECEIVED:
                return new TextViewHolder(inflater.inflate(R.layout.item_chat_message_received, parent, false));
            case TYPE_IMAGE:
                return new ImageViewHolder(inflater.inflate(R.layout.item_chat_image, parent, false));
            case TYPE_VIDEO:
                return new VideoViewHolder(inflater.inflate(R.layout.item_chat_video, parent, false));
            case TYPE_DOCUMENT:
                return new DocumentViewHolder(inflater.inflate(R.layout.item_chat_document, parent, false));
            case TYPE_AUDIO:
                return new AudioViewHolder(inflater.inflate(R.layout.item_chat_audio, parent, false));
            case TYPE_PRESCRIPTION:
                return new PrescriptionViewHolder(inflater.inflate(R.layout.item_chat_prescription, parent, false));
            case TYPE_SERVICE:
                return new ServiceViewHolder(inflater.inflate(R.layout.item_service_message, parent, false));
            default:
                return new TextViewHolder(inflater.inflate(R.layout.item_chat_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            return; // Nothing to bind for header
        }

        // Adjust for header
        int msgPosition = position - 1;
        if (msgPosition < 0 || msgPosition >= messages.size()) return;

        ChatMessage message = messages.get(msgPosition);
        ChatMessage previousMessage = msgPosition > 0 ? messages.get(msgPosition - 1) : null;
        boolean isSent = message.getSenderId().equals(currentUserId);

        if (holder instanceof BaseViewHolder) {
            ((BaseViewHolder) holder).bind(message, previousMessage, isSent);
        }

        String messageId = message.getMessageId();
        holder.itemView.setBackgroundColor(messageId != null && selectedMessageIds.contains(messageId)
                ? 0x2234A853 : android.graphics.Color.TRANSPARENT);
    }

    @Override
    public int getItemCount() {
        return messages.size() + 1; // +1 for Header
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ServiceViewHolder) {
            ((ServiceViewHolder) holder).stopStatusListener();
        }
        super.onViewRecycled(holder);
    }

    public void toggleSelection(ChatMessage message) {
        if (message == null || message.getMessageId() == null) return;
        String messageId = message.getMessageId();
        if (!selectedMessageIds.add(messageId)) selectedMessageIds.remove(messageId);
        int position = getPositionForMessage(messageId);
        if (position >= 0) notifyItemChanged(position);
    }

    public boolean isSelectionMode() {
        return !selectedMessageIds.isEmpty();
    }

    public int getSelectedCount() {
        return selectedMessageIds.size();
    }

    public List<ChatMessage> getSelectedMessages() {
        List<ChatMessage> selected = new ArrayList<>();
        for (ChatMessage message : messagesFull) {
            if (message.getMessageId() != null && selectedMessageIds.contains(message.getMessageId())) {
                selected.add(message);
            }
        }
        return selected;
    }

    public void clearSelection() {
        if (selectedMessageIds.isEmpty()) return;
        selectedMessageIds.clear();
        notifyDataSetChanged();
    }

    public interface OnPrescriptionClickListener {
        void onPrescriptionClick(String prescriptionId);
    }

    private OnPrescriptionClickListener prescriptionClickListener;

    public void setOnPrescriptionClickListener(OnPrescriptionClickListener listener) {
        this.prescriptionClickListener = listener;
    }

    // Voice player management
    private HashMap<String, ChatVoicePlayer> voicePlayers;

    // --- View Holders ---

    // Header ViewHolder
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(v -> {
                if (e2eeHeaderClickListener != null) {
                    e2eeHeaderClickListener.onE2eeHeaderClick();
                }
            });
        }
    }

    abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateSeparator, tvTime;
        LinearLayout messageWrapper, footerContainer;
        View messageContainer;
        ImageView ivMessageStatus, ivSenderAvatar;
        android.widget.ProgressBar pbUpload;

        // Reply views
        View replyPreview;
        TextView tvReplyToName, tvReplyToText;
        View vReplyLine;

        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateSeparator = itemView.findViewById(R.id.tvDateSeparator);
            tvTime = itemView.findViewById(R.id.tvTime);
            messageWrapper = itemView.findViewById(R.id.messageWrapper);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            footerContainer = itemView.findViewById(R.id.footerContainer);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
            ivSenderAvatar = itemView.findViewById(R.id.ivSenderAvatar);
            pbUpload = itemView.findViewById(R.id.pbUpload);

            // Initialize reply views
            replyPreview = itemView.findViewById(R.id.replyPreview);
            tvReplyToName = itemView.findViewById(R.id.tvReplyToName);
            tvReplyToText = itemView.findViewById(R.id.tvReplyToText);
            vReplyLine = itemView.findViewById(R.id.vReplyLine);
        }

        void bind(ChatMessage message, ChatMessage previousMessage, boolean isSent) {
            // Set click listener
            if (messageContainer != null) {
                messageContainer.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onMessageClick(message);
                    }
                });

                // Set long click listener - access outer class listener
                messageContainer.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(message, v);
                        return true;
                    }
                    return false;
                });
            }

            // Date Separator
            if (tvDateSeparator != null) {
                if (previousMessage == null || !DateTimeUtils.isSameDay(message.getTimestamp(), previousMessage.getTimestamp())) {
                    tvDateSeparator.setVisibility(View.VISIBLE);
                    tvDateSeparator.setText(DateTimeUtils.formatDateForSeparator(message.getTimestamp()));
                } else {
                    tvDateSeparator.setVisibility(View.GONE);
                }
            }

            // Time
            if (tvTime != null) {
                tvTime.setText(DateTimeUtils.formatTime(message.getTimestamp()));
            }

            // Sent vs Received
            if (messageWrapper != null && footerContainer != null) {
                LinearLayout.LayoutParams wrapperParams = (LinearLayout.LayoutParams) messageWrapper.getLayoutParams();
                LinearLayout.LayoutParams footerParams = (LinearLayout.LayoutParams) footerContainer.getLayoutParams();

                if (isSent) {
                    wrapperParams.gravity = android.view.Gravity.END;
                    footerParams.gravity = android.view.Gravity.END;
                    
                    if (ivMessageStatus != null) {
                        ivMessageStatus.setVisibility(View.VISIBLE);
                        updateStatusIcon(ivMessageStatus, message.getMessageStatus());
                    }
                } else {
                    wrapperParams.gravity = android.view.Gravity.START;
                    footerParams.gravity = android.view.Gravity.START;
                    if (ivMessageStatus != null) {
                        ivMessageStatus.setVisibility(View.GONE);
                    }
                    
                    // Show avatar for received messages
                    if (ivSenderAvatar != null) {
                        ivSenderAvatar.setVisibility(View.VISIBLE);
                        Glide.with(itemView.getContext())
                                .load(otherUserProfileImageUrl)
                                .placeholder(R.drawable.profile_photo)
                                .error(R.drawable.profile_photo)
                                .into(ivSenderAvatar);
                    }
                }
                messageWrapper.setLayoutParams(wrapperParams);
                footerContainer.setLayoutParams(footerParams);
            }

            // Handle progress bar common logic
            if (pbUpload != null) {
                String status = message.getMessageStatus();
                if ("sending".equalsIgnoreCase(status) || "uploading".equalsIgnoreCase(status) || "starting".equalsIgnoreCase(status)) {
                    pbUpload.setVisibility(View.VISIBLE);
                } else {
                    pbUpload.setVisibility(View.GONE);
                }
            }

            // Handle reply preview visibility and content
            if (replyPreview != null) {
                if (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isEmpty()) {
                    replyPreview.setVisibility(View.VISIBLE);

                    // Set reply sender name
                    String replySender = message.getReplyToSenderName();
                    if (tvReplyToName != null) {
                        tvReplyToName.setText(replySender != null ? replySender : "Unknown");
                    }

                    // Set reply text
                    if (tvReplyToText != null) {
                        tvReplyToText.setText(message.getReplyToText());
                    }

                    // Adapt colors to bubble style
                    if (isSent) {
                        if (tvReplyToName != null) tvReplyToName.setTextColor(0xFFFFFFFF);
                        if (tvReplyToText != null) tvReplyToText.setTextColor(0xD0FFFFFF);
                        if (vReplyLine != null) vReplyLine.setBackgroundColor(0xFFFFFFFF);
                        replyPreview.setBackgroundResource(R.drawable.bg_reply_message_sent);
                    } else {
                        if (tvReplyToName != null) tvReplyToName.setTextColor(itemView.getContext().getColor(R.color.green_primary));
                        if (tvReplyToText != null) tvReplyToText.setTextColor(0xFF666666);
                        if (vReplyLine != null) vReplyLine.setBackgroundColor(itemView.getContext().getColor(R.color.green_primary));
                        replyPreview.setBackgroundResource(R.drawable.bg_reply_message);
                    }

                    // Click to scroll to original message
                    replyPreview.setOnClickListener(v -> {
                        if (replyClickListener != null) {
                            replyClickListener.onReplyClick(message.getReplyToMessageId());
                        }
                    });
                } else {
                    replyPreview.setVisibility(View.GONE);
                }
            }

            bindSpecialized(message, isSent);
        }

        abstract void bindSpecialized(ChatMessage message, boolean isSent);

        protected void updateStatusIcon(ImageView statusIcon, String status) {
            if (status == null) status = "sent";
            switch (status.toLowerCase()) {
                case "sending":
                case "uploading":
                case "starting":
                    statusIcon.setImageResource(R.drawable.ic_clock_regular);
                    break;
                case "sent":
                    statusIcon.setImageResource(R.drawable.ic_check_single);
                    break;
                case "delivered":
                    statusIcon.setImageResource(R.drawable.ic_check_double);
                    break;
                case "read":
                    statusIcon.setImageResource(R.drawable.ic_check_double);
                    statusIcon.setColorFilter(itemView.getContext().getColor(R.color.green_primary));
                    break;
            }
        }
    }

    class TextViewHolder extends BaseViewHolder {
        TextView tvMessage;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            tvMessage.setText(message.getMessage());
            tvMessage.setTextColor(itemView.getContext().getColor(isSent ? android.R.color.white : R.color.text_primary));
        }
    }

    class ImageViewHolder extends BaseViewHolder {
        ImageView ivChatImage;
        TextView tvMessage;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChatImage = itemView.findViewById(R.id.ivChatImage);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            if (tvMessage != null) {
                if (message.getMessage() != null && !message.getMessage().isEmpty() && !message.getMessage().equalsIgnoreCase("Image")) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getMessage());
                    tvMessage.setTextColor(itemView.getContext().getColor(isSent ? android.R.color.white : R.color.text_primary));
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            }

            Glide.with(itemView.getContext())
                    .load(message.getAttachmentUrl())
                    .placeholder(R.drawable.ic_gallery)
                    .error(R.drawable.ic_error_outline)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivChatImage);

            if (pbUpload != null && pbUpload.getVisibility() == View.VISIBLE) {
                ivChatImage.setAlpha(0.5f);
            } else {
                ivChatImage.setAlpha(1.0f);
            }
        }
    }

    class VideoViewHolder extends BaseViewHolder {
        ImageView ivVideoThumbnail;
        ImageView ivPlayVideo;
        TextView tvDuration;
        TextView tvVideoName;
        TextView tvVideoSize;
        ImageView btnDownloadVideo;
        TextView tvMessage;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
            ivPlayVideo = itemView.findViewById(R.id.ivPlayVideo); // ic_play_circle_filled
            tvDuration = itemView.findViewById(R.id.tvVideoDuration); // Fixed ID
            tvVideoName = itemView.findViewById(R.id.tvVideoName); 
            tvVideoSize = itemView.findViewById(R.id.tvVideoSize);
            
            btnDownloadVideo = itemView.findViewById(R.id.ivDownload); // Correct ID from XML
            tvMessage = itemView.findViewById(R.id.tvMessage); 
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
           if (tvVideoName != null) tvVideoName.setText(message.getAttachmentFileName());
           if (tvVideoSize != null) tvVideoSize.setText(message.getAttachmentSize());

            if (tvMessage != null) {
                if (message.getMessage() != null && !message.getMessage().isEmpty() && !message.getMessage().equalsIgnoreCase("Video")) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getMessage());
                    tvMessage.setTextColor(itemView.getContext().getColor(isSent ? android.R.color.white : R.color.text_primary));
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            }

            Glide.with(itemView.getContext())
                    .load(message.getAttachmentUrl())
                    .placeholder(R.drawable.ic_video_outlined)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivVideoThumbnail);

            if (pbUpload != null && pbUpload.getVisibility() == View.VISIBLE) {
                ivVideoThumbnail.setAlpha(0.5f);
            } else {
                ivVideoThumbnail.setAlpha(1.0f);
            }
        }
    }

    class DocumentViewHolder extends BaseViewHolder {
        TextView tvFileName, tvFileSize;
        ImageView ivDocumentIcon;
        TextView tvMessage;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvDocumentName); // distinct ID
            tvFileSize = itemView.findViewById(R.id.tvDocumentSize);
            // btnDownload removed as it's not in XML
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            tvFileName.setText(message.getAttachmentFileName());
            tvFileSize.setText(message.getAttachmentSize());
            
            if (tvMessage != null) {
                if (message.getMessage() != null && !message.getMessage().isEmpty() && !message.getMessage().equalsIgnoreCase("Document")) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getMessage());
                    tvMessage.setTextColor(itemView.getContext().getColor(isSent ? android.R.color.white : R.color.text_primary));
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            }
            if (pbUpload != null && pbUpload.getVisibility() == View.VISIBLE) {
                ivDocumentIcon.setVisibility(View.GONE);
            } else {
                ivDocumentIcon.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Get or create voice player for message
     */
    private ChatVoicePlayer getOrCreateVoicePlayer(ChatMessage message, Context context) {
        if (voicePlayers == null) {
            voicePlayers = new HashMap<>();
        }
        
        String messageId = message.getMessageId();
        ChatVoicePlayer voicePlayer = voicePlayers.get(messageId);
        
        if (voicePlayer == null) {
            voicePlayer = new ChatVoicePlayer(context);
            voicePlayers.put(messageId, voicePlayer);
        }
        
        return voicePlayer;
    }
    
    class AudioViewHolder extends BaseViewHolder {
        ImageView ivPlayPause;
        com.haset.hasetapp.views.VoiceWaveView voiceWaveView;
        View layoutStaticWave;
        TextView tvAudioDuration;

        AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlayPause = itemView.findViewById(R.id.ivPlayPause); // Correct ID
            voiceWaveView = itemView.findViewById(R.id.voiceWaveView); // Correct ID
            layoutStaticWave = itemView.findViewById(R.id.layoutStaticWave); // Added missing field
            tvAudioDuration = itemView.findViewById(R.id.tvAudioDuration);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            // Display duration if available, otherwise show size
            String duration = message.getAttachmentDuration();
            if (duration != null && !duration.isEmpty()) {
                tvAudioDuration.setText(duration);
            } else if (message.getAttachmentSize() != null && !message.getAttachmentSize().equals("N/A")) {
                tvAudioDuration.setText(message.getAttachmentSize());
            } else {
                tvAudioDuration.setText(itemView.getContext().getString(R.string.voice_note_label));
            }
            if (pbUpload != null && pbUpload.getVisibility() == View.VISIBLE) {
                ivPlayPause.setVisibility(View.GONE);
            } else {
                ivPlayPause.setVisibility(View.VISIBLE);
            }
            
            // Set click listener for play/pause button to trigger inline playback
            ivPlayPause.setOnClickListener(v -> {
                // Get voice player for this message
                ChatVoicePlayer voicePlayer = getOrCreateVoicePlayer(message, itemView.getContext());
                
                // Bind UI for wave visualization and play/pause icon updates
                if (voiceWaveView != null && layoutStaticWave != null) {
                    voicePlayer.bindUI(ivPlayPause, tvAudioDuration, pbUpload, voiceWaveView, (android.widget.LinearLayout) layoutStaticWave);
                }
                
                // Get audio path from message and toggle playback
                String audioPath = message.getAttachmentUrl();
                voicePlayer.togglePlayback(audioPath);
            });
        }
    }

    class PrescriptionViewHolder extends BaseViewHolder {
        com.google.android.material.button.MaterialButton btnView;
        TextView tvInfo;

        PrescriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            btnView = itemView.findViewById(R.id.btnViewPrescription);
            tvInfo = itemView.findViewById(R.id.tvPrescriptionInfo);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            btnView.setOnClickListener(v -> {
                if (prescriptionClickListener != null) {
                    prescriptionClickListener.onPrescriptionClick(message.getPrescriptionId());
                }
            });
            // If it's a doctor viewed by patient, or vice versa, the card is always same
            // but we can customize text if needed
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                tvInfo.setText(message.getMessage());
            }
        }
    }

    // Service Payment ViewHolder
    class ServiceViewHolder extends BaseViewHolder {
        TextView tvServiceTitle, tvServiceName, tvTotalFee, tvPatientPay, tvPercentageLabel, tvPaymentStatus;
        com.google.android.material.button.MaterialButton btnPay;
        com.google.firebase.database.DatabaseReference statusReference;
        com.google.firebase.database.ValueEventListener statusListener;
        String boundServiceId;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceTitle = itemView.findViewById(R.id.tvServiceTitle);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvTotalFee = itemView.findViewById(R.id.tvTotalFee);
            tvPatientPay = itemView.findViewById(R.id.tvPatientPay);
            tvPercentageLabel = itemView.findViewById(R.id.tvPercentageLabel);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            btnPay = itemView.findViewById(R.id.btnPay);
        }

        @Override
        void bindSpecialized(ChatMessage message, boolean isSent) {
            try {
                // Parse service JSON from message
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.haset.hasetapp.models.Service service = gson.fromJson(message.getMessage(), com.haset.hasetapp.models.Service.class);
                
                if (service != null) {
                    stopStatusListener();
                    boundServiceId = service.getServiceId();
                    tvServiceName.setText(service.getServiceName() != null ? service.getServiceName() : "Service");
                    
                    // Format currency
                    java.text.NumberFormat formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
                    tvTotalFee.setText(formatter.format(service.getAppointmentFee()) + " TZS");
                    tvPatientPay.setText(formatter.format(service.getPatientPayAmount()) + " TZS");
                    tvPercentageLabel.setText("You Pay (" + service.getPatientPercentage() + "%):");
                    
                    renderPaymentState(message, service, isSent,
                            service.isPaid() || "paid".equalsIgnoreCase(service.getPaymentStatus()));

                    if (boundServiceId != null && !boundServiceId.trim().isEmpty()) {
                        statusReference = com.google.firebase.database.FirebaseDatabase.getInstance()
                                .getReference("service_payment_requests")
                                .child(boundServiceId);
                        statusListener = new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (!boundServiceId.equals(service.getServiceId())) return;
                                if (!snapshot.exists()) return;
                                String status = snapshot.child("status").getValue(String.class);
                                renderPaymentState(message, service, isSent, "paid".equalsIgnoreCase(status));
                            }

                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                        };
                        statusReference.addValueEventListener(statusListener);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void renderPaymentState(ChatMessage message, Service service, boolean isSent, boolean paid) {
                    if (paid) {
                        tvPaymentStatus.setText(itemView.getContext().getString(R.string.payment_paid));
                        tvPaymentStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.green_primary, null));
                        tvPaymentStatus.setBackgroundResource(R.drawable.bg_badge_green);
                        btnPay.setVisibility(View.GONE);
                    } else {
                        tvPaymentStatus.setText(itemView.getContext().getString(R.string.payment_pending));
                        tvPaymentStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.orange_primary, null));
                        tvPaymentStatus.setBackgroundResource(R.drawable.bg_badge_orange);
                        
                        // Show pay button only for patient (not for doctor who sent it)
                        if (!isSent) {
                            btnPay.setVisibility(View.VISIBLE);
                            btnPay.setOnClickListener(v -> {
                                if (serviceClickListener != null) {
                                    serviceClickListener.onServicePayClick(message.getMessageId(), service);
                                }
                            });
                        } else {
                            btnPay.setVisibility(View.GONE);
                        }
                    }
        }

        void stopStatusListener() {
            if (statusReference != null && statusListener != null) {
                statusReference.removeEventListener(statusListener);
            }
            statusReference = null;
            statusListener = null;
            boundServiceId = null;
        }
    }

    public interface OnServicePayClickListener {
        void onServicePayClick(String messageId, Service service);
    }

    public interface OnE2eeHeaderClickListener {
        void onE2eeHeaderClick();
    }

    private OnE2eeHeaderClickListener e2eeHeaderClickListener;

    public void setOnE2eeHeaderClickListener(OnE2eeHeaderClickListener listener) {
        this.e2eeHeaderClickListener = listener;
    }

    private OnServicePayClickListener serviceClickListener;

    public void setOnServicePayClickListener(OnServicePayClickListener listener) {
        this.serviceClickListener = listener;
    }

    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        MessageDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            String oldId = oldList.get(oldPos).getMessageId();
            String newId = newList.get(newPos).getMessageId();
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            ChatMessage oldMsg = oldList.get(oldPos);
            ChatMessage newMsg = newList.get(newPos);
            return oldMsg.getTimestamp() == newMsg.getTimestamp() &&
                    (oldMsg.getMessageStatus() != null &&
                            oldMsg.getMessageStatus().equals(newMsg.getMessageStatus()));
        }
    }
}
