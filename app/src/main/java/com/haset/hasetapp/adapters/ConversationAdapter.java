package com.haset.hasetapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Conversation;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import com.haset.hasetapp.utils.NotificationBadgeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversations;
    private Context context;
    private String currentUserId;
    private OnConversationClickListener listener;
    private OnConversationLongClickListener longClickListener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public interface OnConversationLongClickListener {
        void onConversationLongClick(Conversation conversation);
    }

    public ConversationAdapter(Context context, String currentUserId, OnConversationClickListener listener, OnConversationLongClickListener longClickListener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.conversations = new ArrayList<>();
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }
    
    public List<Conversation> getConversations() {
        return conversations != null ? new ArrayList<>(conversations) : new ArrayList<>();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation, position);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivOtherUserProfile;
        private com.facebook.shimmer.ShimmerFrameLayout shimmerOtherUserProfile;
        TextView tvOtherUserName;
        TextView tvLastMessage;
        TextView tvTimestamp;
        TextView tvUnreadCount;
        private NotificationBadgeHelper badgeHelper;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOtherUserProfile = itemView.findViewById(R.id.ivOtherUserProfile);
            shimmerOtherUserProfile = itemView.findViewById(R.id.shimmerOtherUserProfile);
            tvOtherUserName = itemView.findViewById(R.id.tvOtherUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            
            badgeHelper = new NotificationBadgeHelper(itemView.getContext());

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationClick(conversations.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    longClickListener.onConversationLongClick(conversations.get(position));
                    return true;
                }
                return false;
            });
        }

        public void bind(Conversation conversation, int position) {
            tvOtherUserName.setText(conversation.getOtherUserName());
            
            // Format last message with icon for special types
            String lastMessage = conversation.getLastMessage();
            android.graphics.drawable.Drawable icon;
            
            if (lastMessage != null) {
                String msg = lastMessage.toLowerCase();
                
                if (msg.contains("image") || msg.contains("photo") || msg.contains("pic")) {
                    icon = context.getDrawable(R.drawable.ic_gallery);
                    if (icon != null) icon.setTint(context.getResources().getColor(R.color.text_secondary, context.getTheme()));
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tvLastMessage.setText("Photo");
                } else if (msg.contains("voice") || msg.contains("audio") || msg.contains("recording")) {
                    icon = context.getDrawable(R.drawable.ic_microphone);
                    if (icon != null) icon.setTint(context.getResources().getColor(R.color.text_secondary, context.getTheme()));
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tvLastMessage.setText("Voice note");
                } else if (msg.contains("file") || msg.contains("document") || msg.contains("pdf") || msg.contains("doc")) {
                    icon = context.getDrawable(R.drawable.ic_document);
                    if (icon != null) icon.setTint(context.getResources().getColor(R.color.text_secondary, context.getTheme()));
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tvLastMessage.setText("Document");
                } else if (msg.contains("prescription") || msg.contains("prescribed")) {
                    icon = context.getDrawable(R.drawable.ic_prescription);
                    if (icon != null) icon.setTint(context.getResources().getColor(R.color.text_secondary, context.getTheme()));
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tvLastMessage.setText("Prescription");
                } else if (msg.contains("fee") || msg.contains("payment") || msg.contains("service") || msg.contains("charge")) {
                    icon = context.getDrawable(R.drawable.ic_money);
                    if (icon != null) icon.setTint(context.getResources().getColor(R.color.text_secondary, context.getTheme()));
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tvLastMessage.setText("Payment sent");
                } else {
                    tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
                    tvLastMessage.setText(lastMessage);
                }
            } else {
                tvLastMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
                tvLastMessage.setText("");
            }

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(conversation.getLastMessageTimestamp())));

            // Load profile photo for the other user with shimmer
            ProfilePhotoHelper.loadProfilePhoto(context, conversation.getOtherUserId(), ivOtherUserProfile, shimmerOtherUserProfile);
            
            // Update unread message badge - get actual count from SharedPreferences
            String conversationId = conversation.getConversationId();
            int unreadCount = badgeHelper.getConversationUnreadCount(conversationId);
            NotificationBadgeHelper.updateConversationBadge(tvUnreadCount, unreadCount);
        }
    }
}
