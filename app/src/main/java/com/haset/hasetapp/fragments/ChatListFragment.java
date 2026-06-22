package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.ChatActivity;
import com.haset.hasetapp.adapters.ConversationAdapter;
import com.haset.hasetapp.models.Conversation;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ShimmerHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.ChatListViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListFragment extends Fragment implements ConversationAdapter.OnConversationClickListener, ConversationAdapter.OnConversationLongClickListener {
    private TabLayout tabs;
    private RecyclerView rvConversations;
    private ConversationAdapter conversationAdapter;
    private PreferenceManager preferenceManager;
    private String currentUserId;
    private boolean isFirstLoad = true;
    private ChatListViewModel viewModel;
    private View layoutEmpty;
    private android.widget.TextView tvEmptyMessage;
    private LinearLayout shimmerContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        
        preferenceManager = new PreferenceManager(requireContext());
        currentUserId = preferenceManager.getUserId();

        setupRecyclerView();
        setupTabs();
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ChatListViewModel.class);
        
        setupObservers();
    }

    private void initViews(View view) {
        tabs = view.findViewById(R.id.tabs);
        rvConversations = view.findViewById(R.id.rvConversations);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(requireContext(), currentUserId, this, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.setAdapter(conversationAdapter);
    }

    private void setupTabs() {
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateCurrentList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateCurrentList() {
        if (viewModel != null && currentUserId != null) {
            List<Conversation> all = viewModel.getConversations(currentUserId).getValue();
            filterAndDisplay(all);
        }
    }

    private void filterAndDisplay(List<Conversation> conversations) {
        if (conversations == null) return;
        List<Conversation> filtered = new ArrayList<>();
        boolean showArchived = (tabs.getSelectedTabPosition() == 1);
        
        for (Conversation c : conversations) {
            if (c.isArchived() == showArchived) {
                filtered.add(c);
            }
        }
        
        hideShimmerLoading();
        
        if (conversationAdapter != null) {
            conversationAdapter.setConversations(filtered);
        }

        updateEmptyState(filtered.isEmpty(), showArchived);
    }

    private void updateEmptyState(boolean isEmpty, boolean isArchived) {
        if (layoutEmpty == null) return;

        if (isEmpty) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvConversations.setVisibility(View.GONE);
            if (tvEmptyMessage != null) {
                tvEmptyMessage.setText(isArchived ? "No archived messages" : "No messages yet");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
        }
    }

    private void setupObservers() {
        if (currentUserId == null) return;
        
        showShimmerLoading();
        
        viewModel.getConversations(currentUserId).observe(getViewLifecycleOwner(), conversations -> {
            hideShimmerLoading();
            filterAndDisplay(conversations);

            if (isFirstLoad) {
                isFirstLoad = false;
                viewModel.syncUnreadCounts(currentUserId, conversations);
            }
        });
    }

    private void showShimmerLoading() {
        if (shimmerContainer == null) return;
        shimmerContainer.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 5, R.layout.shimmer_layout_chat_item);
    }

    private void hideShimmerLoading() {
        if (shimmerContainer == null) return;
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
    }



    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(Constants.EXTRA_CHAT_USER_ID, conversation.getOtherUserId());
        intent.putExtra(Constants.EXTRA_CHAT_USER_NAME, conversation.getOtherUserName());
        startActivity(intent);
    }

    @Override
    public void onConversationLongClick(Conversation conversation) {
        String[] options = {
            conversation.isArchived() ? "Unarchive Chat" : "Archive Chat",
            "Delete Chat"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(conversation.getOtherUserName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Archive/Unarchive
                        viewModel.toggleArchive(currentUserId, conversation.getOtherUserId(), !conversation.isArchived());
                        String msg = conversation.isArchived() ? "Chat unarchived" : "Chat archived";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        // Delete
                        showDeleteConfirmation(conversation);
                    }
                })
                .show();
    }

    private void showDeleteConfirmation(Conversation conversation) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_chat)
                .setMessage("Are you sure you want to delete this conversation with " + conversation.getOtherUserName() + "? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteConversation(currentUserId, conversation.getOtherUserId());
                    Toast.makeText(requireContext(), R.string.chat_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clear adapter
        if (rvConversations != null) {
            rvConversations.setAdapter(null);
        }
        conversationAdapter = null;
        
        // Null out view references
        rvConversations = null;
        tabs = null;
        layoutEmpty = null;
        tvEmptyMessage = null;
        shimmerContainer = null;
    }
}
